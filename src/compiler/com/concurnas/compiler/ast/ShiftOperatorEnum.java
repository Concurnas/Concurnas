package com.concurnas.compiler.ast;

import java.util.HashMap;

public enum ShiftOperatorEnum {
	LS("<<", "leftShift"), RS(">>", "rightShift"), URS(">>>", "rightShiftU", false);

	private final String strRep; 
	public final String methodName; 
	public final boolean validForGPU; 
	
	private ShiftOperatorEnum(final String strRep, final String methodName){
		this(strRep, methodName, true);
	}
	
	private ShiftOperatorEnum(final String strRep, final String methodName, final boolean validForGPU){
		this.strRep = strRep; 
		this.methodName = methodName; 
		this.validForGPU = validForGPU;
	}
	
	public String toString(){
		return strRep;
	}
	
	public static HashMap<String, ShiftOperatorEnum> symToEnum = new HashMap<String, ShiftOperatorEnum>();
	static{
		for(ShiftOperatorEnum enu : ShiftOperatorEnum.values()){
			symToEnum.put(enu.strRep, enu);
		}
	}
	
}
