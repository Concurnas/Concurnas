package com.concurnas.compiler.ast;

import java.util.HashSet;

import org.objectweb.asm.Label;

import com.concurnas.compiler.visitors.Visitor;

public abstract class Node implements Copyable {
	/*
	@Override
	public Object accept(Visitor visitor) {
		//return visitor.visit(this);
	}
	*/
	
	protected int line;
	protected int column;
	
	private boolean shouldBePresevedOnStack = true;//MHA
	private boolean ifReturnsExpectImmediateUse = true;//MHA
	public boolean isOnItsOwnLine = false;
	private boolean preceededByThis =false;
	private boolean preceededBySuper =false;
	
	private Label labelOnEntry = null;
	private Label labelAfterCode = null;
	
	public boolean getIfReturnsExpectImmediateUse() {
		return ifReturnsExpectImmediateUse;
	}

	public void setIfReturnsExpectImmediateUse(boolean ifReturnsExpectImmediateUse) {
		this.ifReturnsExpectImmediateUse = ifReturnsExpectImmediateUse;
	}

	public boolean getShouldBePresevedOnStackAndImmediatlyUsed(){
		return this.getShouldBePresevedOnStack() && ifReturnsExpectImmediateUse;
	}
	
	private boolean preceededByDotInDotOperator =false;
	
	private boolean expectNonRef = false;
	
	public boolean shouldAddsetterAccessorIfGetterAdded = false;
	
	public boolean getExpectNonRef() {
		return expectNonRef;
	}
	
	public boolean getCanBeOnItsOwnLine(){
		if(this instanceof CanBeInternallyVectorized) {
			CanBeInternallyVectorized cbiv = (CanBeInternallyVectorized)this;
			if(!cbiv.canBeNonSelfReferncingOnItsOwn() && cbiv.hasVectorizedRedirect()) {
				return true;//errored elsewhere
			}
			
			if(cbiv.hasErroredAlready()) {
				return true;
			}
			
			return cbiv.hasVectorizedRedirect();
		}
		return false;
	}
	
	public boolean getCanReturnAValue(){//most things can
		if(this instanceof CanBeInternallyVectorized) {
			CanBeInternallyVectorized cbiv = (CanBeInternallyVectorized)this;
			if(!cbiv.canBeNonSelfReferncingOnItsOwn() && cbiv.hasVectorizedRedirect()) {
				return true;//errored elsewhere
			}
			
			if(cbiv.hasErroredAlready()) {
				return true;
			}
			
			return cbiv.hasVectorizedRedirect();
		}
		return true;
	}
	
	public void setExpectNonRef(boolean var) {
		expectNonRef = var;
	}
	
	public boolean isPreceededByDotInDotOperator() {
		return preceededByDotInDotOperator;
	}

	public void setPreceededByDotInDotOperator(boolean preceededByDotInDotOperator) {
		this.preceededByDotInDotOperator = preceededByDotInDotOperator;
	}
	
	public boolean getShouldBePresevedOnStackAndNotOnItsOwnLine(){
		return this.getShouldBePresevedOnStack() && !this.isOnItsOwnLine;
	}

	public boolean getShouldBePresevedOnStack()
	{
		return this.shouldBePresevedOnStack;
	}
	
	
	
	public void setShouldBePresevedOnStack(boolean should)
	{
		this.shouldBePresevedOnStack =should;
	}
	
	public boolean isPreceededBySuper() {
		return preceededBySuper;
	}

	public void setPreceededBySuper(boolean preceededBySuper) {
		this.preceededBySuper = preceededBySuper;
	}
	
	public boolean isPreceededByThis() {
		return preceededByThis;
	}

	public void setPreceededByThis(boolean preceededByThis) {
		this.preceededByThis = preceededByThis;
	}
	
	public Node(int line, int column)
	{
		
		this.line = line;
		this.column = column;
	}
	
	public int getLine() { return line; }

	public int getColumn() { return column;	}

	public void setLine(int line) { this.line = line; }

	public void setColumn(int column) { this.column = column;	}
	
	private Type type;
	
	public Type setTaggedType(Type type){
		/*
		 * so this functions ok: 
		 * 	a := 9; //int:
		 * 	b = a!;//int:
		 * 	c = a;//int
		 */
		
		//System.out.println(String.format("setTaggedType %s - %s" ,this.toString(), type ));
		
		/*if(type instanceof NamedType){
			NamedType asNamed = (NamedType)type;
			if(asNamed.getIsRef() && !asNamed.getLockedAsRef()){
				type = asNamed.copyTypeSpecific().getGenTypes().get(0);
			}
			asNamed = asNamed.copyTypeSpecific();
			asNamed.setLockedAsRef(false);
			type = asNamed;
		}*/
		
		
		
		this.type = type;
		
		return type;
	}
	
	public Type getTaggedTypeRaw(){
		return type;
	}
	
	public Type getTaggedType(){
		//Type ret = this.type;
		return type;
		//return this.getExpectNonRef() ?  TypeCheckUtils.getRefType(ret) : ret;
	}
	
	public abstract Object accept(Visitor visitor);
	public boolean isPermissableToGoOnRHSOfADot(Node preceededBy)
	{
		return false;
	}
	
	private boolean duplicateOnStack = false;
	public boolean getDuplicateOnStack(){
		return this.duplicateOnStack;
	}
	public void setDuplicateOnStack(boolean var){
		this.duplicateOnStack = var;
	}
	

	

	public Label getLabelOnEntry() {
		return this.labelOnEntry;
	}

	public Label setLabelOnEntry(Label labelOnEntry) {
		//if (null == this.labelOnEntry) {
		//	this.labelOnEntry = labelOnEntry;
		//} else {
			//System.err.println(String.format("Attempt Overwrite entry label %s with %s", this.labelOnEntry, labelOnEntry));
			this.labelOnEntry = labelOnEntry;
		//}
		return this.labelOnEntry;
	}

	public Label getLabelAfterCode() {
		return this.labelAfterCode;
	}

	public void setLabelAfterCode(Label labelAfterCode) {
		this.labelAfterCode = labelAfterCode;
	}
	
	public final Node copy(){
		Node ret = this.copyTypeSpecific();
		if(null != this.type){
			if(this.type == this){
				ret.type = (Type)ret;//can happen, e.g varnull
			}else{
				ret.type = (Type)this.type.copy();
			}
		}
		ret.ifReturnsExpectImmediateUse=ifReturnsExpectImmediateUse;
		ret.setShouldBePresevedOnStack(this.getShouldBePresevedOnStack());
		return ret;
	}
	
	public abstract Node copyTypeSpecific();
	
	@Override public String toString(){
		return this.line +": " + super.toString();
	}

	private Object foldedConstant;
	public HashSet<String> notes;//Hacks
	
	public Object setFoldedConstant(Object foldedConstant) {
		this.foldedConstant = foldedConstant;
		return this.foldedConstant;
	}

	public Object getFoldedConstant() {
		return this.foldedConstant;
	}
}
