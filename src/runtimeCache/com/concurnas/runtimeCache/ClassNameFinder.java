package com.concurnas.runtimeCache;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;

public class ClassNameFinder extends ClassVisitor implements Opcodes{
	public ClassNameFinder() {
		super(ASM7);
	}
	public String name;
	public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
		this.name = name;
	}
	
	public static String getClassName(byte[] code) {
		ClassNameFinder cnf = new ClassNameFinder();
		ClassReader cr = new ClassReader(code);
		cr.accept(cnf, 0);
		return cnf.name;
	}
	
}
