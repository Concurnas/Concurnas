package com.concurnas.compiler.ast;

import java.util.ArrayList;
import java.util.List;

import com.concurnas.compiler.visitors.Unskippable;
import com.concurnas.compiler.visitors.Visitor;

public class TypedefStatement extends Statement implements REPLDepGraphComponent{
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
		
		if(this.canSkipIterativeCompilation && !(visitor instanceof Unskippable)) {
			return null;
		}
		
		return visitor.visit(this);
	}

	@Override
	public Node copyTypeSpecific() {
		return this;//?
	}

	private boolean canSkipIterativeCompilation=false;
	@Override
	public boolean canSkip() {
		return canSkipIterativeCompilation;
	}

	@Override
	public void setSkippable(boolean skippable) {
		canSkipIterativeCompilation = skippable;
	}
	
	@Override
	public String getName() {
		return name;
	}

	@Override
	public Type getFuncType() {
		return type;
	}

	@Override
	public boolean isNewComponent() {
		return true;
	}

}
