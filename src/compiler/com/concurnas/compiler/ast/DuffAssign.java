package com.concurnas.compiler.ast;

import com.concurnas.compiler.ast.interfaces.Expression;
import com.concurnas.compiler.visitors.Visitor;

public class DuffAssign extends Assign {

	public Expression e;

	public DuffAssign(int line, int col, Expression e) {
		super(line, col, false);
		this.e = e; //stupid, just like a function call etc
	}
	
	public DuffAssign(Expression e) {
		this(e.getLine(), e.getColumn(), e);
	}
	
	@Override
	public boolean getCanBeOnItsOwnLine(){
		return e.getCanBeOnItsOwnLine();
	}
	
	@Override
	public Object accept(Visitor visitor) {
		visitor.setLastLineVisited(super.getLine());
		return visitor.visit(this);
	}
	@Override
	public void setInsistNew(boolean b) {
		throw new RuntimeException("setInsistNew not implemented on DuffAssign");
	}
	
	@Override
	public Node copyTypeSpecific() {
		return new DuffAssign(super.line, super.column, (Expression)e.copy());
	}
	
	public void setShouldBePresevedOnStack(boolean should)
	{
		((Node) e).setShouldBePresevedOnStack(should);
	}
	@Override
	public boolean isInsistNew() {
		return false;
	}


	@Override
	public void setAnnotations(Annotations annotations) {
		//nop
	}

	@Override
	public Annotations getAnnotations() {
		//nop
		return null;
	}
	
	@Override
	public boolean getIsValidAtClassLevel(){
		if(e instanceof ExpressionList){
			if(((ExpressionList)e).astRedirect instanceof AssignNew){
				return true;
			}
		}
		
		return isValidAtClassLevel;
	}

	@Override
	public Expression getRHSExpression() {
		return e;
	}

	@Override
	public Expression setRHSExpression(Expression what) {
		//Expression before = e;
		//this.e = what;
		return e;
	}
	
	
	@Override
	public void setAssignStyleEnum(AssignStyleEnum to) {
	}
}
