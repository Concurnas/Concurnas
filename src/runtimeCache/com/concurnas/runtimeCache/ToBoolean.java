package com.concurnas.runtimeCache;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import com.concurnas.runtime.ConcClassUtil;
import com.concurnas.runtime.cps.analysis.ConcClassWriter;

public class ToBoolean implements Opcodes{

	private static class ToBooleanAdder extends ClassVisitor  {

		private final boolean isString;

		public ToBooleanAdder(ClassVisitor writer, final boolean isString) {
			super(ASM7, writer);
			this.isString=isString;
		}
		
		public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
			super.visit(version, access, name, signature, superName, interfaces);

			MethodVisitor mv = super.visitMethod(ACC_PUBLIC, "toBoolean", "()Z", null, null);
			mv.visitCode();
			
			if(isString){
				mv.visitVarInsn(ALOAD, 0);
				mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "trim", "()Ljava/lang/String;", false);
				mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "isEmpty", "()Z", false);
				Label l1 = new Label();
				mv.visitJumpInsn(IFEQ, l1);
				mv.visitInsn(ICONST_0);
				Label l2 = new Label();
				mv.visitJumpInsn(GOTO, l2);
				mv.visitLabel(l1);
				mv.visitInsn(ICONST_1);
				mv.visitLabel(l2);
			}
			else{
				mv.visitVarInsn(ALOAD, 0);
				mv.visitMethodInsn(INVOKEVIRTUAL, name, "isEmpty", "()Z", false);
				Label l1 = new Label();
				mv.visitJumpInsn(IFEQ, l1);
				mv.visitInsn(ICONST_0);
				Label l2 = new Label();
				mv.visitJumpInsn(GOTO, l2);
				mv.visitLabel(l1);
				mv.visitInsn(ICONST_1);
				mv.visitLabel(l2);
			}
			
			mv.visitInsn(IRETURN);
			mv.visitMaxs(1, 1);
			mv.visitEnd();
		}
	}
	
	public static byte[] addToBoolean(String className, byte[] inputClassBytes, ConcClassUtil clloader, boolean isString){
		ClassReader cr = new ClassReader(inputClassBytes);
		ClassWriter cw = new ConcClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS, clloader.getDetector());// TODO:  turn off compute frames?
		ToBooleanAdder upgrader = new ToBooleanAdder(cw, isString);
		cr.accept(upgrader, 0);
		
		return cw.toByteArray();
	}
	
}
