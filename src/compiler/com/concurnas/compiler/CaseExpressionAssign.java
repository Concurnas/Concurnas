package com.concurnas.compiler;

import java.util.ArrayList;

import com.concurnas.compiler.ast.Node;
import com.concurnas.compiler.ast.Type;
import com.concurnas.compiler.ast.Utils;
import com.concurnas.compiler.ast.interfaces.Expression;
import com.concurnas.compiler.visitors.Visitor;

public class CaseExpressionAssign extends CaseExpression {

	public String varname;
	public boolean forceNew;
	public boolean isFinal;
	public Expression expr;
	public ArrayList<Type> types;

	public CaseExpressionAssign(int cline, int ccol, String varname, Expression expr, ArrayList<Type> types, boolean forceNew, boolean isFinal) {
		super(cline, ccol);
		this.varname=varname;
		this.expr=expr;
		this.types=types;
		this.forceNew=forceNew;
		this.isFinal=isFinal;
	}

	@Override
	public Object accept(Visitor visitor) {
		return visitor.visit(this);
	}

	@Override
	public Node copyTypeSpecific() {
		CaseExpressionAssign ret= new CaseExpressionAssign(line, column, varname, (Expression)expr.copy(), (ArrayList<Type>) Utils.cloneArrayList(this.types), this.forceNew, this.isFinal);
		ret.alsoCondition = this.alsoCondition == null?null:(Expression)this.alsoCondition.copy();
		return ret;
	}

}
