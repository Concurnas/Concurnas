package com.concurnas.compiler.ast;

import java.util.ArrayList;

import com.concurnas.compiler.visitors.Visitor;
import com.concurnas.runtime.Pair;

public interface ObjectProviderLine{
	
	public void setLocalGens(ArrayList<Pair<String, NamedType>> a);
	public ArrayList<Pair<String, NamedType>> getLocalGens();

	public Object accept(Visitor visitor);
}
