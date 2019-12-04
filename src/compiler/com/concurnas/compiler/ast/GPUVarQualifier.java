package com.concurnas.compiler.ast;

import java.util.HashMap;

public enum GPUVarQualifier {
	GLOBAL("global"), LOCAL("local"), CONSTANT("constant"), PRIVATE("private");
	
	private String strname;
	private String shortname;
	
	private GPUVarQualifier(String strname) {
		this.strname = strname;
		this.shortname = ""+strname.charAt(0);
	}
	
	@Override public String toString(){
		return strname;
	}

	public String openClStr() {
		return "__" + strname;
	}
	
	public String shortName() {
		return shortname;
	}
	
	public static HashMap<String, GPUVarQualifier> shortnametoModifier = new HashMap<String, GPUVarQualifier>();
	static {
		for(GPUVarQualifier mod: GPUVarQualifier.values()) {
			shortnametoModifier.put(mod.shortname, mod);
		}
	}
	
}
