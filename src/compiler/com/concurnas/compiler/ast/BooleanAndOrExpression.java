package com.concurnas.compiler.ast;

import java.util.ArrayList;
import java.util.List;

import com.concurnas.compiler.ast.interfaces.Expression;
import com.concurnas.compiler.visitors.Visitor;
import com.concurnas.runtime.Pair;

public abstract class BooleanAndOrExpression extends AbstractExpression implements CanBeInternallyVectorized {
	
	public List<RedirectableExpression> things;
	public Expression head;
	public boolean isAnd;

	public BooleanAndOrExpression(int line, int col, Expression head, ArrayList<RedirectableExpression> ands, boolean isAnd) {
		super(line, col);
		this.head = head;
		this.things = ands;
		this.isAnd = isAnd;
	}
	
	public BooleanAndOrExpression(int line, int col, ArrayList<RedirectableExpression> ands, boolean isAnd) {
		super(line, col);
		this.head = ands.get(0);
		this.things = ands.subList(1, ands.size());
		this.isAnd = isAnd;
	}
	
	public Expression getHead() {
		return this.head;
	}

	public List<RedirectableExpression> getElems() {
		return this.things;
	}

	public String getName(){
		return isAnd?"and":"or";
		
	}
	
	public abstract Object accept(Visitor visitor);
	
	public abstract String getMethodEquiv();
	

	public ArrayList<Pair<Boolean, NullStatus>> depth = null;
	protected Block vectorizedRedirect=null;
	
	@Override
	public boolean hasVectorizedRedirect() {
		return this.vectorizedRedirect != null;
	}

	@Override
	public void setVectorizedRedirect(Node vectRedirect) {
		this.vectorizedRedirect = (Block)vectRedirect;
	}
	
}
