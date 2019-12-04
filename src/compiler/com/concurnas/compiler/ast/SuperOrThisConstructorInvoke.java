package com.concurnas.compiler.ast;

import com.concurnas.compiler.ast.interfaces.Expression;

public abstract class SuperOrThisConstructorInvoke extends AbstractExpression implements Expression {

	public FuncInvokeArgs args;
	public FuncType resolvedFuncType;
	public NamedType parNestorToAdd;
	public boolean isEnumconstru;
	public boolean isEnumconstruSubClass;
	
	public SuperOrThisConstructorInvoke(int line, int column, FuncInvokeArgs args) {
		super(line, column);
		this.args = args;
	}
	
	@Override
	public boolean getCanBeOnItsOwnLine(){
		return true;
	}
}