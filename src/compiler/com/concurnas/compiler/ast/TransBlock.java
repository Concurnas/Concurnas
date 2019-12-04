package com.concurnas.compiler.ast;

import com.concurnas.compiler.ast.interfaces.Expression;
import com.concurnas.compiler.visitors.ScopeAndTypeChecker;
import com.concurnas.compiler.visitors.Visitor;

public class TransBlock extends CompoundStatement {
	public Block astRedirect;
	private boolean shouldBePresevedOnStack;
	public Block blk;

	public TransBlock(int line, int col, Block blk) {
		super(line, col);
		//any objects created in expression above take effect within scope of block
		this.blk = blk;
	}
	@Override
	public Object accept(Visitor visitor) {
		visitor.setLastLineVisited(super.getLine());
		if(null != astRedirect && !(visitor instanceof ScopeAndTypeChecker)){
			return astRedirect.accept(visitor);
		}
		return visitor.visit(this);
	}
	
	@Override
	public Node copyTypeSpecific() {
		TransBlock ret = new TransBlock(line, column, (Block)(blk==null?null:blk.copy()));
		ret.astRedirect = astRedirect==null?null:(Block)astRedirect.copy();
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

	public boolean getShouldBePresevedOnStack(){
		if(null != astRedirect){
			return astRedirect.getShouldBePresevedOnStack();
		}
		return shouldBePresevedOnStack;
	}
	
	public void setShouldBePresevedOnStack(boolean should){
		if(null != astRedirect){
			astRedirect.setShouldBePresevedOnStack(should);
		}
		blk.setShouldBePresevedOnStack(should);
		
		shouldBePresevedOnStack = should;
	}
	
}
