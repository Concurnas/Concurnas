package com.concurnas.compiler.ast;

import com.concurnas.compiler.ast.interfaces.Expression;
import com.concurnas.compiler.visitors.Visitor;

public class AssertStatement extends Statement {

	public Expression e;
	public VarString message;
	public String messageFromExpr;
	
	public AssertStatement(int line, int col, Expression e) {
		super(line, col);
		this.e = e; //must resolve to boolean
	}

	@Override
	public Object accept(Visitor visitor) {
		visitor.setLastLineVisited(super.getLine());
		return visitor.visit(this);
	}
	
	@Override
	public Node copyTypeSpecific() {
		AssertStatement ret = new AssertStatement(super.line, super.column, (Expression)e.copy());
		ret.message = message;
		ret.messageFromExpr = messageFromExpr;
		return ret;
	}
	@Override
	public boolean getCanReturnAValue(){
		return false;
	}	
}
