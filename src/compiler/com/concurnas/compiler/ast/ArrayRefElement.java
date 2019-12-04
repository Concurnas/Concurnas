package com.concurnas.compiler.ast;

import com.concurnas.compiler.ast.interfaces.Expression;
import com.concurnas.compiler.bytecode.BytecodeGennerator;
import com.concurnas.compiler.visitors.Visitor;

public class ArrayRefElement extends Node {
	public Expression e1;
	public boolean isNotActingOnArray = false;
	 
	public enum LISTorMAPType{GET, PUT, GETANDPUT, REMOVE};
	
	public LISTorMAPType liToMap = LISTorMAPType.GET; //default
	public FuncType mapOperationSignature;
	public FuncType mapOperationSignatureforGetter;
	public Type mapTypeOperatingOn;
	public FuncInvoke astOverrideOperatorOverload;
	public FuncInvoke astOverrideOperatorOverloadForGetter;
	
	public Expression rhsOfAssigmentType = null;//bit of a hackthis case a[''] = xxx; sets xxx type here
	public Integer trailingCommas;
	public AssignStyleEnum rhsOfAssigmentEQ;
	
	public ArrayRefElement(int line, int col, Expression e1)
	{
		super(line, col);
		this.e1 = e1;
	}
	
	@Override
	public Object accept(Visitor visitor) {
		visitor.setLastLineVisited(super.getLine());
		if(null != astOverrideOperatorOverload){
			
			if(null != astOverrideOperatorOverloadForGetter) {
				
				if(!(visitor instanceof BytecodeGennerator)) {
					astOverrideOperatorOverload.accept(visitor);
				}
				
				return astOverrideOperatorOverloadForGetter.accept(visitor);
			}
			
			return astOverrideOperatorOverload.accept(visitor);
		}
		return visitor.visit(this);
	}

	@Override
	public Node copyTypeSpecific() {
		ArrayRefElement are = new ArrayRefElement(super.line, super.column,(Expression)e1.copy());
		are.astOverrideOperatorOverload = (FuncInvoke)(astOverrideOperatorOverload==null?null:astOverrideOperatorOverload.copyTypeSpecific());
		are.astOverrideOperatorOverloadForGetter = (FuncInvoke)(astOverrideOperatorOverloadForGetter==null?null:astOverrideOperatorOverloadForGetter.copyTypeSpecific());
		are.rhsOfAssigmentType = (Expression)(rhsOfAssigmentType==null?rhsOfAssigmentType:rhsOfAssigmentType.copy());
		are.rhsOfAssigmentEQ = rhsOfAssigmentEQ;
		are.trailingCommas = trailingCommas;
		return are;
	}
	
	public String getMethodEquivName(){
		return rhsOfAssigmentType!=null?"put":"get";
	}
	
	public boolean isSingleElementRefEle(){
		return true;
	}
	
}
