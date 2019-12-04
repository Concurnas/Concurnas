package com.concurnas.compiler.ast;

public enum FactorPostFixEnum {
	MINUSMINUS("--", "dec"), PLUSPLUS("++", "inc");

	private String str;
	public String asMethod;
	private FactorPostFixEnum(final String str, final String asMethod){
		this.str=str;
		this.asMethod = asMethod;
	}
	public String toString(){
		return str;
	}
}
