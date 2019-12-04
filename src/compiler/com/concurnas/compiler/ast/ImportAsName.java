package com.concurnas.compiler.ast;

import com.concurnas.compiler.visitors.Visitor;

public class ImportAsName extends Node {

	public String impt;
	public String asName;

	public ImportAsName(int line, int col, String impt, String asName) {
		super(line, col);
		this.impt = impt;
		this.asName = asName;
	}

	public ImportAsName(int line, int col, String impt) {
		this(line, col, impt, impt);
	}
	
	@Override
	public boolean getCanBeOnItsOwnLine(){
		return true;
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
}
