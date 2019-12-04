package com.concurnas.compiler.ast;

public abstract class Line extends Node {
	protected boolean isValidAtClassLevel;
	public Line(int line, int column) {
		this(line, column, false);
	}
	
	public Line(int line, int column, boolean isValidAtClassLevel) {
		super(line, column);
		this.isValidAtClassLevel=isValidAtClassLevel;
	}
	
	@Override
	public boolean getCanBeOnItsOwnLine(){
		return true;
	}
	
	public boolean getIsValidAtClassLevel(){
		return isValidAtClassLevel;
	}
	

}
