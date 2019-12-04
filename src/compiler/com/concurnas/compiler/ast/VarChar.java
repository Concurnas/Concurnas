package com.concurnas.compiler.ast;

import com.concurnas.compiler.ast.interfaces.Expression;
import com.concurnas.compiler.visitors.Visitor;

public class VarChar extends AbstractExpression implements Expression {

	public String chr;

	public VarChar(int line, int col, String str) {
		super(line, col);
		this.chr = str;
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
