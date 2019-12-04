package com.concurnas.compiler.ast;

import java.util.HashMap;

public enum AssignStyleEnum {
	MINUS_EQUALS("-=", "minus", false), MUL_EQUALS("*=", "mul", false), DIV_EQUALS("/=", "div", false), 
	MOD_EQUALS("mod=", "mod", false, "%="), POW_EQUALS("**=", "pow", false, null), PLUS_EQUALS("+=", "plus", false), 
	EQUALS_STRICT("\\=", "$", true, "="), EQUALS("=", "", true), OR_EQUALS("or=", "or", false, null), AND_EQUALS("and=", "and", false, null),
	LSH("<<=", "leftShift", false), RSH(">>=", "rightShift", false), RHSU(">>>=", "rightShiftU", false, null),
	BAND("band=", "band", false, "&="), BOR("bor=", "bor", false, "|="), BXOR("bxor=", "bxor", false, "^=");
	//EQUALS_STRICT to bypass operator overloading assign method call on assign
	private final String strRep;
	public final String methodString;
	public final boolean eqAssi;
	public final String useInGPUFunc;
	
	private AssignStyleEnum(final String strRep, final String methodString, final boolean isEqualsAssignment, final String useInGPUFunc){
		this.strRep = strRep;
		this.methodString = methodString + (methodString.isEmpty()?"assign":"Assign");
		this.eqAssi = isEqualsAssignment;
		this.useInGPUFunc = useInGPUFunc;
	}
	
	private AssignStyleEnum(final String strRep, final String methodString, final boolean isEqualsAssignment){
		this(strRep, methodString, isEqualsAssignment, strRep);
	}
	
	public String toString(){
		return this.strRep;
	}

	public boolean isEquals() {
		return this.eqAssi;
	}
	
	public AssignStyleEnum copy() {
		return this;
	}
	
	public static HashMap<String, AssignStyleEnum> assignStyleToEnum = new HashMap<String, AssignStyleEnum>();
	static{
		for( AssignStyleEnum el :AssignStyleEnum.values()){
			assignStyleToEnum.put(el.strRep, el);
		}
	}
	
}
