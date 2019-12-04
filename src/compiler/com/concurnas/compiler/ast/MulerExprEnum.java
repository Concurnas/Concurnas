package com.concurnas.compiler.ast;

public enum MulerExprEnum {
	DIV("/", "div"), MOD("mod", "mod", "%"), MUL("*", "mul");

	private final String strRep;
	public final String methodName;
	public final String gpuVariant;
	
	private MulerExprEnum(final String strRep, final String methodName, final String gpuVariant){
		this.strRep = strRep;
		this.methodName = methodName;
		this.gpuVariant = gpuVariant;
	}
	private MulerExprEnum(final String strRep, final String methodName){
		this(strRep, methodName, strRep);
	}
	
	public String toString(){
		return this.strRep;
	}
	
}
