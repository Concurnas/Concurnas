package com.concurnas.compiler.ast;

import com.concurnas.compiler.ast.interfaces.Expression;
import com.concurnas.compiler.visitors.Visitor;

public class ArrayRefElementPrefixAll extends ArrayRefElement {

	public ArrayRefElementPrefixAll(int line, int col, Expression e1) {
		super(line, col, e1);
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
		ArrayRefElementPrefixAll are = new ArrayRefElementPrefixAll(super.line, super.column,(Expression)e1.copy());
		are.astOverrideOperatorOverload = (FuncInvoke)(astOverrideOperatorOverload==null?null:astOverrideOperatorOverload.copyTypeSpecific());
		are.rhsOfAssigmentType = (Expression)(rhsOfAssigmentType==null?rhsOfAssigmentType:rhsOfAssigmentType.copy());
		are.rhsOfAssigmentEQ = rhsOfAssigmentEQ;
		return are;
	}

	@Override
	public String getMethodEquivName(){
		return rhsOfAssigmentType!=null?"subtoAssign":"subto";
	}
	
	@Override
	public boolean isSingleElementRefEle(){
		return false;
	}
	
}
