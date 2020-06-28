package com.concurnas.compiler.ast.util;

import java.util.ArrayList;

import com.concurnas.compiler.ast.ArrayRefElement;

public class NullableArrayElementss {
	public boolean nullsafe;
	public boolean nna;
	public ArrayList<ArrayRefElement> elements;

	public NullableArrayElementss(boolean nullsafe, boolean nna, ArrayList<ArrayRefElement> elements) {
		this.nullsafe = nullsafe;
		this.nna = nna;
		this.elements = elements;
	}
}