package com.concurnas.compiler.ast;

import java.util.ArrayList;

import com.concurnas.compiler.ast.interfaces.Expression;
import com.concurnas.compiler.visitors.Visitor;

public class AssignMulti extends Statement implements AssignWithRHSExpression {
	public AssignMulti(int line, int col, Expression rhs){
		super(line, col);
		this.rhs = rhs;
	}
	
	public Expression rhs;
	public ArrayList<Assign> assignments = new ArrayList<Assign>();
	
	@Override
	public Expression getRHSExpression() {
		return rhs;
	}

	@Override
	public Object accept(Visitor visitor) {
		visitor.setLastLineVisited(super.getLine());

		return visitor.visit(this);
	}

	@Override
	public Node copyTypeSpecific() {
		AssignMulti ret = new AssignMulti(line, super.column, (Expression)rhs.copy());
		ret.assignments = (ArrayList<Assign>) Utils.cloneArrayList(assignments);
		return ret;
	}

	@Override
	public Expression setRHSExpression(Expression what) {
		Expression was = this.rhs;
		this.rhs = what;
		return was;
	}
	@Override
	public boolean getCanReturnAValue(){
		return false;
	}	

}
