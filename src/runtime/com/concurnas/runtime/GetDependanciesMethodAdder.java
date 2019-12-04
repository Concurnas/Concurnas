package com.concurnas.runtime;

import java.util.HashSet;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import com.concurnas.runtime.IsoTaskMethodAdder.GetGlobalClassesReferenced;
import com.concurnas.runtime.cps.analysis.ConcClassWriter;

public class GetDependanciesMethodAdder implements Opcodes {

	public static byte[] transform(byte[] code, ConcClassUtil loader) {
		HashSet<String> globs = new HashSet<String>();
		GetGlobalClassesReferenced ggcr = new GetGlobalClassesReferenced(globs);
		new ClassReader(code).accept(ggcr, 0);
		if(globs.isEmpty()) {//includes ifaces
			return code;
		}else {
			ClassWriter cw = new ConcClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS, loader.getDetector());// TODO:  turn off compute frames?
			new ClassReader(code).accept(new MethodAdder(cw, globs), 0);
			
			return cw.toByteArray();
		}
	}

	private static class MethodAdder extends ClassVisitor {
		private HashSet<String> globs;

		public MethodAdder(ClassVisitor writer, HashSet<String> globs) {
			super(ASM7, writer);
			this.globs = globs;
		}

		private void addgetGlobalDependancies() {

    		MethodVisitor mv = super.visitMethod(ACC_PUBLIC, "getGlobalDependancies$", "()Ljava/util/ArrayList;", "()Ljava/util/ArrayList<Ljava/lang/String;>;", null);
    		mv.visitCode();
			
    		mv.visitTypeInsn(NEW, "java/util/ArrayList");
    		mv.visitInsn(DUP);
    		mv.visitMethodInsn(INVOKESPECIAL, "java/util/ArrayList", "<init>", "()V", false);
    		
    		for(String dep : globs) {
    			mv.visitInsn(DUP);
        		mv.visitLdcInsn(dep);
        		mv.visitMethodInsn(INVOKEVIRTUAL, "java/util/ArrayList", "add", "(Ljava/lang/Object;)Z", false);
        		mv.visitInsn(POP);
    		}
    		
    		mv.visitInsn(ARETURN);
    		mv.visitMaxs(4, 2);
    		mv.visitEnd();
		}
		
		public void visitEnd() {
			addgetGlobalDependancies();
			super.visitEnd();
		}
	}
}
