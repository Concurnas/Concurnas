package com.concurnas.compiler.ast;

import java.util.ArrayList;

import com.concurnas.compiler.ast.interfaces.Expression;
import com.concurnas.compiler.visitors.VectorizedRedirector;
import com.concurnas.compiler.visitors.Visitor;
import com.concurnas.runtime.Pair;

public class VectorizedFuncRef extends AbstractExpression implements Expression, HasDepth, CanBeInternallyVectorized {
	public final boolean doubledot;
	public final boolean nullsafe;
	public final Expression expr;
	public FuncRef funcRef;
	private Block vectorizedRedirect=null;

	public VectorizedFuncRef(int line, int col, Expression name, FuncRefArgs funcRefArgs, ArrayList<Type> genTypes, Expression expr, boolean doubledot, boolean nullsafe) {
		super(line, col);
		funcRef = new FuncRef(line, col, name, funcRefArgs);
		funcRef.genTypes = genTypes;
		this.expr=expr;
		this.doubledot=doubledot;
		this.nullsafe=nullsafe;
	}
	
	public VectorizedFuncRef(FuncRef from, Expression expr, boolean doubledot, boolean nullsafe) {
		this(from.getLine(), from.getColumn(), from.functo, from.args == null?null:(FuncRefArgs)from.args.copy(), from.genTypes==null?null:new ArrayList<Type>(from.genTypes), expr, doubledot, nullsafe);
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
		VectorizedFuncRef ret = new VectorizedFuncRef(line, column, funcRef.functo, funcRef.args == null?null:(FuncRefArgs)funcRef.args.copy(), funcRef.genTypes==null?null:(ArrayList<Type>) Utils.cloneArrayList(funcRef.genTypes), (Expression)expr.copy(), doubledot, nullsafe);
		ret.vectorizedRedirect = vectorizedRedirect==null?null: (Block)vectorizedRedirect.copy();
		return ret;
	}
	
	@Override
	public boolean getShouldBePresevedOnStack()	{
		return this.funcRef.getShouldBePresevedOnStack();
	}
	
	@Override
	public void setShouldBePresevedOnStack(boolean should)	{
		funcRef.setShouldBePresevedOnStack(should);
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
