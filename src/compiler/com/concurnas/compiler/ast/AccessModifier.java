package com.concurnas.compiler.ast;

import java.util.HashMap;

import org.objectweb.asm.Opcodes;


public enum AccessModifier {
	PUBLIC(4, Opcodes.ACC_PUBLIC, "public"), PACKAGE(3, 0, "package"), PROTECTED(2, Opcodes.ACC_PROTECTED, "protected"), PRIVATE(1, Opcodes.ACC_PRIVATE, "private");
	
	private int cat;
	private int bytecode;
	private String ts;
	private AccessModifier(int cat, int bytecode, String ts){
		this.cat =cat;
		this.bytecode =bytecode;
		this.ts =ts;
	}
	
	public int getCatagory(){
		return cat;
	}
	
	public AccessModifier copy(){
		return this;
	}
	
	public int getByteCodeAccess(){
		return bytecode;
	}
	
	public String toString(){
		return ts;
	}
	
	private static final HashMap<String, AccessModifier> nameToModifier = new HashMap<String, AccessModifier>();
	static {
		for(AccessModifier ist : AccessModifier.values()) {
			nameToModifier.put(ist.ts, ist);
		}
	}
	
	public static AccessModifier getAccessModifier(String name) {
		return nameToModifier.get(name);
	}
}
