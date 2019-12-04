package com.concurnas.compiler.ast;

import java.util.ArrayList;
import java.util.List;

import com.concurnas.compiler.ast.interfaces.Expression;
import com.concurnas.compiler.visitors.VectorizedRedirector;
import com.concurnas.compiler.visitors.Visitor;
import com.concurnas.runtime.Pair;

public class FuncRefInvoke extends AbstractExpression implements Expression, CanBeInternallyVectorized {

	public Expression funcRef;
	public FuncInvokeArgs args;

	public FuncInvoke astredirect;
	private Block vectorizedRedirect=null;
	
	public FuncRefInvoke(int line, int col, Expression funcRef, FuncInvokeArgs args) {
		super(line, col);
		//args may be structred such that currying is performed
		this.funcRef = funcRef;
		this.args = args;
	}
	@Override
	public Object accept(Visitor visitor) {
		visitor.setLastLineVisited(super.getLine());
		
		if(null != vectorizedRedirect){
			if(!(visitor instanceof VectorizedRedirector)){
				return vectorizedRedirect.accept(visitor);
			}
		}
		
		if(null != astredirect){
			return astredirect.accept(visitor);
		}
		
		return visitor.visit(this);
	}
	
	@Override
	public Node copyTypeSpecific() {
		FuncRefInvoke ret =new FuncRefInvoke(super.getLine(), super.getColumn(), (Expression)funcRef.copy(), (FuncInvokeArgs)args.copy());
		ret.astredirect = astredirect;
		ret.preceedingExpression = preceedingExpression==null?null:(Expression)preceedingExpression.copy();
		ret.resolvedInputType = resolvedInputType==null?null:(FuncType)resolvedInputType.copy();
		ret.mappedArgs = mappedArgs==null?null:(ArrayList<Expression>) Utils.cloneArrayList(mappedArgs) ; 
		ret.vectorizedRedirect = vectorizedRedirect==null?null: (Block)vectorizedRedirect.copy();
		
		return ret;
	}
	private Expression preceedingExpression;
	public FuncType resolvedInputType;
	private List<Expression> mappedArgs;
	
	private FuncInvokeArgs mappedArgsAsArgs = null;
	public ArrayList<Pair<Boolean, NullStatus>> depth=null;
	
	public void setMappedArgs(List<Expression> mappedArgs){
		this.mappedArgs=mappedArgs;
		mappedArgsAsArgs = new FuncInvokeArgs(0,0);
		mappedArgsAsArgs.addAll(mappedArgs);
	}
	
	public FuncInvokeArgs getArgs(){
		if(null != mappedArgsAsArgs){
			return mappedArgsAsArgs;
		}else{
			return args;
		}
	}
	public FuncInvokeArgs getArgsOrig(){
		return args;
	}
	
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
	
	public void setPreceededByThis(boolean preceededByThis) {
		((Node)this.funcRef).setPreceededByThis(preceededByThis);
	}
	
	public void setPreceededBySuper(boolean preceededBysup) {
		((Node)this.funcRef).setPreceededBySuper(preceededBysup);
	}
	
	@Override
	public boolean getCanBeOnItsOwnLine(){
		return true;
	}
	


	@Override
	public boolean hasVectorizedRedirect() {
		return this.vectorizedRedirect != null;
	}

	@Override
	public void setVectorizedRedirect(Node vectRedirect) {
		this.vectorizedRedirect = (Block)vectRedirect;
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
	
	/*public void setShouldBePresevedOnStack(boolean should) {
		super.s
		((Node)this.funcRef).setShouldBePresevedOnStack(should);
	}*/
}
