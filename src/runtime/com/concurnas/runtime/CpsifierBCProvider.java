package com.concurnas.runtime;

import java.util.HashMap;

public class CpsifierBCProvider {

	public final ConcClassUtil clloader;
	private final HashMap<String, byte[]> overrodeName = new HashMap<String, byte[]>();
	
	public CpsifierBCProvider(ConcClassUtil clloader) {
		this.clloader = clloader;
	}
	
	public void overrideName(String namea, byte[] code) {
		overrodeName.put(namea, code);
	}
	
	public byte[] getByteCode(String name){
		byte[] ret = overrodeName.get(name);
		if(null == ret){
			ret = this.clloader.getBytecode(name);
		}
		return ret;
	}

	public Class<?> loadClassFromPrimordial(String className) throws ClassNotFoundException {
		return this.clloader.loadClassFromPrimordial(className);
	}
	
}
