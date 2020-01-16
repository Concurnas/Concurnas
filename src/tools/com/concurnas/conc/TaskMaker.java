package com.concurnas.conc;

import java.util.Set;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public abstract class TaskMaker implements Opcodes{

	protected String classBeingTested;
	protected String invokerclassName;
	protected Set<String> newvars;
	protected boolean hasCmdLineParams;
	public static final String CMDLineParamsStr = "cmdLineParams";

	public TaskMaker(String invokerclassName, String classBeingTested, Set<String> newvars, boolean hasCmdLineParams){
		this.invokerclassName = invokerclassName;
		this.classBeingTested = classBeingTested;
		this.newvars = newvars;
		this.hasCmdLineParams = hasCmdLineParams;
	}
	
	public byte[] gennerate() {
		ClassWriter cw = new ClassWriter(0);
		FieldVisitor fv;
		MethodVisitor mv;

		cw.visit(52, ACC_PUBLIC + ACC_SUPER, invokerclassName, "Lcom/concurnas/bootstrap/runtime/cps/IsoTask;", "com/concurnas/bootstrap/runtime/cps/IsoTask", null);

		cw.visitSource(invokerclassName + ".java", null);
		cw.visitInnerClass("com/concurnas/bootstrap/lang/Lambda$Function0", "com/concurnas/bootstrap/lang/Lambda", "Function0", ACC_PUBLIC + ACC_STATIC + ACC_ABSTRACT);
		
		{
			//if(null != newvars) {
				fv = cw.visitField(ACC_PRIVATE, "result", "Ljava/lang/String;", null, null);
				fv.visitEnd();
			//}
			
			if(hasCmdLineParams) {
				fv = cw.visitField(ACC_PUBLIC, CMDLineParamsStr, "[Ljava/lang/String;", null, null);
				fv.visitEnd();
			}
			
			fv = cw.visitField(ACC_PRIVATE, "$isInitCompleteFlag", "Lcom/concurnas/bootstrap/runtime/ref/DefaultRef;", "Lcom/concurnas/bootstrap/runtime/ref/DefaultRef<Ljava/lang/Boolean;>;", null);
			fv.visitEnd();
		}
		
		{
			mv = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
			mv.visitCode();
			Label l0 = new Label();
			mv.visitLabel(l0);
			mv.visitVarInsn(ALOAD, 0);

			mv.visitInsn(ACONST_NULL);
			
			mv.visitMethodInsn(INVOKESPECIAL, "com/concurnas/bootstrap/runtime/cps/IsoTask", "<init>", "(Ljava/lang/Class;)V", false);
			
			mv.visitVarInsn(ALOAD, 0);
			mv.visitTypeInsn(NEW, "com/concurnas/runtime/ref/Local");
			mv.visitInsn(DUP);
			mv.visitInsn(ICONST_1);
			mv.visitTypeInsn(ANEWARRAY, "java/lang/Class");
			mv.visitInsn(DUP);
			mv.visitInsn(ICONST_0);
			mv.visitLdcInsn(org.objectweb.asm.Type.getType("Ljava/lang/Boolean;"));
			mv.visitInsn(AASTORE);
			mv.visitMethodInsn(INVOKESPECIAL, "com/concurnas/runtime/ref/Local", "<init>", "([Ljava/lang/Class;)V", false);
			mv.visitFieldInsn(PUTFIELD, invokerclassName, "$isInitCompleteFlag", "Lcom/concurnas/bootstrap/runtime/ref/DefaultRef;");
			
			
			mv.visitInsn(RETURN);
			mv.visitEnd();
		}
		
		
		{
			makeApply(cw);
		}
		
		
		{
			mv = cw.visitMethod(ACC_PUBLIC, "signature", "()[Ljava/lang/Object;", null, null);
			mv.visitCode();
			Label l0 = new Label();
			mv.visitLabel(l0);
			mv.visitInsn(ACONST_NULL);
			mv.visitInsn(ARETURN);
			mv.visitMaxs(1, 1);
			mv.visitEnd();
		}
		{
			mv = cw.visitMethod(ACC_PUBLIC + ACC_BRIDGE + ACC_SYNTHETIC, "apply", "()Ljava/lang/Object;", null, null);
			mv.visitCode();
			Label l0 = new Label();
			mv.visitLabel(l0);
			mv.visitVarInsn(ALOAD, 0);
			mv.visitMethodInsn(INVOKEVIRTUAL, invokerclassName, "apply", "()Ljava/lang/Void;", false);
			mv.visitInsn(ARETURN);
			mv.visitMaxs(1, 1);
			mv.visitEnd();
		}
		
		{//getIsInitCompleteFlag
			mv = cw.visitMethod(ACC_PUBLIC, "getIsInitCompleteFlag", "()Lcom/concurnas/bootstrap/runtime/ref/DefaultRef;", "()Lcom/concurnas/bootstrap/runtime/ref/DefaultRef<Ljava/lang/Boolean;>;", null);
			mv.visitCode();
			Label l0 = new Label();
			mv.visitLabel(l0);
			mv.visitVarInsn(ALOAD, 0);
			mv.visitFieldInsn(GETFIELD, invokerclassName, "$isInitCompleteFlag", "Lcom/concurnas/bootstrap/runtime/ref/DefaultRef;");
			mv.visitInsn(ARETURN);
			mv.visitMaxs(1, 1);
			mv.visitEnd();
		}
		
		
		
		//if(null != newvars) {
			{
				mv = cw.visitMethod(ACC_PRIVATE + ACC_SYNCHRONIZED, "setResult", "(Ljava/lang/String;)V", null, null);
				mv.visitCode();
				Label l0 = new Label();
				mv.visitLabel(l0);
				mv.visitVarInsn(ALOAD, 0);
				mv.visitVarInsn(ALOAD, 1);
				mv.visitFieldInsn(PUTFIELD, invokerclassName, "result", "Ljava/lang/String;");
				Label l1 = new Label();
				mv.visitLabel(l1);
				mv.visitVarInsn(ALOAD, 0);
				mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Object", "notifyAll", "()V", false);
				Label l2 = new Label();
				mv.visitLabel(l2);
				mv.visitInsn(RETURN);
				mv.visitMaxs(2, 2);
				mv.visitEnd();
			}
			{
				mv = cw.visitMethod(ACC_PUBLIC + ACC_SYNCHRONIZED, "getResult", "()Ljava/lang/String;", null, new String[] { "java/lang/InterruptedException" });
				mv.visitCode();
				Label l0 = new Label();
				mv.visitLabel(l0);
				Label l1 = new Label();
				mv.visitJumpInsn(GOTO, l1);
				Label l2 = new Label();
				mv.visitLabel(l2);
				mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
				mv.visitVarInsn(ALOAD, 0);
				mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Object", "wait", "()V", false);
				mv.visitLabel(l1);
				mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
				mv.visitVarInsn(ALOAD, 0);
				mv.visitFieldInsn(GETFIELD, invokerclassName, "result", "Ljava/lang/String;");
				mv.visitJumpInsn(IFNULL, l2);
				Label l3 = new Label();
				mv.visitLabel(l3);
				mv.visitVarInsn(ALOAD, 0);
				mv.visitFieldInsn(GETFIELD, invokerclassName, "result", "Ljava/lang/String;");
				mv.visitInsn(ARETURN);
				mv.visitMaxs(1, 1);
				mv.visitEnd();
			}
		//}
		
		cw.visitEnd();

		return cw.toByteArray();
	}
	
	protected abstract void makeApply(ClassWriter cw);
}

