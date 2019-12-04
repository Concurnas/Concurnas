package com.concurnas.compiler.ast;

import com.concurnas.compiler.visitors.Visitor;

public class NOP extends Statement {

	public boolean oneAndPop;

	public NOP(boolean oneAndPop) {
		super(0, 0, true);
		this.oneAndPop = oneAndPop;
	}
	
	public NOP() {
		this(false);
	}

	@Override
	public Node copyTypeSpecific() {
		return this;
	}

	@Override
	public Object accept(Visitor visitor) {
		visitor.setLastLineVisited(super.getLine());
		return visitor.visit(this);
	}
}
