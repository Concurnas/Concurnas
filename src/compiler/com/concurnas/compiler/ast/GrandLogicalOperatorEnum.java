package com.concurnas.compiler.ast;

import java.util.HashMap;

public enum GrandLogicalOperatorEnum {
	LT(true, "<", "<"), GT(true, ">", ">"), GTEQ(true, ">==", ">="), LTEQ(true , "<==", "<="),
	EQ(false, "==", "=="), NE(false, "<>", "!="), REFEQ(false, "&==", null), REFNE(false, "&<>", null);
	
	private final boolean isrela; 
	private final String strRep; 
	public final String gpuVariant;
	
	private GrandLogicalOperatorEnum(final boolean isrela, final String strRep, final String gpuVariant){
		this.isrela = isrela; 
		this.strRep = strRep; 
		this.gpuVariant = gpuVariant; 
	}
	
	public boolean isRelational() {
		return isrela;
	}
	
	public String toString(){
		return strRep;
	}
	
	public static HashMap<String, GrandLogicalOperatorEnum> symToEnum = new HashMap<String, GrandLogicalOperatorEnum>();
	static{
		for(GrandLogicalOperatorEnum enu : GrandLogicalOperatorEnum.values()){
			symToEnum.put(enu.strRep, enu);
		}
	}
	
}
