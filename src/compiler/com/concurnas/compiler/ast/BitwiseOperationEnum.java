package com.concurnas.compiler.ast;

public enum BitwiseOperationEnum {
	AND("band", "&"), OR("bor", "|"), XOR("bxor", "^");
	
	public final String operationName;
	public final String gpuName;
	
	private BitwiseOperationEnum(final String operationName, final String gpuName){
		this.operationName=operationName;
		this.gpuName = gpuName;
	}
	
	public String toString(){
		return operationName;
	}
}
