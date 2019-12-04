package com.concurnas.compiler;

import com.concurnas.compiler.ast.CaseOperatorEnum;
import com.concurnas.compiler.ast.Node;
import com.concurnas.compiler.ast.interfaces.Expression;
import com.concurnas.compiler.visitors.Visitor;

public class CaseExpressionPost extends CaseExpression {

	public CaseOperatorEnum cop;
	public Expression e;

	public CaseExpressionPost(int line, int column, CaseOperatorEnum cop, Expression e) {
		super(line, column);
		this.cop=cop;
		this.e=e;
	}

	@Override
	public Object accept(Visitor visitor) {
		return visitor.visit(this);
	}

	@Override
	public Node copyTypeSpecific() {
		CaseExpressionPost ret = new CaseExpressionPost(line, column, cop, (Expression)e.copy());
		ret.alsoCondition = this.alsoCondition == null?null:(Expression)this.alsoCondition.copy();
		return ret;
	}

}
