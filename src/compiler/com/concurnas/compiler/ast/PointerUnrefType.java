package com.concurnas.compiler.ast;

public enum PointerUnrefType {
	PNT("*"), PNT2PNT("**");
	
	private String openclStr;

	private PointerUnrefType(String openclStr) {
		this.openclStr=  openclStr;
	}
	
	public String toString() {
		return openclStr;
	}
}
