package com.concurnas.compiler.ast;

import java.util.ArrayList;
import java.util.List;

import com.concurnas.compiler.visitors.Visitor;

public class TypedefStatement extends Statement {
	public String name;
	public Type type;
	public AccessModifier accessModifier;
	public List<String> typedefargs;

	public TypedefStatement(int line, int column, AccessModifier accessModifier, String name, Type type, List<String> typedefargs) {
		super(line, column);
		this.accessModifier = accessModifier;
		this.name = name;
		this.type = type;
		this.typedefargs = typedefargs;
	}

	@Override
	public Object accept(Visitor visitor) {
		visitor.setLastLineVisited(super.getLine());
		return visitor.visit(this);
	}

	@Override
	public Node copyTypeSpecific() {
		return this;//?
	}

}
