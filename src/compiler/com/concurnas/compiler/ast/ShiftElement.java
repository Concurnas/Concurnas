package com.concurnas.compiler.ast;

import com.concurnas.compiler.ast.interfaces.Expression;
import com.concurnas.compiler.visitors.Visitor;

public class ShiftElement extends Node {

	public ShiftOperatorEnum shiftOp;
	public Expression expr;
	public FuncInvoke astOverrideOperatorOverload=null;

	public ShiftElement(int line, int col, ShiftOperatorEnum shiftOp, Expression expr) {
		super(line, col);
		this.shiftOp = shiftOp;
		this.expr = expr;
	}
	
	@Override
	public Object accept(Visitor visitor) {
		visitor.setLastLineVisited(super.getLine());
		if(astOverrideOperatorOverload != null){
			return visitor.visit(astOverrideOperatorOverload);
		}
		
		return visitor.visit(this);
	}

	@Override
	public Node copyTypeSpecific() {
		ShiftElement ret = new ShiftElement(super.getLine(), super.getColumn(), shiftOp, (Expression)expr.copy());
		ret.astOverrideOperatorOverload = (FuncInvoke)(astOverrideOperatorOverload==null?null:astOverrideOperatorOverload.copy());
		
		return ret;
	}

}
