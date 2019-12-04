package com.concurnas.compiler.ast;

import com.concurnas.compiler.ast.interfaces.Expression;
import com.concurnas.compiler.visitors.ScopeAndTypeChecker;
import com.concurnas.compiler.visitors.Visitor;

/*
 * Can map onto a NamedType in this situations Myclass.somethign&();// <- Myclass gets mapped
 */
public class RefNamedType extends AbstractExpression implements Expression {//TODO: rename class to VarBoolean

	public final NamedType mynamed;

	public RefNamedType(int line, int col, NamedType mynamed) {
		super(line, col);
		this.mynamed = mynamed;
	}

	@Override
	public Object accept(Visitor visitor) {
		visitor.setLastLineVisited(super.getLine());
		if(visitor instanceof ScopeAndTypeChecker){
			return ((ScopeAndTypeChecker)visitor).visit(mynamed, true);//dont check the generics here (these check checked in the FuncRef)
		}
		
		return visitor.visit(mynamed);
	}
	
	@Override
	public Node copyTypeSpecific() {
		RefNamedType ret =  new RefNamedType(super.getLine(), super.getColumn(), (NamedType)(null == this.mynamed?null:this.mynamed.copy()));
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
	
	public boolean isPermissableToGoOnRHSOfADot(Node preceededBy){
		return true;
	}
	
}
