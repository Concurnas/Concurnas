package com.concurnas.compiler.ast;

import com.concurnas.compiler.ast.interfaces.Expression;
import com.concurnas.compiler.visitors.Visitor;

public class AddMinusExpressionElement extends Node {
	
	public Boolean isPlus = null; //null means its the start
	public Expression exp;
	public boolean shouldStringyFyAtBCTime=false;
	public FuncInvoke astOverrideOperatorOverload=null;
		
	public AddMinusExpressionElement(int line, int col, boolean isPlus, Expression exp) {
		super(line, col);
		this.isPlus = isPlus;
		this.exp = exp;
	}

	@Override
	public Object accept(Visitor visitor) {
		visitor.setLastLineVisited(super.getLine());
		if(null != astOverrideOperatorOverload){
			return visitor.visit(this.astOverrideOperatorOverload);
		}
		
		return visitor.visit(this);
	}

	@Override
	public Node copyTypeSpecific() {
		AddMinusExpressionElement ret =  new AddMinusExpressionElement(super.line, super.column, isPlus, (Expression) exp.copy());
		ret.astOverrideOperatorOverload = (FuncInvoke)(astOverrideOperatorOverload==null?null:astOverrideOperatorOverload.copy());
		
		return ret;
	}
}
