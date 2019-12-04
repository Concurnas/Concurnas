package com.concurnas.compiler.ast;

import java.util.ArrayList;

import com.concurnas.compiler.visitors.Visitor;

public class ClassDefArgs extends Node {

	public ArrayList<ClassDefArg> aargs;
	
	public ClassDefArgs(int line, int col, ArrayList<ClassDefArg> aargs) {
		super(line, col);
		this.aargs = aargs;
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
