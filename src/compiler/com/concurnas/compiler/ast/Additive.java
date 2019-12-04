package com.concurnas.compiler.ast;

import java.util.ArrayList;
import java.util.List;

import com.concurnas.compiler.ast.interfaces.Expression;
import com.concurnas.compiler.utils.Fourple;
import com.concurnas.compiler.utils.Thruple;
import com.concurnas.compiler.visitors.VectorizedRedirector;
import com.concurnas.compiler.visitors.Visitor;
import com.concurnas.runtime.Pair;

public class Additive extends AbstractExpression implements Expression, CanBeInternallyVectorized, AutoVectorizableElements {

	public ArrayList<AddMinusExpressionElement> elements;
	public Expression head;
	public boolean headshouldStringyFyAtBCTime=false;
	public Type headType;
	private Block vectorizedRedirect=null;
	
	public Additive(int line, int col, Expression head, ArrayList<AddMinusExpressionElement> elements) {
		super(line, col);
		this.head = head;
		this.elements = elements;
	}
	public Additive(int line, int col, Expression head, boolean isPlus, Expression one) {
		super(line, col);
		this.head = head;
		this.elements = new ArrayList<AddMinusExpressionElement>();
		elements.add(new AddMinusExpressionElement(line, col, isPlus, one));
	}
	
	
	@Override
	public List<VectorizationConfig> getAllElements() {
		List<VectorizationConfig> allExprs = new ArrayList<VectorizationConfig>(elements.size() + 1);
		
		Expression exprToAdd = null;
		Boolean canBeObject= null;
		String opOverload= null;
		
		exprToAdd = this.head;
		
		for(AddMinusExpressionElement expr : this.elements) {
			canBeObject= false;
			opOverload= expr.isPlus?"plus":"minus";
			
			allExprs.add(new VectorizationConfig(exprToAdd, canBeObject, opOverload, expr.isPlus, false, false));
			
			canBeObject=null;
			opOverload=null;
			exprToAdd = expr.exp;//expr.astOverrideOperatorOverload != null ? expr.astOverrideOperatorOverload : expr.exp;
		}
		
		allExprs.add(new VectorizationConfig(exprToAdd, canBeObject, opOverload, null, false, false));
		
		return allExprs;
	}
	
	@Override
	public void setAllElements(List<Expression> newones) {
		head = newones.get(0);
		int n=0;
		for(Expression replace : newones.subList(1, newones.size())) {
			elements.get(n++).exp = replace;
		}
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
		Additive x = new Additive(super.line, super.column, (Expression)head.copy(), (ArrayList<AddMinusExpressionElement>) Utils.cloneArrayList(elements));
		x.headshouldStringyFyAtBCTime = headshouldStringyFyAtBCTime;	
		x.headType = headType;	
		x.preceedingExpression = preceedingExpression==null?null:(Expression)preceedingExpression.copy();
		x.vectorizedRedirect = vectorizedRedirect==null?null: (Block)vectorizedRedirect.copy();

		return x;
	}
	
	private Expression preceedingExpression;
	public ArrayList<Pair<Boolean, NullStatus>> depth=null;
	@Override
	public void setPreceedingExpression(Expression expr) {
		this.preceedingExpression = expr;
	}
	@Override
	public Expression getPreceedingExpression() {
		return preceedingExpression;
	}
	
	public void setShouldBePresevedOnStack(boolean should){
		super.setShouldBePresevedOnStack(should);
		for(AddMinusExpressionElement e : elements){
			((Node)e.exp).setShouldBePresevedOnStack(should);
		}
		
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
