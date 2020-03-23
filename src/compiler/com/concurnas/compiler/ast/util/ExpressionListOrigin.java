package com.concurnas.compiler.ast.util;

import com.concurnas.compiler.ast.ExpressionList;

public class ExpressionListOrigin {
	public ExpressionList origin;
	public int argstart;
	public int argEnd;

	public ExpressionListOrigin(ExpressionList origin, int argstart, int argEnd) {
		this.origin = origin;
		this.argstart = argstart;
		this.argEnd = argEnd;
	}
	
	@Override
	public String toString() {
		return String.format("%s %s-%s", origin, argstart, argEnd);
	}
	
}
