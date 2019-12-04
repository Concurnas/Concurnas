package com.concurnas.compiler.ast;

import java.util.ArrayList;
import java.util.List;

import com.concurnas.compiler.ast.interfaces.Expression;
import com.concurnas.compiler.utils.Fourple;
import com.concurnas.compiler.visitors.VectorizedRedirector;
import com.concurnas.compiler.visitors.Visitor;
import com.concurnas.runtime.Pair;

public class ShiftExpression extends AbstractExpression implements Expression, CanBeInternallyVectorized, AutoVectorizableElements {

	public ArrayList<ShiftElement> elements;
	public Expression header;

	public ShiftExpression(int line, int col, Expression header, ArrayList<ShiftElement> elements) {
		super(line, col);
		this.header = header;
		this.elements = elements;
	}
	
	public ShiftExpression(int line, int col, Expression header, ShiftOperatorEnum shiftOp, Expression expr) {
		super(line, col);
		this.header = header;
		this.elements = new ArrayList<ShiftElement>();
		
		this.elements.add(new ShiftElement(line, col, shiftOp, expr));
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
		ShiftExpression ret = new ShiftExpression(super.getLine(), super.getColumn(), (Expression)header.copy(), (ArrayList<ShiftElement>) Utils.cloneArrayList(elements) );
		ret.preceedingExpression = preceedingExpression==null?null:(Expression)preceedingExpression.copy();
		ret.vectorizedRedirect = vectorizedRedirect==null?null: (Block)vectorizedRedirect.copy();
		
		return ret;
	}
	
	
	
	@Override
	public List<VectorizationConfig> getAllElements() {
		List<VectorizationConfig> allExprs = new ArrayList<VectorizationConfig>(elements.size() + 1);
		
		Expression exprToAdd = null;
		Boolean canBeObject= null;
		String opOverload= null;
		
		exprToAdd = this.header;
		
		for(ShiftElement expr : this.elements) {
			canBeObject= false;
			opOverload= expr.shiftOp.methodName;
			
			allExprs.add(new VectorizationConfig(exprToAdd, canBeObject, opOverload, false, false, false));
			
			canBeObject=null;
			opOverload=null;
			exprToAdd =  expr.expr;//expr.astOverrideOperatorOverload != null ? expr.astOverrideOperatorOverload : expr.e2;
		}
		
		allExprs.add(new VectorizationConfig(exprToAdd, canBeObject, opOverload, null, false, false));
		
		return allExprs;
	}
	
	@Override
	public void setAllElements(List<Expression> newones) {
		header = newones.get(0);
		int n=0;
		for(Expression replace : newones.subList(1, newones.size())) {
			elements.get(n++).expr = replace;
		}
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
