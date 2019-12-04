package com.concurnas.compiler.ast;

import org.objectweb.asm.Label;

import com.concurnas.compiler.visitors.Visitor;

//TODO: is this class even needed?
public class LineHolder extends Node {

	public Line l;
	public boolean lastLine = false;
	
	public LineHolder(int line, int col, Line l)
	{
		super(line, col);
		this.l = l;
	}
	public LineHolder(Line l)
	{
		this(l.getLine(), l.getColumn(), l);
	}
	
	@Override
	public Object accept(Visitor visitor) {
		visitor.setLastLineVisited(super.getLine());
		return visitor.visit(this);
	}
	
	@Override
	public Node copyTypeSpecific() {
		return new LineHolder(super.line, super.column, (Line)this.l.copy());
	}

	public Label getLabelOnEntry() {
		return this.l.getLabelOnEntry();
	}

	@Override
	public boolean getCanBeOnItsOwnLine(){
		return l.getCanBeOnItsOwnLine();
	}
	
	public Label setLabelOnEntry(Label labelOnEntry) {
		return this.l.setLabelOnEntry(labelOnEntry);
	}

	public Label getLabelAfterCode() {
		return this.l.getLabelAfterCode();
	}

	public void setLabelAfterCode(Label labelAfterCode) {
		this.l.setLabelAfterCode(labelAfterCode);
	}
	
	
	public boolean getShouldBePresevedOnStack()
	{
		return this.l.getShouldBePresevedOnStack();
	}
	
	public void setShouldBePresevedOnStack(boolean should)
	{
		this.l.setShouldBePresevedOnStack(should);
	}
	
	public String toString(){
		return this.getLine() + ": " + this.l;
	}
	
}
