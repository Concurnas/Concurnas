package com.concurnas.compiler.ast;

import com.concurnas.compiler.ast.interfaces.Expression;
import com.concurnas.compiler.visitors.ScopeAndTypeChecker;
import com.concurnas.compiler.visitors.Visitor;

public class WithBlock extends CompoundStatement {

	public Expression expr;
	public Block blk;
	public boolean shouldBePresevedOnStack;
	public Block astOverride;

	public WithBlock(int line, int col, Expression expr, Block blk) {
		super(line, col);
		//any objects created in expression above take effect within scope of block
		this.expr = expr;
		this.blk = blk;
	}
	@Override
	public Object accept(Visitor visitor) {
		visitor.setLastLineVisited(super.getLine());
		if(null != astOverride && !(visitor instanceof ScopeAndTypeChecker)){
			return astOverride.accept(visitor);
		}
		return visitor.visit(this);
	}
	
	@Override
	public Node copyTypeSpecific() {
		WithBlock wb = new WithBlock(super.line, super.column, (Expression) expr.copy(), (Block)blk.copy());
		wb.shouldBePresevedOnStack = shouldBePresevedOnStack;
		wb.preceedingExpression = preceedingExpression==null?null:(Expression)preceedingExpression.copy();

		return wb;
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

	public boolean getShouldBePresevedOnStack(){
		if(null != astOverride){
			return astOverride.getShouldBePresevedOnStack();
		}
		return shouldBePresevedOnStack;
	}
	
	public void setShouldBePresevedOnStack(boolean should){
		if(null != astOverride){
			astOverride.setShouldBePresevedOnStack(should);
		}
		blk.setShouldBePresevedOnStack(should);
		
		shouldBePresevedOnStack = should;
	}
	
}
