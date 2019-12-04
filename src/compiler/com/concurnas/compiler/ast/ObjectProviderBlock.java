package com.concurnas.compiler.ast;

import java.util.ArrayList;

import com.concurnas.compiler.visitors.Visitor;

public class ObjectProviderBlock extends Node {

	public ArrayList<ObjectProviderLine> lines = new ArrayList<ObjectProviderLine>();
	
	public ObjectProviderBlock(int line, int column) {
		super(line, column);
	}
	
	public void addLine(ObjectProviderLine line) {
		lines.add(line);
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
