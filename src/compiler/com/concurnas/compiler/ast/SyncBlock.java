package com.concurnas.compiler.ast;

import com.concurnas.compiler.ast.interfaces.Expression;
import com.concurnas.compiler.visitors.Visitor;

public class SyncBlock extends CompoundStatement {

	public Expression syncOnObj;
	public Block b;

	public SyncBlock(int line, int col, Expression syncOnObj, Block b) {
		super(line, col);
		this.syncOnObj = syncOnObj;
		this.b = b;
	}
	@Override
	public Object accept(Visitor visitor) {
		visitor.setLastLineVisited(super.getLine());
		return null;
		//return visitor.visit(this);
	}
	
	@Override
	public Node copyTypeSpecific() {
		SyncBlock ret = new SyncBlock(super.line, super.column, (Expression)syncOnObj.copy(), (Block)b.copy());
		ret.preceedingExpression = preceedingExpression==null?null:(Expression)preceedingExpression.copy();

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
}
