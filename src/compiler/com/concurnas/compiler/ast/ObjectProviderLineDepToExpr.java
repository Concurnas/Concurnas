package com.concurnas.compiler.ast;

import java.util.ArrayList;

import com.concurnas.compiler.ast.interfaces.Expression;
import com.concurnas.compiler.visitors.Visitor;
import com.concurnas.runtime.Pair;

public class ObjectProviderLineDepToExpr extends Node implements ObjectProviderLine{

	public Type dependency;
	public Type typeOnlyRHS;
	public Block fulfilment;
	public boolean single;
	public boolean shared;
	
	public String name;
	public ArrayList<ObjectProviderLineDepToExpr> nestedDeps;

	public ObjectProviderLineDepToExpr(int line, int column, Type dependency, Expression fulfilment, Boolean single, Boolean shared, String name, Type typeOnlyRHS) {
		super(line, column);
		this.dependency = dependency;
		this.typeOnlyRHS = typeOnlyRHS;
		if(null!= fulfilment) {
			this.fulfilment = new Block(line, column);
			this.fulfilment.isolated=true;
			this.fulfilment.setShouldBePresevedOnStack(true);
			this.fulfilment.add(new LineHolder(new DuffAssign(fulfilment)));
		}
		this.single = single;
		this.shared = shared;
		this.name = name;
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
	
	ArrayList<Pair<String, NamedType>> localGens;
	@Override
	public void setLocalGens(ArrayList<Pair<String, NamedType>> a) {
		localGens = a;
	}

	@Override
	public ArrayList<Pair<String, NamedType>> getLocalGens() {
		return localGens;
	}
}