package com.concurnas.compiler.ast;

import com.concurnas.compiler.ast.interfaces.Expression;
import com.concurnas.compiler.visitors.Visitor;

public class RedirectableExpression  extends AbstractExpression implements Expression {

	public Expression exp;
	public Expression astOverrideOperatorOverload;
	
	public RedirectableExpression(Expression exp){
		super(exp.getLine(), exp.getColumn());
		this.exp = exp;
	}
	
	
	@Override
	public Object accept(Visitor visitor) {
		visitor.setLastLineVisited(super.getLine());
		/*if(visitor instanceof ScopeAndTypeChecker){
			return exp.accept(visitor);
		}
		*/
		if(null != this.astOverrideOperatorOverload){
			return astOverrideOperatorOverload.accept(visitor);
		}
		return exp.accept(visitor);
	}

	@Override
	public Node copyTypeSpecific() {
		RedirectableExpression ret = new RedirectableExpression(exp);
		ret.astOverrideOperatorOverload = (Expression)(astOverrideOperatorOverload==null?null:astOverrideOperatorOverload.copy());
		ret.preceedingExpression = preceedingExpression==null?null:(Expression)preceedingExpression.copy();

		return ret;
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


/*	@Override
	public void setTaggedType(Type rhs){
		
	}*/
}
