package com.concurnas.compiler.ast;

import java.util.ArrayList;

import com.concurnas.compiler.ast.interfaces.Expression;
import com.concurnas.compiler.visitors.VectorizedRedirector;
import com.concurnas.compiler.visitors.Visitor;
import com.concurnas.runtime.Pair;

public class VectorizedNew extends AbstractExpression implements Expression, HasDepth, CanBeInternallyVectorized {
	public final boolean doubledot;
	public final boolean nullsafe;
	public final boolean noNullAssertion;
	public final Expression lhs;
	public Expression constru;
	private Block vectorizedRedirect=null;

	public VectorizedNew(int line, int col, Expression lhs, Expression constru, boolean doubledot, boolean nullsafe, boolean noNullAssertion) {
		super(line, col);
		this.lhs = lhs;
		this.constru = constru;
		this.doubledot = doubledot;
		this.nullsafe = nullsafe;
		this.noNullAssertion = noNullAssertion;
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
		VectorizedNew ret = new VectorizedNew(line, column, (Expression)lhs.copy(), (Expression)constru.copy(), doubledot, nullsafe, noNullAssertion);
		ret.vectorizedRedirect = vectorizedRedirect==null?null: (Block)vectorizedRedirect.copy();
		return ret;
	}
	
	@Override
	public boolean getShouldBePresevedOnStack()	{
		return ((Node)this.constru).getShouldBePresevedOnStack();
	}
	
	@Override
	public void setShouldBePresevedOnStack(boolean should)	{
		((Node)this.constru).setShouldBePresevedOnStack(should);
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
	@Override
	public boolean hasErroredAlready() {
		return hasErrored;
	}
	@Override
	public void setHasErroredAlready(boolean hasError) {
		this.hasErrored=hasError;
	}
}
