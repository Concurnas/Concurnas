package com.concurnas.compiler;

import java.util.ArrayList;

import com.concurnas.compiler.ast.Node;
import com.concurnas.compiler.ast.Type;
import com.concurnas.compiler.ast.Utils;
import com.concurnas.compiler.ast.interfaces.Expression;
import com.concurnas.compiler.visitors.Visitor;

public class CaseExpressionObjectTypeAssign extends CaseExpression {

	public String varname;
	public boolean forceNew;
	public boolean isFinal;
	public CaseExpressionWrapper expr;

	public CaseExpressionObjectTypeAssign(int cline, int ccol, String varname, CaseExpressionWrapper expr, boolean forceNew, boolean isFinal) {
		super(cline, ccol);
		this.varname=varname;
		this.expr=expr;
		this.forceNew=forceNew;
		this.isFinal=isFinal;
	}

	@Override
	public Object accept(Visitor visitor) {
		return visitor.visit(this);
	}

	@Override
	public Node copyTypeSpecific() {
		CaseExpressionObjectTypeAssign ret= new CaseExpressionObjectTypeAssign(line, column, varname, (CaseExpressionWrapper)expr.copy(), this.forceNew, this.isFinal);
		ret.alsoCondition = this.alsoCondition == null?null:(Expression)this.alsoCondition.copy();
		return ret;
	}

}
