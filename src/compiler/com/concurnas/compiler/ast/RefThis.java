package com.concurnas.compiler.ast;

import com.concurnas.compiler.ast.interfaces.Expression;
import com.concurnas.compiler.visitors.ScopeAndTypeChecker;
import com.concurnas.compiler.visitors.Visitor;

public class RefThis extends AbstractExpression implements Expression {
	
	public String qualifier;
	//public int thisVar=0;
	private Expression preceedingExpression;
	public RefName astRedirect;
	
	public RefThis(int line, int column, String qualifier) {
		super(line, column);
		this.qualifier = qualifier;
	}
	
	public RefThis(int line, int column) {
		this(line, column, null);
	}

	@Override
	public Object accept(Visitor visitor) {
		visitor.setLastLineVisited(super.getLine());
		
		if(null != astRedirect && !(visitor instanceof ScopeAndTypeChecker)){
			return astRedirect.accept(visitor);
		}
		
		return visitor.visit(this);
	}
	@Override
	public Node copyTypeSpecific() {
		RefThis ret = new RefThis(line, column, qualifier);
		ret.preceedingExpression=preceedingExpression;
		return ret;
	}
	
	@Override
	public void setPreceedingExpression(Expression expr) {
		this.preceedingExpression = expr;
	}
	@Override
	public Expression getPreceedingExpression() {
		return preceedingExpression;
	}
}
