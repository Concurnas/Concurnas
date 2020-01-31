package com.concurnas.compiler.ast;

import java.util.ArrayList;

import com.concurnas.compiler.ast.interfaces.Expression;
import com.concurnas.compiler.visitors.ScopeAndTypeChecker;
import com.concurnas.compiler.visitors.VectorizedRedirector;
import com.concurnas.compiler.visitors.Visitor;
import com.concurnas.runtime.Pair;

public class InExpression extends AbstractExpression implements Expression, CanBeInternallyVectorized {

	public Expression thing;
	public Expression insideof;
	public boolean inverted=false;
	public Expression containsMethodCall;
	public boolean isRegexPatternMatch;
	public Block isArrayMatch;
	private Block vectorizedRedirect=null;

	public InExpression(int line, int col, Expression thing, Expression insideof, boolean inverted) {
		super(line, col);
		this.thing=thing;
		this.insideof=insideof;
		this.inverted=inverted;
	}
	
	
	@Override
	public Node copyTypeSpecific() {
		InExpression ret = new InExpression(line, column, (Expression)thing.copy(), (Expression)insideof.copy(), inverted);
		ret.isRegexPatternMatch = isRegexPatternMatch;
		ret.isArrayMatch = isArrayMatch;
		ret.preceedingExpression = preceedingExpression==null?null:(Expression)preceedingExpression.copy();
		ret.vectorizedRedirect = vectorizedRedirect==null?null: (Block)vectorizedRedirect.copy();
		ret.containsMethodCall = containsMethodCall == null ? null: (Expression)containsMethodCall.copy();
		
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
	
	@Override
	public Object accept(Visitor visitor) {
		visitor.setLastLineVisited(super.getLine());
		
		if(null != vectorizedRedirect){
			if(!(visitor instanceof VectorizedRedirector)){
				return vectorizedRedirect.accept(visitor);
			}
		}
		
		if(null != isArrayMatch && !(visitor instanceof ScopeAndTypeChecker)){
			return visitor.visit(isArrayMatch);
		}
		
		if(null != containsMethodCall &&  !(visitor instanceof ScopeAndTypeChecker)) {
			return containsMethodCall.accept(visitor);
		}
		
		return visitor.visit(this);
	}
	
	@Override
	public boolean hasVectorizedRedirect() {
		return this.vectorizedRedirect != null;
	}

	@Override
	public void setVectorizedRedirect(Node vectRedirect) {
		this.vectorizedRedirect = (Block)vectRedirect;
	}


	private boolean hasErrored=false;
	public ArrayList<Pair<Boolean, NullStatus>> depth;
	@Override
	public boolean hasErroredAlready() {
		return hasErrored;
	}
	@Override
	public void setHasErroredAlready(boolean hasError) {
		this.hasErrored=hasError;
	}

}
