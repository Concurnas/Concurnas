package com.concurnas.compiler.ast;

import java.util.ArrayList;
import java.util.List;

import com.concurnas.compiler.ast.interfaces.Expression;
import com.concurnas.compiler.utils.Fourple;
import com.concurnas.compiler.utils.Thruple;
import com.concurnas.compiler.visitors.VectorizedRedirector;
import com.concurnas.compiler.visitors.Visitor;
import com.concurnas.runtime.Pair;

public class EqReExpression extends AbstractExpression implements Expression, CanBeInternallyVectorized, AutoVectorizableElements{

	public Expression head;
	public ArrayList<GrandLogicalElement> elements;

	public EqReExpression(int line, int col, Expression head,
			ArrayList<GrandLogicalElement> elements) {
		super(line, col);
		this.head = head;
		this.elements = elements;
	}
	
	public EqReExpression(int line, int col, Expression head, GrandLogicalOperatorEnum compOp2, Expression e2) {
		super(line, col);
		this.head = head;
		ArrayList<GrandLogicalElement> elements = new ArrayList<GrandLogicalElement>();
		elements.add(new GrandLogicalElement(line, col, compOp2, e2) );
		this.elements = elements;
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
		
		exprToAdd = this.head;
		
		for(GrandLogicalElement expr : this.elements) {
			boolean relatioal = expr.compOp.isRelational();
			canBeObject= !relatioal;
			//expr.compOp == GrandLogicalOperatorEnum.EQ || expr.compOp == GrandLogicalOperatorEnum.NE;
			if(expr.compOp == GrandLogicalOperatorEnum.REFEQ || expr.compOp == GrandLogicalOperatorEnum.REFNE) {
				opOverload = null;
			}else if(relatioal) {
				opOverload = "compareTo";
			}else {
				opOverload = null;
			}
			
			
			allExprs.add(new VectorizationConfig(exprToAdd, canBeObject, opOverload, false, false, canBeObject));
			
			canBeObject=null;
			opOverload=null;
			exprToAdd =  expr.e2;//expr.astOverrideOperatorOverload != null ? expr.astOverrideOperatorOverload : expr.e2;
		}
		
		allExprs.add(new VectorizationConfig(exprToAdd, true, opOverload, null, false, false));
		
		return allExprs;
	}
	
	@Override
	public void setAllElements(List<Expression> newones) {
		head = newones.get(0);
		int n=0;
		for(Expression replace : newones.subList(1, newones.size())) {
			elements.get(n++).e2 = replace;
		}
	}
	
	public boolean isFirstThingRelationalOrNothing()
	{
		return elements.isEmpty() || elements.get(0).compOp.isRelational();
	}

	@Override
	public Node copyTypeSpecific() {
		EqReExpression ret = new EqReExpression(super.line, super.column, (Expression)head.copy() , (ArrayList<GrandLogicalElement>)Utils.cloneArrayList(elements));
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
