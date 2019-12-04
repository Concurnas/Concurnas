package com.concurnas.compiler.ast;

import java.util.HashMap;

public enum GPUInOutFuncParamModifier {
	in("I"), out("O");
	
	private String shortname;
	
	private GPUInOutFuncParamModifier(String shortname) {
		this.shortname = shortname;
	}
	
	public String getShortName() {
		return shortname;
	}
	
	public static HashMap<String, GPUInOutFuncParamModifier> shortnametoModifier = new HashMap<String, GPUInOutFuncParamModifier>();
	static {
		for(GPUInOutFuncParamModifier mod: GPUInOutFuncParamModifier.values()) {
			shortnametoModifier.put(mod.shortname, mod);
		}
	}
}
