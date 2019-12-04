package com.concurnas.compiler.ast;

public class CopyExprIncOnly implements CopyExprItem {
	public String incOnly;

	public CopyExprIncOnly(String incOnly) {
		this.incOnly = incOnly;
	}

	@Override
	public Copyable copy() {
		return this;
	}
}
