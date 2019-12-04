package com.concurnas.compiler.ast;

import java.util.ArrayList;

import com.concurnas.compiler.ast.interfaces.Expression;
import com.concurnas.compiler.visitors.Visitor;

public class DeleteStatement extends Statement {

	public ArrayList<Expression> exprs;
	public DSOpOn operatesOn;
	
	public DeleteStatement(int line, int col, ArrayList<Expression> exprs) {
		super(line, col);
		this.exprs = exprs; //must resolve to boolean
	}

	@Override
	public Object accept(Visitor visitor) {
		visitor.setLastLineVisited(super.getLine());
		return visitor.visit(this);
	}
	
	@Override
	public Node copyTypeSpecific() {
		DeleteStatement ret = new DeleteStatement(super.line, super.column, (ArrayList<Expression>) Utils.cloneArrayList(exprs));
		ret.operatesOn=operatesOn;
		return ret;
	}
	
	public static enum DSOpOn{
		LOCALVAR, MAPREF, LISTREF, OBJREF;
	}

	@Override
	public boolean getCanReturnAValue(){
		return false;
	}	
}
