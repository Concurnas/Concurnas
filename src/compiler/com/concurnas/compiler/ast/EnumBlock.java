package com.concurnas.compiler.ast;

import java.util.ArrayList;

import com.concurnas.compiler.visitors.Visitor;

public class EnumBlock extends Node {

	public ArrayList<EnumItem> enumItemz;
	public Block mainBlock;

	public EnumBlock(int line, int column, ArrayList<EnumItem> enumItemz, Block mainBlock) {
		super(line, column);
		this.enumItemz=enumItemz;
		this.mainBlock=mainBlock;
	}

	@Override
	public Object accept(Visitor visitor) {
		visitor.setLastLineVisited(super.getLine());
		return visitor.visit(this);
	}

	@Override
	public Node copyTypeSpecific() {
		EnumBlock eb = new EnumBlock(line, column, (ArrayList<EnumItem>) Utils.cloneArrayList(enumItemz), (Block)mainBlock.copy());
		
		return eb;
	}

}
