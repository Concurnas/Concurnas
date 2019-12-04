package com.concurnas.compiler.ast;

import com.concurnas.compiler.ast.interfaces.Expression;
import com.concurnas.compiler.visitors.Visitor;
import com.concurnas.runtime.Pair;

public class AsyncBlock extends Block implements HasExtraCapturedVars {
	//TOOD: rename to block?
	public Block body;
	public Expression executor;
	private FuncParams extraCapturedLocalVars = new FuncParams(0,0);
	private boolean isAsync = true;
	public String methodName;
	public FuncRef fakeFuncRef;
	public boolean noReturn = false;
	public LambdaDef fakeLambdaDef;	
	public AssignExisting theAssToStoreRefIn;//the ass will have the 
	public Pair<String, String> lamDets;
	
	public AsyncBlock(int line, int col,Block b)
	{
		this(line, col, b, false, null);
	}
	
	public AsyncBlock(int line, int col,Block b, Expression executor) {
		this(line, col, b, false, executor);
	}
	
	public AsyncBlock(int line, int col,Block b, boolean isAsync) {
		this(line, col, b, isAsync, null);
	}

	public AsyncBlock(int line, int col,Block b, boolean isAsync, Expression executor) {
		super(line, col);
		this.body = b;
		this.body.isAsyncBody=true;
		this.isAsync = isAsync;
		this.executor = executor;
	}
	
	@Override
	public Object accept(Visitor visitor) {
		visitor.setLastLineVisited(super.getLine());
		return visitor.visit(this);
	}
	
	@Override
	public Node copyTypeSpecific() {
		AsyncBlock ret =  new AsyncBlock(super.line, super.column, (Block)body.copy(), isAsync, executor==null?null:(Expression)executor.copy());
		ret.methodName = methodName;
		ret.fakeFuncRef = null==fakeFuncRef?null:(FuncRef)fakeFuncRef.copy();
		ret.noReturn = noReturn;
		ret.fakeLambdaDef = null==fakeLambdaDef?null:(LambdaDef)fakeLambdaDef.copy();
		ret.theAssToStoreRefIn = null==theAssToStoreRefIn?null:(AssignExisting)theAssToStoreRefIn.copy();
		ret.extraCapturedLocalVars = null==extraCapturedLocalVars?null:(FuncParams)extraCapturedLocalVars.copy();
		ret.lamDets = lamDets;
		return ret;
	}
	
	/*//////////////////////////////
	 * 
	 * this is special because we need to make copies of all variables et al refernced when we enter the scopeframe to make everything unchangable.
		ScopeFrame
		Ordered
				
		/*
		int x = 9;
		{
			x = x+8; //this is not allowed
			but we hack it to
			int Scope1$x = 8; //this does work...
		}!
		*/

	public void setAsync(boolean isAsync) {
		//this.isAsync = isAsync;
		//now assume all is async...
		//TODO: fix me along with parser so it can do
		// {asd();a;} vs, {asd();a;}!
	}
	
	public void setShouldBePresevedOnStack(boolean should)
	{
		super.setShouldBePresevedOnStack(should);
		this.body.setShouldBePresevedOnStack(should);
	}
	
	public boolean getShouldBePresevedOnStack()
	{
		return super.getShouldBePresevedOnStack();
	}

	@Override
	public FuncParams getExtraCapturedLocalVars() {
		return this.extraCapturedLocalVars;
	}

	@Override
	public void setExtraCapturedLocalVars(FuncParams extraCapturedLocalVars) {
		this.extraCapturedLocalVars = extraCapturedLocalVars;
	}
	
	@Override
	public String toString() {
		return "" + this.getLine() + " -> " + this.lamDets;
	}
	

	/*public boolean isPermissableToGoOnRHSOfADot(Node preceededBy)
	{
		return true;
	}*/
	
}
