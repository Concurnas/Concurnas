package com.concurnas.compiler.ast;

import java.util.HashMap;

public enum CaseOperatorEnum {
	EQ("=="), NEQ("<>"), REFEQ("&=="), NREFEQ("&<>"), GT(">"), GTEQ(">=="), LT("<"), LTEQ("<=="), IN("in"), NOTIN("not in");
	private String strRep;

	private CaseOperatorEnum(String strRep){
		this.strRep = strRep;
	}
	
	@Override
	public String toString(){
		return strRep;
	}
	
	public static final HashMap<String, CaseOperatorEnum> symToCaseOperatorEnum = new HashMap<String, CaseOperatorEnum>();
	static{
		for(CaseOperatorEnum en : CaseOperatorEnum.values()){
			symToCaseOperatorEnum.put(en.strRep, en);
		}
		symToCaseOperatorEnum.put("notin", NOTIN);
	}
	
}
