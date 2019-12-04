package com.concurnas.compiler;

import java.util.HashMap;

import com.concurnas.compiler.ast.Node;
import com.concurnas.compiler.ast.RefName;
import com.concurnas.compiler.ast.interfaces.Expression;

public abstract class CaseExpression extends Node {

	public CaseExpression(int line, int column) {
		super(line, column);
	}

	public Expression alsoCondition;
	
	/*@Override
	public Object accept(Visitor visitor) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Node copyTypeSpecific() {
		// TODO Auto-generated method stub
		return null;
	}*/

}
