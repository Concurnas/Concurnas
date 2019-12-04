package com.concurnas.compiler.ast;

import java.util.ArrayList;

import com.concurnas.compiler.visitors.Visitor;

public class Await extends OnChange {
	//await(x,y | changed>10 ) // if x or y is updated then call the conditional code to check - if done then set result
	public Await(int line, int col, ArrayList<Node> expr, Block check) {
		super(line, col, expr, check, new ArrayList<String>());
	}
	
	@Override
	public Object accept(Visitor visitor) {
		visitor.setLastLineVisited(super.getLine());
		return visitor.visit(this);
	}
	
	@Override
	public String getName(){
		return "await";
	}
	
	@Override
	public boolean getCanBeOnItsOwnLine(){
		return true;
	}
	
	@Override
	protected OnChange copyCoreType(){
		return new Await(super.line, super.column, (ArrayList<Node>) Utils.cloneArrayList(this.exprs), body==null?null:(Block)body.copy());
	}
	
	public void setShouldBePresevedOnStack(boolean should)
	{
		super.setShouldBePresevedOnStack(false);
		if(null!=this.body){
			super.body.setShouldBePresevedOnStack(true);
		}
	}
	
	
	
}
