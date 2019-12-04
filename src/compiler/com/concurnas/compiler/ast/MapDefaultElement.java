package com.concurnas.compiler.ast;

import com.concurnas.compiler.ast.interfaces.Expression;
import com.concurnas.compiler.visitors.Visitor;

public class MapDefaultElement extends Node implements IsAMapElement {

	public Expression value;
	public Type valType;
	public Type keyType;
	public String mdeClass;
	public String origClass;

	public LambdaDef astRedirect;
	
	public MapDefaultElement(int line, int col, Expression value) {
		super(line, col);
		this.value = value;
	}

	@Override
	public Object accept(Visitor visitor) {
		visitor.setLastLineVisited(super.getLine());
		if(null != astRedirect){
			return visitor.visit(astRedirect);
		}
		else{
			return visitor.visit(this);
		}
	}
	
	@Override
	public Node copyTypeSpecific() {
		MapDefaultElement ret = new MapDefaultElement(super.getLine(), super.getColumn(), (Expression)value.copy() );
		ret.astRedirect = this.astRedirect;
		return ret;
	}
}
