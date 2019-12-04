package com.concurnas.compiler.ast;

import com.concurnas.compiler.ast.interfaces.Expression;
import com.concurnas.compiler.visitors.Visitor;

public class AsyncRefRef extends AbstractExpression implements Expression {

	public Expression b;
	public int refCntLevels;
	public boolean checkRefLevels;
	//public boolean followedByRefOperation;

	public AsyncRefRef(int line, int col, Expression b, int refCntLevels) {
		super(line, col);
		this.b = b;
		this.refCntLevels = refCntLevels;
	}
	
	public void setPreceededByThis(boolean preceededByThis) {
		((Node)this.b).setPreceededByThis(preceededByThis);
	}
	
	@Override
	public Object accept(Visitor visitor) {
		visitor.setLastLineVisited(super.getLine());
		return visitor.visit(this);
	}

	@Override
	public Node copyTypeSpecific() {
		AsyncRefRef ret = new AsyncRefRef(super.line, super.column, (Expression)b.copy(), refCntLevels);
		ret.checkRefLevels = this.checkRefLevels;
		ret.preceedingExpression = preceedingExpression==null?null:(Expression)preceedingExpression.copy();
		//ret.followedByRefOperation = this.followedByRefOperation;
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
	public boolean isPermissableToGoOnRHSOfADot(Node preceededBy){
		if(preceededBy instanceof AsyncRefRef){
			return false;
		}
		return true;
	}
	
	@Override
	public boolean getCanBeOnItsOwnLine(){
		return true;
	}
	
	public void setShouldBePresevedOnStack(boolean should)
	{
		super.setShouldBePresevedOnStack(should);
		((Node)b).setShouldBePresevedOnStack(should);
	}
	
	public void setPreceededBySuper(boolean preceededBySuper) {
		super.setPreceededBySuper(preceededBySuper);
		((Node)b).setPreceededBySuper(preceededBySuper);
	}
	
}
