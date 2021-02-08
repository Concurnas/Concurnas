package com.concurnas.runtime;

import java.util.Objects;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import com.concurnas.runtime.cps.analysis.ConcClassWriter;

/**
 * @author jason
 */

public class UsafeIsHiddenRemapper extends ClassVisitor implements Opcodes {
	public UsafeIsHiddenRemapper(ClassVisitor classVisitor) {
		super(ASM7, classVisitor);
	}

	public MethodVisitor visitMethod(final int access, final String name, final String descriptor, final String signature, final String[] exceptions) {
		return new IsHiddenRemapper(cv.visitMethod(access, name, descriptor, signature, exceptions));
	}

	private class IsHiddenRemapper extends MethodVisitor {
		public IsHiddenRemapper(MethodVisitor methodVisitor) {
			super(ASM7, methodVisitor);
		}
		public void visitMethodInsn(final int opcode, final String owner, final String name, final String descriptor, final boolean isInterface) {
			
			if(opcode == INVOKEVIRTUAL && name.equals("isHidden") && owner.equals("java/lang/Class") && descriptor.equals("()Z") && !isInterface) {
				super.visitInsn(POP);
				super.visitInsn(ICONST_0);
				return;
			}
			
			super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
		}
	}

	public static byte[] transform(byte[] code, ConcClassUtil loader) {
		ClassWriter cw = new ConcClassWriter( ClassWriter.COMPUTE_MAXS, loader.getDetector());
		new ClassReader(code).accept(new UsafeIsHiddenRemapper(cw), 0);
		return cw.toByteArray();
	}

}
