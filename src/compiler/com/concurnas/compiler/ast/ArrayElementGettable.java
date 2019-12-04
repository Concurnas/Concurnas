package com.concurnas.compiler.ast;

import java.util.ArrayList;

import com.concurnas.compiler.ast.interfaces.Expression;
import com.concurnas.compiler.visitors.Visitor;

public interface ArrayElementGettable {
	public ArrayList<Expression> getArrayElements(Visitor askee);
}
