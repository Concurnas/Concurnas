package com.concurnas.compiler.ast;

import java.util.ArrayList;

import com.concurnas.compiler.ast.interfaces.Expression;
import com.concurnas.compiler.visitors.Visitor;

public class VarString extends AbstractExpression implements Expression {

	public String str;
	public ArrayList<Expression> subExpressions = null;

	public VarString(int line, int col, String str) {
		super(line, col);
		this.str= str;
	}
	
	
	@Override
	public Object accept(Visitor visitor) {
		visitor.setLastLineVisited(super.getLine());
		return visitor.visit(this);
	}
	
	@Override
	public Node copyTypeSpecific() {
		VarString ret = new VarString(line, column, str);
		
		if(subExpressions != null){
			ret.subExpressions = new ArrayList<Expression>(subExpressions.size());
			for(Expression e: subExpressions){
				ret.subExpressions.add((Expression)((Node)e).copy());
			}
		}

		ret.preceedingExpression = preceedingExpression==null?null:(Expression)preceedingExpression.copy();
		ret.foundSubs = foundSubs;

		return ret;
	}
	private Expression preceedingExpression;
	public boolean foundSubs = false;
	@Override
	public void setPreceedingExpression(Expression expr) {
		this.preceedingExpression = expr;
	}
	@Override
	public Expression getPreceedingExpression() {
		return preceedingExpression;
	}
}
