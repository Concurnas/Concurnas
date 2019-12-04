package com.concurnas.compiler.ast;

import java.util.ArrayList;

import com.concurnas.compiler.visitors.Visitor;
import com.concurnas.runtime.Pair;

public class ObjectProviderLineProvide extends Node implements ObjectProviderLine {

	public Type provides;
	public String provName;
	public Block provideExpr;
	
	public ArrayList<Pair<String, NamedType>> localGens;
	public ArrayList<ObjectProviderLineDepToExpr> nestedDeps;
	public AccessModifier accessModi = AccessModifier.PUBLIC;
	public boolean single = false;
	public boolean shared = false;
	public String fieldName;
	
	public ObjectProviderLineProvide(int line, int column, Type provides, String provName) {
		super(line, column);
		this.provides = provides;
		this.provName = provName;
	}

	public ObjectProviderLineProvide(int line, int column, Type provides) {
		this(line, column, provides, null);
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

	@Override
	public void setLocalGens(ArrayList<Pair<String, NamedType>> a) {
		localGens = a;
	}

	@Override
	public ArrayList<Pair<String, NamedType>> getLocalGens() {
		return localGens;
	}
}
