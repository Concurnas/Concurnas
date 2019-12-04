package com.concurnas.compiler.ast;

public abstract class Statement extends Line {

	public Statement(int line, int column) {
		this(line, column, false);
	}
	
	public Statement(int line, int column, boolean validAtClsLevel) {
		super(line, column, validAtClsLevel);
	}

}
