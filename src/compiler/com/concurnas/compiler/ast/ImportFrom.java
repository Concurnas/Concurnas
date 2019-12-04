package com.concurnas.compiler.ast;

import java.util.ArrayList;

import com.concurnas.compiler.visitors.Visitor;

public class ImportFrom extends ImportStatement {

	public String from;
	public ArrayList<ImportAsName> froms = new ArrayList<ImportAsName>();

	public ImportFrom(int line, int col, boolean isNormalImport, String from) {
		super(line, col, isNormalImport);
		this.from = from;
	}

	public void add(ImportAsName imp) {
		this.froms.add(imp);
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
