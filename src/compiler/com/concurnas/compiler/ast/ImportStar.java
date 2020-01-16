package com.concurnas.compiler.ast;

import java.util.ArrayList;

import com.concurnas.compiler.visitors.Visitor;

public class ImportStar extends ImportStatement{

	public String from;

	public ImportStar(int line, int col, boolean isNormalImport, String from) {
		super(line, col,isNormalImport);
		this.from = from;
	}
	@Override
	public Object accept(Visitor visitor) {
		visitor.setLastLineVisited(super.getLine());
		return visitor.visit(this);
	}
	
	@Override
	public Node copyTypeSpecific() {
		return this;
	}
	
	/*
	 * public ArrayList<String> getNames(){ ArrayList<String> ret = new
	 * ArrayList<String>(); ret.add(from + ".*"); return ret; }
	 */
	

	@Override
	public boolean persistant() { return true;}
}
