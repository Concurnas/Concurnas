package com.concurnas.compiler.ast;

import com.concurnas.compiler.ast.interfaces.Expression;
import com.concurnas.compiler.visitors.Visitor;

public class RefOf extends AbstractExpression implements Expression {
	public DotOperator resolveToOf;
	
	public RefOf(int line, int column) {
		super(line, column);
	}

	@Override
	public Object accept(Visitor visitor) {
		visitor.setLastLineVisited(super.getLine());
		return visitor.visit(this);
	}
	@Override
	public Node copyTypeSpecific() {
		RefOf ret = new RefOf(line, column);
		ret.resolveToOf = (DotOperator)(resolveToOf==null?null:resolveToOf.copyTypeSpecific());
		ret.preceedingExpression = preceedingExpression==null?null:(Expression)preceedingExpression.copy();

		return ret;
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
