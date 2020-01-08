package com.concurnas.compiler.ast;

import java.util.ArrayList;
import java.util.List;

import com.concurnas.compiler.visitors.ScopeAndTypeChecker;
import com.concurnas.compiler.visitors.Unskippable;
import com.concurnas.compiler.visitors.Visitor;

public class TypedefStatement extends Statement implements REPLTopLevelComponent{
	public String name;
	public Type type;
	public AccessModifier accessModifier;
	public List<String> typedefargs;

	public TypedefStatement(int line, int column, AccessModifier accessModifier, String name, Type type, List<String> typedefargs) {
		super(line, column);
		this.accessModifier = accessModifier;
		this.name = name;
		this.type = type;
		this.typedefargs = typedefargs;
	}

	@Override
	public Object accept(Visitor visitor) {
		visitor.setLastLineVisited(super.getLine());
		
		if(this.canSkipIterativeCompilation && !(visitor instanceof Unskippable)) {
			return null;
		}

		if(visitor instanceof ScopeAndTypeChecker) {
			this.hasErrors = false;
		}
		visitor.pushErrorContext(this);
		Object ret = visitor.visit(this);
		visitor.popErrorContext();
		return ret;
	}

	@Override
	public Node copyTypeSpecific() {
		return this;//?
	}

	private boolean canSkipIterativeCompilation=false;
	@Override
	public boolean canSkip() {
		return canSkipIterativeCompilation;
	}

	@Override
	public void setSkippable(boolean skippable) {
		canSkipIterativeCompilation = skippable;
	}
	
	@Override
	public String getName() {
		return name;
	}

	@Override
	public Type getFuncType() {
		return type;
	}

	@Override
	public boolean isNewComponent() {
		return true;
	}

	@Override
	public boolean persistant() { 
		return true;
	}

	public boolean hasErrors = false;
	@Override
	public void setErrors(boolean hasErrors) {
		this.hasErrors = hasErrors;
	}
	@Override
	public boolean getErrors() {
		return hasErrors;
	}
	
	private boolean supressErrors = false;
	@Override
	public void setSupressErrors(boolean supressErrors) {
		this.supressErrors = supressErrors;
	}
	@Override
	public boolean getSupressErrors() {
		return supressErrors;
	}
}