package com.concurnas.compiler.ast;

import com.concurnas.compiler.ast.interfaces.Expression;
import com.concurnas.compiler.visitors.Visitor;

public class RefClass extends AbstractExpression implements Expression {
	
	public Type lhsType;
	
	public RefClass(int line, int column, Type lhsType) {
		super(line, column);
		this.lhsType = lhsType;
	}

	@Override
	public Object accept(Visitor visitor) {
		visitor.setLastLineVisited(super.getLine());
		return visitor.visit(this);
	}
	@Override
	public Node copyTypeSpecific() {
		RefClass ret = new RefClass(line, column, lhsType==null?null:(Type)lhsType.copy());
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
