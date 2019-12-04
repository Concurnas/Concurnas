package com.concurnas.compiler.ast;

import com.concurnas.compiler.ast.interfaces.Expression;
import com.concurnas.compiler.visitors.Visitor;

public class ArrayRefElementSubList extends ArrayRefElement
{
	public Expression e2;

	public ArrayRefElementSubList(int line, int col, Expression lhs, Expression rhs) {
		super(line, col, lhs);
		this.e2 = rhs;
	}
	
	@Override
	public Object accept(Visitor visitor) {
		visitor.setLastLineVisited(super.getLine());
		if(null != astOverrideOperatorOverload ){
			return astOverrideOperatorOverload.accept(visitor);
		}
		return visitor.visit(this);
	}
	
	@Override
	public Node copyTypeSpecific() {
		ArrayRefElementSubList are = new ArrayRefElementSubList(super.line, super.column,(Expression)e1.copy(), (Expression)e2.copy());
		are.astOverrideOperatorOverload = (FuncInvoke)(astOverrideOperatorOverload==null?null:astOverrideOperatorOverload.copyTypeSpecific());
		are.rhsOfAssigmentType = (Expression)(rhsOfAssigmentType==null?rhsOfAssigmentType:rhsOfAssigmentType.copy());
		are.rhsOfAssigmentEQ = rhsOfAssigmentEQ;
		return are;
	}

	@Override
	public String getMethodEquivName(){
		return rhsOfAssigmentType!=null?"subAssign":"sub";
	}
	
	@Override
	public boolean isSingleElementRefEle(){
		return false;
	}
	
}
