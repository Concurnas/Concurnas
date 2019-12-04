package com.concurnas.compiler.ast;

import java.util.ArrayList;
import java.util.List;

import com.concurnas.compiler.ast.interfaces.Expression;
import com.concurnas.compiler.utils.Fourple;
import com.concurnas.compiler.visitors.VectorizedRedirector;
import com.concurnas.compiler.visitors.Visitor;
import com.concurnas.runtime.Pair;

public class BitwiseOperation extends AbstractExpression implements Expression, CanBeInternallyVectorized, AutoVectorizableElements {

	public List<RedirectableExpression> things;
	public Expression head;
	public BitwiseOperationEnum oper;
	
	public BitwiseOperation(int line, int col, BitwiseOperationEnum oper, Expression head, ArrayList<RedirectableExpression> things) {
		super(line, col);
		this.oper = oper;
		this.head = head;
		this.things = things;
	}
	
	public BitwiseOperation(int line, int col, BitwiseOperationEnum oper, Expression head, RedirectableExpression thing) {
		super(line, col);
		this.oper = oper;
		this.head = head;
		this.things = new ArrayList<RedirectableExpression>();
		things.add(thing);
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
		BitwiseOperation expr = new BitwiseOperation(super.line, super.column, oper, (Expression)head.copy(), (ArrayList<RedirectableExpression>) Utils.cloneArrayList(things));

		expr.preceedingExpression = preceedingExpression==null?null:(Expression)preceedingExpression.copy();
		expr.vectorizedRedirect = vectorizedRedirect==null?null: (Block)vectorizedRedirect.copy();

		return expr;
	}
	
	public String getMethodEquiv(){
		return oper.operationName;
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
	
	@Override
	public List<VectorizationConfig> getAllElements() {
		List<VectorizationConfig> allExprs = new ArrayList<VectorizationConfig>(things.size() + 1);
		
		Expression exprToAdd = null;
		Boolean canBeObject= null;
		String opOverload= null;
		
		exprToAdd = this.head;
		
		for(RedirectableExpression expr : this.things) {
			canBeObject= false;
			opOverload= oper.operationName;
			
			allExprs.add(new VectorizationConfig(exprToAdd, canBeObject, opOverload, false, false, false));
			
			canBeObject=null;
			opOverload=null;
			exprToAdd =  expr.exp;//expr.astOverrideOperatorOverload != null ? expr.astOverrideOperatorOverload : expr.e2;
		}
		
		allExprs.add(new VectorizationConfig(exprToAdd, canBeObject, opOverload, null, false, false));
		
		return allExprs;
	}
	
	@Override
	public void setAllElements(List<Expression> newones) {
		head = newones.get(0);
		int n=0;
		for(Expression replace : newones.subList(1, newones.size())) {
			things.get(n++).exp = replace;
		}
	}
	
}


