package com.concurnas.compiler.ast;

import java.util.ArrayList;

import com.concurnas.compiler.ast.interfaces.Expression;
import com.concurnas.compiler.visitors.VectorizedRedirector;
import com.concurnas.compiler.visitors.Visitor;
import com.concurnas.runtime.Pair;

public class VectorizedFuncInvoke extends AbstractExpression implements Expression, HasDepth, CanBeInternallyVectorized {
	public final boolean doubledot;
	public final boolean nullsafe;
	public final boolean noNullAssertion;
	public final Expression expr;
	public FuncInvoke funcInvoke;
	private Block vectorizedRedirect=null;

	public VectorizedFuncInvoke(int line, int col, String name, FuncInvokeArgs args, ArrayList<Type> genTypes, Expression expr, boolean doubledot, boolean nullsafe, boolean noNullAssertion) {
		super(line, col);
		funcInvoke = new FuncInvoke(line, col, name, args, genTypes);
		this.expr=expr;
		this.doubledot=doubledot;
		this.nullsafe=nullsafe;
		this.noNullAssertion=noNullAssertion;
	}
	
	public VectorizedFuncInvoke(FuncInvoke from, Expression expr, boolean doubledot, boolean nullsafe, boolean nna) {
		this(from.getLine(), from.getColumn(), from.funName, from.args == null?null:(FuncInvokeArgs)from.args.copy(), from.genTypes==null?null:new ArrayList<Type>(from.genTypes), expr,doubledot, nullsafe, nna);
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

		if(null != vectorizedRedirect){
			if(!(visitor instanceof VectorizedRedirector)){
				return vectorizedRedirect.accept(visitor);
			}
		}
		
		return visitor.visit(this);
	}

	@Override
	public Node copyTypeSpecific() {
		VectorizedFuncInvoke ret = new VectorizedFuncInvoke(line, column, funcInvoke.funName, funcInvoke.args == null?null:(FuncInvokeArgs)funcInvoke.args.copy(), funcInvoke.genTypes==null?null:(ArrayList<Type>) Utils.cloneArrayList(funcInvoke.genTypes), (Expression)expr.copy(), doubledot, nullsafe, noNullAssertion);
		ret.vectorizedRedirect = vectorizedRedirect==null?null: (Block)vectorizedRedirect.copy();
		return ret;
	}
	
	@Override
	public boolean getShouldBePresevedOnStack()	{
		return this.funcInvoke.getShouldBePresevedOnStack();
	}
	
	@Override
	public void setShouldBePresevedOnStack(boolean should)	{
		funcInvoke.setShouldBePresevedOnStack(should);
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
