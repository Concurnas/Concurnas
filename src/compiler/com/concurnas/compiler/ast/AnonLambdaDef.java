package com.concurnas.compiler.ast;

import java.util.ArrayList;

import com.concurnas.compiler.ast.interfaces.Expression;
import com.concurnas.compiler.visitors.Visitor;
import com.concurnas.runtime.Pair;

public class AnonLambdaDef extends Node implements Expression, AnonLambdaDefOrLambdaDef {

	public ArrayList<String> paramNames;
	public Block body;
	public LambdaDef astRedirect;
	public Type retType;
	public ArrayList<Type> paramTypes;
	
	
	public AnonLambdaDef(int line, int column, ArrayList<String> paramNames, Block body, ArrayList<Type> paramTypes, Type retType) {
		super(line, column);
		this.paramNames = paramNames;
		this.body = body;
		this.paramTypes = paramTypes;
		this.retType = retType;
	}

	@Override
	public Object accept(Visitor visitor) {
		visitor.setLastLineVisited(super.getLine());
		if(astRedirect != null) {
			return astRedirect.accept(visitor);
		}
		
		return visitor.visit(this);
	}

	@Override
	public Node copyTypeSpecific() {
		AnonLambdaDef ret = new AnonLambdaDef(line, column, (ArrayList<String>)paramNames.clone(), (Block)body.copy(), (ArrayList<Type>)Utils.cloneArrayList(paramTypes),  retType==null?null:(Type)retType.copy());
		return ret;
	}

	private Expression prev;
	@Override
	public void setPreceedingExpression(Expression prev) {
		this.prev = prev;
	}

	@Override
	public Expression getPreceedingExpression() {
		return prev;
	}

	@Override
	public boolean hasBeenVectorized() {
		return false;
	}
	
	private Boolean iftCache = null;
	public boolean isFullyTyped() {
		if(null == iftCache) {
			iftCache = !this.paramTypes.isEmpty() && this.paramTypes.stream().noneMatch(a -> a==null);
		}
		return iftCache;
	}
	
	public void setShouldBePresevedOnStack(boolean should)
	{
		super.setShouldBePresevedOnStack(should);
		body.setShouldBePresevedOnStack(should);
	}

	public ArrayList<Pair<String, Type>> getInputs() {
		ArrayList<Pair<String, Type>> inp = new ArrayList<Pair<String, Type>>(paramNames.size());
		int n=0;
		for(String name : this.paramNames) {
			inp.add(new Pair<String, Type>(name, !paramTypes.isEmpty()?paramTypes.get(n++):null));
		}
		return inp;
	}

	public Type getReturnType() {
		return retType;
	}

	public Type getTaggedType() {
		if(this.astRedirect != null) {
			return this.astRedirect.getTaggedType();
		}
		return super.getTaggedType();
	}
	
}
