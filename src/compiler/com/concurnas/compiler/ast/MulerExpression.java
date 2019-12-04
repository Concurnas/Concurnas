package com.concurnas.compiler.ast;

import java.util.ArrayList;
import java.util.List;

import com.concurnas.compiler.ast.interfaces.Expression;
import com.concurnas.compiler.utils.Fourple;
import com.concurnas.compiler.utils.Thruple;
import com.concurnas.compiler.visitors.VectorizedRedirector;
import com.concurnas.compiler.visitors.Visitor;
import com.concurnas.runtime.Pair;

public class MulerExpression extends AbstractExpression implements Expression, CanBeInternallyVectorized, AutoVectorizableElements {

	public ArrayList<MulerElement> elements;
	public Expression header;

	public MulerExpression(int line, int col, Expression header, ArrayList<MulerElement> elements) {
		super(line, col);
		this.header = header;
		this.elements = elements;
	}
	
	public MulerExpression(int line, int col, Expression header, MulerExprEnum mulOper, Expression expr) {
		super(line, col);
		this.header = header;
		this.elements = new ArrayList<MulerElement>();
		this.elements.add(new MulerElement(line, col, mulOper, expr));
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
	public List<VectorizationConfig> getAllElements() {
		List<VectorizationConfig> allExprs = new ArrayList<VectorizationConfig>(elements.size() + 1);
		
		Expression exprToAdd = null;
		Boolean canBeObject= null;
		String opOverload= null;
		
		exprToAdd = this.header;
		
		for(MulerElement expr : this.elements) {
			canBeObject= false;
			opOverload= expr.mulOper.methodName;
			
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
	
	@Override
	public Node copyTypeSpecific() {
		MulerExpression ret = new MulerExpression(super.getLine(), super.getColumn(), (Expression)header.copy(), (ArrayList<MulerElement>) Utils.cloneArrayList(elements) );
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
