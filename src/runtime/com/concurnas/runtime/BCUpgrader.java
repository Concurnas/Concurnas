package com.concurnas.runtime;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.JSRInlinerAdapter;

import com.concurnas.runtime.cps.analysis.ConcClassWriter;

/**
 * used for code which has been built pre vrsion 1.6/7 (?) which is missing stack frames, adds stack frames for rest of concurnas analysis
 * also inlines jsr instructions
 */
public class BCUpgrader implements Opcodes{

	private static class ClassUpgrader extends ClassVisitor{

		public ClassUpgrader(ClassVisitor writer) {
			super(ASM7, writer);
		}
		
		public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
			return new JSRInlinerAdapter(super.visitMethod(access, name, desc, signature, exceptions), access, name, desc, signature, exceptions);
		}
	}
	
	public static byte[] upgradeCode(String className, byte[] inputClassBytes, ConcClassUtil clloader){
		ClassReader cr = new ClassReader(inputClassBytes);
		ClassWriter cw = new ConcClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS, clloader.getDetector());// TODO:  turn off compute frames?
		ClassUpgrader upgrader = new ClassUpgrader(cw);
		cr.accept(upgrader, 0);
		
		return cw.toByteArray();
	}
	
}
