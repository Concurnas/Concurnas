package com.concurnas.compiler.ast;

import java.util.ArrayList;
import java.util.List;

import com.concurnas.compiler.visitors.Visitor;

public class OnEvery extends OnChange {
	//onevery(x){ x }
	public OnEvery(int line, int col, ArrayList<Node> expr, Block check, List<String> options) {
		super(line, col, expr, check, options);
	}
	
	@Override
	public Object accept(Visitor visitor) {
		visitor.setLastLineVisited(super.getLine());
		return visitor.visit(this);
	}
	
	@Override
	public String getName(){
		return "every";
	}
	
	@Override
	protected OnChange copyCoreType(){
		return new OnEvery(super.line, super.column, (ArrayList<Node>) Utils.cloneArrayList(this.exprs), body==null?null:(Block)body.copy(), options);
	}
}

