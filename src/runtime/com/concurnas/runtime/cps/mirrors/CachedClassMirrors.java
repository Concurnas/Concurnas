package com.concurnas.runtime.cps.mirrors;

import java.lang.reflect.Modifier;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import com.concurnas.compiler.utils.Thruple;
import com.concurnas.runtime.ConcClassUtil;
import com.concurnas.runtimeCache.BootstrapModuleLoader;

/**
 * CachedClassMirrors caches information about a set of classes that are loaded
 * through byte arrays, and which are not already loaded by the classloader
 **/

public class CachedClassMirrors  {
	final static String[] EMPTY_SET = new String[0];

	private final RuntimeClassMirrors parentMirror;
	private HashMap<String, ClassMirror> cachedClasses = new HashMap<String, ClassMirror>();
	private final ConcClassUtil ccl;
	//private Detector d;
	
	public CachedClassMirrors(ConcClassUtil cl/*, Detector da*/) {
		parentMirror = new RuntimeClassMirrors(cl.getParentCL());
		ccl = cl;
		//this.d=da;
	}

	public ClassMirror classForNameNoEx(String className) {
		try{
			return classForName(className);
		}
		catch(ClassMirrorNotFoundException e){
			return null;
		}
	}
	
	public ClassMirror classForName(String className) throws ClassMirrorNotFoundException {
		// defer to loaded class objects first, then to cached class mirrors.
		ClassMirror ret = cachedClasses.get(className);

		if (ret == null) {
			//ret = delegate.classForName(className);
			
			try{
				Class<?> primordial = ccl.loadClassFromPrimordial(className.replace("/", "."));
				if(null != primordial){
					ret = new RuntimeClassMirror(primordial);
				}
			}
			catch(Exception e){
			}
			
			if(null == ret){
				byte[] primativeState = ccl.getBytecode(className);
				
				if(null != primativeState){
					ret = new SimpleClassDefProperties(primativeState, this/*, this.d*/);
				}
				else{
					//ret = parentMirror.classForName(className);//found from primordial?
					
					//no wtf?
					
				}
			}
			
			cachedClasses.put(className, ret);
		}
		
		return ret;
	}

	public ClassMirror mirror(Class<?> clazz) {
		//dunno
		return parentMirror.mirror(clazz);
	}

	public ClassMirror mirror(String className, byte[] bytecode) {
		// if it is loaded by the classLoader already, we will
		// not load the classNode, even if the bytes are different
		ClassMirror ret = null;
		if (!parentMirror.isLoaded(className)) {
			ret = new SimpleClassDefProperties(bytecode, this/*, this.d*/);
			String name = ret.getName().replace('/', '.'); // Class.forName
															// format
			this.cachedClasses.put(name, ret);
		}
		return ret;
	}
	
	public boolean isFinalPrimordialMethod(String className, String name, String desc) {
		try{
			Class<?> fromPrimarch = ccl.loadClassFromPrimordial(className.replace("/", "."));
			if(null != fromPrimarch){
				RuntimeClassMirror rtcm = new RuntimeClassMirror(fromPrimarch);
				if(rtcm.isFinal()){
					return true;//method has to be final because class is
				}
				for (MethodMirror om : rtcm.getDeclaredMethods()) {
		            if (om.getName().equals(name)) {
		                // when comparing descriptors only compare arguments, not return types
		                String omDesc= om.getMethodDescriptor();
		            
		                if (omDesc.substring(0,omDesc.indexOf(")")).equals(desc.substring(0,desc.indexOf(")")))) {
		                    if (om.isBridge())  continue;
		                    return om.isFinal();
		                }
		            }
		        }
			}
		}
		catch(ClassNotFoundException e){
			return false;//assume worst case and gennerate both later on
		}
		
		return false;
	}

	public boolean overridesPrimordialMethod(String className, String methodName, String desc) {
		//System.err.println("pri " + methodName);
		if(methodName.endsWith("init>")){
			return false;//shortcut
		}
		try{
			Class<?> fromPrimarch = ccl.loadClassFromPrimordial(className.replace("/", "."));
			if(null != fromPrimarch){
				RuntimeClassMirror rtcm = new RuntimeClassMirror(fromPrimarch);
				
				for (MethodMirror om : rtcm.getDeclaredMethods()) {
		            if (om.getName().equals(methodName)) {
		                // when comparing descriptors only compare arguments, not return types
		                String omDesc= om.getMethodDescriptor();
		            
		                if (omDesc.substring(0,omDesc.indexOf(")")).equals(desc.substring(0,desc.indexOf(")")))) {
		                    if (om.isBridge())  continue;
		                    return true;//its primordial
		                }
		            }
		        }
			}
			else{
				ClassMirror me = classForName(className);
				String sup = me.getSuperclass();
				if(null != sup && overridesPrimordialMethod(sup, methodName, desc)){
					return true;
				}
				else{
					//if any interface does it
					for(String i : me.getInterfaces()){
						if(overridesPrimordialMethod(i, methodName, desc)){
							return true;
						}
					}
				}
			}
		}
		catch(ClassMirrorNotFoundException e){
			return true;//assume worst case and gennerate both later on
		}
		catch(ClassNotFoundException e){
			return true;//assume worst case and gennerate both later on
		}
		
		return false;
	}

	public int isNestedClass(String className) {
		Class<?> fromPrimarch = null;
		
		try{
			fromPrimarch = ccl.loadClassFromPrimordial(className.replace("/", "."));
		}
		catch(Exception ignore){ }
		
		if(null != fromPrimarch){
			RuntimeClassMirror rtcm = new RuntimeClassMirror(fromPrimarch);
			return rtcm.isNestedClass();
		}
		else{
			ClassMirror me = classForNameNoEx(className);
			
			if(null == me){ return 0; }
			
			return me.isNestedClass();
		}
	}
	
	public boolean invokedCallIsStackPausable(String className, String methodName, String desc) {
		if(methodName.equals("<init>")){
			return false;//shortcut
		}
		Class<?> fromPrimarch = null;
		
		try{
			fromPrimarch = ccl.loadClassFromPrimordial(className.replace("/", "."));
		}
		catch(Exception ignore){ }
		
		if(null != fromPrimarch){
			RuntimeClassMirror rtcm = new RuntimeClassMirror(fromPrimarch);
			
			for (MethodMirror om : rtcm.getDeclaredMethods()) {
	            if (om.getName().equals(methodName)) {
	                // when comparing descriptors only compare arguments, not return types
	                String omDesc= om.getMethodDescriptor();
	            
	                if (omDesc.substring(0,omDesc.indexOf(")")).equals(desc.substring(0,desc.indexOf(")")))) {
	                    if (om.isBridge())  continue;
	                    return false;//its primordial
	                }
	            }
	        }
		}
		else{
			if( methodName.equals("init") || methodName.equals("copy")){//another hack
				return true;//MHA: asume its a method we added in an earlier step? - should fix this...
				//should really invalidate cache benhind classForNameNoEx and reload code with extra init methods in Cpsifier
			}
			
			if(className.endsWith("$Globals$")){
				return true;//MHA: another hack
			}
			
			ClassMirror me = classForNameNoEx(className);
			
			if(null == me){ 
				return false; }
			
			MethodMirror[] mms = me.getDeclaredMethods();
			
			if(null == mms){ return false; }
			
			for(MethodMirror mm : mms){
				if(mm.getMethodDescriptor().equals(desc) && mm.getName().equals(methodName)){
					return true;
				}
			}
			String sup = me.getSuperclassNoEx();
			if(null != sup && invokedCallIsStackPausable(sup, methodName, desc)){
				return true;
			}
			else{
				//if any interface does it
				String[] ifaces = me.getInterfacesNoEx();
				if(null != ifaces){
					for(String i : ifaces){
						if(invokedCallIsStackPausable(i, methodName, desc)){
							return true;
						}
					}
				}
			}
		}
		return false;//assume worst case and invoke slower one
		
		
	}

}

class SimpleClassDefProperties extends ClassVisitor implements ClassMirror {

	String name;
	boolean isEnum;
	boolean isInterface;
	MethodMirror[] declaredMethods;
	MethodMirror[] filteredConstructors;//only those having 
	ConstructorMirror[] declaredConstructorsWithoutHiddenArgs;
	String[] interfaceNames;
	String superName;
	private CachedClassMirrors ccm;
	
	private List<BytecodeMethodMirror> tmpMethodList; // used only while processing bytecode.

	public SimpleClassDefProperties(byte[] bytecode, CachedClassMirrors ccm/*, Detector d*/) {
		super(Opcodes.ASM7);
		//this.d=d;
		ClassReader cr = new ClassReader(bytecode);
		this.ccm=ccm;
		cr.accept(this, /* flags */0);//JPT: eek in consturcotr?
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public boolean isInterface() {
		return isInterface;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof SimpleClassDefProperties) {
			SimpleClassDefProperties mirr = (SimpleClassDefProperties) obj;
			return mirr.name.equals(this.name) && mirr.isInterface == this.isInterface;
		}

		return false;
	}

	@Override
	public int hashCode() {
		return this.name.hashCode();
	}

	/**
	 * Includes constructors
	 */
	@Override
	public MethodMirror[] getDeclaredMethods() {
		return (declaredMethods == null) ? new MethodMirror[0] : declaredMethods;
	}
	
	private String[] genArguments = null;
	
	@Override
	public String[] getGenericArguments() {
		//throw new RuntimeException("getGenericArguments not implemneted for SimpleClassDefProperties");
		//TODO: better to implement this via processing of signature?
		if(null == genArguments) {
			TypeVariable[] tvs;
			try {
				tvs = Class.forName(this.name.replace('/', '.')).getTypeParameters();
			} catch (ClassNotFoundException e) {
				genArguments = new String[0];
				return genArguments;
			}
			
			int n=0;
			genArguments = new String[tvs.length];
			for(TypeVariable tva : tvs) {
				genArguments[n++] = tva.getName();
			}
		}
		return genArguments;
	}
	

	@Override
	public ConstructorMirror[] getDeclaredConstructorsWithoutHiddenArgs() {
		//only used for classRefIface gnneration currently
		if(filteredConstructors == null){
			return new ConstructorMirror[0];
		}else if(declaredConstructorsWithoutHiddenArgs == null){
			if(filteredConstructors.length > 0){
				ArrayList<ConstructorMirror> tmpconList = new ArrayList<ConstructorMirror>();
				for(MethodMirror met : filteredConstructors){
					//if(met.getName().equals("<init>")){
						tmpconList.add(new BytecodeConstructorMirror(met.getMethodDescriptor(), met.isPublicAndNonStatic()));
					//}
				}
				
				declaredConstructorsWithoutHiddenArgs = tmpconList.toArray(new ConstructorMirror[0]);
				
			}else{
				declaredConstructorsWithoutHiddenArgs = new ConstructorMirror[0];
			}
			
		}
		return declaredConstructorsWithoutHiddenArgs;
	}

	@Override
	public String[] getInterfaces() throws ClassMirrorNotFoundException {
		return interfaceNames;
	}

	@Override
	public String[] getInterfacesNoEx() {
		return interfaceNames;
	}
	
	public ClassMirror[] getInterfacesM() throws ClassMirrorNotFoundException {
		String[] xxx = this.getInterfaces();
		ClassMirror[] ret = new ClassMirror[xxx.length];
		for(int n=0; n< xxx.length; n++){
			ret[n] = ccm.classForName(xxx[n]);
		}
		return ret;
	}
	
	@Override
	public String getSuperclass() throws ClassMirrorNotFoundException {
		return superName;
	}

	@Override
	public String getSuperclassNoEx() {
		return superName;
	}
	
	@Override
	public boolean isAssignableFrom(ClassMirror c) throws ClassMirrorNotFoundException {
		//System.err.println("isAssignableFrom: " + c.getName());
		
		//Detector d = Detector.getDetector();
		if (this.equals(c)) {
			return true;
		}

		if(null == c) {
			return false;
		}
		
		String sup = c.getSuperclass();
		if(null == sup){
			return false;
		}
		
		ClassMirror supcl = ccm.classForName(sup.replace('/', '.'));
		if (isAssignableFrom(supcl))
			return true;
		for (String icl : c.getInterfaces()) {
			supcl = ccm.classForName(icl.replace('/', '.'));
			if (isAssignableFrom(supcl))
				return true;
		}
		return false;
	}
	
	public ClassMirror getSuperClassMir() throws ClassMirrorNotFoundException{
		return ccm.classForName(this.getSuperclass());
	}
	
	
	private HashMap<String, FieldMirror> fieldsmap = null;
	
	@Override
	public HashMap<String, FieldMirror> getFields() {
		
		if(null == fieldsmap) {
			fieldsmap = new HashMap<String, FieldMirror>();
			for(Thruple<String, String, Boolean> fieldName : fields) {
				String name = fieldName.getA();
				fieldsmap.put(name, new FieldMirror(name, fieldName.getB(), fieldName.getC()));
			}
		}
		
		return fieldsmap;
	}

	// ClassVisitor implementation
	public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
		this.name = name;
		this.superName = superName;
		this.interfaceNames = interfaces == null ? CachedClassMirrors.EMPTY_SET : interfaces;
		
		
		this.isInterface = (access & Opcodes.ACC_INTERFACE) > 0;
		this.isEnum  ="java/lang/Enum".equals(superName);
		
		//processSignature(signature);
	}
	/*
	private void processSignature(String signature) {
		if(signature.startsWith("<")) {
			
			
			
		}
		
		int firstClo = signature.indexOf(">");
		if(firstClo > -1){
			//<X:Ljava/lang/Object;>
			String[] gens = signature.substring(0, firstClo).split(";");
			
			
		}
	}*/

	public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
		if (tmpMethodList == null) {
			tmpMethodList = new ArrayList<BytecodeMethodMirror>();
		}
		
		BytecodeMethodMirror bcmm = new BytecodeMethodMirror(access, name, desc, signature, exceptions);
		tmpMethodList.add(bcmm);
		
		if(name.equals("<init>")){
			constructorArgs = Type.getArgumentTypes(desc);
			return new RemoveHidenArgsMV(bcmm, super.visitMethod(access, name, desc, signature, exceptions)); // null MethodVisitor to avoid examining the instructions.
		}
		return null;
	}
	
	private void addTempConstructor(BytecodeMethodMirror bcmm){
		String newdesc = org.objectweb.asm.Type.getMethodType(Type.getReturnType(bcmm.getMethodDescriptor()), constructorArgs).getDescriptor();
		BytecodeMethodMirror filderedbcmm = new BytecodeMethodMirror(bcmm.access, bcmm.getName(), newdesc, bcmm.getMethodsignature(), bcmm.getExceptionTypes());
		
		if (tmpConstructorList == null) {
			tmpConstructorList = new ArrayList<BytecodeMethodMirror>();
		}
		tmpConstructorList.add(filderedbcmm);
	}
	
	private List<BytecodeMethodMirror> tmpConstructorList; // used only while processing bytecode.
	private org.objectweb.asm.Type[] constructorArgs;
	
	private class RemoveHidenArgsMV extends MethodVisitor{

		private BytecodeMethodMirror bcmm;
		private int currentArg = 0;
		private HashSet<Integer> hiddenArgs;

		public RemoveHidenArgsMV(BytecodeMethodMirror bcmm, MethodVisitor mv) {
			super(Opcodes.ASM7, mv);
			this.bcmm = bcmm;
			hiddenArgs = new HashSet<Integer>();
		}
		
		@Override
		public void visitEnd() {
			
			if(hiddenArgs.size() > 0){
				org.objectweb.asm.Type[] filteredconstructorArgs = new org.objectweb.asm.Type[constructorArgs.length - hiddenArgs.size()];
				int m=0;
				for(int n = 0; n < constructorArgs.length; n++){
					if(!hiddenArgs.contains(n)){
						filteredconstructorArgs[m++] = constructorArgs[n];
					}
				}
				constructorArgs = filteredconstructorArgs;
			}

			addTempConstructor(bcmm);
			super.visitEnd();
		}
		
		@Override
		public AnnotationVisitor visitParameterAnnotation(int parameter, String desc, boolean visible) {
			AnnotationVisitor av = super.visitParameterAnnotation(parameter, desc, visible);
			if(desc.equals("Lcom/concurnas/lang/ParamName;")){
				currentArg++;
				return new AnnotationVisitorHiddenArgRemove(av);
			}
			else if(desc.equals("Lcom/concurnas/lang/SyntheticParam;")){
				hiddenArgs.add(currentArg);
			}
			return null;//?
		}
		
		private class AnnotationVisitorHiddenArgRemove extends AnnotationVisitor{

			public AnnotationVisitorHiddenArgRemove(AnnotationVisitor av) {
				super(Opcodes.ASM7, av);
			}
			
		    public void visit(String name, Object value) {
		    	if(value instanceof String && ((String)value).contains("$n")) {
		    		hiddenArgs.add(currentArg-1);
		    	}
		    	super.visit(name, value);//adjust arguments as approperiate
		    }
		}
	}

	public void visitEnd() {
		if (tmpMethodList != null) {
			declaredMethods = new MethodMirror[tmpMethodList.size()];
			int i = 0;
			for (MethodMirror mm : tmpMethodList) {
				declaredMethods[i++] = mm;
			}
			tmpMethodList = null;
		}
		if (tmpConstructorList != null) {
			filteredConstructors = new MethodMirror[tmpConstructorList.size()];
			int i = 0;
			for (MethodMirror mm : tmpConstructorList) {
				filteredConstructors[i++] = mm;
			}
			tmpConstructorList = null;
		}
	}

	// Dummy methods

	public void visitSource(String source, String debug) {
	}

	public void visitOuterClass(String owner, String name, String desc) {
		//System.err.println("/outer Class: "+ name+" defined in "+owner);
	}

	private ArrayList<String> annotations = new ArrayList<String>();
	
	public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
		annotations.add(desc.substring(1, desc.length()-1));
		return DummyAnnotationVisitor.singleton;
	}

	public void visitAttribute(Attribute attr) {
	}

	private int nestingLevel = 0;
	
	public void visitInnerClass(String name, String outerName, String innerName, int access) {
		//System.err.println("Inner Class: "+ innerName+" defined in "+outerName);
		if(!Modifier.isStatic(access) && name.length() <= this.name.length()){
			nestingLevel++;
		}
	}
	

	@Override
	public int isNestedClass() {
		return nestingLevel;
	}
	
	@Override
	public boolean isEnum() {
		return isEnum;
	}

	private ArrayList<Thruple<String, String, Boolean>> fields = new ArrayList<Thruple<String, String, Boolean>>();
	
	private class CCMFieldVisitor extends FieldVisitor{
		private final String name;
		private final String desc;
		
		public CCMFieldVisitor(final FieldVisitor fv, String name, String desc) {
			super(Opcodes.ASM7,fv);
			this.name = name;
			this.desc = desc;
		}
		
		private boolean isShared = false;

		public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
			if(!isShared && desc.equals("Lcom/concurnas/lang/Shared;")) {
				isShared=true;
			}
			return null;
		}
		
		 public void visitEnd() {
	        if (fv != null) {
	            fv.visitEnd();
	        }
	        
	       /* if(!this.isShared) {
	        	if(this.desc.startsWith("L")) {
	        		String className = this.desc.substring(1,  this.desc.length()-1).replace("/", ".");
	        		try {
						for(String x : ccm.classForName(className).getAnnotations()) {
							if(x.equals("com/concurnas/lang/Shared")) {
								this.isShared=true;
								break;
							}
						}
					} catch (Throwable e) {
					}
	        		
	        	}
	        	
	        }*/
	        
	        fields.add(new Thruple<String, String, Boolean>(this.name, this.desc, this.isShared));
	    }
		
	}
	
	public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
		return new CCMFieldVisitor(super.visitField(access, name, desc, signature, value), name, desc);
	}

	static class DummyAnnotationVisitor extends AnnotationVisitor {
		public DummyAnnotationVisitor() {
			super(Opcodes.ASM7);
		}

		static DummyAnnotationVisitor singleton = new DummyAnnotationVisitor();

		public void visit(String name, Object value) {
		}

		public AnnotationVisitor visitAnnotation(String name, String desc) {
			return this;
		}

		public AnnotationVisitor visitArray(String name) {
			return DummyAnnotationVisitor.singleton;
		}

		public void visitEnd() {
		}

		public void visitEnum(String name, String desc, String value) {
		}
	}

	@Override
	public HashSet<String> getAnnotations() {
		return new HashSet<String>(annotations);//.toArray(new String[0]);
	}

	@Override
	public FieldMirror getField(String field) {
		return this.getFields().get(field);
	}

	@Override
	public MethodMirror getDeclaredMethod(String name, String desc) {
		MethodMirror[] mm = this.getDeclaredMethods();
		for(MethodMirror m : mm) {
			if(m.getName().equals(name) && m.getMethodDescriptor().equals(desc)) {
				return m;
			}
		}
		
		return null;
	}
}

class BytecodeConstructorMirror implements ConstructorMirror{
	private String desc;
	private boolean publicc;

	public BytecodeConstructorMirror(String desc, boolean publicc){
		this.desc = desc;
		this.publicc=publicc;
	}
	
	@Override
	public String getMethodDescriptor() {
		return desc;
	}

	@Override
	public boolean isPublic() {
		return publicc;
	}
}

class BytecodeMethodMirror implements MethodMirror {

	private final String[] exceptions;
	private final String desc;
	private final String signature;
	private final String name;
	private final boolean isBridge;
	private final boolean isFinal;
	private final boolean isPublicAndNonStatic;
	public final int access;

	public BytecodeMethodMirror(int access, String name, String desc, String signature, String[] exceptions) {
		this.name = name;
		this.desc = desc;
		this.access = access;
		this.signature = signature;
		this.exceptions = (exceptions == null) ? CachedClassMirrors.EMPTY_SET : exceptions;
		isBridge = (access & Opcodes.ACC_BRIDGE) > 0;
		isFinal = Modifier.isFinal(access);
		isPublicAndNonStatic = Modifier.isPublic(access) && !Modifier.isStatic(access);
	}

	public String getName() {
		return name;
	}

	public String[] getExceptionTypes() /*throws ClassMirrorNotFoundException*/ {
		return exceptions;
	}

	public String getMethodDescriptor() {
		return desc;
	}
	
	public boolean isPublicAndNonStatic(){
		return isPublicAndNonStatic;
	}
	
	public String getMethodsignature() {
		return signature;
	}

	public boolean isBridge() {
		return isBridge;
	}

	@Override
	public boolean hasBeenConced() {
		return true;
	}
	
	public boolean isFinal(){
		return isFinal;
	}
}
