package com.concurnas.compiler.ast;

public enum InoutGenericModifier {
	IN("in", "-"), OUT("out", "+");
	private final String name;
	private final String typePrefix;
	private InoutGenericModifier(String name, String typePrefit){
		this.name = name;
		this.typePrefix = typePrefit;
	}
	
	public String toString(){
		return this.name;
	}
	
	public String typePrefix() {
		return this.typePrefix;
	}
}
