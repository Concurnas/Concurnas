package com.concurnas.compiler.ast;

import java.util.ArrayList;

import com.concurnas.compiler.ast.interfaces.Expression;
import com.concurnas.compiler.visitors.VectorizedRedirector;
import com.concurnas.compiler.visitors.Visitor;
import com.concurnas.runtime.Pair;

public class ElvisOperator extends AbstractExpression implements Expression, CanBeInternallyVectorized{

	public Expression lhsExpression;
	public Expression rhsExpression;
	public ArrayList<Pair<Boolean, NullStatus>> depth = null;//vectorization

	public ElvisOperator(int line, int col, Expression lhsExpression, Expression rhsExpression) {
		super(line, col);
		this.lhsExpression = lhsExpression;
		this.rhsExpression = rhsExpression;
	}
	
	@Override
	public boolean getCanBeOnItsOwnLine(){
		return true;
	}
	
	@Override
	public Node copyTypeSpecific() {
		ElvisOperator ret = new ElvisOperator(super.getLine(), super.getColumn(), (Expression)lhsExpression.copy(), (Expression)rhsExpression.copy());
		ret.preceedingExpression = preceedingExpression==null?null:(Expression)preceedingExpression.copy();
		ret.vectorizedRedirect = vectorizedRedirect==null?null: (Block)vectorizedRedirect.copy();

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
		
		return visitor.visit(this);
	}
	
	private Block vectorizedRedirect=null;
	
	@Override
	public boolean hasVectorizedRedirect() {
		return this.vectorizedRedirect != null;
	}

	@Override
	public void setVectorizedRedirect(Node vectRedirect) {
		this.vectorizedRedirect = (Block)vectRedirect;
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
