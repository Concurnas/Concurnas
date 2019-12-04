package com.concurnas.compiler.ast;

import com.concurnas.compiler.visitors.Visitor;

public class DottedAsName extends Node {

	public String origonalName;
	public String refName;
	public boolean singleEntry = false;

	public DottedAsName(int line, int col, String origonalName, String refName) {
		super(line, col);
		this.origonalName = origonalName;
		this.refName = refName;
	}

	public DottedAsName(int line, int col, String origonalName) {
		this(line, col, origonalName, origonalName);
		singleEntry=true;
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
