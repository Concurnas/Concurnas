package com.concurnas.compiler.ast;

import java.util.ArrayList;

import com.concurnas.compiler.ast.interfaces.Expression;
import com.concurnas.compiler.visitors.VectorizedRedirector;
import com.concurnas.compiler.visitors.Visitor;
import com.concurnas.runtime.Pair;

public class NotNullAssertion extends AbstractExpression implements Expression, CanBeInternallyVectorized{

	public Expression expr;

	public NotNullAssertion(int line, int col, Expression expr) {
		super(line, col);
		this.expr = expr;
	}
	
	@Override
	public boolean getCanBeOnItsOwnLine(){
		return true;
	}
	
	@Override
	public Node copyTypeSpecific() {
		NotNullAssertion ret = new NotNullAssertion(super.getLine(), super.getColumn(), (Expression)expr.copy());
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
	
	
	public ArrayList<Pair<Boolean, NullStatus>> depth = null;
	public Block vectorizedRedirect=null;
	
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

	@Override
	public boolean isPermissableToGoOnRHSOfADot(Node preceededBy) {
		return true;
	}
	
	@Override
	public void setPreceededByThis(boolean preceededByThis) {
		
		((Node)this.expr).setPreceededByThis(preceededByThis);
	}
}
