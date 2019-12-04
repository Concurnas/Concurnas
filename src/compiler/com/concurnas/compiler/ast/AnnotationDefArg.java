package com.concurnas.compiler.ast;

import com.concurnas.compiler.ast.interfaces.Expression;
import com.concurnas.compiler.visitors.Visitor;

public class AnnotationDefArg extends Node implements HasAnnotations {
	public Annotations annotations;
	public String name;
	public Type optionalType;
	public Expression expression;

	public AnnotationDefArg(int line, int column, String name, Type optionalType, Expression expression, Annotations annotations) {
		super(line, column);
		this.name = name;
		this.optionalType = optionalType;
		this.expression = expression;
		this.annotations = annotations;
	}

	@Override
	public Object accept(Visitor visitor) {
		visitor.setLastLineVisited(super.getLine());
		return null;
	}

	@Override
	public Node copyTypeSpecific() {
		return new AnnotationDefArg(line, column, name, (Type)(optionalType == null?null:optionalType.copy()), expression==null?null:(Expression)expression.copy(), annotations==null?null:(Annotations)annotations.copy());
	}

	@Override
	public void setAnnotations(Annotations annotations) {
		this.annotations = annotations;
	}

	@Override
	public Annotations getAnnotations() {
		return annotations;
	}
	@Override
	public boolean getCanReturnAValue(){
		return false;
	}	

}
