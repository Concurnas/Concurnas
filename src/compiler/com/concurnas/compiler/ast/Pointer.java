package com.concurnas.compiler.ast;

public enum Pointer {
	PNT("*"), PNT2PNT("**");
	
	private String openclStr;

	private Pointer(String openclStr) {
		this.openclStr=  openclStr;
	}
	
	public String toString() {
		return openclStr;
	}
}
