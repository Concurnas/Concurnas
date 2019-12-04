package com.concurnas.compiler.ast;

import java.util.ArrayList;

import com.concurnas.compiler.ast.interfaces.Expression;
import com.concurnas.compiler.visitors.Visitor;

public class TupleExpression extends Node implements Expression {

	public ArrayList<Expression> tupleElements;

	public TupleExpression(int line, int column, ArrayList<Expression> tupleElements) {
		super(line, column);
		this.tupleElements = tupleElements;
	}

	private Expression prec;
	@Override
	public void setPreceedingExpression(Expression expr) {
		this.prec = expr;
	}

	@Override
	public Expression getPreceedingExpression() {
		return prec;
	}

	@Override
	public boolean hasBeenVectorized() {
		return false;
	}

	@Override
	public Object accept(Visitor visitor) {
		visitor.setLastLineVisited(super.getLine());
		return visitor.visit(this);
	}

	@Override
	public Node copyTypeSpecific() {
		TupleExpression ret = new TupleExpression(super.line, super.column, (ArrayList<Expression>) Utils.cloneArrayList(tupleElements));
		return ret;
	}

}
