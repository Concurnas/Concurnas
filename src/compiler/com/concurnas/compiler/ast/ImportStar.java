package com.concurnas.compiler.ast;

import com.concurnas.compiler.visitors.Visitor;

public class ImportStar extends ImportStatement implements REPLTopLevelComponent{

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
}
