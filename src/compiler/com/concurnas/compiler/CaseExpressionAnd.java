package com.concurnas.compiler;

import java.util.ArrayList;

import com.concurnas.compiler.ast.Node;
import com.concurnas.compiler.ast.Utils;
import com.concurnas.compiler.ast.interfaces.Expression;
import com.concurnas.compiler.visitors.Visitor;

public class CaseExpressionAnd extends CaseExpression {
	public CaseExpression head;
	public ArrayList<CaseExpression> caseAnds;

	public CaseExpressionAnd(int line, int col, CaseExpression head, ArrayList<CaseExpression> caseAnds) {
		super(line, col);
		this.head = head;
		this.caseAnds = caseAnds;
	}

	@Override
	public Object accept(Visitor visitor) {
		return visitor.visit(this);
	}

	@Override
	public Node copyTypeSpecific() {
		CaseExpressionOr ret = new CaseExpressionOr(line, super.column, (CaseExpression)head.copy(), (ArrayList<CaseExpression>) Utils.cloneArrayList(caseAnds));
		ret.alsoCondition = this.alsoCondition == null?null:(Expression)this.alsoCondition.copy();
		return ret;
	}

}
