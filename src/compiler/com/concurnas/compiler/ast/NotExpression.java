package com.concurnas.compiler.ast;

import java.util.ArrayList;

import org.objectweb.asm.Label;

import com.concurnas.compiler.ast.interfaces.Expression;
import com.concurnas.compiler.visitors.VectorizedRedirector;
import com.concurnas.compiler.visitors.Visitor;
import com.concurnas.runtime.Pair;

public class NotExpression extends AbstractExpression implements Expression, CanBeInternallyVectorized {

	public Expression expr;
	public Label labelAfterNot;
	public FuncInvoke astOverrideOperatorOverload;
	
	public NotExpression(int line, int col, Expression expr) {
		super(line, col);
		this.expr = expr;
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
		NotExpression not = new NotExpression(super.getLine(), super.getColumn(), (Expression)expr.copy() );
		not.astOverrideOperatorOverload = (FuncInvoke)(astOverrideOperatorOverload==null?null:astOverrideOperatorOverload.copy());
		not.preceedingExpression = preceedingExpression==null?null:(Expression)preceedingExpression.copy();
		not.vectorizedRedirect = vectorizedRedirect==null?null: (Block)vectorizedRedirect.copy();

		return not;
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

	public ArrayList<Pair<Boolean, NullStatus>> depth = null;
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
