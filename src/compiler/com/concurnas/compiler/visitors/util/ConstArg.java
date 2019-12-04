package com.concurnas.compiler.visitors.util;

import com.concurnas.compiler.ast.Annotations;
import com.concurnas.compiler.ast.Type;
import com.concurnas.compiler.ast.interfaces.Expression;

public class ConstArg {
	public String name;
	public Type argType;
	public boolean isFinal;
	public Annotations annotationsForConstructor;
	public Expression defaultValue;
	public boolean isVararg;
	public boolean isLazy;
	public boolean isShared;

	public ConstArg(String name, Type argType, boolean isFinal, Annotations annotationsForConstructor, Expression defaultValue, boolean isVararg, boolean isLazy, boolean isShared) {
		this.name = name;
		this.argType = argType;
		this.isFinal = isFinal;
		this.annotationsForConstructor = annotationsForConstructor;
		this.defaultValue = defaultValue;
		this.isVararg = isVararg;
		this.isLazy = isLazy;
		this.isShared = isShared;
	}
}
