package com.concurnas.runtime;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import com.concurnas.runtime.cps.analysis.ANFTransform;
import com.concurnas.runtime.cps.analysis.ConcClassWriter;
import com.concurnas.runtime.cps.mirrors.ClassMirror;
import com.concurnas.runtime.cps.mirrors.ClassMirrorNotFoundException;
import com.concurnas.runtime.cps.mirrors.FieldMirror;

/**
 * Convert module level static fields and functions in class XYZ to XYZ$$Gloabls
 * - with singleton initalizer for accessing the Globals class contaiing the
 * items also reroute all access to static fields and methods to the respective
 * global instances
 * 
 * except for enums which require the values method to be decared for reflection purposes
 * 
 * we don't map to global versions on if they originate from primordials - unless they are in the staticLambdaClasses set
 * 
 * we don't map trait methods either
 * 
 */
public class Globalizer implements Opcodes {
	private final byte[] inputClassBytes;
	private final MaxLocalFinderCache mfl;
	private HashSet<String> staticLambdaClasses;

	private static boolean isTraitMethod(String name) {
		return name.endsWith("$traitM");
	}
	
	private static boolean isNonGlobalerORPrimordial(String owner, String desc, String name, HashSet<String> staticLambdaClasses, ConcClassUtil clloader){
		
		if(name.equals("metaBinary") && "()[Ljava/lang/String;".equals(desc)){
			return true;
		}
		
		if(NoGlobalizationException(owner)){
			return true;
		}
		
		if(null != staticLambdaClasses) {
			return !staticLambdaClasses.contains(owner);//primordial only if not in this list
		}
		
		try {//primordials have no global code
			HashSet<String> slcs = clloader.getStaticLambdaClasses();
			if(null != slcs && slcs.contains(owner)) {
				return false;//use the globalized version
			}
			
			Class<?> gt = clloader.loadClassFromPrimordial(owner.replace("/", "."));//no .replace("/", ".")
			return gt != null;
		} catch (ClassNotFoundException e) {
		}
		return false;
	}
	
	public static boolean NoGlobalizationException(String x){
		x = x.replace(".", "/");
		if(x.startsWith("com/concurnas/bootstrap/runtime/cps/")){
			return true;
		}
		else if(x.startsWith("com/concurnas/bootstrap/lang/Stringifier")){//TODO: maybe there is a more elegant way to express this
			return true;
		}
		else if(x.startsWith("com/concurnas/lang/Hasher")){//TODO: maybe there is a more elegant way to express this
			return true;
		}
		else if(x.startsWith("com/concurnas/repl/REPLRuntimeState")) {
			return true;
		}
		/*else if(x.equals("java/lang/System")){//TODO: maybe there is a more elegant way to express this
			return true;
		}*/
		return false;
	}
	
	public Globalizer(byte[] inputClassBytes, MaxLocalFinderCache mfl, HashSet<String> staticLambdaClasses) {
		this.inputClassBytes = inputClassBytes;
		this.mfl = mfl;
		this.staticLambdaClasses = staticLambdaClasses;
	}

	private static class StaticAccessMethodRepoint extends MethodVisitor {
		private final String fromName;
		private final String classname;
		private int maxLocalSize;
		private final ConcClassUtil clloader;
		private final HashSet<OwnerNameDesc> methodsWhichWereForcedPublic;
		private HashSet<String> staticLambdaClasses;
		private HashSet<String> locallyDefinedFields;
		private Set<String> locallyDefinedMethods;
		private String superClass;
		private String[] ifaces;
		/**
		 * @param fromName - set this only on second run - i.e. when gennerating code in globalizer, so as to avoid 
		 * claling getInstance on own stuff
		 */
		protected StaticAccessMethodRepoint(HashSet<String> staticLambdaClasses, MethodVisitor mv, int maxLocalSize, String fromName, ConcClassUtil clloader, HashSet<OwnerNameDesc> methodsWhichWereForcedPublic, HashSet<String> locallyDefinedFields, Set<String> locallyDefinedMethods, String superClass, String[] ifaces, String classname) {
			super(ASM7, mv);
			this.fromName=fromName;
			this.classname=classname;
			this.maxLocalSize=maxLocalSize;
			if(this.fromName!= null){//if fromName them indicates that we're making this a non static method when it used to be static, thus we must shift the count of maxvars by 1
				this.maxLocalSize++;
			}
			this.clloader=clloader;
			this.methodsWhichWereForcedPublic=methodsWhichWereForcedPublic;
			this.staticLambdaClasses = staticLambdaClasses;
			this.locallyDefinedFields = locallyDefinedFields;
			this.locallyDefinedMethods = locallyDefinedMethods;
			
			this.superClass = superClass;
			this.ifaces = ifaces;
		}
		
		public StaticAccessMethodRepoint(HashSet<String> staticLambdaClasses, MethodVisitor mv, int maxLocalSize, ConcClassUtil clloader, HashSet<String> locallyDefinedFields, Set<String> locallyDefinedMethods, String superClass, String[] ifaces, String classname) {
			this(staticLambdaClasses, mv, maxLocalSize, null, clloader, null, locallyDefinedFields, locallyDefinedMethods, superClass, ifaces, classname);
		}
		
		private void genericSswap(String desc) {
			//caters for double, int on stack - to swap
			if(desc.equals("J") || desc.equals("D")){//long and double take up two slots insteead of 1
				super.visitInsn(Opcodes.DUP_X2);
				super.visitInsn(Opcodes.POP);
			}
			else{//1 slot each
				super.visitInsn(Opcodes.SWAP);
			}
		}
		
		
		private String findLocationOfFieldOrMethod(boolean isField, String name, String desc, String supCls, String[] ifaces) {
			return findLocationOfFieldOrMethod(isField, name, desc, supCls, ifaces, new HashSet<String>());
		}
		
		private String findLocationOfFieldOrMethod(boolean isField, String name, String desc, String supCls, String[] ifaces, HashSet<String> searchAlready) {
			String curSup = supCls;
			while(null != curSup && !curSup.equals("java/lang/Object") && !searchAlready.contains(curSup)) {
				try {					
					ClassMirror sup = this.clloader.getDetector().classForName(curSup);
					
					if(isField) {
						FieldMirror fm = sup.getField(name);
						if(null != fm && fm.desc.equals(desc)) {
							return curSup;
						}
					}else {
						if(null != sup.getDeclaredMethod(name, desc)) {
							return curSup;
						}
					}
					
					searchAlready.add(curSup);
					for(String iface : sup.getInterfacesNoEx()) {
						String ret = findLocationOfFieldOrMethod(isField, name, desc, iface, null, searchAlready);
						if(null != ret) {
							return ret;
						}
					}
					curSup = sup.getSuperclass();
				} catch (ClassMirrorNotFoundException e) {
					curSup = null;
				}
			}
			
			if (ifaces != null) {
				for (int n = 0; n < ifaces.length; n++) {
					String iface = ifaces[n];
					String ret = findLocationOfFieldOrMethod(isField, name, desc, iface, null, searchAlready);
					if(ret != null) {
						return ret;
					}
					
				}
			}

			return null;
		}
		
		public void visitFieldInsn(int opcode, String owner, String name, String desc) {
			//repoint static field access
			if ((opcode == GETSTATIC || opcode == PUTSTATIC) && !isNonGlobalerORPrimordial(owner, desc, name, staticLambdaClasses, clloader)) {
				
				boolean isShared = false;
				try {//shared fields are not converted into the Global class, but are kept static
					isShared = clloader.getDetector().classForName(owner).getField(name).isShared;
				} catch (Throwable e) {//is this possible?
					//throw new RuntimeException("Cannot find class: "+ owner + " as " + e.getMessage(), e);
				}
				
				String globOwner = owner + "$Globals$";
				int opcodea = opcode == GETSTATIC ? GETFIELD : PUTFIELD;
				if(this.fromName !=null && this.fromName.equals(owner)){//ref self in constructor
					if(null == this.locallyDefinedFields || this.locallyDefinedFields.contains(name + desc)) {//check self actually has feild in question
						super.visitVarInsn(ALOAD, 0);
					}else {//, else map to that which does...
						String newglobOwner = findLocationOfFieldOrMethod(true, name, desc, this.superClass, this.ifaces);
						
						if(newglobOwner == null) {
							super.visitVarInsn(ALOAD, 0);
						}else {
							if(null == this.staticLambdaClasses || this.staticLambdaClasses.contains(newglobOwner)){
								globOwner = newglobOwner + "$Globals$";
								super.visitMethodInsn(INVOKESTATIC, globOwner, "getInstance?", "()L" + globOwner + ";", false);
							}else {
								super.visitVarInsn(ALOAD, 0);
							}
						}
					}
				}else if(this.classname.equals(owner) && !this.locallyDefinedFields.contains(name + desc)) {
					//check self actually has feild in question
					String newglobOwner = findLocationOfFieldOrMethod(true, name, desc, this.superClass, this.ifaces);
					
					if(newglobOwner == null) {
						super.visitMethodInsn(INVOKESTATIC, globOwner, "getInstance?", "()L" + globOwner + ";", false);
					}else {
						if(null == this.staticLambdaClasses || this.staticLambdaClasses.contains(newglobOwner)){
							globOwner = newglobOwner + "$Globals$";
						}
						super.visitMethodInsn(INVOKESTATIC, globOwner, "getInstance?", "()L" + globOwner + ";", false);
					}
				}else{
					super.visitMethodInsn(INVOKESTATIC, globOwner, "getInstance?", "()L" + globOwner + ";", false);
				}
					
				if(!isShared) {//shared, just call init, but dont extrat from globalizer (this calls clinit equvilents)
					if(PUTFIELD == opcodea){//only for put operation
						genericSswap(desc);
					}
					super.visitFieldInsn(opcodea, globOwner, name, desc);
					return;
				}else {
					mv.visitInsn(POP);	
				}
			} 
			
			super.visitFieldInsn(opcode, owner, name, desc);
		}
		
		public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
			if (opcode == Opcodes.INVOKESTATIC && !isNonGlobalerORPrimordial(owner, null, name, staticLambdaClasses, clloader) ) {
				
				if(name.equals("getStandardStrength")) {
					int h=9;
				}
				
				if(isTraitMethod(name)) {
					super.visitMethodInsn(opcode, owner, name, desc, itf);
					return;
				}
				
				String globOwner = owner + "$Globals$";		
				
				List<Character> charz = ANFTransform.getPrimOrObj(desc);
				int cnt = charz.size(); //cnt==0 do nothing!
				
				Stack<Integer> tmpVars = new Stack<Integer>();
				
				if(cnt >= 2){//store as vars
					for(int n=charz.size()-1; n >=0; n--){//store in vars
						char x = charz.get(n);
						tmpVars.add(maxLocalSize);
						super.visitVarInsn(ANFTransform.getStoreOp(x), maxLocalSize++);
						if(x == 'D' || x=='J'){
							maxLocalSize++;
						}
            		}
				}
				
				if(this.fromName !=null && this.fromName.equals(owner)){//oh, references itself, should use this reference in that case in constructor
					if(null == this.locallyDefinedMethods || this.locallyDefinedMethods.contains(name + desc)) {//check self actually has feild in question
						super.visitVarInsn(ALOAD, 0);
					}else {//, else map to that which does...
						String newglobOwner = findLocationOfFieldOrMethod(false, name, desc, this.superClass, this.ifaces);
						
						if(newglobOwner == null) {
							super.visitVarInsn(ALOAD, 0);
						}else {
							if(null == this.staticLambdaClasses || this.staticLambdaClasses.contains(newglobOwner)){
								globOwner = newglobOwner + "$Globals$";
								super.visitMethodInsn(INVOKESTATIC, globOwner, "getInstance?", "()L" + globOwner + ";", false);
							}else {
								super.visitVarInsn(ALOAD, 0);
							}
						}
					}
				}else if(this.classname.equals(owner) && !this.locallyDefinedMethods.contains(name + desc)) {
					//check self actually has feild in question
					String newglobOwner = findLocationOfFieldOrMethod(false, name, desc, this.superClass, this.ifaces);
					
					if(newglobOwner == null) {
						super.visitMethodInsn(INVOKESTATIC, globOwner, "getInstance?", "()L" + globOwner + ";", false);
					}else {
						if(null == this.staticLambdaClasses || this.staticLambdaClasses.contains(newglobOwner)){
							globOwner = newglobOwner + "$Globals$";
						}
						super.visitMethodInsn(INVOKESTATIC, globOwner, "getInstance?", "()L" + globOwner + ";", false);
					}
				}else{
					super.visitMethodInsn(INVOKESTATIC, globOwner, "getInstance?", "()L" + globOwner + ";", false);
				}
				
				if(cnt >= 2){//unstore as vars
					for(char c : charz){//reload the stack
						/*if(c == 'D' || c=='J'){
							--maxLocalSize;
						}
						super.visitVarInsn(ANFTransform.getLoadOp(c), --maxLocalSize);*/
						super.visitVarInsn(ANFTransform.getLoadOp(c), tmpVars.pop());
            		}
				}
				else if(cnt == 1){
					genericSswap(charz.get(0).toString());
					//super.visitInsn(Opcodes.SWAP);
				}
				//if zero then do nothing, nothing to swap as no args
				super.visitMethodInsn(INVOKEVIRTUAL, globOwner, name, desc, false);
			}
			else{
				//so we make the thing INVOKEVIRTUAL instead of invokespecial (probably) iff it's not an init and we override the origonal one to be public from private (since not calling this/parent instance of private method anymore)
				super.visitMethodInsn((this.fromName!=null && !name.equals("<init>") && this.methodsWhichWereForcedPublic != null && this.methodsWhichWereForcedPublic.contains(new OwnerNameDesc(owner, name, desc)))?INVOKEVIRTUAL: opcode, owner, name, desc, itf);
			}
		}
		
		//adding an extra '0' var [indicated by setting fromName for globals class] (this ref) so point everything to the right...
		@Override
		public void visitVarInsn(int opcode, int var) {
			super.visitVarInsn(opcode, fromName!=null?var+1:var);
		}
		
		@Override
	    public void visitMaxs(int maxStack, int maxLocals) {
	    	super.visitMaxs(maxStack, fromName!=null?maxLocals+1:maxLocals);
	    }
		@Override
		public void visitIincInsn(int var, int increment) {
			super.visitIincInsn(fromName!=null?var+1:var, increment);
		}
	}
	
	private static void addstaticFinalVars(MethodVisitor mv, ArrayList<FieldVisitorHolder> staticFinalVars, String putTo) {
		for(FieldVisitorHolder sfv : staticFinalVars) {
			mv.visitLabel(new Label());
			mv.visitVarInsn(ALOAD, 0);
			
			switch(sfv.desc) {
				case "Ljava/lang/String;": mv.visitLdcInsn(sfv.value.toString()); break;
				case "J": BytecodeGenUtils.longOpcode(mv, ((Long)sfv.value).longValue() ); break;
				case "F": BytecodeGenUtils.floatOpcode(mv, ((Float)sfv.value).floatValue() ); break;
				case "D": BytecodeGenUtils.doubleOpcode(mv, ((Double)sfv.value).doubleValue() ); break;
				default: BytecodeGenUtils.intOpcode(mv, ((Integer)sfv.value).intValue() );
			}	
			
			mv.visitFieldInsn(PUTFIELD, putTo, sfv.name, sfv.desc);
		}
	}
	
	/**
	 * Used to turn clinit method into an init method 
	 *
	 */
	private static class InitCreator extends StaticAccessMethodRepoint  {//TODO: i think this class can be removed entierly
		private ArrayList<FieldVisitorHolder> staticFinalVars;
		public InitCreator(HashSet<String> staticLambdaClasses, MethodVisitor mv, int maxLocalSize, String fromName, ConcClassUtil clloader, HashSet<OwnerNameDesc> methodsWhichWereForcedPublic, ArrayList<FieldVisitorHolder> staticFinalVars, HashSet<String> locallyDefinedFields, Set<String> locallyDefinedMethods, String superClass, String[] ifaces, String className) {
			super(staticLambdaClasses, mv, maxLocalSize, fromName, clloader, methodsWhichWereForcedPublic, locallyDefinedFields, locallyDefinedMethods, superClass, ifaces, className);
			this.staticFinalVars = staticFinalVars;
		}
		
		
		public void visitCode() {
			if(!staticFinalVars.isEmpty()) {//we have some variables declared as static final resolving to a constant, so we must add setters for these here
				addstaticFinalVars(this.mv, this.staticFinalVars, super.fromName+"$Globals$");
			}
			
			super.visitCode();
		}
	}
	
	private static class OwnerNameDesc{
		private String owner;
		private String name;
		private String desc;
		
		public OwnerNameDesc(String owner, String name, String desc){
			this.owner = owner;
			this.name = name;
			this.desc = desc;
		}
		
		public OwnerNameDesc(String owner, String name){
			this(owner, name, "");
		}
		
		public boolean equals(Object o){//assume
			OwnerNameDesc o2 = (OwnerNameDesc)o;
			return o2.owner.equals(owner) && o2.desc.equals(desc) && o2.name.equals(name);
		}
		
		public int hashCode(){
			return this.owner.hashCode() +  this.desc.hashCode() +  this.name.hashCode(); 
		}
		
	}
	
	
	private static class StaticRedirector extends ClassVisitor  {
		private HashMap<String, Integer> maxLocalSize;
		private final ConcClassUtil clloader;
		private final HashSet<OwnerNameDesc> shouldbeForcedPublic;
		private final HashSet<OwnerNameDesc> shouldbeForcedPublicField;
		public final HashSet<OwnerNameDesc> methodsWhichWereForcedPublic = new HashSet<OwnerNameDesc>();
		private HashSet<String> staticLambdaClasses;
		
		public StaticRedirector(HashSet<String> staticLambdaClasses, ClassVisitor cv, HashMap<String, Integer> maxLocalSize, ConcClassUtil clloader, HashSet<OwnerNameDesc> shouldbeForcedPublic, HashSet<OwnerNameDesc> shouldbeForcedPublicField) {
			super(ASM7, cv);
			this.maxLocalSize = maxLocalSize;
			this.clloader = clloader;
			this.shouldbeForcedPublic = shouldbeForcedPublic;
			this.shouldbeForcedPublicField = shouldbeForcedPublicField;
			this.staticLambdaClasses = staticLambdaClasses;
		}

		//private Stack<String> className = new Stack<String>();
		private String className;
		private boolean isEnumCls = false;
		private String supername;
		private String[] interfaces;
		
		public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
			className = name;
			isEnumCls = (access & Opcodes.ACC_ENUM) != 0 && superName.equals("java/lang/Enum");
			this.supername = superName;
			this.interfaces = interfaces;
			super.visit(version, access, name, signature, superName, interfaces);
	    }
		
		public void visitEnd() {
			super.visitEnd();
		}

		public HashMap<String, String> origClassClinitToNewOne = new HashMap<String, String>();
		
		private String getGlobName(){
			//return getGlobName(className.peek());
			return getGlobName(className);
		}
		
		private String getGlobName(String className){
			
			String ret = origClassClinitToNewOne.get(className);
			
			if(null == ret){
				ret = className + "$Globals$";
				origClassClinitToNewOne.put(className, ret);
			}
			return ret;
		}
		
		
		
		private HashSet<String> locallyDefinedFields = new HashSet<String>();
		
		public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
			locallyDefinedFields.add(name + desc);
			
			//String cClass = this.className.peek();
			
			if(className.contains("$")) {//if nested class method needs to be public
				access = makePublic(access);
			}
			
			if(staticLambdaClasses != null && isNonGlobalerORPrimordial(className, null, name, staticLambdaClasses, clloader)) {
				return super.visitField(access, name, desc, signature, value);
			}
			
			boolean shared = false;
			boolean isStatic = Modifier.isStatic(access);
			if(isStatic){//skip if static...
				try {
					shared = this.clloader.getDetector().classForName(className).getField(name).isShared;
				} catch (Throwable e) {
					//throw new RuntimeException("Cannot find class: "+ cClass + " as " + e.getMessage(), e);
				}
				
				if(!shared) {//but dont skip if shared
					getGlobName();//may not have a clinit so we call it anyway
					
					if(isEnumCls){
						if(name.equals("ENUM$VALUES")){
							access = Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL | Opcodes.ACC_STATIC | Opcodes.ACC_SYNTHETIC;
						}
						return super.visitField(access, name, desc, signature, value);
					}
					
					return null;
				}
			}
			
			OwnerNameDesc me = new OwnerNameDesc(className, name);
			if(this.shouldbeForcedPublicField.contains(me) || className.contains("AtomicBoolean")){
				//if the field is used in subclass/function which is static, then force it public (bit of a hack, should use psar)
				access = ACC_PUBLIC;
				if(shared && isStatic) {
					access += ACC_STATIC;
				}
			}
			
			return super.visitField(access, name, desc, signature, value);
	    }
		
		public boolean permittedStatic(String name, String desc){
			if(name.equals("metaBinary") && desc.equals("()[Ljava/lang/String;")){
				return true;
			}
			return false;
		} 
		
		private int makePublic(int access) {
			if(!Modifier.isPublic(access)) {
				if (Modifier.isPrivate(access)) {
					access &= ~ACC_PRIVATE;
				}else if(Modifier.isProtected(access)) {
					access &= ~ACC_PROTECTED;
				}
				access += ACC_PUBLIC;
			}
			return access;
		}
		
		public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
			if(className.contains("$")) {//if nested class method needs to be public
				access = makePublic(access);
			}
			
			if(staticLambdaClasses != null && isNonGlobalerORPrimordial(className, null, name, staticLambdaClasses, clloader)) {
				return new StaticAccessMethodRepoint(this.staticLambdaClasses, super.visitMethod(access, name, desc, signature, exceptions), maxLocalSize.get(name + desc), clloader, locallyDefinedFields, maxLocalSize.keySet(), supername, interfaces, className);
			}
			
			if (name.equals("<clinit>")) {				
				// dont process this now, instead use it in init block of globals class
				String globName = getGlobName(className);
				
				super.visitInnerClass(globName, className, "Globals$", ACC_PUBLIC + ACC_STATIC);
				
				return null;
			} else {
				
				if(Modifier.isStatic(access) /*&& !isEnumCls*/){// && !name.equals("doings")){ //TODO: the doings hack
					//skip static module methods
					getGlobName();//may not have a clinit so we call it anyway

					if(Modifier.isNative(access) || isTraitMethod(name)) {
						access = makePublic(access);
						return super.visitMethod(access, name, desc, signature, exceptions);
					}

					MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
					
					if(name.endsWith("$sharedInit")) {
						//return mv;
						return new StaticAccessMethodRepoint(this.staticLambdaClasses, mv, maxLocalSize.get(name + desc), clloader, locallyDefinedFields, maxLocalSize.keySet(), supername, interfaces, className);
					}
					
					if(isEnumCls || permittedStatic(name, desc)/*|| ismetaBinary*/){
						if(isEnumCls && name.equals("values")){
							return new EnumValuesFieldRepointer(mv);//+ $Globals$
						}else{
							return mv;
						}
					}else{
						redirectStaticMethodProxytoGlobal(mv, name, desc);
						return mv;
					}
				}
				else{
					OwnerNameDesc me = new OwnerNameDesc(className, name, desc);
					if(this.shouldbeForcedPublic.contains(me)){
						//if the thing is used in subclass/function which is static, then force it public (bit of a hack, should use psar but cant be arsed)
						access = ACC_PUBLIC;
						methodsWhichWereForcedPublic.add(me);
					}
					MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
					return new StaticAccessMethodRepoint(this.staticLambdaClasses, mv, maxLocalSize.get(name + desc), clloader, locallyDefinedFields, maxLocalSize.keySet(), supername, interfaces, className);// repoint all static field/method access
				}
			}
		}
		
		private void redirectStaticMethodProxytoGlobal(MethodVisitor mv, String name, String desc){
			String globName = getGlobName(className);
			//return new StaticMethodProxytoGlobal(mv);//+ $Globals$
			mv.visitCode();
			mv.visitLabel(new Label());
			
			mv.visitMethodInsn(INVOKESTATIC, globName, "getInstance?", "()L"+globName+";", false);
			
			int argSpace = 0;
			for(Type arg : Type.getArgumentTypes(desc)){
				mv.visitVarInsn(arg.getOpcode(Opcodes.ILOAD), argSpace);
				argSpace += arg.getSize();
			}
			
			mv.visitMethodInsn(INVOKEVIRTUAL, globName, name, desc, false);
			Type retType = Type.getReturnType(desc);
			
			int opcode =  retType.getOpcode(Opcodes.IRETURN); //Opcodes.ARETURN;
			/*if(retType.getDimensions() == 1){
				
			}*/
			
			mv.visitInsn(opcode);
			mv.visitMaxs(1, 0);
			mv.visitEnd();
		}
		
	}
	
/*	private static class StaticMethodProxytoGlobal extends MethodVisitor  {
		public StaticMethodProxytoGlobal(MethodVisitor mv) {
			super(ASM7, mv);
		}
	}*/
	
	private static class EnumValuesFieldRepointer extends MethodVisitor  {
		public EnumValuesFieldRepointer(MethodVisitor mv) {
			super(ASM7, mv);
		}
		
		public void visitFieldInsn(int opcode, String owner, String name, String desc) {
			//->
			//INVOKESTATIC bytecodeSandbox$MYE$Globals$.getInstance? ()LbytecodeSandbox$MYE$Globals$;
			//GETFIELD TestClass$MyEnum.ENUM$VALUES : TestClass$MyEnum[] -> GETSTATIC TestClass$MyEnum$Globals$.ENUM$VALUES : TestClass$MyEnum[]
			if(opcode == Opcodes.GETSTATIC && name.equals("ENUM$VALUES")){
				owner += "$Globals$";
				mv.visitMethodInsn(INVOKESTATIC, owner, "getInstance?", "()L"+owner+";", false);
				opcode = Opcodes.GETFIELD;
			}
			
			super.visitFieldInsn(opcode, owner, name, desc);
		}
	}
		
	
	
	/*
	 * ...//nested...
	public static class Globals$ {
		private static volatile Globals instance;
		
		public final int thingy;
		public void something(){ }
		
		private Globals$() {//to clinit code
		}
		
		private init(Globals$ x) {//to clinit code
			thingy = 9;
		}
		
	
		public static Globals$ getInstance() {
			if (instance == null) {
				synchronized (Globals$.class) {
					if (instance == null) {
						instance = new Globals$();
						instance.init(instance);
					}
				}
			}
			return instance;
		}
	}
	*/
		
	private static class GlobalClassGennerator extends ClassVisitor  {

		private final String fromName;
		private final String globName;
		private boolean isEnumCls;
		private final ConcClassUtil clloader;
		private final HashMap<String, Integer> maxLocalSize;
		private final HashSet<OwnerNameDesc> methodsWhichWereForcedPublic;
		private final ArrayList<FieldVisitorHolder> staticFinalVars;
		private HashSet<String> staticLambdaClasses;
		private String[] ifaces;
		private String superClass;
		
		
		public GlobalClassGennerator(HashSet<String> staticLambdaClasses, ClassVisitor cv, String fromName, String globName, HashMap<String, Integer> maxLocalSize, ConcClassUtil clloader, HashSet<OwnerNameDesc> methodsWhichWereForcedPublic, ArrayList<FieldVisitorHolder> staticFinalVars) {
			super(ASM7, cv);
			this.fromName=fromName;
			this.globName=globName;
			this.maxLocalSize=maxLocalSize;
			this.clloader=clloader;
			this.methodsWhichWereForcedPublic=methodsWhichWereForcedPublic;
			this.staticFinalVars=staticFinalVars;
			this.staticLambdaClasses=staticLambdaClasses;
		}
		
		public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
			isEnumCls = (access & Opcodes.ACC_ENUM) != 0 && superName.equals("java/lang/Enum");
			
			if(fromName.equals(name)){//TODO: remove this check
				super.visit(version, ACC_PUBLIC + ACC_SUPER + ACC_STATIC, globName, null, "com/concurnas/bootstrap/runtime/cps/CObject", null);
			}
			
			this.superClass = superName;
			this.ifaces = interfaces;
			
			return;
	    }
		
		//cw.visitInnerClass("Akimbo$Globals$", "Akimbo", "Globals$", ACC_PUBLIC + ACC_STATIC);
		
		public void visitInnerClass(String name, String outerName, String innerName, int access) {
			super.visitInnerClass(name, outerName, innerName, access);
	    }
		
		private void createInializerAndSupportMethods(boolean visitInit){
			//only visitInit - true: when there is something to actually init
			
			/*
			public class Skiplas {
		
				private static WeakHashMap<Fiber, Skiplas> instance = new WeakHashMap<Fiber, Skiplas>();
				
				//++clinit for above
				
				{
					mv = cw.visitMethod(ACC_STATIC, "<clinit>", "()V", null, null);
					mv.visitCode();
					Label l0 = new Label();
					mv.visitLabel(l0);
					mv.visitTypeInsn(NEW, "java/util/WeakHashMap");
					mv.visitInsn(DUP);
					mv.visitMethodInsn(INVOKESPECIAL, "java/util/WeakHashMap", "<init>", "()V", false);
					mv.visitFieldInsn(PUTSTATIC, "Skiplas", "instance", "Ljava/util/WeakHashMap;");
					mv.visitInsn(RETURN);
					mv.visitMaxs(2, 0);
					mv.visitEnd();
				}
				
				
				public static Skiplas getInstance(){
					Fiber fib = Fiber.getCurrentFiber();
					Skiplas ret = instance.get(fib);
					if(null == ret){
						ret = new Skiplas();
						instance.put(fib, ret);
					}
					return ret;
				}
				
				public static Skiplas copyInstance(){
					Fiber fib = Fiber.getCurrentFiber();
					Skiplas ret = instance.get(fib);//may be null, dont create new one
					return null==ret?null:(Skiplas)Cloner.cloner.clone(new ConcurnificationTracker(), ret);
				}
				
				public static void setInstance(Skiplas skip){
					instance.put(Fiber.getCurrentFiber(), skip);
				}
				
				public static void removeInstance(Skiplas skip){
					instance.remove(Fiber.getCurrentFiber());
				}
			}
			*/
			
			String instance = "instance?";
			
			String globNameInPoshForm = "L"+globName+";";
			/*{
				FieldVisitor fv = super.visitField(ACC_PRIVATE + ACC_STATIC + ACC_VOLATILE, "instance", globNameInPoshForm, null, null);
				fv.visitEnd();
			}*/
			
			{
				FieldVisitor fv = super.visitField(ACC_PRIVATE + ACC_STATIC, instance, "Ljava/util/WeakHashMap;", "Ljava/util/WeakHashMap<Lcom/concurnas/bootstrap/runtime/cps/Fiber;"+globNameInPoshForm+">;", null);
				fv.visitEnd();
			}
			
			{
				MethodVisitor mv = super.visitMethod(ACC_STATIC, "<clinit>", "()V", null, null);
				mv.visitCode();
				Label l0 = new Label();
				mv.visitLabel(l0);
				mv.visitTypeInsn(NEW, "java/util/WeakHashMap");
				mv.visitInsn(DUP);
				mv.visitMethodInsn(INVOKESPECIAL, "java/util/WeakHashMap", "<init>", "()V", false);
				mv.visitFieldInsn(PUTSTATIC, this.globName, instance, "Ljava/util/WeakHashMap;");
				mv.visitInsn(RETURN);
				mv.visitMaxs(2, 0);
				mv.visitEnd();
				}
			
			
			/*{
				//MHA: the ? at the end makes it java and conc invalid method name! therefor no clash! - a pirate may introduce a clash by fiddling with bytecode tho...
				MethodVisitor mv = super.visitMethod(ACC_PUBLIC + ACC_STATIC + ACC_SYNTHETIC, "getInstance?", "()"+globNameInPoshForm, null, null);
				mv.visitCode();
				Label l0 = new Label();
				mv.visitLabel(l0);
				mv.visitFieldInsn(GETSTATIC, this.globName, "instance", globNameInPoshForm);
				Label l1 = new Label();
				mv.visitJumpInsn(IFNONNULL, l1);
				Label l2 = new Label();
				mv.visitLabel(l2);
				mv.visitFieldInsn(GETSTATIC, this.globName, "instance", globNameInPoshForm);
				mv.visitJumpInsn(IFNONNULL, l1);
				Label l3 = new Label();
				mv.visitLabel(l3);
				mv.visitTypeInsn(NEW, this.globName);
				mv.visitInsn(DUP);
				mv.visitMethodInsn(INVOKESPECIAL, this.globName, "<init>", "()V", false);
				mv.visitFieldInsn(PUTSTATIC, this.globName, "instance", globNameInPoshForm);
				if(visitInit){
					Label l9 = new Label();
					mv.visitLabel(l9);
					mv.visitFieldInsn(GETSTATIC, this.globName, "instance", globNameInPoshForm);
					//mv.visitFieldInsn(GETSTATIC, this.globName, "instance", globNameInPoshForm);
					//mv.visitMethodInsn(INVOKESPECIAL, this.globName, "init", "("+globNameInPoshForm+")V", false);
					mv.visitMethodInsn(INVOKESPECIAL, this.globName, "init", "()V", false);
				}
				mv.visitLabel(l1);
				mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
				mv.visitFieldInsn(GETSTATIC, this.globName, "instance", globNameInPoshForm);
				mv.visitInsn(ARETURN);
				mv.visitMaxs(2, 1);
				mv.visitEnd();
			}*/
			
			
			
			
			{
				MethodVisitor mv = super.visitMethod(ACC_PUBLIC + ACC_STATIC + ACC_SYNTHETIC + ACC_SYNCHRONIZED, "getInstance?", "()" + globNameInPoshForm, null, null);
				mv.visitCode();
				mv.visitLabel(new Label());
				mv.visitMethodInsn(INVOKESTATIC, "com/concurnas/bootstrap/runtime/cps/Fiber", "getCurrentFiber", "()Lcom/concurnas/bootstrap/runtime/cps/Fiber;", false);
				mv.visitVarInsn(ASTORE, 0);
				Label l1 = new Label();
				mv.visitLabel(l1);
				mv.visitFieldInsn(GETSTATIC, this.globName, instance, "Ljava/util/WeakHashMap;");
				mv.visitVarInsn(ALOAD, 0);
				mv.visitMethodInsn(INVOKEVIRTUAL, "java/util/WeakHashMap", "get", "(Ljava/lang/Object;)Ljava/lang/Object;", false);
				mv.visitTypeInsn(CHECKCAST, this.globName);
				mv.visitVarInsn(ASTORE, 1);
				

				
		/*		mv.visitFieldInsn(GETSTATIC, "java/lang/System", "err", "Ljava/io/PrintStream;");
				mv.visitTypeInsn(NEW, "java/lang/StringBuilder");
				mv.visitInsn(DUP);
				mv.visitLdcInsn("getInstance on '" +  this.globName + "' for key: ");
				mv.visitMethodInsn(INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "(Ljava/lang/String;)V", false);

				mv.visitMethodInsn(INVOKESTATIC, "com/concurnas/bootstrap/runtime/cps/Fiber", "getCurrentFiber", "()Lcom/concurnas/bootstrap/runtime/cps/Fiber;", false);
				mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/Object;)Ljava/lang/StringBuilder;", false);
				mv.visitLdcInsn(" got: ");
				mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
				mv.visitVarInsn(ALOAD, 1);
				mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/Object;)Ljava/lang/StringBuilder;", false);
				mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false);
				mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V", false);
				*/
				
				Label l2 = new Label();
				mv.visitLabel(l2);
				mv.visitVarInsn(ALOAD, 1);
				Label l3 = new Label();
				mv.visitJumpInsn(IFNONNULL, l3);
				Label l4 = new Label();
				mv.visitLabel(l4);
				mv.visitTypeInsn(NEW, this.globName);
				mv.visitInsn(DUP);
				mv.visitMethodInsn(INVOKESPECIAL, this.globName, "<init>", "()V", false);
				mv.visitVarInsn(ASTORE, 1);
				
				
				/*mv.visitFieldInsn(GETSTATIC, "java/lang/System", "err", "Ljava/io/PrintStream;");
				mv.visitLdcInsn("getInstance WAS NULL so making new? [%s] of %s -> %s");
				mv.visitInsn(ICONST_3);
				mv.visitTypeInsn(ANEWARRAY, "java/lang/Object");
				mv.visitInsn(DUP);
				mv.visitInsn(ICONST_0);
				mv.visitVarInsn(ALOAD, 1);
				mv.visitInsn(AASTORE);
				mv.visitInsn(DUP);
				mv.visitInsn(ICONST_1);
				mv.visitVarInsn(ALOAD, 1);
				mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Object", "getClass", "()Ljava/lang/Class;", false);
				mv.visitInsn(AASTORE);
				mv.visitInsn(DUP);
				mv.visitInsn(ICONST_2);
				mv.visitVarInsn(ALOAD, 1);
				mv.visitMethodInsn(INVOKESTATIC, "java/lang/System", "identityHashCode", "(Ljava/lang/Object;)I", false);
				mv.visitMethodInsn(INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", false);
				mv.visitInsn(AASTORE);
				mv.visitMethodInsn(INVOKESTATIC, "java/lang/String", "format", "(Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/String;", false);
				mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V", false);*/
				
				
				
				
				/*Object ab = "hi";
				System.err.println(String.format("getInstance WAS NULL so making new? [%s] of %s -> %s", ab, ab.getClass(), System.identityHashCode(ab)));*/
				
				Label l5 = new Label();
				mv.visitLabel(l5);
				mv.visitFieldInsn(GETSTATIC, this.globName, instance, "Ljava/util/WeakHashMap;");
				mv.visitVarInsn(ALOAD, 0);
				mv.visitVarInsn(ALOAD, 1);
				mv.visitMethodInsn(INVOKEVIRTUAL, "java/util/WeakHashMap", "put", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", false);
				mv.visitInsn(POP);				
				if(visitInit){
					mv.visitVarInsn(ALOAD, 1);
					mv.visitMethodInsn(INVOKESPECIAL, this.globName, "init", "()V", false);
				}
				mv.visitLabel(l3);
				mv.visitFrame(Opcodes.F_APPEND,2, new Object[] {"com/concurnas/bootstrap/runtime/cps/Fiber", this.globName}, 0, null);
				
				
				
			/*	mv.visitFieldInsn(GETSTATIC, "java/lang/System", "err", "Ljava/io/PrintStream;");
				mv.visitLdcInsn("getInstance? [%s] of %s -> %s");
				mv.visitInsn(ICONST_3);
				mv.visitTypeInsn(ANEWARRAY, "java/lang/Object");
				mv.visitInsn(DUP);
				mv.visitInsn(ICONST_0);
				mv.visitVarInsn(ALOAD, 1);
				mv.visitInsn(AASTORE);
				mv.visitInsn(DUP);
				mv.visitInsn(ICONST_1);
				mv.visitVarInsn(ALOAD, 1);
				mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Object", "getClass", "()Ljava/lang/Class;", false);
				mv.visitInsn(AASTORE);
				mv.visitInsn(DUP);
				mv.visitInsn(ICONST_2);
				mv.visitVarInsn(ALOAD, 1);
				mv.visitMethodInsn(INVOKESTATIC, "java/lang/System", "identityHashCode", "(Ljava/lang/Object;)I", false);
				mv.visitMethodInsn(INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", false);
				mv.visitInsn(AASTORE);
				mv.visitMethodInsn(INVOKESTATIC, "java/lang/String", "format", "(Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/String;", false);
				mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V", false);
				*/
				
				/*Object ab = "hi";
				System.err.println(String.format("getInstance? [%s] of %s -> %s", ab, ab.getClass(), System.identityHashCode(ab)));		*/

				mv.visitVarInsn(ALOAD, 1);
				mv.visitInsn(ARETURN);
				Label l6 = new Label();
				mv.visitLabel(l6);
				mv.visitMaxs(3, 2);
				mv.visitEnd();
			}

			
			
			
			
			{
				MethodVisitor mv = super.visitMethod(ACC_PUBLIC + ACC_STATIC + ACC_SYNTHETIC + ACC_SYNCHRONIZED, "copyInstance?", "(Lcom/concurnas/bootstrap/runtime/CopyTracker;)" + globNameInPoshForm, null, null);
				mv.visitCode();
				mv.visitLabel(new Label());
				
				/*mv.visitTypeInsn(NEW, "java/lang/Exception");
				mv.visitInsn(DUP);
				mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Exception", "<init>", "()V", false);
				mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Exception", "printStackTrace", "()V", false);
*/
				
				mv.visitMethodInsn(INVOKESTATIC, "com/concurnas/bootstrap/runtime/cps/Fiber", "getCurrentFiber", "()Lcom/concurnas/bootstrap/runtime/cps/Fiber;", false);
				mv.visitVarInsn(ASTORE, 1);
				Label l1 = new Label();
				mv.visitLabel(l1);
				mv.visitFieldInsn(GETSTATIC, this.globName, instance, "Ljava/util/WeakHashMap;");
				mv.visitVarInsn(ALOAD, 1);
				mv.visitMethodInsn(INVOKEVIRTUAL, "java/util/WeakHashMap", "get", "(Ljava/lang/Object;)Ljava/lang/Object;", false);
				mv.visitTypeInsn(CHECKCAST, this.globName);
				mv.visitVarInsn(ASTORE, 2);
				Label l2 = new Label();
				mv.visitLabel(l2);
				
				
				
				/*mv.visitFieldInsn(GETSTATIC, "java/lang/System", "err", "Ljava/io/PrintStream;");
				mv.visitLdcInsn("copyInstance: [%s] of %s -> %s");
				mv.visitInsn(ICONST_3);
				mv.visitTypeInsn(ANEWARRAY, "java/lang/Object");
				mv.visitInsn(DUP);
				mv.visitInsn(ICONST_0);
				mv.visitVarInsn(ALOAD, 1);
				mv.visitInsn(AASTORE);
				mv.visitInsn(DUP);
				mv.visitInsn(ICONST_1);
				mv.visitVarInsn(ALOAD, 1);
				mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Object", "getClass", "()Ljava/lang/Class;", false);
				mv.visitInsn(AASTORE);
				mv.visitInsn(DUP);
				mv.visitInsn(ICONST_2);
				mv.visitVarInsn(ALOAD, 1);
				mv.visitMethodInsn(INVOKESTATIC, "java/lang/System", "identityHashCode", "(Ljava/lang/Object;)I", false);
				mv.visitMethodInsn(INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", false);
				mv.visitInsn(AASTORE);
				mv.visitMethodInsn(INVOKESTATIC, "java/lang/String", "format", "(Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/String;", false);
				mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V", false);
				
				*/
				
				mv.visitVarInsn(ALOAD, 2);
				Label l3 = new Label();
				mv.visitJumpInsn(IFNONNULL, l3);
				mv.visitInsn(ACONST_NULL);
				Label l4 = new Label();
				mv.visitJumpInsn(GOTO, l4);
				mv.visitLabel(l3);
				//mv.visitFrame(Opcodes.F_APPEND,2, new Object[] {"com/concurnas/bootstrap/runtime/cps/Fiber", this.globName}, 0, null);
				//mv.visitFieldInsn(GETSTATIC, "com/concurnas/runtime/bootstrapCloner/Cloner", "cloner", "Lcom/concurnas/runtime/bootstrapCloner/Cloner;");
				
				mv.visitMethodInsn(INVOKESTATIC, "com/concurnas/runtime/bootstrapCloner/Cloner$Globals$", "getInstance?", "()Lcom/concurnas/runtime/bootstrapCloner/Cloner$Globals$;", false);
				

				Label la1 = new Label();
				mv.visitLabel(la1);
				
        		mv.visitFieldInsn(GETFIELD, "com/concurnas/runtime/bootstrapCloner/Cloner$Globals$", "cloner", "Lcom/concurnas/runtime/bootstrapCloner/Cloner;");
				
        		Label la2 = new Label();
				mv.visitLabel(la2);
				
				//cloner is not doing a clone of itself?
				
				/*mv.visitTypeInsn(NEW, "com/concurnas/bootstrap/runtime/CopyTracker");
				mv.visitInsn(DUP);
				mv.visitMethodInsn(INVOKESPECIAL, "com/concurnas/bootstrap/runtime/CopyTracker", "<init>", "()V", false);*/
				mv.visitVarInsn(ALOAD, 0);
				mv.visitVarInsn(ALOAD, 2);
				mv.visitInsn(ACONST_NULL);
				mv.visitMethodInsn(INVOKEVIRTUAL, "com/concurnas/runtime/bootstrapCloner/Cloner", "clone", "(Lcom/concurnas/bootstrap/runtime/CopyTracker;Ljava/lang/Object;Lcom/concurnas/bootstrap/runtime/CopyDefinition;)Ljava/lang/Object;", false);
				mv.visitTypeInsn(CHECKCAST, this.globName);
				mv.visitLabel(l4);

				mv.visitInsn(ARETURN);
				mv.visitMaxs(3, 2);
				mv.visitEnd();
			}
			
			
			{
				MethodVisitor mv = super.visitMethod(ACC_PUBLIC + ACC_STATIC + ACC_SYNTHETIC + ACC_SYNCHRONIZED, "setInstance?", "(Lcom/concurnas/bootstrap/runtime/cps/Fiber;"+globNameInPoshForm+")V", null, null);
				mv.visitCode();
				Label l0 = new Label();
				mv.visitLabel(l0);
				
				mv.visitFieldInsn(GETSTATIC, this.globName, instance, "Ljava/util/WeakHashMap;");
				mv.visitVarInsn(ALOAD, 0);
				mv.visitVarInsn(ALOAD, 1);
				mv.visitMethodInsn(INVOKEVIRTUAL, "java/util/WeakHashMap", "put", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", false);
				mv.visitInsn(POP);
				Label l1 = new Label();
				mv.visitLabel(l1);
				mv.visitInsn(RETURN);
				mv.visitMaxs(3, 2);
				mv.visitEnd();
			}
			
			
			{
				MethodVisitor mv = super.visitMethod(ACC_PUBLIC + ACC_STATIC + ACC_SYNTHETIC + ACC_SYNCHRONIZED, "removeInstance?", "()V", null, null);
				mv.visitCode();
				Label l0 = new Label();
				mv.visitLabel(l0);
				mv.visitFieldInsn(GETSTATIC, this.globName, instance, "Ljava/util/WeakHashMap;");
				mv.visitMethodInsn(INVOKESTATIC, "com/concurnas/bootstrap/runtime/cps/Fiber", "getCurrentFiber", "()Lcom/concurnas/bootstrap/runtime/cps/Fiber;", false);
				mv.visitMethodInsn(INVOKEVIRTUAL, "java/util/WeakHashMap", "remove", "(Ljava/lang/Object;)Ljava/lang/Object;", false);
				mv.visitInsn(POP);
				Label l1 = new Label();
				mv.visitLabel(l1);
				mv.visitInsn(RETURN);
				mv.visitMaxs(2, 1);
				mv.visitEnd();
			}
			
		}
		
		
		private void createInializerAndSupportMethodsEnum(){
			//only visitInit - true: when there is something to actually init
			
			String instance = "singleEnuminstance?";
			
			String globNameInPoshForm = "L"+globName+";";
			
			{
				FieldVisitor fv = super.visitField(ACC_PRIVATE + ACC_STATIC, instance, globNameInPoshForm, globNameInPoshForm, null);
				fv.visitEnd();
			}
			
			{
				MethodVisitor mv = super.visitMethod(ACC_STATIC, "<clinit>", "()V", null, null);
				mv.visitCode();
				mv.visitLabel(new Label());
				mv.visitInsn(RETURN);
				mv.visitMaxs(2, 0);
				mv.visitEnd();
			}
			
			
			{
				MethodVisitor mv = super.visitMethod(ACC_PUBLIC + ACC_STATIC + ACC_SYNTHETIC + ACC_SYNCHRONIZED, "getInstance?", "()" + globNameInPoshForm, null, null);
				mv.visitCode();
				mv.visitLabel(new Label());
				
				mv.visitFieldInsn(GETSTATIC, this.globName, instance, globNameInPoshForm);
				Label end = new Label();
				mv.visitJumpInsn(IFNONNULL, end);
				mv.visitLabel(new Label());
				mv.visitTypeInsn(NEW, this.globName);
				mv.visitInsn(DUP);
				mv.visitInsn(DUP);
				mv.visitMethodInsn(INVOKESPECIAL, this.globName, "<init>", "()V", false);
				mv.visitMethodInsn(INVOKESPECIAL, this.globName, "init", "()V", false);
				mv.visitFieldInsn(PUTSTATIC, this.globName, instance, globNameInPoshForm);
				
				mv.visitLabel(end);


				mv.visitFieldInsn(GETSTATIC, this.globName, instance, globNameInPoshForm);
				
				mv.visitInsn(ARETURN);
				mv.visitMaxs(3, 2);
				mv.visitEnd();
			}

			
			
			{
				MethodVisitor mv = super.visitMethod(ACC_PUBLIC + ACC_STATIC + ACC_SYNTHETIC + ACC_SYNCHRONIZED, "copyInstance?", "(Lcom/concurnas/bootstrap/runtime/CopyTracker;)" + globNameInPoshForm, null, null);
				mv.visitCode();
				mv.visitLabel(new Label());
				mv.visitMethodInsn(INVOKESTATIC, this.globName, "getInstance?", "()" + globNameInPoshForm, false);
				mv.visitInsn(ARETURN);
				mv.visitMaxs(3, 2);
				mv.visitEnd();
			}
			
			
			{//nop
				MethodVisitor mv = super.visitMethod(ACC_PUBLIC + ACC_STATIC + ACC_SYNTHETIC + ACC_SYNCHRONIZED, "setInstance?", "(Lcom/concurnas/bootstrap/runtime/cps/Fiber;"+globNameInPoshForm+")V", null, null);
				mv.visitCode();
				mv.visitLabel(new Label());
				mv.visitInsn(RETURN);
				mv.visitMaxs(3, 2);
				mv.visitEnd();
			}
			
			
			{//nop
				MethodVisitor mv = super.visitMethod(ACC_PUBLIC + ACC_STATIC + ACC_SYNTHETIC + ACC_SYNCHRONIZED, "removeInstance?", "()V", null, null);
				mv.visitCode();
				mv.visitLabel(new Label());
				mv.visitInsn(RETURN);
				mv.visitMaxs(3, 2);
				mv.visitEnd();
			}
			
		}
		
		
		private boolean visitedClinit = false;
		
		private void addEmptyInit(){//super simple
			MethodVisitor mv = super.visitMethod(ACC_PRIVATE, "<init>", "()V", null, null);
			mv.visitCode();
			mv.visitVarInsn(ALOAD, 0);
			mv.visitMethodInsn(INVOKESPECIAL, "com/concurnas/bootstrap/runtime/cps/CObject", "<init>", "()V", false);
			mv.visitInsn(RETURN);
			mv.visitMaxs(1, 1);
			mv.visitEnd();
		}

		private ArrayList<String> shareInitsToMake = new ArrayList<String>();
		
		public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
			
			if (name.equals("<clinit>") ) {
				visitedClinit=true;
				//steal the contents! - use this in the init method
				if(isEnumCls){
					createInializerAndSupportMethodsEnum();
				}else{
					createInializerAndSupportMethods(true);
				}
				addEmptyInit();
				//return new InitCreator(super.visitMethod(ACC_PUBLIC, "init", "(L"+this.globName+";)V", signature, exceptions), maxLocalSize.get(name+desc), fromName, this.clloader);
				return new InitCreator(this.staticLambdaClasses, super.visitMethod(ACC_PUBLIC, "init", "()V", signature, exceptions), maxLocalSize.get(name+desc), fromName, this.clloader, methodsWhichWereForcedPublic, this.staticFinalVars, this.locallyDefinedFields, maxLocalSize.keySet(), superClass, ifaces, fromName);
				
			} else {
				if(Modifier.isStatic(access)){
					
					if(name.endsWith("$sharedInit")) {
						//repoint this to instance with anothersharedInit etc
						shareInitsToMake.add(name);
						return null;
					}
					
					if(Modifier.isNative(access) ) {
						return new RepointNativeStaticMethodBackToNonGlobal(super.visitMethod( ACC_PUBLIC, name, desc, signature, exceptions), name, desc, fromName);
					}
					else if(isTraitMethod(name)) {
						return null;
					}
								
					if(/*name.equals("metaBinary") &&*/ desc.equals("(Lcom/concurnas/bootstrap/lang/offheap/Decoder;)V")){
						return null;
					}
					
				/*	if(this.fromName.contains("TestCase")) {
						
						if(!(desc.contains("J")|| desc.contains("D"))) {
							int h=9;
							System.err.println(desc);
							return null;
						}

						System.err.println(String.format("%s -> %s %s", this.fromName, name, desc));
						return null;
						
					}*/
					
					//route to static instance, but also route all local static field and method invokations to local instance (ALOAD 0; etc)
					return new StaticAccessMethodRepoint(this.staticLambdaClasses, super.visitMethod( ACC_PUBLIC, name, desc, signature, exceptions), maxLocalSize.get(name+desc), fromName, this.clloader, methodsWhichWereForcedPublic, this.locallyDefinedFields, maxLocalSize.keySet(), superClass, ifaces, this.fromName);//no longer static - cheat and make public i.e. ignore: access &= ~Modifier.STATIC
				}
			}
			
			return null;
		}
		
		private HashSet<String> locallyDefinedFields = new HashSet<String>();
		
		public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
			locallyDefinedFields.add(name + desc);
			if(Modifier.isStatic(access)  ){
				boolean shared = false;
				try {
					shared = this.clloader.getDetector().classForName(fromName).getField(name).isShared;
				} catch (Throwable e) {
					//throw new RuntimeException("Cannot find class: "+ fromName + " as " + e.getMessage(), e);
				}
				
				if(!shared) {//shared are not mapped
					int accessp = ACC_PUBLIC;
					
					if(Modifier.isFinal(access)) {
						accessp += ACC_FINAL;
					}
					
					return super.visitField( accessp, name, desc, signature, value);//TODO: treat as local field, non static, cheat and make public [nasty]
				}
			}
			
			return null;
	    }
		
		
		private void addInitforStaticFinalVars(){//super simple
			MethodVisitor mv = super.visitMethod(ACC_PUBLIC, "init", "()V", null, null);
			mv.visitCode();
			addstaticFinalVars(mv, this.staticFinalVars, this.fromName+"$Globals$");
			mv.visitInsn(RETURN);
			mv.visitMaxs(1, 1);
			mv.visitEnd();
		}
		
	    public void visitEnd() {
	    	if(!visitedClinit   ){//didnt have a clinit, so create a simple init and getter now...
	    		if(!this.staticFinalVars.isEmpty()) {
	    			addInitforStaticFinalVars();
	    			createInializerAndSupportMethods(true);
	    		}else {
		    		createInializerAndSupportMethods(false);
	    		}
	    		//TODO: this code is only really useful in case where u do this class X{ fromouter = 99 class Y{ y = 1 + fromouter }} //since fromouter is from psar which is static...
	    		addEmptyInit();
	    	}
	    	
	    	
			if(!shareInitsToMake.isEmpty()) {

				String origClassName = this.fromName;//this.className.peek();
				String globalClass = this.globName;//this.origClassClinitToNewOne.get(origClassName);
				
				for(String sharedinit : shareInitsToMake) {
					String initOnceVarName = sharedinit + "$onceVar";
					{
						FieldVisitor fv = super.visitField(ACC_PRIVATE + ACC_VOLATILE + ACC_STATIC, initOnceVarName, "Ljava/lang/Object;", null, null);
					    fv.visitEnd();
					}
					
					{
						MethodVisitor mv = super.visitMethod(ACC_PUBLIC, sharedinit, "()V", null, null);
						mv.visitCode();
						Label l0 = new Label();
						Label l1 = new Label();
						Label l2 = new Label();
						mv.visitTryCatchBlock(l0, l1, l2, null);
						Label l3 = new Label();
						mv.visitTryCatchBlock(l2, l3, l2, null);
						Label l4 = new Label();
						mv.visitLabel(l4);
						mv.visitFieldInsn(GETSTATIC, globalClass, initOnceVarName, "Ljava/lang/Object;");
						Label l5 = new Label();
						mv.visitJumpInsn(IFNONNULL, l5);
						Label l6 = new Label();
						mv.visitLabel(l6);
						mv.visitVarInsn(ALOAD, 0);
						mv.visitInsn(DUP);
						mv.visitVarInsn(ASTORE, 1);
						mv.visitInsn(MONITORENTER);
						mv.visitLabel(l0);
						mv.visitFieldInsn(GETSTATIC, globalClass, initOnceVarName, "Ljava/lang/Object;");
						Label l7 = new Label();
						mv.visitJumpInsn(IFNONNULL, l7);
						Label l8 = new Label();
						mv.visitLabel(l8);
						mv.visitIntInsn(BIPUSH, 111);
						mv.visitMethodInsn(INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", false);
						mv.visitFieldInsn(PUTSTATIC, globalClass, initOnceVarName, "Ljava/lang/Object;");
						Label l9 = new Label();
						mv.visitLabel(l9);
						mv.visitMethodInsn(INVOKESTATIC, origClassName, sharedinit, "()V", false);//this is called only once
						mv.visitLabel(l7);
						mv.visitVarInsn(ALOAD, 1);
						mv.visitInsn(MONITOREXIT);
						mv.visitLabel(l1);
						mv.visitJumpInsn(GOTO, l5);
						mv.visitLabel(l2);
						mv.visitVarInsn(ALOAD, 1);
						mv.visitInsn(MONITOREXIT);
						mv.visitLabel(l3);
						mv.visitInsn(ATHROW);
						mv.visitLabel(l5);
						mv.visitInsn(RETURN);
						mv.visitMaxs(2, 2);
						mv.visitEnd();
					}
				}
			}
	    	
	    	super.visitEnd();
	    }
	}
	
	private static class RepointNativeStaticMethodBackToNonGlobal extends MethodVisitor {

		private String methodName;
		private String desc;
		private String origClsName;

		protected RepointNativeStaticMethodBackToNonGlobal(MethodVisitor mv, String name, String desc, String origClsName) {
			super(ASM7, mv);
			this.methodName = name;
			this.desc = desc;
			this.origClsName = origClsName;
		}
		
		@Override
		public void visitEnd() {//normal wew'd use visitCode, but there is no code to visit!
			int locVar=1;
			
			for(char c : ANFTransform.getPrimOrObj(desc)){//reload the stack from vars
				mv.visitVarInsn(ANFTransform.getLoadOp(c), locVar++);
				if(c == 'D' || c=='J'){
					locVar++;
				}
			}
			
			mv.visitMethodInsn( INVOKESTATIC, origClsName, methodName, desc, false);
			
			String dd = Type.getMethodType(desc).getReturnType().getDescriptor();
			mv.visitInsn(dd.startsWith("[")?ARETURN:ANFTransform.getReturnOp(desc.charAt(desc.length()-1)));//return
		}
	}
	

	private HashMap<String, byte[]> globalizerClasses = new HashMap<String, byte[]>();
	
	
	
	/**
	 * Find all constructors OR Fields refered visited inside all nested static method/classes. If one of these is created in parent class, then force it public.
	 * Since it will now be called from static globalizer class [which wont have access without a psar - which is an ugly java hack anyway imho...]
	 */
	private static class UsedPrivConstruFinder extends ClassVisitor{

		public UsedPrivConstruFinder() {
			super(ASM7);
		}

		private Stack<Boolean> isCurrentStatic = new Stack<Boolean>();
		public final HashSet<OwnerNameDesc> shouldbeForcedPublic = new HashSet<OwnerNameDesc>();//esnetially just a list of all constructors method visited during static method/class
		public final HashSet<OwnerNameDesc> shouldbeForcedPublicField = new HashSet<OwnerNameDesc>();
		
		private class MethodV extends MethodVisitor {
			public MethodV(MethodVisitor mv) {
				super(ASM7, mv);
			}

			public void visitEnd() {
				super.visitEnd();
				isCurrentStatic.pop();// bit dirty to stick this here oh well
			}

		    public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
		    	if(isCurrentStatic.peek()){
		    		shouldbeForcedPublic.add(new OwnerNameDesc(owner, name, desc));
		    	}
		        super.visitMethodInsn(opcode, owner, name, desc, itf);
		    }
		    
		    public void visitFieldInsn(int opcode, String owner, String name, String desc) {
		    	if(isCurrentStatic.peek()){
		    		shouldbeForcedPublicField.add(new OwnerNameDesc(owner, name));
		    	}
		    	super.visitFieldInsn(opcode, owner, name, desc);
		    }
		    
			
		}
		
		private void goIn(int access){
			boolean isStatic = Modifier.isStatic(access);
			if(isCurrentStatic.isEmpty()){
				isCurrentStatic.push(isStatic);
			}
			else{
				isCurrentStatic.push(isCurrentStatic.peek() || isStatic);
			}
		}
		
		public void visitInnerClass(String name, String outerName, String innerName, int access) {
			goIn(access);
			super.visitInnerClass(name,  outerName,  innerName, access);
			isCurrentStatic.pop();
	    }
		
		public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
			goIn(access);
	        return new MethodV(super.visitMethod(access, name, desc, signature, exceptions));
	    }
	}
	
	private static class FieldVisitorHolder{
		public int access;
		public String name;
		public String desc;
		public String signature;
		public Object value;

		public FieldVisitorHolder(int access, String name, String desc, String signature, Object value) {
			this.access = access;
			this.name = name;
			this.desc = desc;
			this.signature = signature;
			this.value = value;
		}
	}
	
	private static class StaticFinalVariableFinder extends ClassVisitor{

		public StaticFinalVariableFinder() {
			super(ASM7);
		}
		
		public ArrayList<FieldVisitorHolder> staticFinalVars = new ArrayList<FieldVisitorHolder>();
		
		public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
			if(Modifier.isStatic(access) && Modifier.isFinal(access) && null != value) {
				staticFinalVars.add( new FieldVisitorHolder( access,  name,  desc,  signature,  value) );
			}
			
			return super.visitField(access, name, desc, signature, value);
		}
		
	}
	
	public HashMap<String, byte[]> transform(String name, ConcClassUtil clloader) {
		ClassReader cr = new ClassReader(this.inputClassBytes);
		//calculate the maxlocals for each method
		HashMap<String, Integer> maxlocalMap = this.mfl.getMaxlocalMap(name, cr);
		
		//find private constructors used within nested static functions or classes
		UsedPrivConstruFinder upcf = new UsedPrivConstruFinder();
		cr.accept(upcf, 0);
		HashSet<OwnerNameDesc> shouldbeForcedPublic = upcf.shouldbeForcedPublic;
		HashSet<OwnerNameDesc> shouldbeForcedPublicField = upcf.shouldbeForcedPublicField;
		
		ClassWriter cw = new ConcClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS, clloader.getDetector());// TODO:  turn off compute frames?
		// cv forwards all events to cw
		StaticRedirector staticRedirector = new StaticRedirector(this.staticLambdaClasses, cw, maxlocalMap, clloader, shouldbeForcedPublic, shouldbeForcedPublicField);
		cr.accept(staticRedirector, 0);
		globalizerClasses.put(name, cw.toByteArray());
		
		//TODO: only redirect if in staticLambdaClasses
		
		if(staticLambdaClasses == null || staticLambdaClasses.contains(name)) {
			HashSet<OwnerNameDesc> methodsWhichWereForcedPublic = staticRedirector.methodsWhichWereForcedPublic;
			
			if(!staticRedirector.origClassClinitToNewOne.isEmpty()){
				for(String clsName : staticRedirector.origClassClinitToNewOne.keySet()){
					String globName = staticRedirector.origClassClinitToNewOne.get(clsName);
					
					ClassWriter gcw = new ConcClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS, clloader.getDetector() );// TODO:  turn off compute frames?
					
					StaticFinalVariableFinder sfvf = new StaticFinalVariableFinder();
					cr.accept(sfvf, 0);
					ArrayList<FieldVisitorHolder> staticFinalVars = sfvf.staticFinalVars;
					
					GlobalClassGennerator gcg = new GlobalClassGennerator(this.staticLambdaClasses, gcw, clsName, globName, maxlocalMap, clloader, methodsWhichWereForcedPublic, staticFinalVars);

					cr.accept(gcg, 0);
					globalizerClasses.put(globName, gcw.toByteArray());
				}
			}
		}
		
		return globalizerClasses; // b2 represents the same class as b1
	}

}
