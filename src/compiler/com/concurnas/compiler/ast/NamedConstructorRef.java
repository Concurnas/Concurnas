package com.concurnas.compiler.ast;

import com.concurnas.compiler.ast.interfaces.Expression;
import com.concurnas.compiler.visitors.ScopeAndTypeChecker;
import com.concurnas.compiler.visitors.Visitor;


public class NamedConstructorRef extends ConstructorInvoke{

	public New namedConstructor;
	public FuncRef funcRef;
	public FuncRefArgs argz;

	public NamedConstructorRef(int line, int col, ConstructorInvoke nt, FuncRefArgs argz) {
		super(line, col);
		this.namedConstructor = (New)nt;
		this.argz = argz;
	}

	@Override
	public Object accept(Visitor visitor) {
		visitor.setLastLineVisited(super.getLine());
		
		if(visitor instanceof ScopeAndTypeChecker){
			return ((ScopeAndTypeChecker)visitor).visit(this);
		}
		if(funcRef != null){
			return funcRef.accept(visitor);
		}
		return null;
	}
	
	@Override
	public Node copyTypeSpecific() {
		NamedConstructorRef ret = new NamedConstructorRef(this.getLine(), this.getColumn(), (New)namedConstructor.copy(), null==argz?null:(FuncRefArgs)argz.copy());
		ret.funcRef = null==this.funcRef?null:(FuncRef) this.funcRef.copy();
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
