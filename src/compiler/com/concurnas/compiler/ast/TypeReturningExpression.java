package com.concurnas.compiler.ast;

import com.concurnas.compiler.ast.interfaces.Expression;
import com.concurnas.compiler.visitors.Visitor;

/**
 * Used where we know the type but have no expression from which it originates, e.g. faking up expressions for funcref arguments
 */
public class TypeReturningExpression extends AbstractExpression implements Expression {
	
	public Type type;
	public TypeReturningExpression(Type type) {
		super(0,0);
		this.type = type;
	}
	
	public TypeReturningExpression(int line, int col, Type type) {
		super(line, col);
		this.type = type;
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
	
	@Override
	public Object accept(Visitor visitor) {
		visitor.setLastLineVisited(super.getLine());
		return visitor.visit(this);
	}
	
	@Override
	public Node copyTypeSpecific() {
		return new TypeReturningExpression(super.line, super.column, type==null?null:(Type)type.copy());
	}
}
