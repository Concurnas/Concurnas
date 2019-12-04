package com.concurnas.compiler.ast;

import java.util.List;

public class CopyExprExclOnly implements CopyExprItem {
	public List<String> excludeOnly;

	public CopyExprExclOnly(List<String> incOnly) {
		this.excludeOnly = incOnly;
	}

	@Override
	public Copyable copy() {
		return this;
	}
}
