package com.concurnas.compiler.ast;

import java.util.ArrayList;

import com.concurnas.compiler.ast.interfaces.Expression;
import com.concurnas.compiler.visitors.ScopeAndTypeChecker;
import com.concurnas.compiler.visitors.Visitor;


public class ArrayConstructor extends ConstructorInvoke {

	public Type type;
	public ArrayList<Expression> arrayLevels;
	public Expression defaultValue;

	public ArrayConstructor(int line, int col, Type t, ArrayList<Expression> arrayLevels, Expression defaultValue) {
		super(line, col);
		this.type = t;
		this.arrayLevels = arrayLevels;
		this.defaultValue = defaultValue;
	}

	@Override
	public Object accept(Visitor visitor) {
		visitor.setLastLineVisited(super.getLine());
		if(null != astRedirect && !(visitor instanceof ScopeAndTypeChecker) ){
			return visitor.visit(astRedirect);
		}
		
		return visitor.visit(this);
	}

	@Override
	public Node copyTypeSpecific() {
		ArrayConstructor ret = new ArrayConstructor(super.line, super.column, (Type)type.copy(), (ArrayList<Expression>) Utils.cloneArrayList(arrayLevels), defaultValue==null?null:(Expression)defaultValue.copy());
		ret.preceedingExpression = preceedingExpression==null?null:(Expression)preceedingExpression.copy();
		return ret;
	}
	private Expression preceedingExpression;
	public Block astRedirect;
	@Override
	public void setPreceedingExpression(Expression expr) {
		this.preceedingExpression = expr;
	}
	@Override
	public Expression getPreceedingExpression() {
		return preceedingExpression;
	}
	
	@Override
	public boolean hasBeenVectorized(){
		return false;
	}
	
	
}
