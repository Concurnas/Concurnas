package com.concurnas.compiler;

import java.util.ArrayList;

import com.concurnas.compiler.ast.Node;
import com.concurnas.compiler.ast.Type;
import com.concurnas.compiler.ast.Utils;
import com.concurnas.compiler.ast.interfaces.Expression;
import com.concurnas.compiler.visitors.Visitor;

public class TypedCaseExpression extends CaseExpression {
	public  ArrayList<Type> types;
	public CaseExpression caseExpression;

	public TypedCaseExpression(int line, int col, ArrayList<Type> types, CaseExpression caseExpression) {
		super(line, col);
		this.types = types;
		this.caseExpression = caseExpression;
	}


	@Override
	public Object accept(Visitor visitor) {
		return visitor.visit(this);
	}
	@Override
	public Node copyTypeSpecific() {
		TypedCaseExpression ret = new TypedCaseExpression(line, column, (ArrayList<Type>) Utils.cloneArrayList(this.types), (CaseExpression)caseExpression.copy());
		ret.alsoCondition = this.alsoCondition == null?null:(Expression)this.alsoCondition.copy();
		return ret;
	}

}
