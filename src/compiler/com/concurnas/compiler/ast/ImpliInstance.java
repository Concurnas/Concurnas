package com.concurnas.compiler.ast;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.concurnas.compiler.visitors.Visitor;

public class ImpliInstance  extends Node {
	public String traitName;
	public List<Type> traitGenricList;
	public Map<Type, Type> iffaceTypeToClsGeneric = new HashMap<Type, Type>();//implements mapping
	public ClassDef resolvedIface;
	
	public ImpliInstance(int line, int col, String ifaceName, List<Type> interfaceGenricList) {
		super(line, col);
		this.traitName = ifaceName;
		this.traitGenricList = interfaceGenricList;
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
}
