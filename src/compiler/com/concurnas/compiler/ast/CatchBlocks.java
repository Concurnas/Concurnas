package com.concurnas.compiler.ast;

import java.util.ArrayList;

import org.objectweb.asm.Label;

import com.concurnas.compiler.visitors.Visitor;

public class CatchBlocks extends Node {

	public String var;
	public ArrayList<Type> caughtTypes;
	public Block catchBlock;

	public CatchBlocks(int line, int col, String var, ArrayList<Type> caughtTypes, Block catchBlock) {
		super(line, col);
		this.var = var;
		this.caughtTypes = caughtTypes;
		if(caughtTypes.isEmpty()){
			caughtTypes.add(new NamedType(line, col, "java.lang.Throwable"));
		}
		
		this.catchBlock = catchBlock;
	}
	
	@Override
	public Object accept(Visitor visitor) {
		visitor.setLastLineVisited(super.getLine());
		return visitor.visit(this);
	}

	@Override
	public Node copyTypeSpecific() {
		return new CatchBlocks(super.line, super.column, var, (ArrayList<Type>) Utils.cloneArrayList(this.caughtTypes), (Block)catchBlock.copy());
	}
	
	public Block attachedFinalBlock;
	public Label entryLabel;
	public Label finLabel;
	public Label attachedkFinalBlockEntryLabel;
	public boolean endsInException;
	public Type blockType;
}
