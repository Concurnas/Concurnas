package com.concurnas.runtime;

import java.util.HashSet;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import com.concurnas.runtime.cps.analysis.ConcClassWriter;


/**
 * figure out if extends IsoTask, if it does:
 * 
 * Add: 'setupGlobals(Fiber isoFiber)' and 'teardownGlobals' 
 * setupGlobals has current fiber set to the one which is scheduling the task, isoFiber gets passed in.
 * teardownGlobals has current fiber set to the one which we're shutting down [isofiber]
 *
 * blow up if defined already
 *
 * methods are will be gennerated such that all globals which can be referenced by the code path are instantiated (via copy and sets on globals)
 * to approperiate values for the fiber upon which the task is being gennerated - found via static code analysis as par below. ignores Primordials
 *
 */
public final class IsoTaskMethodAdder implements Opcodes {

	private final byte[] data;
	private final String targetClassName;
	private final ConcClassUtil clsll;

	public IsoTaskMethodAdder(byte[] data, String targetClassName, ConcClassUtil clsll) {
		this.data = data;
		this.targetClassName = targetClassName;
		this.clsll = clsll;
	}
	
	private static class IsoTaskFinder extends ClassVisitor{

		private final String targetClassName;
		
		public IsoTaskFinder(String targetClassName) {
			super(ASM7);
			this.targetClassName = targetClassName;
		}
		
		public boolean isIsoTask = false;
		public boolean isAwait = false;
		
		private static final HashSet<String> isoTaskSuperTypes = new HashSet<String>();
		static{
			isoTaskSuperTypes.add("com/concurnas/bootstrap/runtime/cps/IsoTaskAwait");
			isoTaskSuperTypes.add("com/concurnas/bootstrap/runtime/cps/IsoTaskEvery");
			isoTaskSuperTypes.add("com/concurnas/bootstrap/runtime/cps/IsoTask");
			isoTaskSuperTypes.add("com/concurnas/bootstrap/runtime/cps/AbstractIsoTask");
			isoTaskSuperTypes.add("com/concurnas/bootstrap/runtime/cps/IsoTaskNotifiable");
		}
		
	    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
	    	if(this.targetClassName.equals(name)){//TODO: is this name check needed - yes! 
	    		isAwait = "com/concurnas/bootstrap/runtime/cps/IsoTaskAwait".equals(superName);
	    		isIsoTask = isoTaskSuperTypes.contains(superName) && !isoTaskSuperTypes.contains(name); //ensure this applies to only 
	    	}
	    	super.visit(version, access, name, signature, superName, interfaces);
	    }
	}

	public static class GetGlobalClassesReferenced extends ClassVisitor implements Opcodes {

		private class MethodAnalayser extends MethodVisitor{

			public MethodAnalayser(MethodVisitor mv) {
				super(ASM7, mv);
			}
			
			public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
				super.visitMethodInsn(opcode, owner, name, desc, itf);
				if(opcode == INVOKESTATIC && owner.endsWith("$Globals$")){
					globs.add(owner);
				}
			}
			
		}
		
		private HashSet<String> globs;

		public GetGlobalClassesReferenced(HashSet<String> globs) {
			super(ASM7);
			this.globs = globs;
		}
		
	    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
	    	return new MethodAnalayser(super.visitMethod(access, name, desc, signature, exceptions));
	    }
		
	}
	
	
	private HashSet<String> obtainGlobalDependancies(String className, ClassReader cr){
		HashSet<String> got = this.clsll.getClassToGlobalDependancies().get(className);
		if(null == got){
			got = new HashSet<String>();//create first so as to avoid inf loop
			this.clsll.getClassToGlobalDependancies().put(className, got);
			
			GetGlobalClassesReferenced ggcr = new GetGlobalClassesReferenced(got);
			cr.accept(ggcr, 0);
			
			for(String dep : got){//transitive
				String depNoGlob = dep.substring(0, dep.length() - 9);//preglobal version since we've not transformed that code yet
				depNoGlob = depNoGlob.replaceAll("/", ".");
				byte[] derpdata = clsll.getBytecode(depNoGlob);
				got.addAll(obtainGlobalDependancies(dep, new ClassReader(derpdata)));
			}
		}
		return got;
	}

    private class MethodAdder extends ClassVisitor {

    	private final HashSet<String> dependancies;
    	private final boolean isAwait;
    	
        private void addMethodTearDown(){
    		MethodVisitor mv = super.visitMethod(ACC_PUBLIC, "teardownGlobals", "()V", null, null);
    		mv.visitCode();
    		
    		for(String d : dependancies){
        		Label l0 = new Label();
        		mv.visitLabel(l0);
        		mv.visitMethodInsn(INVOKESTATIC, d, "removeInstance?", "()V", false);
    		}
    		
    		Label l1 = new Label();
    		mv.visitLabel(l1);
    		mv.visitInsn(RETURN);
    		mv.visitMaxs(0, 1);
    		mv.visitEnd();
        }
        
        private void addMethodSetupGlobals(){
    		MethodVisitor mv = super.visitMethod(ACC_PUBLIC, "setupGlobals", "(Lcom/concurnas/bootstrap/runtime/cps/Fiber;Lcom/concurnas/bootstrap/runtime/CopyTracker;)Ljava/util/HashSet;", "(Lcom/concurnas/bootstrap/runtime/cps/Fiber;Lcom/concurnas/runtime/ConcurnificationTracker;)Ljava/util/HashSet<Ljava/lang/String;>;", null);
    		
    		{
    			AnnotationVisitor av0 = mv.visitAnnotation("Lcom/concurnas/runtime/ForceCPS;", true);
    			av0.visitEnd();
    		}
    		
    		mv.visitCode();
    		

    		mv.visitTypeInsn(NEW, "java/util/HashSet");
    		mv.visitInsn(DUP);
    		mv.visitMethodInsn(INVOKESPECIAL, "java/util/HashSet", "<init>", "()V", false);
    		
    		
    		if(isAwait){
    			for(String fullClassName : dependancies){
            		mv.visitInsn(DUP);
            		
            		mv.visitLabel(new Label());
        			//if await then we just make a reference for the new fiber with a pointer to the origonal from the current fiber
        			mv.visitVarInsn(ALOAD, 1);
        			mv.visitMethodInsn(INVOKESTATIC, fullClassName, "getInstance?", "()L"+fullClassName+";", false);
        			mv.visitMethodInsn(INVOKESTATIC, fullClassName, "setInstance?", "(Lcom/concurnas/bootstrap/runtime/cps/Fiber;L"+fullClassName+";)V", false);
        		
        		
            		mv.visitLdcInsn(fullClassName);
            		mv.visitMethodInsn(INVOKEVIRTUAL, "java/util/HashSet", "add", "(Ljava/lang/Object;)Z", false);
            		mv.visitInsn(POP);
        		}
    		}else {
    			if(!dependancies.isEmpty()) {

            		mv.visitMethodInsn(INVOKESTATIC, "com/concurnas/runtime/bootstrapCloner/Cloner$Globals$", "getInstance?", "()Lcom/concurnas/runtime/bootstrapCloner/Cloner$Globals$;", false);
            		mv.visitFieldInsn(GETFIELD, "com/concurnas/runtime/bootstrapCloner/Cloner$Globals$", "cloner", "Lcom/concurnas/runtime/bootstrapCloner/Cloner;");
            		mv.visitVarInsn(ASTORE, 3);
    				
    				for(String fullClassName : dependancies){
                		mv.visitInsn(DUP);
                		
                		mv.visitLabel(new Label());
                		mv.visitVarInsn(ALOAD, 1);
                		//mv.visitFieldInsn(GETSTATIC, "com/concurnas/runtime/bootstrapCloner/Cloner", "cloner", "Lcom/concurnas/runtime/bootstrapCloner/Cloner;");
                		

                		mv.visitVarInsn(ALOAD, 3);
                		
                		//mv.visitTypeInsn(NEW, "com/concurnas/bootstrap/runtime/CopyTracker");
                		//mv.visitInsn(DUP);
                		//mv.visitMethodInsn(INVOKESPECIAL, "com/concurnas/bootstrap/runtime/CopyTracker", "<init>", "()V", false);
                		mv.visitVarInsn(ALOAD, 2);
                		mv.visitInsn(DUP);
                		mv.visitMethodInsn(INVOKESTATIC, fullClassName, "copyInstance?", "(Lcom/concurnas/bootstrap/runtime/CopyTracker;)L"+fullClassName+";", false);
                		mv.visitInsn(ACONST_NULL);
                		mv.visitMethodInsn(INVOKEVIRTUAL, "com/concurnas/runtime/bootstrapCloner/Cloner", "clone", "(Lcom/concurnas/bootstrap/runtime/CopyTracker;Ljava/lang/Object;Lcom/concurnas/bootstrap/runtime/CopyDefinition;)Ljava/lang/Object;", false);
                		mv.visitTypeInsn(CHECKCAST, fullClassName);
                		mv.visitMethodInsn(INVOKESTATIC, fullClassName, "setInstance?", "(Lcom/concurnas/bootstrap/runtime/cps/Fiber;L"+fullClassName+";)V", false);
            		
                		
                		mv.visitLdcInsn(fullClassName);
                		mv.visitMethodInsn(INVOKEVIRTUAL, "java/util/HashSet", "add", "(Ljava/lang/Object;)Z", false);
                		mv.visitInsn(POP);
                		
            		}
    			}
    		}
    		
    		
    		
    		mv.visitLabel(new Label());
    		mv.visitInsn(ARETURN);
    		mv.visitMaxs(4, 2);
    		mv.visitEnd();
        }
    	
		public MethodAdder(ClassVisitor writer, HashSet<String> dependancies, boolean isAwait){
			super(ASM7, writer);
			this.dependancies = dependancies;
			this.isAwait = isAwait;
		}
		
		public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
    		if( (name.equals("teardownGlobals") && desc.equals("()V")) || (name.equals("setupGlobals") && desc.equals("(Lcom/concurnas/bootstrap/runtime/cps/Fiber;Lcom/concurnas/bootstrap/runtime/CopyTracker;)Ljava/util/HashSet;"))  ){
    			//this is overwritten above
    		}
    		else{
    			return super.visitMethod(access, name, desc, signature,  exceptions);
    		}
	        return null;
	    }
		
		public void visitEnd() {
			addMethodSetupGlobals();
			addMethodTearDown();
			super.visitEnd();
		}
    	
    }
    

	public byte[] transform() {
		ClassReader cr = new ClassReader(this.data);
		String clsName = targetClassName.replace(".",  "/");
		IsoTaskFinder finder = new IsoTaskFinder(clsName);
		//figure out if extends IsoTask and which methods to gennerate
		//figure out all globals which can possibly be referenced
		cr.accept(finder, 0);
		
		if(finder.isIsoTask){
			//find global dependancies
			HashSet<String> dependancies = obtainGlobalDependancies(clsName, cr);
			
			ClassWriter cw = new ConcClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS, this.clsll.getDetector());// TODO:  turn off compute frames?
			MethodAdder adder = new MethodAdder(cw, dependancies, finder.isAwait);
			cr.accept(adder, 0);
			
			return cw.toByteArray();
		}
		
		return this.data;
	}	
	
	
}
