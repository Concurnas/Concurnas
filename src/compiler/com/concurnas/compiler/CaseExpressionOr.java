package com.concurnas.compiler;

import java.util.ArrayList;

import com.concurnas.compiler.ast.ElifUnit;
import com.concurnas.compiler.ast.Node;
import com.concurnas.compiler.ast.Utils;
import com.concurnas.compiler.ast.interfaces.Expression;
import com.concurnas.compiler.visitors.Visitor;

public class CaseExpressionOr extends CaseExpression {

	public CaseExpression head;
	public ArrayList<CaseExpression> caseOrs;

	public CaseExpressionOr(int line, int col, CaseExpression head, ArrayList<CaseExpression> caseOrs) {
		super(line, col);
		this.head = head;
		this.caseOrs = caseOrs;
	}

	@Override
	public Object accept(Visitor visitor) {
		return visitor.visit(this);
	}

	@Override
	public Node copyTypeSpecific() {
		CaseExpressionOr ret = new CaseExpressionOr(line, super.column, (CaseExpression)head.copy(), (ArrayList<CaseExpression>) Utils.cloneArrayList(caseOrs));
		ret.alsoCondition = this.alsoCondition == null?null:(Expression)this.alsoCondition.copy();
		return ret;
	}
}
