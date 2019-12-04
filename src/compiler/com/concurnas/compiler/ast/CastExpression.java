package com.concurnas.compiler.ast;

import java.util.ArrayList;

import com.concurnas.compiler.ast.interfaces.Expression;
import com.concurnas.compiler.visitors.VectorizedRedirector;
import com.concurnas.compiler.visitors.Visitor;
import com.concurnas.runtime.Pair;

public class CastExpression extends Node implements Expression, CanBeInternallyVectorized {

	public Type t;
	public Expression o;
	private Block vectorizedRedirect;

	public CastExpression(int line, int col, Type t, Expression o) {
		super(line, col);
		this.t =t ;
		this.o = o;
	}
	
	@Override
	public Object accept(Visitor visitor) {
		visitor.setLastLineVisited(super.getLine());
		
		if(null != vectorizedRedirect){
			if(!(visitor instanceof VectorizedRedirector)){
				//if(visitor instanceof ScopeAndTypeChecker){
				//	visitor.visit(this);//visit self as normal in satc
				//}
				
				//if vectorizeRedirector, ignore
				return vectorizedRedirect.accept(visitor);
			}
		}
		
		return visitor.visit(this);
	}

	public void setShouldBePresevedOnStack(boolean should) {
		((Node)this.o).setShouldBePresevedOnStack(should);
	}
	
	@Override
	public Node copyTypeSpecific() {
		CastExpression ret = new CastExpression(super.line, super.column, (Type)t.copy(), (Expression) o.copy());
		ret.preceedingExpression = preceedingExpression==null?null:(Expression)preceedingExpression.copy();
		ret.vectorizedExpr = vectorizedExpr;
		ret.vectorizedRedirect = vectorizedRedirect==null?null: (Block)vectorizedRedirect.copy();
		
		return ret;
	}
	private Expression preceedingExpression;
	public ArrayList<Pair<Boolean, NullStatus>> vectorizedExpr = null;
	
	@Override
	public void setPreceedingExpression(Expression expr) {
		this.preceedingExpression = expr;
	}
	@Override
	public Expression getPreceedingExpression() {
		return preceedingExpression;
	}
	
	@Override
	public boolean hasBeenVectorized(){
		return this.vectorizedExpr != null; 
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
	@Override
	public boolean hasErroredAlready() {
		return hasErrored;
	}
	@Override
	public void setHasErroredAlready(boolean hasError) {
		this.hasErrored=hasError;
	}	
}
