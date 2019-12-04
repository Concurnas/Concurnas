package com.concurnas.conc;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;

public class ConcUtis {
	public static class GetClassName extends ClassVisitor implements Opcodes {
		public String classname;
		public GetClassName() {
			super(ASM7);
		}
		
		@Override
		public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
			classname = name;
		}
	}
	
	public static String extractClassName(byte[] codex) {
		GetClassName gcn = new GetClassName();
		new ClassReader(codex).accept(gcn, 0);
		return gcn.classname;
	}
}
