package com.concurnas.compiler.ast;

import java.util.ArrayList;

import com.concurnas.compiler.ast.interfaces.Expression;
import com.concurnas.compiler.visitors.VectorizedRedirector;
import com.concurnas.compiler.visitors.Visitor;
import com.concurnas.runtime.Pair;

public class VectorizedFieldRef extends AbstractExpression implements Expression, HasDepth, CanBeInternallyVectorized {
	public final boolean doubledot;
	public final boolean nullsafe;
	public final boolean noNullAssertion;
	public final Expression expr;
	public RefName name;
	private Block vectorizedRedirect=null;

	public VectorizedFieldRef(int line, int col, RefName name,  Expression expr, boolean doubledot, boolean nullsafe, boolean noNullAssertion) {
		super(line, col);
		this.name = name;
		this.expr=expr;
		this.doubledot=doubledot;
		this.nullsafe=nullsafe;
		this.noNullAssertion=noNullAssertion;
	}

	private Expression preceedingExpression;
	private ArrayList<Pair<Boolean, NullStatus>> depth;
	
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
		
		return visitor.visit(this);
	}

	@Override
	public Node copyTypeSpecific() {
		VectorizedFieldRef ret = new VectorizedFieldRef(line, column, name, (Expression)expr.copy(), doubledot, nullsafe, noNullAssertion);
		ret.vectorizedRedirect = vectorizedRedirect==null?null: (Block)vectorizedRedirect.copy();
		
		return ret;
	}
	
	
	@Override
	public boolean getCanBeOnItsOwnLine(){
		return true;
	}
	
	public ArrayList<Pair<Boolean, NullStatus>> getDepth() {
		return depth;
	}
	
	public void setDepth(ArrayList<Pair<Boolean, NullStatus>> depth) {
		this.depth = depth;
	}

	@Override
	public boolean hasVectorizedRedirect() {
		return this.vectorizedRedirect != null;
	}

	@Override
	public void setVectorizedRedirect(Node vectRedirect) {
		this.vectorizedRedirect = (Block)vectRedirect;
	}
	
	@Override
	public boolean canBeNonSelfReferncingOnItsOwn() {
		return true;
	}
	

	
	private boolean hasErrored=false;
	public boolean validAtThisLocation=false;
	@Override
	public boolean hasErroredAlready() {
		return hasErrored;
	}
	@Override
	public void setHasErroredAlready(boolean hasError) {
		this.hasErrored=hasError;
	}
}
