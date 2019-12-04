package com.concurnas.compiler.ast;

import com.concurnas.compiler.ast.interfaces.Expression;
import com.concurnas.compiler.visitors.Visitor;

public class Vectorized extends AbstractExpression implements Expression {
	public  Expression expr;
	public boolean doubledot;
	public boolean nullsafe;

	public Vectorized(int line, int column, Expression expr, boolean doubledot, boolean nullsafe) {
		super(line, column);
		this.expr=expr;
		this.doubledot = doubledot;
		this.nullsafe = nullsafe;
	}
	public Vectorized(Expression expr) {
		super(expr.getLine(), expr.getColumn());
		this.expr=expr;
		this.doubledot = false;
		this.nullsafe = false;
	}

	private Expression preceedingExpression;
	public boolean validAtThisLocation=false;
	@Override
	public void setPreceedingExpression(Expression expr) {
		this.preceedingExpression = expr;
	}
	@Override
	public Expression getPreceedingExpression() {
		return preceedingExpression;
	}

	@Override
	public Object accept(Visitor visitor) {
		visitor.setLastLineVisited(super.getLine());
		return visitor.visit(this);
	}

	@Override
	public Node copyTypeSpecific() {
		return new Vectorized(line, column, (Expression)expr.copy(), doubledot, nullsafe);
	}

}
