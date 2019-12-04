package com.concurnas.compiler.ast;

import com.concurnas.compiler.ast.interfaces.Expression;

public abstract class ConstructorInvoke extends Node implements Expression {

	//public boolean isActor;
	
	public ConstructorInvoke(int line, int column) {
		super(line, column);
	}

	@Override
	public boolean isPermissableToGoOnRHSOfADot(Node preceededBy)
	{
		return true;
	}

	@Override
	public boolean getCanBeOnItsOwnLine(){
		return true;
	}
	
	@Override
	public boolean hasBeenVectorized(){
		return false;
	}
}
