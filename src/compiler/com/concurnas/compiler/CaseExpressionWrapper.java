package com.concurnas.compiler;

import com.concurnas.compiler.ast.Node;
import com.concurnas.compiler.ast.WithBlock;
import com.concurnas.compiler.ast.interfaces.Expression;
import com.concurnas.compiler.visitors.Visitor;

public class CaseExpressionWrapper extends CaseExpression {
	
	public WithBlock repointedToWithBlock;
	public String repointedToWithBlockStr;
	
	public Expression e;

	public CaseExpressionWrapper(int line, int column, Expression e) {
		super(line, column);
		this.e=e;
	}

	@Override
	public Object accept(Visitor visitor) {
		return visitor.visit(this);
	}

	@Override
	public Node copyTypeSpecific() {
		CaseExpressionWrapper ret = new CaseExpressionWrapper(line, column, (Expression)e.copy());
		ret.alsoCondition = this.alsoCondition == null?null:(Expression)this.alsoCondition.copy();
		return ret;
	}

}
