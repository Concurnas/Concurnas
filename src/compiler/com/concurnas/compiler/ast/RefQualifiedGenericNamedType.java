package com.concurnas.compiler.ast;

import java.util.ArrayList;

import com.concurnas.compiler.ast.interfaces.Expression;
import com.concurnas.compiler.visitors.ScopeAndTypeChecker;
import com.concurnas.compiler.visitors.Visitor;

/*
 * Can map onto a NamedType or a FuncRef depending on context
 */
public class RefQualifiedGenericNamedType extends AbstractExpression implements Expression {//TODO: rename class to VarBoolean

	public final NamedType mynamed;
	public boolean isInDotOperator = false;

	private RefQualifiedGenericNamedType(int line, int col, NamedType mynamed) {
		super(line, col);
		this.mynamed = mynamed;
	}
	
	public RefQualifiedGenericNamedType(int line, int col, String string, ArrayList<Type> gg) {
		this(line, col, new NamedType(line,  col,  string,  gg));
	}
	
	private static NamedType makeActorNamedType(boolean isActor, final NamedType myNamed) {
		if (isActor) {
			NamedType actor = ScopeAndTypeChecker.const_typed_actor.copyTypeSpecific();
			actor.setGenTypes(myNamed);
			actor.isDefaultActor = true;
			return actor;
		}
		
		return myNamed;
	}
	
	public RefQualifiedGenericNamedType(int line, int col, String string, ArrayList<Type> gg, boolean isActor) {
		this(line, col, makeActorNamedType(isActor, new NamedType(line, col, string, gg)));
	}
	
	public RefQualifiedGenericNamedType(int line, int col, String string, boolean isActor) {
		this(line, col, makeActorNamedType(isActor, new NamedType(line, col, string)));
	}

	@Override
	public Object accept(Visitor visitor) {
		visitor.setLastLineVisited(super.getLine());
		if(visitor instanceof ScopeAndTypeChecker){
			return ((ScopeAndTypeChecker)visitor).visit(this);
		}
		
		return visitor.visit(mynamed);
	}
	
	@Override
	public Node copyTypeSpecific() {
		RefQualifiedGenericNamedType ret =  new RefQualifiedGenericNamedType(super.getLine(), super.getColumn(), (NamedType)(null == this.mynamed?null:this.mynamed.copy()));
		ret.isInDotOperator= isInDotOperator;
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
	
	@Override
	public boolean isPermissableToGoOnRHSOfADot(Node preceededBy){
		return true;
	}
}
