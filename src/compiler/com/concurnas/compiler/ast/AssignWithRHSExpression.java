package com.concurnas.compiler.ast;

import com.concurnas.compiler.ast.interfaces.Expression;

public interface AssignWithRHSExpression {

	public abstract Expression getRHSExpression();
	public abstract Expression setRHSExpression(Expression what);
}
