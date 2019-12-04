package com.concurnas.compiler.ast;

import java.util.ArrayList;

import com.concurnas.compiler.ast.interfaces.Expression;
import com.concurnas.compiler.visitors.ScopeAndTypeChecker;
import com.concurnas.compiler.visitors.Visitor;
import com.concurnas.runtime.Pair;

public class VectorizedArrayRef extends AbstractExpression implements Expression, HasDepth {
	public final boolean doubledot;
	public final boolean nullsafe;
	public final Expression expr;
	public ArrayRefLevelElementsHolder arrayLevelElements=null;
	public ArrayRef astOverridearrayRef;
	
	public VectorizedArrayRef(int line, int col, Expression expr, ArrayRefLevelElementsHolder arrayLevelElements, boolean doubledot, boolean nullsafe) {
		super(line, col);
		this.expr=expr;
		this.arrayLevelElements=arrayLevelElements;
		this.doubledot=doubledot;
		this.nullsafe=nullsafe;
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

		if(null != astOverridearrayRef){
			if(!(visitor instanceof ScopeAndTypeChecker)){
				return astOverridearrayRef.accept(visitor);
			}
		}
		
		return visitor.visit(this);
	}

	@Override
	public Node copyTypeSpecific() {
		return new VectorizedArrayRef(line, column, (Expression)expr.copy(), arrayLevelElements.clone(), doubledot, nullsafe);
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

	/*@Override
	public boolean canBeNonSelfReferncingOnItsOwn() {
		return true;
	}
	*/

	
	private boolean hasErrored=false;
	public boolean validAtThisLocation=false;
	/*
	@Override
	public boolean hasErroredAlready() {
		return hasErrored;
	}
	@Override
	public void setHasErroredAlready(boolean hasError) {
		this.hasErrored=hasError;
	}*/
}
