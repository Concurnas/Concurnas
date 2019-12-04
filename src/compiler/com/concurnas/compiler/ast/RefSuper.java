package com.concurnas.compiler.ast;

import com.concurnas.compiler.ast.interfaces.Expression;
import com.concurnas.compiler.visitors.Visitor;

public class RefSuper extends AbstractExpression implements Expression {
	public String superQuali;
	
	public RefSuper(int line, int column) {
		this(line, column, null);
	}
	
	public RefSuper(int line, int column, String superQuali) {
		super(line, column);
		this.superQuali = superQuali;
	}

	@Override
	public Object accept(Visitor visitor) {
		visitor.setLastLineVisited(super.getLine());
		return visitor.visit(this);
	}
	@Override
	public Node copyTypeSpecific() {
		return this;//haha
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
