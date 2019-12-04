package com.concurnas.compiler.ast;

import com.concurnas.compiler.ast.interfaces.Expression;
import com.concurnas.compiler.visitors.Visitor;

public class ThrowStatement extends Statement implements CanEndInReturnOrException {

	public Expression thingTothrow;

	public ThrowStatement(int line, int col, Expression thingTothrow) {
		super(line, col);
		this.thingTothrow = thingTothrow;
	}
	@Override
	public Object accept(Visitor visitor) {
		visitor.setLastLineVisited(super.getLine());
		return visitor.visit(this);
	}
	
	@Override
	public Node copyTypeSpecific() {
		return new ThrowStatement(super.getLine(), super.getColumn(), (Expression)thingTothrow.copy());
	}
	
}
