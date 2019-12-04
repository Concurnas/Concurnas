package com.concurnas.compiler.ast;

import com.concurnas.compiler.ast.interfaces.Expression;
import com.concurnas.compiler.visitors.Visitor;

public class GrandLogicalElement extends Node {

	public GrandLogicalOperatorEnum compOp;
	public Expression e2;
	public Expression astOverrideOperatorOverload;

	public GrandLogicalElement(int line, int col, GrandLogicalOperatorEnum compOp2, Expression e2) {
		super(line, col);
		this.compOp = compOp2;
		this.e2 = e2;
	}

	@Override
	public Object accept(Visitor visitor) {
		visitor.setLastLineVisited(super.getLine());
		if(null != astOverrideOperatorOverload){
			return astOverrideOperatorOverload.accept(visitor);
		}
		return visitor.visit(this);
	}
	
	@Override
	public Node copyTypeSpecific() {
		GrandLogicalElement ret = new GrandLogicalElement(line, super.column, compOp, (Expression)e2.copy());
		ret.astOverrideOperatorOverload = astOverrideOperatorOverload==null?null:astOverrideOperatorOverload;
		return ret;
	}
	
}
