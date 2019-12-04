package com.concurnas.compiler.ast;

import java.util.ArrayList;

import com.concurnas.compiler.visitors.Visitor;

public class UsingStatement extends Statement {

	public UsingStatement(int line, int column) {
		super(line, column);
	}
	public ArrayList<DottedAsName> asnames = new ArrayList<DottedAsName>();
	
	public void add(DottedAsName d) {
		asnames.add(d);
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
