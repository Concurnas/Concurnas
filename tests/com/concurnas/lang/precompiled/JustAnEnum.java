package com.concurnas.lang.precompiled;

import java.util.HashMap;

public enum JustAnEnum {
	GLOBAL("global"), LOCAL("local"), CONSTANT("constant"), PRIVATE("private");
	
	private String strname;
	private String shortname;
	
	private JustAnEnum(String strname) {
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
	
	public static HashMap<String, JustAnEnum> shortnametoModifier = new HashMap<String, JustAnEnum>();
	static {
		for(JustAnEnum mod: JustAnEnum.values()) {
			shortnametoModifier.put(mod.shortname, mod);
		}
	}
}
