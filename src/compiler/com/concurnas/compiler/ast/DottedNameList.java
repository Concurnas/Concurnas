package com.concurnas.compiler.ast;

import java.util.ArrayList;

import com.concurnas.compiler.visitors.Visitor;

public class DottedNameList extends Node {

	public DottedNameList(int line, int column) {
		super(line, column);
	}

	public ArrayList<String> dottedNames = new ArrayList<String>();
	
	public void add(String dottedname) {
		this.dottedNames.add(dottedname);
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
