package com.concurnas.compiler;

import java.util.ArrayList;

import com.concurnas.compiler.ast.Assign;
import com.concurnas.compiler.ast.Node;
import com.concurnas.compiler.ast.Type;
import com.concurnas.compiler.ast.Utils;
import com.concurnas.compiler.ast.interfaces.Expression;
import com.concurnas.compiler.visitors.Visitor;

public class CaseExpressionAssignTuple extends CaseExpression {

	public Expression expr;
	public ArrayList<Assign> lhss;

	public CaseExpressionAssignTuple(int cline, int ccol, Expression expr, ArrayList<Assign> lhss) {
		super(cline, ccol);
		this.expr=expr;
		this.lhss=lhss;
	}

	@Override
	public Object accept(Visitor visitor) {
		return visitor.visit(this);
	}

	@Override
	public Node copyTypeSpecific() {
		CaseExpressionAssignTuple ret= new CaseExpressionAssignTuple(line, column, expr == null?null:(Expression)expr.copy(), (ArrayList<Assign>) Utils.cloneArrayList(this.lhss));
		ret.alsoCondition = this.alsoCondition == null?null:(Expression)this.alsoCondition.copy();
		return ret;
	}

}
