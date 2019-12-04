package com.concurnas.compiler.ast;

import com.concurnas.compiler.ast.interfaces.Expression;
import com.concurnas.compiler.visitors.Visitor;

public class MulerElement extends Node {

	public MulerExprEnum mulOper;
	public Expression expr;
	public FuncInvoke astOverrideOperatorOverload=null;

	public MulerElement(int line, int col, MulerExprEnum mulOper, Expression expr) {
		super(line, col);
		this.mulOper = mulOper;
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
		MulerElement ret = new MulerElement(super.getLine(), super.getColumn(), mulOper, (Expression)expr.copy());
		ret.astOverrideOperatorOverload = (FuncInvoke)(astOverrideOperatorOverload==null?null:astOverrideOperatorOverload.copy());
		
		return ret;
	}

}
