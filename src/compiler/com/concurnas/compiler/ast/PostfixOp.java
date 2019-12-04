package com.concurnas.compiler.ast;

import java.util.ArrayList;
import java.util.List;

import com.concurnas.compiler.ast.AutoVectorizableElements.VectorizationConfig;
import com.concurnas.compiler.ast.interfaces.Expression;
import com.concurnas.compiler.visitors.TypeCheckUtils;
import com.concurnas.compiler.visitors.VectorizedRedirector;
import com.concurnas.compiler.visitors.Visitor;
import com.concurnas.runtime.Pair;

public class PostfixOp extends AbstractExpression implements Expression, CanBeInternallyVectorized, AutoVectorizableElements {

	public FactorPostFixEnum postfix;
	public Expression p2;
	public DotOperator ASTDivert;
	public FuncInvoke astOverrideOperatorOverload;

	public PostfixOp(int line, int col, FactorPostFixEnum postfix, Expression p2) {
		super(line, col);
		this.postfix = postfix;
		this.p2 = p2;
	}
	
	@Override
	public Node copyTypeSpecific() {
		PostfixOp ret = new PostfixOp(super.getLine(), super.getColumn(), postfix, (Expression)p2.copy());
		ret.ASTDivert = ASTDivert;
		ret.astOverrideOperatorOverload = (FuncInvoke)(astOverrideOperatorOverload==null?null:astOverrideOperatorOverload.copy());
		ret.setExpectNonRef(this.getExpectNonRef());
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
		
		allExprs.add(new VectorizationConfig(this.p2, false, postfix.asMethod, false, true, false));
		
		return allExprs;
	}
	
	@Override
	public void setAllElements(List<Expression> newones) {
		p2 = newones.get(0);
	}
	
	@Override
	public boolean getCanBeOnItsOwnLine(){
		return true;
	}
	
	@Override
	public Object accept(Visitor visitor) {
		visitor.setLastLineVisited(super.getLine());

		if(null != vectorizedRedirect){
			if(!(visitor instanceof VectorizedRedirector)){
				return vectorizedRedirect.accept(visitor);
			}
		}
		
		/*if(null != astOverrideOperatorOverload){
			p2.accept(visitor);
			return astOverrideOperatorOverload.accept(visitor);
		}*/
		
		if(null != ASTDivert)
		{
			return visitor.visit(ASTDivert);
		}
		else
		{
			return visitor.visit(this);
		}
	}
	
	public void setExpectNonRef(boolean var){
		if(null != ASTDivert && ASTDivert.getElements(null) != null){
			ArrayList<Expression> elements = ASTDivert.getElements(null);
			((Node)elements.get(elements.size()-1)).setExpectNonRef(var);
		}
		else{
			super.setExpectNonRef(var);
		}
	}
	
	public void setShouldBePresevedOnStack(boolean should)
	{
		if(null != ASTDivert)
		{
			ASTDivert.setShouldBePresevedOnStack(should);
		}
		
		super.setShouldBePresevedOnStack(should);
	}
	
	public Type getTaggedType(){
		Type ret = super.getTaggedType();
		return this.getExpectNonRef() ?  TypeCheckUtils.getRefType(ret) : ret;
	}
	
	public Type setTaggedType(Type type){
		return super.setTaggedType(type);
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
