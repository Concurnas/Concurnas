package com.concurnas.compiler.ast;

import com.concurnas.compiler.ast.interfaces.Expression;

public abstract class AbstractExpression extends Node implements Expression {
	public AbstractExpression(int line, int column) {
		super(line, column);
	}

	public boolean hasBeenVectorized(){
		return false;
	}
}
