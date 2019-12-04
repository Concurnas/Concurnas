package com.concurnas.compiler.ast;

import java.util.HashSet;
import java.util.List;

import com.concurnas.compiler.ast.interfaces.Expression;
import com.concurnas.compiler.visitors.Visitor;

public class CopyExpression extends AbstractExpression implements Expression {

	public Expression expr;
	public List<CopyExprItem> copyItems;
	public List<String> modifiers;
	private Expression preceedingExpression;
	public HashSet<String> copySpecMustInclude;
	public boolean nodefault;

	public CopyExpression(int line, int col, Expression expr, List<CopyExprItem> copyItems, List<String> modifiers) {
		super(line, col);
		this.expr = expr;
		this.copyItems = copyItems;
		this.modifiers = modifiers;
	}
	
	@Override
	public Object accept(Visitor visitor) {
		visitor.setLastLineVisited(super.getLine());
		return visitor.visit(this);
	}
	
	@Override
	public Node copyTypeSpecific() {
		CopyExpression ret = new CopyExpression(super.getLine(), super.getColumn(), (Expression)expr.copy(), (List<CopyExprItem>)Utils.cloneArrayList(copyItems), modifiers);
		ret.preceedingExpression = preceedingExpression==null?null:(Expression)preceedingExpression.copy();
		ret.copySpecMustInclude = copySpecMustInclude;
		ret.nodefault = nodefault;
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
