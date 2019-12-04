package com.concurnas.compiler.ast;

import com.concurnas.compiler.ast.interfaces.Expression;
import com.concurnas.compiler.visitors.ScopeAndTypeChecker;
import com.concurnas.compiler.visitors.Visitor;

public class MapDefElement extends Node implements IsAMapElement {
	private Expression key;
	private Expression value;
	public Type keyType;
	public Type valType;
	private VarString keyOverride;
	private VarString valueOverride;

	public MapDefElement(int line, int col, Expression key, Expression value) {
		super(line, col);
		this.key = key;//no null keys
		this.value = value;
	}

	public void setKeyOverride(VarString keyOverride) {
		this.keyOverride = keyOverride;
	}
	public void setValueOverride(VarString valueOverride) {
		this.valueOverride = valueOverride;
	}
	
	public Expression getKey(Visitor vis) {
		if(vis instanceof ScopeAndTypeChecker) {
			return key;
		}
		
		return null != keyOverride? keyOverride:key;
	}
	
	public Expression getValue(Visitor vis) {
		if(vis instanceof ScopeAndTypeChecker) {
			return value;
		}
		
		return null != valueOverride?valueOverride:value;
	}
	
	@Override
	public Object accept(Visitor visitor) {
		visitor.setLastLineVisited(super.getLine());
		return visitor.visit(this);
	}
	
	@Override
	public Node copyTypeSpecific() {
		return new MapDefElement(super.getLine(), super.getColumn(), (Expression)key.copy(), (Expression)value.copy() );
	}
}
