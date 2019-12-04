package com.concurnas.compiler.ast;

import java.util.HashMap;

public enum PrimativeTypeEnum {
	BOOLEAN("Z", "boolean", false, false), 
	INT("I", "int", true, true), 
	SIZE_T("T", "size_t", true, true), 
	LONG("J", "long", true, true), 
	FLOAT("F", "float", true, false), 
	DOUBLE("D", "double", true, false), 
	SHORT("S", "short", true, true), 
	BYTE("B", "byte", true, true), 
	CHAR("C", "char", true, true),
	VOID("V", "void", false, false), 
	LAMBDA("Lcom/concurnas/bootstrap/lang/Lambda;", "com/concurnas/bootstrap/lang/Lambda", "com.concurnas.bootstrap.lang.Lambda", false, false);
	//TODO: remove lambda?
	private final String bytecodeType;
	private final String bytecodeTypeGeneric;
	private final String javaName;
	private final boolean isNumerical;
	private final boolean isIntegral;
	
	private PrimativeTypeEnum(String bytecodeType, String javaName, boolean isNumerical, boolean isIntegral)
	{
		this.bytecodeType = bytecodeType;
		this.bytecodeTypeGeneric = bytecodeType;
		//this.boxedName = boxedName;
		this.javaName = javaName;
		this.isNumerical = isNumerical;
		this.isIntegral = isIntegral;
	}
	
	private PrimativeTypeEnum(String bytecodeType, String bytecodeTypeGeneric, String javaName, boolean isNumerical, boolean isIntegral){
		this.bytecodeType = bytecodeType;
		this.bytecodeTypeGeneric = bytecodeTypeGeneric;
		this.javaName = javaName;
		this.isNumerical = isNumerical;
		this.isIntegral = isIntegral;
	}
	
	public String getBytecodeType()
	{
		return this.bytecodeType;
	}

	public String getNonGenericPrettyName() {
		return bytecodeTypeGeneric;
	}
	
	public String getJavaName() {
		return javaName;
	}
	
	public boolean isNumerical(){
		return this.isNumerical;
	}
	
	public boolean isIntegral(){
		return this.isIntegral;
	}
	
	@Override
	public String toString(){
		return getJavaName();
	}
	
	public static HashMap<String, PrimativeTypeEnum> nameToEnum = new HashMap<String, PrimativeTypeEnum>();
	static{
		for(PrimativeTypeEnum pp : PrimativeTypeEnum.values()){
			nameToEnum.put(pp.javaName, pp);
		}
		nameToEnum.put("bool", BOOLEAN);
	}
	
}
