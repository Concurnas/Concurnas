/* Copyright (c) 2006, Sriram Srinivasan
 *
 * You may distribute this software under the terms of the license 
 * specified in the file "License"
 */

package com.concurnas.runtime.cps.analysis;

import static com.concurnas.runtime.cps.Constants.D_FIBER;
import static com.concurnas.runtime.cps.Constants.STATE_CLASS;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Handle;
import org.objectweb.asm.MethodTooLargeException;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.IincInsnNode;
import org.objectweb.asm.tree.InnerClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

import com.concurnas.runtime.BcUtils;
import com.concurnas.runtime.ConcClassUtil;
import com.concurnas.runtime.cps.CPSException;
import com.concurnas.runtime.cps.Constants;
import com.concurnas.runtime.cps.mirrors.CachedClassMirrors;
import com.concurnas.runtime.cps.mirrors.Detector;

/**
 * This class is the main entry point for the Weave tool. It uses ClassFlow to
 * parse and analyze a class file, and writes out a CPS transformed file if
 * needed
 */
public class Fiberizer implements Opcodes {
	public ClassFlow classFlow;
	
	private final boolean augmentJdk; 
	
	List<ClassInfo> classInfoList = new LinkedList<ClassInfo>();
	/*static ThreadLocal<HashMap<String, ClassInfo>> stateClasses_ = new ThreadLocal<HashMap<String, ClassInfo>>() {
		//TODO: is threadlocal a good idea?
		protected HashMap<String, ClassInfo> initialValue() {
			return new HashMap<String, ClassInfo>();
		}
	};*/
	
	private HashMap<String, ClassInfo> stateClasses = new HashMap<String, ClassInfo>();
	public void reset() {
		//stateClasses_.set(new HashMap<String, ClassInfo>());
		stateClasses = new HashMap<String, ClassInfo>();
	}

	// private final ClassLoader classLoader;
	private final ConcClassUtil clsll;
	private final byte[] origData;
	private final boolean verbose;

    public static class OrigCode extends ClassVisitor{

		public OrigCode() {
			super(Opcodes.ASM7);
		}
		
		public HashMap<String, MethodNode> nameToMN = new HashMap<String, MethodNode>();
		
	    @Override
	    @SuppressWarnings( { "unchecked" })
	    public MethodVisitor visitMethod(
	            final int access,
	            final String name,
	            final String desc,
	            final String signature,
	            final String[] exceptions)
	    {
	    	MethodNode mn = new MethodNode(access, name, desc, signature, exceptions);
	        //super.methods.add(mn);
	    	nameToMN.put(name+desc, mn);
	        return mn;
	    }
    }
	
    public HashSet<String> methodsNeedingInvokeDynamicMapping = new HashSet<String>();
    
    private final OrigCode origCode = new OrigCode();
    private final OrigCode origCode2 = new OrigCode();
    private final OrigCode origCode3 = new OrigCode();

   // private final boolean noPausableMethods;
    
	public Fiberizer(byte[] data, ConcClassUtil clsll, final boolean augmentJdk/*, Detector d*/, final boolean verbose/*, final boolean noPausableMethods*/) {
		classFlow = new ClassFlow(data, new Detector(new CachedClassMirrors(clsll/*, d*/)), origCode, origCode2, origCode3);
		// this.classLoader = clloader;
		this.clsll = clsll;
		this.origData = data;
		this.augmentJdk = augmentJdk;
		this.verbose = verbose;
		//this.noPausableMethods = noPausableMethods;
		
	}

	public byte[] weave() throws CPSException {
		try {
			classFlow.analyze(false);
			if ( needsWeaving() && classFlow.isPausable() && !isAnnotation()) {
				boolean computeFrames = (classFlow.version & 0x00FF) >= 50;
				boolean failed = true;
				ClassWriter cw=null;
				byte[] ret = null;
				while(failed){//if we failed to cps a method then we have another go until we get it right, ill method is marked for non concurnifiation
					cw = new com.concurnas.runtime.cps.analysis.ConcClassWriter(ClassWriter.COMPUTE_MAXS|ClassWriter.COMPUTE_FRAMES, classFlow.detector());
					failed = accept(cw);
					
					try {
						ret = cw.toByteArray();
					}catch(MethodTooLargeException mtle){
						failed=true;
						String desc = mtle.getDescriptor();
						forEasyWeaving.add(mtle.getMethodName() + desc);
						int idxOf = desc.lastIndexOf("Lcom/concurnas/bootstrap/runtime/cps/Fiber;)");
						desc = desc.substring(0, idxOf) + desc.substring(idxOf+43);
						
						forEasyWeaving.add(mtle.getMethodName() + desc);
					}
				}
				
				
				//if above fails then skip ill method
				
				if(!this.methodsNeedingInvokeDynamicMapping.isEmpty()){//map invoke dynamic calls with fiber method
					try {
						ClassReader cr = new ClassReader(ret);
						cw = new com.concurnas.runtime.cps.analysis.ConcClassWriter(ClassWriter.COMPUTE_MAXS|ClassWriter.COMPUTE_FRAMES, classFlow.detector());
						InvokeDynamicRepointerCV repointer = new InvokeDynamicRepointerCV(cw, this.methodsNeedingInvokeDynamicMapping);
						cr.accept(repointer, 0);
						ret = cw.toByteArray();
					}catch(Throwable thr) {
						
					}
				}
				
				addClassInfo(new ClassInfo(classFlow.getClassName(), ret));
				return ret;
			}
		}catch(Exception e) {
			e.printStackTrace();
			//
		}
		
		return this.origData;
	}
	
	private static class InvokeDynamicRepointerCV extends ClassVisitor{

		private HashSet<String> methodsNeedingInvokeDynamicMapping;

		public InvokeDynamicRepointerCV( ClassVisitor cv, HashSet<String> methodsNeedingInvokeDynamicMapping) {
			super(ASM7, cv);
			this.methodsNeedingInvokeDynamicMapping = methodsNeedingInvokeDynamicMapping;
		}
		
		public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
			MethodVisitor ret = cv.visitMethod(access, name, desc, signature, exceptions);
			if(this.methodsNeedingInvokeDynamicMapping.contains(name + desc)) {
				ret = new InvokeDynamicRepointer(ret);
			}
			
			return ret;
		}
		
	    private static class InvokeDynamicRepointer extends MethodVisitor implements Opcodes{

			public InvokeDynamicRepointer(MethodVisitor mv) {
				super(ASM7, mv);
			}
			
			public static String replaceLast(String string, String toReplace, String replacement) {
			    int pos = string.lastIndexOf(toReplace);
			    if (pos > -1) {
			        return string.substring(0, pos)
			             + replacement
			             + string.substring(pos + toReplace.length(), string.length());
			    } else {
			        return string;
			    }
			}
			
			private static String appendFiber(String desc) {
				Type[] args = Type.getArgumentTypes(desc);
				if(args.length == 0 || !args[args.length-1].getDescriptor().equals(D_FIBER)) {
					desc = replaceLast(desc, ")", D_FIBER + ')');
				}
				return desc;
			}
			/*
			 * private void clinitReplace() { if(Fiber.getCurrentFiber() != null) { String a
			 * = "fiberized version"; }else { String a = "unfiberized version"; } }
			 */
			
			public void visitInvokeDynamicInsn(String name, String desc, Handle bsm, Object... bsmArgs) {
				Object[] newargs = new Object[bsmArgs.length];
				for (int n = 0; n < bsmArgs.length; n++) {
					Object item = bsmArgs[n];
					if (item instanceof Handle) {
						Handle asHandle = (Handle) item;
						item = new Handle(asHandle.getTag(), asHandle.getOwner(), asHandle.getName(), appendFiber(asHandle.getDesc()));
					} else if (item instanceof Type) {
						Type asT = (Type) item;
						item = Type.getType(appendFiber(asT.getDescriptor()));
					}
					newargs[n] = item;
				}
				mv.visitInvokeDynamicInsn(name, desc, bsm, newargs);
			}
			
			
			
			/*
			 * public void visitInvokeDynamicInsn(String name, String desc, Handle bsm,
			 * Object... bsmArgs) { if(isClinit) {//better to do this once for whole method
			 * as opposed to each invokedynamiccall mv.visitLabel(new Label());
			 * mv.visitMethodInsn(INVOKESTATIC, "com/concurnas/bootstrap/runtime/cps/Fiber",
			 * "getCurrentFiber", "()Lcom/concurnas/bootstrap/runtime/cps/Fiber;", false);
			 * Label unfiberized = new Label(); mv.visitJumpInsn(IFNULL, unfiberized);
			 * mv.visitLabel(new Label());
			 * 
			 * 
			 * mv.visitFieldInsn(GETSTATIC, "java/lang/System", "err",
			 * "Ljava/io/PrintStream;"); mv.visitLdcInsn("Fiber is NOT null");
			 * mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println",
			 * "(Ljava/lang/String;)V", false);
			 * 
			 * augmentInvokeDynamic(name, desc, bsm, bsmArgs); Label after = new Label();
			 * mv.visitJumpInsn(GOTO, after); mv.visitLabel(unfiberized);
			 * 
			 * mv.visitFieldInsn(GETSTATIC, "java/lang/System", "err",
			 * "Ljava/io/PrintStream;"); mv.visitLdcInsn("Fiber is null");
			 * mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println",
			 * "(Ljava/lang/String;)V", false);
			 * 
			 * mv.visitInvokeDynamicInsn(name, desc, bsm, bsmArgs); mv.visitLabel(after);
			 * }else { augmentInvokeDynamic(name, desc, bsm, bsmArgs); } }
			 */
			
		}
	}
	
	private void addExtraFibArgAndIncArguments(MethodNode m){
		 int incon = BcUtils.incArgPointsFromDesc(m.desc, m.access);
		 
		 m.desc = m.desc.replace(")", D_FIBER + ')');
        if (m.signature != null){ m.signature = m.signature.replace(")", D_FIBER + ')');};
        
        InsnList newInstructions = new InsnList();
        
        for(AbstractInsnNode inst : m.instructions.toArray()){
        	//these are the only two which require incrementing the arguments for...
        	if(inst instanceof IincInsnNode){
        		IincInsnNode asInc = (IincInsnNode)inst;
        		inst = new IincInsnNode(asInc.var + (asInc.var>=incon?1:0), asInc.incr);//increment var if incon
        	}
        	else if(inst instanceof VarInsnNode){
        		VarInsnNode asVar = (VarInsnNode)inst;
        		inst = new VarInsnNode(asVar.getOpcode(), asVar.var + (asVar.var>=incon?1:0));
        	}
        	
        	newInstructions.add(inst);
        }
        
        m.instructions = newInstructions;

        //add extra annotation param with respect to fiber
        if(m.visibleParameterAnnotations == null) {
        	List<AnnotationNode>[] item = new ArrayList[] {null};
        	m.visibleParameterAnnotations = item;
        	m.visibleAnnotableParameterCount=1;
        }else {
        	List<AnnotationNode>[] item = new ArrayList[m.visibleParameterAnnotations.length+1];
        	int n=0;
        	for(; n < m.visibleParameterAnnotations.length; n++) {
        		item[n] = m.visibleParameterAnnotations[n];
        	}
        	m.visibleParameterAnnotations = item;
        	m.visibleAnnotableParameterCount = item.length;
        }
	}
	
	
	/*private static class SyncMethodRewriter extends MethodVisitor{
		private final int incAfter;//arg after which we must increment by 1 and also the position of the fiber
		private final String selfclassname;
		public SyncMethodRewriter(MethodVisitor mv, int incAfter, String selfclassname) {
			super(ASM7, mv);
			this.incAfter=incAfter;
			this.selfclassname=selfclassname;
		}

		@Override
		public void visitVarInsn(int opcode, int var) {
			super.visitVarInsn(opcode, var >= incAfter ? var+1 : var);
		}
		
		@Override
	    public void visitMaxs(int maxStack, int maxLocals) {
	    	super.visitMaxs(maxStack, maxLocals >= incAfter ? maxLocals+1 : maxLocals);
	    }
		@Override
		public void visitIincInsn(int var, int increment) {
			super.visitIincInsn(var >= incAfter ? var+1 : var, increment);
		}
		
		public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
			if(owner.equals("com/concurnas/bootstrap/runtime/cps/Fiber") || (owner.equals(selfclassname) && !name.equals("<init>"))){
				desc = desc.replace(")", FIBER_SUFFIX);
				mv.visitVarInsn(ALOAD, incAfter);
			}
			super.visitMethodInsn(opcode, owner, name, desc, itf);
		}
		
		//inc fellas and link to fiber
	}*/
	
	
	/**
	 * Annoated Uninterruptible or a native method
	 */
	private void addFiberForAnnotUninteruptableOrNativeOrConstructor(final MethodFlow m, final ClassVisitor cv){
		String methodName = m.name;
		String desc = m.desc;
		String augmentedDesc = desc.replace(")", FIBER_SUFFIX);
		
		boolean isStatic = Modifier.isStatic(m.access);
		
		MethodVisitor mv = cv.visitMethod(ACC_PUBLIC + (isStatic?ACC_STATIC:0), methodName, augmentedDesc, null, null);
		
		{
			if(null != m.visibleAnnotations){//TOOD: this is a bit rubbish, as the arguments going to the annotations are not preserved
				for(AnnotationNode annot : m.visibleAnnotations){
					if(annot.desc.equals(HotSpotIntrinsicCandidate_ANNOTATION)) {
						continue;//scrub out this annotation
					}
					
					AnnotationVisitor av0 = mv.visitAnnotation(annot.desc, true);
					av0.visitEnd();
				}
			}
		}
		
		mv.visitCode();
		
		int locVar=0;
		
		if(!isStatic){//not static
			mv.visitVarInsn(ALOAD, 0);
			locVar=1;
		}
		
		for(char c : ANFTransform.getPrimOrObj(desc)){//reload the stack from vars
			mv.visitVarInsn(ANFTransform.getLoadOp(c), locVar++);
			if(c == 'D' || c=='J'){
				locVar++;
			}
		}
		
		String name = m.classFlow.name;
		
		int opcode = isStatic? INVOKESTATIC : INVOKEVIRTUAL;
		
		if(methodName.equals("<init>")) {
			opcode = INVOKESPECIAL;
		}
		
		mv.visitMethodInsn( opcode, name, methodName, desc, false);
		
		
		String dd = Type.getMethodType(desc).getReturnType().getDescriptor();
		mv.visitInsn(dd.startsWith("[")?ARETURN:ANFTransform.getReturnOp(desc.charAt(desc.length()-1)));//return
		mv.visitMaxs(1, 20);
		mv.visitEnd();
	}
	
	
	//private HashSet<String> fiberizationAdded = new HashSet<String>();
	private HashSet<String> addedExtaArgForNoFibNeeded = new HashSet<String>();
	//private HashSet<String> addFiberNullingOnFailedCase = new HashSet<String>();
	
	private static final String UNINTERUPTABLE_ANNOTATION = "Lcom/concurnas/lang/Uninterruptible;";
	public static final String HotSpotIntrinsicCandidate_ANNOTATION = "Ljdk/internal/HotSpotIntrinsicCandidate;";
	
	private static final ArrayList<String> excludePathCorouteen = new ArrayList<String>(1);
	static{
		excludePathCorouteen.add("org/objectweb/asm");
		
	}
	
	private boolean accept(final ClassVisitor cv) {
		ClassFlow cf = classFlow;
		// visits header
		
		
		String[] interfaces = toStringArray(cf.interfaces);
		String className = cf.name;
		cv.visit(cf.version, cf.access, className, cf.signature, cf.superName, interfaces);
		// visits source
		if (cf.sourceFile != null || cf.sourceDebug != null) {
			cv.visitSource(cf.sourceFile, cf.sourceDebug);
		}
		// visits outer class
		if (cf.outerClass != null) {
			cv.visitOuterClass(cf.outerClass, cf.outerMethod, cf.outerMethodDesc);
		}
		// visits attributes and annotations
		int i, n;
		AnnotationNode an;
		n = cf.visibleAnnotations == null ? 0 : cf.visibleAnnotations.size();

		boolean isUninteruptableAtClassLevel = false;
		
		for(String excl : excludePathCorouteen){
			if(className.startsWith(excl)){
				isUninteruptableAtClassLevel=true;
				break;
			}
		}
		
		if(!isUninteruptableAtClassLevel){
			for (i = 0; i < n; ++i) {
				an = (AnnotationNode) cf.visibleAnnotations.get(i);
				String desc = an.desc;
				if(desc.equals(UNINTERUPTABLE_ANNOTATION)){
					isUninteruptableAtClassLevel = true;
				}
				an.accept(cv.visitAnnotation(desc, true));
			}
		}
		
		n = cf.invisibleAnnotations == null ? 0 : cf.invisibleAnnotations.size();
		for (i = 0; i < n; ++i) {
			an = (AnnotationNode) cf.invisibleAnnotations.get(i);
			an.accept(cv.visitAnnotation(an.desc, false));
		}

		n = cf.attrs == null ? 0 : cf.attrs.size();
		for (i = 0; i < n; ++i) {
			cv.visitAttribute((Attribute) cf.attrs.get(i));
		}
		// visits inner classes
		for (i = 0; i < cf.innerClasses.size(); ++i) {
			((InnerClassNode) cf.innerClasses.get(i)).accept(cv);
		}
		// visits fields
		for (i = 0; i < cf.fields.size(); ++i) {
			((FieldNode) cf.fields.get(i)).accept(cv);
		}
		
		if(null != cf.nestMembers) {
			for(String mem : cf.nestMembers) {
				cv.visitNestMember(mem);
			}
		}
		
		
		if(cf.nestHostClass != null) {
			cv.visitNestHost(cf.nestHostClass);
		}
		
		
		/*
		 * Mark this class as "processed" by adding a dummy field, so that we
		 * don't weave an already woven file
		 */
		//cv.visitField(ACC_PUBLIC | ACC_STATIC | ACC_FINAL, WOVEN_FIELD, "Z", "Z", Boolean.TRUE);
		// visits methods
		for (i = 0; i < cf.methods.size(); ++i) {
			MethodFlow m = (MethodFlow) cf.methods.get(i);
			boolean isUninteruptable=false;
			boolean isHotSpotIntrinsic=false;
			if(!isUninteruptableAtClassLevel && m.visibleAnnotations != null){//if not already Uninterruptible, check individual methods
				for(AnnotationNode axn : m.visibleAnnotations){
					if(axn.desc.equals(UNINTERUPTABLE_ANNOTATION)){
						isUninteruptable=true;
						break;
					}
					if(axn.desc.equals(HotSpotIntrinsicCandidate_ANNOTATION)) {//
						isHotSpotIntrinsic=true;
						break;
					}
				}
			}
			
			if(m.name.equals("<init>")){
				addFiberForAnnotUninteruptableOrNativeOrConstructor(m, cv);
			}
			
			if ((onlyMethods==null || onlyMethods.contains(m.name)) && needsWeaving(m) ) {
				MethodNode orig = origCode.nameToMN.get(m.name+m.desc);
				orig.accept(cv);
				//add fiber variant
				String uName = m.getUniqueName();
				
				if(isHotSpotIntrinsic || isUninteruptable || isUninteruptableAtClassLevel || forEasyWeaving.contains(uName) || Modifier.isNative(m.access) ){
					//augment call of method above with fiber argument then ignore it
					//redirect to origonal method invokation - same for native as well so as to preserve bindings
					addFiberForAnnotUninteruptableOrNativeOrConstructor(m, cv);
				}
				/*else if(Modifier.isSynchronized(m.access)){
					//System.err.println("sync in: " + uName);
					addSynchonizeMethodHandler(orig, cv, cf.name);
				}*/
				else{
					MethodWeaver mw = new MethodWeaver(this, m, augmentJdk);
					if(mw.noFibizationToBeAdded() ){//this case for when we are adding the fiber argument, which will not be used for anything (of course we need to increment local variable calls)
						MethodNode orig2 = origCode2.nameToMN.get(m.name+m.desc);
						if(!addedExtaArgForNoFibNeeded.contains(m.name+m.desc)){
							addExtraFibArgAndIncArguments(orig2);//mutates state yuck
							addedExtaArgForNoFibNeeded.add(m.name+m.desc);//only augment once
						}
						orig2.accept(cv);
					}
					else{
						mw.accept(cv);//does weaving
					}
					
					if(augmentJdk && mw.failed ){
						if(this.verbose){
							System.out.println(String.format("WARN: Unable to augment method: %s %s of %s", m.name, m.desc, m.classFlow.name) );
						}
						//if we fail then we need to have to make this method for easyWeaving and start again...
						forEasyWeaving.add(uName);//mak it for next attempt
						return true;
					}
				}
			} else {
				MethodNode orig3 = origCode3.nameToMN.get(m.name+m.desc);//but yucky because we dont have a clone function on MethodNode
				
				/*if(shouldAddAlt){
					//we couldn't conc it, so... set the current fiber to nothing [com.concurnas.bootstrap.runtime.cps.Fiber.currentFiber] and add the code above IF one of the power3
					if(!addFiberNullingOnFailedCase.contains(m.name+m.desc)){
						addFibNullOnFailureCode(orig3);
						addFiberNullingOnFailedCase.add(m.name+m.desc);//only augment once
					}
				}*/
				orig3.accept(cv);
				
				//m.restoreNonInstructionNodes();
				//m.accept(cv);
			}
			
			if(m.hasInvokeDynamicCall){
				if(m.name.equals("<clinit>")) {
					//System.err.println("lambda in clinit: " + className);
					//methodsNeedingInvokeDynamicMapping.add(m.name + m.desc);
				}else {
					methodsNeedingInvokeDynamicMapping.add(m.name + m.desc.replace(")", D_FIBER + ')'));
				}
			}
			
		}
		cv.visitEnd();
		return false;
	}
	
	private final HashSet<String> forEasyWeaving = new HashSet<String>(); //if fiberization would make it too large

	public HashSet<String> onlyMethods = null;//limit analysis to only these methods

	@SuppressWarnings(value = { "unchecked" })
	static String[] toStringArray(List list) {
		String[] array = new String[list.size()];
		list.toArray(array);
		return array;
	}

	void addClassInfo(ClassInfo ci) {
		classInfoList.add(ci);
	}

	public List<ClassInfo> getClassInfos() {
		return classInfoList;
	}

	/*
	 * A method needs weaving ordinarily if it is marked pausable. However, if
	 * there exists another method with the same name and parameters and an
	 * additional Fiber parameter as the last one, then this method doesn't need
	 * weaving. Examples are kilim.Task.yield and kilim.Task.sleep
	 */
	public static String FIBER_SUFFIX = D_FIBER + ')';

	boolean needsWeaving(MethodFlow mf) {
		List<AnnotationNode> methodAnnotations = mf.visibleAnnotations;
		boolean forceFiber = false; 
		if(methodAnnotations != null){
			for(AnnotationNode an : methodAnnotations){
				if(an.desc.equals("Lcom/concurnas/runtime/ForceCPS;")){
					forceFiber = true;
				}
				break;
			}
		}
		if ( (!mf.isPausable() || mf.name.equals("<clinit>") || mf.name.equals("<init>") || mf.desc.endsWith(FIBER_SUFFIX)) && !forceFiber ){
			return false;
		}
		String fdesc = mf.desc.replace(")", FIBER_SUFFIX);
		for (MethodFlow omf : classFlow.getMethodFlows()) {
			if (omf == mf){ continue; }
			if (mf.name.equals(omf.name) && fdesc.equals(omf.desc)) {
				return false;
			}
		}
		return true;
	}

	private boolean isAnnotation(){
		List<String> ifaces = classFlow.interfaces;
		if(ifaces != null){
			for(String ifa : ifaces){
				if(ifa.equals("java/lang/annotation/Annotation")){
					return true;
				}
			}
		}
		return false;
	}
	
	boolean needsWeaving() {
		if (classFlow.isWoven)
			return false;
		for (MethodFlow mf : classFlow.getMethodFlows()) {
			if (needsWeaving(mf))
				return true;
		}
		return false;
	}

	/**
	 * Create a custom class (structure) to hold the state. The name of the
	 * state reflects the numbers of the various VMtypes in valInfoList. class
	 * kilim.SO2IJ3 reflects a class that stores two Objects one Integer and 3
	 * longs.
	 * 
	 * <pre>
	 *            class kilim.SO2IJ3 extends kilim.State {
	 *               public Object f1, f2;
	 *               public int f3;
	 *               public long f4, f5, f6;
	 *            }
	 * </pre>
	 * 
	 * If there's no data to store, we use the kilim.State class directly to
	 * store the basic amount of data necessary to restore the stack.
	 */

	String createStateClass(ValInfoList valInfoList) {
		int numByType[] = { 0, 0, 0, 0, 0 };
		for (ValInfo vi : valInfoList) {
			numByType[vi.vmt]++;
		}
		String className = makeClassName(numByType);
		ClassInfo classInfo = null;
		//classInfo = stateClasses_.get().get(className);
		classInfo = stateClasses.get(className);
		if (classInfo == null) {
			ClassWriter cw = new com.concurnas.runtime.cps.analysis.ConcClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS, classFlow.detector());
			cw.visit(V1_8, ACC_PUBLIC | ACC_FINAL, className, null, Constants.STATE_CLASS, null);

			// Create default constructor
			// <init>() {
			// super(); // call java/lang/Object.<init>()
			// }
			MethodVisitor mw = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
			mw.visitVarInsn(ALOAD, 0);
			mw.visitMethodInsn(INVOKESPECIAL, STATE_CLASS, "<init>", "()V", false);
			mw.visitInsn(RETURN);
			// this code uses a maximum of one stack element and one local
			// variable
			mw.visitMaxs(1, 1);
			mw.visitEnd();
			// create fields of the appropriate type.
			for (ValInfo vi : valInfoList) {
				cw.visitField(ACC_PUBLIC, vi.fieldName, vi.fieldDesc(), null, null);
			}
			byte[] code = cw.toByteArray();
			synchronized(this.clsll) {
				HashMap<String, byte[]> cpsc = this.clsll.getCpsStateClasses();
				if(null!= cpsc){
					cpsc.put(className, code);
				}
			}
			
			classInfo = new ClassInfo(className, code);
			//stateClasses_.get().put(className, classInfo);
			stateClasses.put(className, classInfo);
		}
		if (!classInfoList.contains(classInfo))
			addClassInfo(classInfo);
		return className;
	}

	private String makeClassName(int[] numByType) {
		StringBuilder sb = new StringBuilder(30);
		sb.append("com/concurnas/S_");
		for (int t = 0; t < 5; t++) {
			int c = numByType[t];
			if (c == 0)
				continue;
			sb.append(VMType.abbrev[t]);
			if (c > 1) {
				sb.append(c);
			}
		}
		return sb.toString();
	}

/*	private boolean isInterface() {
		return classFlow.isInterface();
	}*/
}
