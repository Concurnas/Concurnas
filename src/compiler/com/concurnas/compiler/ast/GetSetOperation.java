package com.concurnas.compiler.ast;

import com.concurnas.compiler.ast.interfaces.Expression;
import com.concurnas.compiler.typeAndLocation.TypeAndLocation;
import com.concurnas.compiler.visitors.Visitor;

public class GetSetOperation extends AbstractExpression implements Expression {
	
	public boolean ispostfix;
	public AssignStyleEnum incOperation;
	public Expression toAddMinus;
	public String getter;
	public String setter;
	public TypeAndLocation getterTAL;
	public TypeAndLocation setterTAL;

	public GetSetOperation(int line, int col, String getter, String setter, AssignStyleEnum incOperation, boolean ispostfix, Expression toAddMinus) {
		super(line, col);
		this.getter = getter;
		this.setter = setter;
		this.incOperation = incOperation;
		this.ispostfix = ispostfix;
		this.toAddMinus = toAddMinus;
	}
	
	
	
	@Override
	public Object accept(Visitor visitor) {
		visitor.setLastLineVisited(super.getLine());
		return visitor.visit(this);
	}
	
	@Override
	public boolean getCanBeOnItsOwnLine(){
		return true;
	}
	
	@Override
	public boolean isPermissableToGoOnRHSOfADot(Node preceededBy) { return true; }



	@Override
	public Node copyTypeSpecific() {
		GetSetOperation ret = new GetSetOperation(super.getLine(), super.getColumn(),getter, setter, incOperation,  ispostfix, (Expression)toAddMinus.copy());
		ret.getterTAL = getterTAL;
		ret.setterTAL = setterTAL;
		ret.preceedingExpression = preceedingExpression==null?null:(Expression)preceedingExpression.copy();
		return ret;
	}
	private Expression preceedingExpression;
	@Override
	public void setPreceedingExpression(Expression expr) {
		this.preceedingExpression = expr;
	}
	@Override
	public Expression getPreceedingExpression() {
		return preceedingExpression;
	}
}
