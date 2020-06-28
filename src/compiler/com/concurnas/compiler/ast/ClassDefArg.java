package com.concurnas.compiler.ast;

import com.concurnas.compiler.ast.interfaces.Expression;
import com.concurnas.compiler.visitors.Visitor;

public class ClassDefArg extends Node implements HasAnnotations{

	public AccessModifier am;
	public boolean isFinal;
	public String prefix;
	public String name;
	public Type type;
	public Annotations annotations;
	public Expression defaultValue = null;
	public boolean isVararg=false;
	public boolean isNullableVarArg=false;
	public boolean isTransient=false;
	public Boolean isShared=false;
	public Boolean isLazy=false;
	public boolean isOverride = false;

	public ClassDefArg(int line, int col, AccessModifier am, boolean isFinal, String prefix, String name, Type t)
	{
		super(line, col);
		this.am = am;
		this.isFinal = isFinal;
		this.prefix = prefix;
		this.name = name;
		this.type = t;
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
	public void setAnnotations(Annotations annotations) {
		this.annotations=annotations;
	}

	@Override
	public Annotations getAnnotations() {
		return annotations;
	}

	public void setDefaultValue(Expression defaultValue) {
		this.defaultValue =defaultValue;
	}
}
