package com.concurnas.compiler.ast;

import java.util.ArrayList;

import com.concurnas.compiler.ast.interfaces.Expression;
import com.concurnas.compiler.visitors.VectorizedRedirector;
import com.concurnas.compiler.visitors.Visitor;

public class AndExpression extends BooleanAndOrExpression implements Expression {

	public AndExpression(int line, int col, Expression head, ArrayList<RedirectableExpression> ands) {
		super(line, col, head, ands, true);
	}
	
	public static AndExpression AndExpressionBuilder(int line, int col, Expression head, RedirectableExpression otherOne) {
		ArrayList<RedirectableExpression> ands = new ArrayList<RedirectableExpression>(1);
		ands.add(otherOne);
		return new AndExpression(line, col, head, ands);
	}

	@Override
	public Object accept(Visitor visitor) {
		visitor.setLastLineVisited(super.getLine());
		if(null != vectorizedRedirect){
			if(!(visitor instanceof VectorizedRedirector)){
				return vectorizedRedirect.accept(visitor);
			}
		}
		return visitor.visit(this);
	}


	@Override
	public Node copyTypeSpecific() {
		AndExpression expr = new AndExpression(super.line, super.column, (Expression)head.copy(), (ArrayList<RedirectableExpression>) Utils.cloneArrayList(super.things));

		expr.preceedingExpression = preceedingExpression==null?null:(Expression)preceedingExpression.copy();
		expr.vectorizedRedirect = vectorizedRedirect==null?null: (Block)vectorizedRedirect.copy();

		return expr;
	}
	
	public String getMethodEquiv(){
		return "and";
	}
	private Expression preceedingExpression;
	@Override
	public void setPreceedingExpression(Expression expr) {
		this.preceedingExpression = expr;
	}
	@Override
	public Expression getPreceedingExpression() {
		return preceedingExpression;
	}
	
	private boolean hasErrored=false;
	@Override
	public boolean hasErroredAlready() {
		return hasErrored;
	}
	@Override
	public void setHasErroredAlready(boolean hasError) {
		this.hasErrored=hasError;
	}
}
