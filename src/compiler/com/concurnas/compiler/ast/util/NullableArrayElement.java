package com.concurnas.compiler.ast.util;

import com.concurnas.compiler.ast.ArrayRefElement;

public class NullableArrayElement {
	public boolean nullsafe;
	public boolean nna;
	public ArrayRefElement element;

	public NullableArrayElement(boolean nullsafe, boolean nna, ArrayRefElement element) {
		this.nullsafe = nullsafe;
		this.nna = nna;
		this.element = element;
	}
}
