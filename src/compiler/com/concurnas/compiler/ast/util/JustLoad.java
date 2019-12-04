package com.concurnas.compiler.ast.util;

import com.concurnas.compiler.ast.Node;
import com.concurnas.compiler.ast.Type;
import com.concurnas.compiler.ast.interfaces.Expression;
import com.concurnas.compiler.visitors.Visitor;

/**
 * For use 
 *
 */
public class JustLoad extends Node implements Expression{
	public Type type;
	public int slot;
	public Integer tupleDecompSlot;
	public Type tupleDecompType;
	
	public JustLoad(int line, int column, int slot, Type type) {
		super(line, column);
		this.slot = slot;
		this.type = type;
	}
	
	@Override
	public Object accept(Visitor visitor) {
		return visitor.visit(this);
	}

	@Override
	public Node copyTypeSpecific() {
		return this;
	}

	private Expression before;
	@Override
	public void setPreceedingExpression(Expression expr) {
		before = expr;
	}

	@Override
	public Expression getPreceedingExpression() {
		return before;
	}

	@Override
	public boolean hasBeenVectorized() {
		return false;
	}
	
}
