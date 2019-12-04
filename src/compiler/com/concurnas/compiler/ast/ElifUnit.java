package com.concurnas.compiler.ast;

import org.objectweb.asm.Label;

import com.concurnas.compiler.ast.Block;
import com.concurnas.compiler.ast.Node;
import com.concurnas.compiler.ast.interfaces.Expression;
import com.concurnas.compiler.visitors.Visitor;

public class ElifUnit extends Node {

	public Expression eliftest;
	public Block elifb;
	public Label labelOnCacthEntry;
	public Label nextLabelOnCatchEntry;

	public ElifUnit(int line, int col, Expression eliftest, Block elifb) {
		super(line, col);
		this.eliftest = eliftest;
		this.elifb = elifb;
	}

	@Override
	public Object accept(Visitor visitor) {
		visitor.setLastLineVisited(super.getLine());
		return visitor.visit(this);
	}

	@Override
	public Node copyTypeSpecific() {
		return new ElifUnit(super.line, super.column, (Expression)eliftest.copy(), (Block)elifb.copy());
	}

}
