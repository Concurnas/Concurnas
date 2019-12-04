package com.concurnas.compiler;

import com.concurnas.compiler.ast.CaseOperatorEnum;
import com.concurnas.compiler.ast.Node;
import com.concurnas.compiler.ast.interfaces.Expression;
import com.concurnas.compiler.visitors.Visitor;

public class JustAlsoCaseExpression extends CaseExpression {

	public JustAlsoCaseExpression(int line, int column, Expression alsoCondition) {
		super(line, column);
		super.alsoCondition=alsoCondition;
	}

	@Override
	public Object accept(Visitor visitor) {
		return visitor.visit(this);
	}

	@Override
	public Node copyTypeSpecific() {
		return new JustAlsoCaseExpression(line, column, this.alsoCondition == null?null:(Expression)this.alsoCondition.copy());
	}

}
