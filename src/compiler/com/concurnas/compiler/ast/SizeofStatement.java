package com.concurnas.compiler.ast;

import com.concurnas.compiler.ast.interfaces.Expression;
import com.concurnas.compiler.visitors.ScopeAndTypeChecker;
import com.concurnas.compiler.visitors.Visitor;

public class SizeofStatement extends AbstractExpression implements Expression {

	public Expression e;
	public String variant;
	
	public SizeofStatement(int line, int col, Expression e, String variant) {
		super(line, col);
		this.e = e; //must resolve to boolean
		this.variant = variant; //must resolve to boolean
	}

	@Override
	public Object accept(Visitor visitor) {
		visitor.setLastLineVisited(super.getLine());
		
		if(null != astOverride && !(visitor instanceof ScopeAndTypeChecker)) {
			return astOverride.accept(visitor);
		}
		
		return visitor.visit(this);
	}
	
	@Override
	public Node copyTypeSpecific() {
		SizeofStatement ret = new SizeofStatement(super.line, super.column, (Expression)e.copy(), variant);
		ret.preceedingExpression = preceedingExpression==null?null:(Expression)preceedingExpression.copy();
		ret.astOverride = astOverride==null?null:(Expression)astOverride.copy();
		return ret;
	}
	private Expression preceedingExpression;
	public Expression astOverride;
	
	@Override
	public void setPreceedingExpression(Expression expr) {
		this.preceedingExpression = expr;
	}
	@Override
	public Expression getPreceedingExpression() {
		return preceedingExpression;
	}
}
