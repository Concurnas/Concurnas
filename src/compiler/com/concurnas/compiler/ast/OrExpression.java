package com.concurnas.compiler.ast;

import java.util.ArrayList;

import com.concurnas.compiler.ast.interfaces.Expression;
import com.concurnas.compiler.visitors.VectorizedRedirector;
import com.concurnas.compiler.visitors.Visitor;

public class OrExpression extends BooleanAndOrExpression implements Expression {

	public OrExpression(int line, int col, Expression head, ArrayList<RedirectableExpression> ors) {
		super(line, col, head, ors, false);
	}
	
	private static ArrayList<RedirectableExpression> toAR(RedirectableExpression... things){
		ArrayList<RedirectableExpression> items = new ArrayList<RedirectableExpression>(things.length);
		for(RedirectableExpression e : things){
			items.add(e);
		}
		return items;
	}
	
	public OrExpression(int line, int col, Expression head, RedirectableExpression... ors) {
		this(line, col, head, toAR(ors));
	}
	
	public OrExpression(int line, int col, ArrayList<RedirectableExpression> ors) {
		super(line, col, ors, false);
	}
	
	public static OrExpression buildOrExpression(int line, int col, RedirectableExpression... ee){
		ArrayList<RedirectableExpression> ors = new ArrayList<RedirectableExpression> ();
		for(RedirectableExpression e : ee){
			ors.add(e);
		}
		return new OrExpression(line, col, ors);
	}
	
	@Override
	public Node copyTypeSpecific() {
		OrExpression expr = new OrExpression(super.getLine(), super.getColumn(), super.head,  (ArrayList<RedirectableExpression>) Utils.cloneArrayList(super.things));
		expr.preceedingExpression = preceedingExpression==null?null:(Expression)preceedingExpression.copy();
		expr.vectorizedRedirect = vectorizedRedirect==null?null: (Block)vectorizedRedirect.copy();

		return expr;
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
	
	@Override
	public Object accept(Visitor visitor) {
		visitor.setLastLineVisited(super.getLine());
		if(null != vectorizedRedirect){
			if(!(visitor instanceof VectorizedRedirector)){
				return vectorizedRedirect.accept(visitor);
			}
		}
		return visitor.visit(this);
	}
	
	public String getMethodEquiv(){
		return "or";
	}
	
	private boolean hasErrored=false;
	@Override
	public boolean hasErroredAlready() {
		return hasErrored;
	}
	@Override
	public void setHasErroredAlready(boolean hasError) {
		this.hasErrored=hasError;
	}

}
