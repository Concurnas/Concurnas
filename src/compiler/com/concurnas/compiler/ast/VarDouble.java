package com.concurnas.compiler.ast;

import com.concurnas.compiler.ast.interfaces.Expression;
import com.concurnas.compiler.visitors.Visitor;

public class VarDouble extends AbstractExpression implements Expression {

	public double doubler;

	public VarDouble(int line, int col, double doubler) {
		super(line, col);
		this.doubler = doubler;
	}
	
	@Override
	public Object accept(Visitor visitor) {
		visitor.setLastLineVisited(super.getLine());
		return visitor.visit(this);
	}
	
	@Override
	public Node copyTypeSpecific() {
		return this;
	}
	private Expression preceedingExpression;
	@Override
	public void setPreceedingExpression(Expression expr) {
		this.preceedingExpression = expr;
	}
	@Override
	public Expression getPreceedingExpression() {
		return preceedingExpression;
	}
}
