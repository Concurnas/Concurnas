package com.concurnas.conc;

import java.lang.reflect.Method;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import com.concurnas.compiler.bytecode.BytecodeGennerator;

public class ConcTaskMaker extends TaskMaker implements Opcodes{

	private Method meth;
	public boolean hasExitCode = false;
	public static final String ExitCodeField = "exitCode";
	
	public ConcTaskMaker(String invokerclassName, String classBeingTested, Method meth) {
		super(invokerclassName, classBeingTested, null, true);
		this.meth = meth;
	}

	@Override
	protected void makeApply(ClassWriter cw) {
		{//apply
			MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "apply", "()Ljava/lang/Void;", null, null);
			mv.visitCode();
			
			Label start = new Label();
			Label end = new Label();
			Label handle = new Label();
			mv.visitTryCatchBlock(start, end, handle, "java/lang/Throwable");
			
			mv.visitLabel(start);
			
			
			
			if(meth == null) {
				mv.visitMethodInsn(INVOKESTATIC, classBeingTested, BytecodeGennerator.metaMethodName, "()Ljava/lang/String;", false);
				mv.visitInsn(POP);
			}else {
				String retType = "";
				String signature;
				if(meth.getParameterCount()==1) {//consume cmd line params
					mv.visitVarInsn(ALOAD, 0);
					mv.visitFieldInsn(GETFIELD, invokerclassName, TaskMaker.CMDLineParamsStr, "[Ljava/lang/String;");
					retType = this.meth.getReturnType().descriptorString();
					signature = "([Ljava/lang/String;)"+retType;
					
				}else {//no params
					retType = this.meth.getReturnType().descriptorString();
					signature = "()"+retType;
				}
				
				if(!retType.equals("V")) {
					
					if(retType.equals("I")) {
						mv.visitMethodInsn(INVOKESTATIC, classBeingTested, "main", signature, false);
						mv.visitVarInsn(ALOAD, 0);
						mv.visitInsn(SWAP);
						mv.visitFieldInsn(PUTFIELD, invokerclassName, ExitCodeField, "I");
						this.hasExitCode = true;
					}else {
						mv.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
						mv.visitMethodInsn(INVOKESTATIC, classBeingTested, "main", signature, false);
						mv.visitMethodInsn(INVOKESTATIC, "com/concurnas/bootstrap/lang/Stringifier", "stringify", "(Ljava/lang/Object;)Ljava/lang/String;", false);
						mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V", false);
					}
				}else {
					mv.visitMethodInsn(INVOKESTATIC, classBeingTested, "main", signature, false);
				}
			}
			
			
			mv.visitLabel(end);
			Label toRet = new Label();
			mv.visitJumpInsn(GOTO, toRet);
	
			mv.visitLabel(handle);
			
			mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Throwable", "printStackTrace", "()V", false);
			
			mv.visitLabel(toRet);
			
			mv.visitVarInsn(ALOAD, 0);
			mv.visitMethodInsn(INVOKEVIRTUAL, invokerclassName, "setDone", "()V", false);
			
			mv.visitInsn(ACONST_NULL);
			mv.visitInsn(ARETURN);
			mv.visitEnd();
		
		}
		
		if(this.hasExitCode){
			FieldVisitor fv = cw.visitField(ACC_PUBLIC, ExitCodeField, "I", null, null);
			fv.visitEnd();
		}
		
		{
			FieldVisitor fv = cw.visitField(ACC_PRIVATE, "isDone", "Z", null, null);
			fv.visitEnd();
		}
		
		
		{//setDone
			MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "setDone", "()V", null, null);
			mv.visitCode();
			Label l0 = new Label();
			Label l1 = new Label();
			Label l2 = new Label();
			mv.visitTryCatchBlock(l0, l1, l2, null);
			Label l3 = new Label();
			mv.visitTryCatchBlock(l2, l3, l2, null);
			Label l4 = new Label();
			mv.visitLabel(l4);
			mv.visitVarInsn(ALOAD, 0);
			mv.visitInsn(DUP);
			mv.visitVarInsn(ASTORE, 1);
			mv.visitInsn(MONITORENTER);
			mv.visitLabel(l0);
			mv.visitVarInsn(ALOAD, 0);
			mv.visitInsn(ICONST_1);
			mv.visitFieldInsn(PUTFIELD, invokerclassName, "isDone", "Z");
			Label l5 = new Label();
			mv.visitLabel(l5);
			mv.visitVarInsn(ALOAD, 0);
			mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Object", "notifyAll", "()V", false);
			Label l6 = new Label();
			mv.visitLabel(l6);
			mv.visitVarInsn(ALOAD, 1);
			mv.visitInsn(MONITOREXIT);
			mv.visitLabel(l1);
			Label l7 = new Label();
			mv.visitJumpInsn(GOTO, l7);
			mv.visitLabel(l2);
			mv.visitVarInsn(ALOAD, 1);
			mv.visitInsn(MONITOREXIT);
			mv.visitLabel(l3);
			mv.visitInsn(ATHROW);
			mv.visitLabel(l7);
			mv.visitInsn(RETURN);
			Label l8 = new Label();
			mv.visitLabel(l8);
			mv.visitMaxs(2, 2);
			mv.visitEnd();
		}
		{//isDone
			MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "isDone", "()V", null, new String[] { "java/lang/InterruptedException" });
			mv.visitCode();
			Label l0 = new Label();
			Label l1 = new Label();
			Label l2 = new Label();
			mv.visitTryCatchBlock(l0, l1, l2, null);
			Label l3 = new Label();
			mv.visitTryCatchBlock(l2, l3, l2, null);
			Label l4 = new Label();
			mv.visitLabel(l4);
			mv.visitVarInsn(ALOAD, 0);
			mv.visitInsn(DUP);
			mv.visitVarInsn(ASTORE, 1);
			mv.visitInsn(MONITORENTER);
			mv.visitLabel(l0);
			Label l5 = new Label();
			mv.visitJumpInsn(GOTO, l5);
			Label l6 = new Label();
			mv.visitLabel(l6);
			mv.visitVarInsn(ALOAD, 0);
			mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Object", "wait", "()V", false);
			mv.visitLabel(l5);
			mv.visitVarInsn(ALOAD, 0);
			mv.visitFieldInsn(GETFIELD, invokerclassName, "isDone", "Z");
			mv.visitJumpInsn(IFEQ, l6);
			Label l7 = new Label();
			mv.visitLabel(l7);
			mv.visitVarInsn(ALOAD, 1);
			mv.visitInsn(MONITOREXIT);
			mv.visitLabel(l1);
			Label l8 = new Label();
			mv.visitJumpInsn(GOTO, l8);
			mv.visitLabel(l2);
			mv.visitVarInsn(ALOAD, 1);
			mv.visitInsn(MONITOREXIT);
			mv.visitLabel(l3);
			mv.visitInsn(ATHROW);
			mv.visitLabel(l8);
			mv.visitInsn(RETURN);
			Label l9 = new Label();
			mv.visitLabel(l9);
			mv.visitMaxs(2, 2);
			mv.visitEnd();
		}
	}
	
	/*private boolean isDone = false;
	public void setDone() {
		synchronized(this){
			isDone=true;
			this.notifyAll();
		}
	}
	
	public void isDone() throws InterruptedException {
		synchronized(this){
			while(!isDone) {
				this.wait();
			}
		}
	}*/
	

}
