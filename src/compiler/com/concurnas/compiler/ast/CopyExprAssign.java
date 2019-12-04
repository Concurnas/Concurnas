package com.concurnas.compiler.ast;

import com.concurnas.compiler.ast.interfaces.Expression;

public class CopyExprAssign implements CopyExprItem {
	public Expression assignment;
	public String field;

	public CopyExprAssign(String field, Expression assignment) {
		this.field = field;
		this.assignment = assignment;
	}

	@Override
	public Copyable copy() {
		return new CopyExprAssign(field, (Expression)assignment.copy());
	}
	
}
