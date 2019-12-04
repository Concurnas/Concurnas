package com.concurnas.compiler.ast;

import java.util.ArrayList;

import com.concurnas.compiler.ast.interfaces.Expression;
import com.concurnas.compiler.visitors.Visitor;

public class AssignTupleDeref extends Assign{
	public ArrayList<Assign> lhss;
	public AssignStyleEnum eq;
	public Expression expr;

	public AssignTupleDeref(int line, int column, ArrayList<Assign> lhss, AssignStyleEnum eq, Expression expr) {
		super(line, column, true);
		this.lhss = lhss;
		this.eq = eq;
		this.expr = expr;
	}


	@Override
	public void setAnnotations(Annotations annotations) {
	}

	@Override
	public Annotations getAnnotations() {
		return null;
	}

	@Override
	public Expression getRHSExpression() {
		return expr;
	}

	@Override
	public Expression setRHSExpression(Expression what) {
		Expression was = this.expr;
		this.expr = what;
		return was;
	}

	@Override
	public void setInsistNew(boolean b) {
		
	}

	@Override
	public boolean isInsistNew() {
		return false;
	}

	@Override
	public Object accept(Visitor visitor) {
		visitor.setLastLineVisited(super.getLine());
		return visitor.visit(this);
	}

	@Override
	public Node copyTypeSpecific() {
		AssignTupleDeref ret = new AssignTupleDeref(line, column, (ArrayList<Assign>) Utils.cloneArrayList(lhss), eq, (Expression)expr.copy());
		return ret;
	}
	
	@Override
	public void setAssignStyleEnum(AssignStyleEnum to) {
		this.eq = to;
	}

	@Override
	public boolean getCanReturnAValue(){
		return false;
	}	
}
