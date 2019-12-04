package com.concurnas.compiler.ast;

import com.concurnas.compiler.ast.interfaces.Expression;
import com.concurnas.compiler.visitors.ScopeAndTypeChecker;
import com.concurnas.compiler.visitors.Visitor;

public class PointerUnref extends Node implements Expression {

	public Expression rhs;
	public int size;

	public PointerUnref(int line, int column, int size, Expression rhs) {
		super(line, column);
		this.size=size;
		this.rhs=rhs;
	}

	private Expression preecindExpre;
	public ArrayConstructor astOverride;
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
		if(null != astOverride && !(visitor instanceof ScopeAndTypeChecker)) {
			return visitor.visit(astOverride);
		}
		
		
		return visitor.visit(this);
	}

	@Override
	public Node copyTypeSpecific() {
		PointerUnref ret = new PointerUnref(line, column, size, (Expression)rhs.copy());
		return ret;
	}
}
