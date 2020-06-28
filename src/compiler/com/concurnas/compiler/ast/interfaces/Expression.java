package com.concurnas.compiler.ast.interfaces;

import com.concurnas.compiler.ast.Copyable;
import com.concurnas.compiler.ast.Node;
import com.concurnas.compiler.ast.Type;
import com.concurnas.compiler.visitors.Visitor;

public interface Expression extends Copyable{
	public Object accept(Visitor visitor);
	public boolean isPermissableToGoOnRHSOfADot(Node preceededBy);
	public int getLine();
	public int getColumn();
	public Type setTaggedType(Type type);
	public Type getTaggedType();
	//used for arrayref where we need to keep it on stack in order to perform pre or post increment operation
	public boolean getDuplicateOnStack();
	public void setDuplicateOnStack(boolean var);
	public void setPreceededByDotInDotOperator(boolean var);
	public boolean getCanReturnAValue();
	public boolean getCanBeOnItsOwnLine();
	public Object setFoldedConstant(Object foldedConstant);
	public Object getFoldedConstant();
	public  void setPreceedingExpression(Expression expr);
	public  Expression getPreceedingExpression();
	public boolean hasBeenVectorized();
	public Type getTaggedTypeRaw();
	public void setPreceededBySafeCall(boolean safeCallRet);
}
