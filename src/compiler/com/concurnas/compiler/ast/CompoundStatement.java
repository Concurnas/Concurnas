package com.concurnas.compiler.ast;

import com.concurnas.compiler.ast.interfaces.Expression;

public abstract class CompoundStatement extends Statement implements Expression, CanEndInReturnOrException {

	public CompoundStatement(int line, int column) {
		this(line, column, false);
	}
	
	public CompoundStatement(int line, int column, boolean validatclasslevel) {
		super(line, column, validatclasslevel);
	}

	public boolean compoundStatementIsItsOwnLine=false;//e.g. a = {...}//block isnt, but {...} - is!


	public boolean hasBeenVectorized(){
		return false;
	}
	
	/*public boolean isCompoundStatementIsItsOwnLine() {
		return compoundStatementIsItsOwnLine;
	}

	public void setCompoundStatementIsItsOwnLine(boolean compoundStatementIsItsOwnLine) {
		this.compoundStatementIsItsOwnLine = compoundStatementIsItsOwnLine;
	}*/
	
}
