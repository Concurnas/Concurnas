package com.concurnas.compiler.ast;

import java.util.ArrayList;
import java.util.List;

import com.concurnas.compiler.ast.interfaces.Expression;
import com.concurnas.compiler.utils.Fourple;
import com.concurnas.compiler.visitors.VectorizedRedirector;
import com.concurnas.compiler.visitors.Visitor;
import com.concurnas.runtime.Pair;

public class PrefixOp extends AbstractExpression implements Expression, CanBeInternallyVectorized, AutoVectorizableElements {

	public FactorPrefixEnum prefix;
	public Expression p1;
	public DotOperator ASTDivert = null;
	public FuncInvoke astOverrideOperatorOverload;

	public PrefixOp(int line, int col, FactorPrefixEnum prefix, Expression p1) {
		super(line, col);
		this.prefix = prefix;
		this.p1 = p1;
	}
	
	@Override
	public boolean getCanBeOnItsOwnLine(){
		return true;
	}
	
	@Override
	public Node copyTypeSpecific() {
		PrefixOp ret = new PrefixOp(super.getLine(), super.getColumn(), prefix, (Expression)p1.copy());
		ret.ASTDivert = ASTDivert;
		ret.astOverrideOperatorOverload = (FuncInvoke)(astOverrideOperatorOverload==null?null:astOverrideOperatorOverload.copy());
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
	public List<VectorizationConfig> getAllElements() {
		List<VectorizationConfig> allExprs = new ArrayList<VectorizationConfig>(2);
		
		allExprs.add(new VectorizationConfig(this.p1, false, prefix.asMethod, false, true, false));
		
		return allExprs;
	}
	
	@Override
	public void setAllElements(List<Expression> newones) {
		p1 = newones.get(0);
	}
	
	
	public void setShouldBePresevedOnStack(boolean should)
	{	
		if(null != ASTDivert)
		{
			ASTDivert.setShouldBePresevedOnStack(should);
		}
		
		super.setShouldBePresevedOnStack(should);
	}
	
	@Override
	public Object accept(Visitor visitor) {
		visitor.setLastLineVisited(super.getLine());
		

		if(null != vectorizedRedirect){
			if(!(visitor instanceof VectorizedRedirector)){
				return vectorizedRedirect.accept(visitor);
			}
		}
		
		if(null != ASTDivert)
		{
			return visitor.visit(ASTDivert);
		}
		else
		{
			return visitor.visit(this);
		}
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
	
	@Override
	public boolean canBeNonSelfReferncingOnItsOwn() {
		return true;
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
