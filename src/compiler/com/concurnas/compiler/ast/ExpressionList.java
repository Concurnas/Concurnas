package com.concurnas.compiler.ast;

import java.util.ArrayList;

import com.concurnas.compiler.ast.interfaces.Expression;
import com.concurnas.compiler.visitors.ScopeAndTypeChecker;
import com.concurnas.compiler.visitors.Visitor;
import com.concurnas.compiler.visitors.algos.ExpressionListExpander;

public class ExpressionList extends AbstractExpression implements Expression {

	public final ArrayList<Expression> exprs;
	public boolean couldBeAnAssignmentDecl = false;
	private Expression preceedingExpression;
	public Node astRedirect;
	public String redirectedStringRep = null;
	public ExpressionListExpander exapnder;
	public boolean supressLastItemDoubleDotAttempt=false;;
	
	public ExpressionList(int line, int col, ArrayList<Expression> exprs){
		super(line, col);
		this.exprs=exprs;
	}
	

	@Override
	public Object accept(Visitor visitor) {
		visitor.setLastLineVisited(super.getLine());
		
		if(astRedirect != null && !(visitor instanceof ScopeAndTypeChecker)){
			return astRedirect.accept(visitor);
		}
		
		return visitor.visit(this);
	}

	@Override
	public Node copyTypeSpecific() {
		ExpressionList ret =  new ExpressionList(super.line, super.column, (ArrayList<Expression>) Utils.cloneArrayList(exprs));
		ret.astRedirect = astRedirect == null?null:(Node)astRedirect.copy();
		ret.redirectedStringRep = redirectedStringRep;
		//ret.exapnder = exapnder==null?null:exapnder.cloneme();
		ret.couldBeAnAssignmentDecl=couldBeAnAssignmentDecl;
		ret.supressLastItemDoubleDotAttempt=supressLastItemDoubleDotAttempt;
		return ret;
	}

	
	@Override
	public void setPreceedingExpression(Expression expr) {
		this.preceedingExpression = expr;
	}
	
	@Override
	public Type getTaggedType() {
		if(astRedirect != null) {
			return astRedirect.getTaggedType();
		}
		return super.getTaggedType();
	}
	
	@Override
	public Expression getPreceedingExpression() {
		return preceedingExpression;
	}
	
	
	public boolean getCanBeOnItsOwnLine(){
		
		if(this.astRedirect != null){
			return this.astRedirect.getCanBeOnItsOwnLine();
		}
		
		return true;
	}
	
	public void setShouldBePresevedOnStack(boolean should)
	{
		if(astRedirect!= null) {
			astRedirect.setShouldBePresevedOnStack(should);
		}
		
		super.setShouldBePresevedOnStack(should);
		
	} 
	
}
