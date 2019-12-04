package com.concurnas.compiler.ast;

import com.concurnas.compiler.ast.interfaces.Expression;
import com.concurnas.compiler.visitors.ScopeAndTypeChecker;
import com.concurnas.compiler.visitors.Visitor;

public class PointerAddress extends Node implements Expression {

	public Expression rhs;

	public PointerAddress(int line, int column, Expression rhs) {
		super(line, column);
		this.rhs=rhs;
	}

	private Expression preecindExpre;
	@Override
	public void setPreceedingExpression(Expression preecindExpre) {
		this.preecindExpre = preecindExpre;
	}

	@Override
	public Expression getPreceedingExpression() {
		return preecindExpre;
	}

	@Override
	public boolean hasBeenVectorized() {
		return false;
	}

	@Override
	public Object accept(Visitor visitor) {
		return visitor.visit(this);
	}

	@Override
	public Node copyTypeSpecific() {
		PointerAddress ret = new PointerAddress(line, column, (Expression)rhs.copy());
		return ret;
	}
}
