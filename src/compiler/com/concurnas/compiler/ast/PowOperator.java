package com.concurnas.compiler.ast;

import java.util.ArrayList;
import java.util.List;

import com.concurnas.compiler.ast.interfaces.Expression;
import com.concurnas.compiler.utils.Fourple;
import com.concurnas.compiler.utils.Thruple;
import com.concurnas.compiler.visitors.ScopeAndTypeChecker;
import com.concurnas.compiler.visitors.VectorizedRedirector;
import com.concurnas.compiler.visitors.Visitor;
import com.concurnas.runtime.Pair;

public class PowOperator extends AbstractExpression implements Expression, CanBeInternallyVectorized, AutoVectorizableElements {

	public Expression expr;
	public Expression raiseTo;
	public DotOperator astOverrideOperatorOverload=null;

	public PowOperator(int line, int col, Expression expr, Expression raiseTo) {
		super(line, col);
		this.expr = expr;
		this.raiseTo = raiseTo;
	}
	@Override
	public Object accept(Visitor visitor) {
		visitor.setLastLineVisited(super.getLine());

		if(null != vectorizedRedirect){
			if(!(visitor instanceof VectorizedRedirector)){
				return vectorizedRedirect.accept(visitor);
			}
		}
		
		if(visitor instanceof ScopeAndTypeChecker){//sac always via normal method of visitation
			return visitor.visit(this);
		}
		
		if(null != this.astOverrideOperatorOverload){
			return visitor.visit(this.astOverrideOperatorOverload);
		}
		return visitor.visit(this);
	}
	
	@Override
	public Node copyTypeSpecific() {
		PowOperator pow = new PowOperator(super.getLine(), super.getColumn(), (Expression)expr.copy(), (Expression)raiseTo.copy());
		pow.astOverrideOperatorOverload = (DotOperator)(astOverrideOperatorOverload==null?null:astOverrideOperatorOverload.copy());
		pow.preceedingExpression = preceedingExpression==null?null:(Expression)preceedingExpression.copy();
		pow.vectorizedRedirect = vectorizedRedirect==null?null: (Block)vectorizedRedirect.copy();

		return pow;
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
		/*List<Expression> allExprs = new ArrayList<Expression>(2);
		allExprs.add(this.expr);
		allExprs.add(this.raiseTo);
		return allExprs;*/
		
		List<VectorizationConfig> allExprs = new ArrayList<VectorizationConfig>(2);
		
		allExprs.add(new VectorizationConfig(this.expr, false, "pow", false, false, false));
		allExprs.add(new VectorizationConfig(this.raiseTo, null, null, null, false, false));
		
		return allExprs;
	}
	
	@Override
	public void setAllElements(List<Expression> newones) {
		expr = newones.get(0);
		raiseTo = newones.get(1);
	}
	
	
}

