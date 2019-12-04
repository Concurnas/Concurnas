package com.concurnas.compiler.ast;

public enum ForBlockVariant {
	PARFOR("parfor"), PARFORSYNC("parforsync");

	private String string;
	private ForBlockVariant(String name) {
		string = name;
	}

	@Override
	public String toString() {
		return string;
	}

}
