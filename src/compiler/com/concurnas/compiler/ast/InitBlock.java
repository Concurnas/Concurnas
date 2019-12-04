package com.concurnas.compiler.ast;

import com.concurnas.compiler.ast.interfaces.Expression;
import com.concurnas.compiler.visitors.Visitor;

public class InitBlock extends CompoundStatement {

	public Block block;

	public InitBlock(int line, int column, Block blk) {
		super(line, column, true);
		this.block=blk;
		compoundStatementIsItsOwnLine=true;
	}

	@Override
	public Object accept(Visitor visitor) {
		visitor.setLastLineVisited(super.getLine());
		return visitor.visit(this);
	}
	
	@Override
	public Node copyTypeSpecific() {
		InitBlock ret = new InitBlock(line, column, (Block)this.block.copyTypeSpecific());
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
