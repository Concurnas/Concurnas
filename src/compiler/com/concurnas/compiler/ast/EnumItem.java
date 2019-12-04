package com.concurnas.compiler.ast;

import com.concurnas.compiler.visitors.Visitor;

public class EnumItem extends Node implements HasAnnotations{

	public String name;
	public FuncInvokeArgs args;
	public ClassDef fakeclassDef;
	public Block block;
	public String className;
	public New mappedConstructor;
	public int idx;
	public Annotations annotations;

	public EnumItem(int line, int column, String name, FuncInvokeArgs args, Block block) {
		super(line, column);
		this.name = name;
		this.args = args;
		this.block = block;
	}

	@Override
	public Object accept(Visitor visitor) {
		visitor.setLastLineVisited(super.getLine());
		return visitor.visit(this);
	}

	@Override
	public Node copyTypeSpecific() {
		EnumItem ei = new EnumItem(line, column, name, (FuncInvokeArgs)args.copy(), block ==null?null:(Block)block.copy());
		ei.fakeclassDef = fakeclassDef==null?null:(ClassDef)fakeclassDef.copy();
		ei.className = className;
		ei.mappedConstructor = mappedConstructor==null?null:(New)mappedConstructor.copy();//TODO: remove null check
		ei.idx=idx;
		ei.annotations = annotations==null?null:(Annotations)annotations.copy();
		return ei;
	}

	@Override
	public void setAnnotations(Annotations annotations) {
		this.annotations = annotations;
	}

	@Override
	public Annotations getAnnotations() {
		return annotations;
	}
}
