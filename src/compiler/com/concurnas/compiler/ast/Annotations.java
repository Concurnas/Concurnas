package com.concurnas.compiler.ast;

import java.lang.annotation.ElementType;
import java.util.ArrayList;

import com.concurnas.compiler.bytecode.BytecodeGennerator;
import com.concurnas.compiler.visitors.Visitor;

public class Annotations extends Node {

	public ArrayList<Annotation> annotations;
	public BytecodeGennerator bcVisitor;
	
	public Annotations(int line, int column, ArrayList<Annotation> annotations) {
		super(line, column);
		this.annotations = annotations;
	}
	
	public Annotations(Annotation... annotations) {
		super(0, 0);
		
		this.annotations = new ArrayList<Annotation>(annotations.length);
		for(Annotation a : annotations){
			this.annotations.add(a);
		}
	}

	@Override
	public Object accept(Visitor visitor) {
		visitor.setLastLineVisited(super.getLine());
		return visitor.visit(this);
	}

	@Override
	public Node copyTypeSpecific() {
		Annotations annots = new Annotations(line, column, (ArrayList<Annotation>) Utils.cloneArrayList(annotations) );
		return annots;
	}

	public void setUsedAt(ElementType usedAt) {
		for(Annotation annot : annotations){
			annot.usedAt = usedAt;
		}
	}
	
	public boolean hasAnnotation(String annotClassName) {
		return null != getAnnotation(annotClassName);
	}
	
	public Annotation getAnnotation(String annotClassName) {
		if(null != annotations) {
			for(Annotation annot : annotations) {
				if(annot.className.equals(annotClassName)) {
					return annot;
				}
			}
		}
		return null;
	}
	@Override
	public boolean getCanReturnAValue(){
		return false;
	}	

}
