package com.concurnas.compiler.ast;

public enum FactorPrefixEnum {
	MINUSMINUS("--", "dec", true), PLUSPLUS("++", "inc", true), NEG("-", "neg", false), PLUS("+", "plus", false), COMP("comp", "comp", false, "~");
	
	private String str;
	public String asMethod;
	public boolean isDoubleThing;
	public String gpuVersion;
	
	private FactorPrefixEnum(final String str, final String asMethod, final boolean isDoubleThing, final String gpuVersion){
		this.str=str;
		this.asMethod=asMethod;
		this.isDoubleThing=isDoubleThing;
		this.gpuVersion=gpuVersion;
	}
	
	private FactorPrefixEnum(final String str, final String asMethod, final boolean isDoubleThing){
		this(str, asMethod, isDoubleThing, str);
	}
	
	public String toString(){
		return str;
	}
}
