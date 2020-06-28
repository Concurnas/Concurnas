package com.concurnas.compiler.ast;

import com.concurnas.compiler.ast.interfaces.Expression;
import com.concurnas.compiler.visitors.Visitor;

public class JustReturnType extends Node implements Expression {

	public JustReturnType(int line, int column) {
		super(line, column);
	}
	
	@Override
	public Object accept(Visitor visitor) {
		return this.getTaggedType();
	}

	@Override
	public Node copyTypeSpecific() {
		return this;
	}

	private Expression prec;
	@Override
	public void setPreceedingExpression(Expression expr) {
		this.prec = expr;
	}

	@Override
	public Expression getPreceedingExpression() {
		return prec;
	}

	@Override
	public boolean hasBeenVectorized() {
		return false;
	}
}
