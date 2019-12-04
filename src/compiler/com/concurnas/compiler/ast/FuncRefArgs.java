package com.concurnas.compiler.ast;

import java.util.ArrayList;
import java.util.List;

import com.concurnas.compiler.ast.interfaces.Expression;
import com.concurnas.compiler.visitors.Visitor;
import com.concurnas.compiler.visitors.Utils.CurriedVararg;
import com.concurnas.runtime.Pair;

public class FuncRefArgs extends Node implements HasNameMap{

	public FuncRefArgs(int line, int column) {
		super(line, column);
	}

	public FuncRefArgs(FuncInvokeArgs args) {
		//create from a FuncInvokeArgs
		super(args.getLine(), args.getColumn());
		for(Expression e : args.asnames){
			addExpr(e);
		}
		for(Pair<String, Object> x : args.nameMap){
			this.addName(x.getA(), x.getB());
		}
		
	}

	public List<Object> exprOrTypeArgsList = new ArrayList<Object>();

	public ArrayList<Pair<String, Object>> nameMap = new ArrayList<Pair<String, Object>>();

	public CurriedVararg curriedVararg;//for capturing where varargs have been mapped to an array including curried elements (that we want to preserve)
	
	public List<Pair<String, Object>> getNameMap(){
		return nameMap;
	}
	
	//public boolean beenThroughOneCompilationCycleAlready = false; 
	
	public void addName(String name, Object e) {
		nameMap.add(new Pair<String, Object>(name, e));
	}
	
	public void replaceExpr(int at, Object with){
		exprOrTypeArgsList.set(at, with);
	}
	
	@Override
	public Node copyTypeSpecific() {
		FuncRefArgs ret = new FuncRefArgs(line, column);
		ret.exprOrTypeArgsList =  (List<Object>) Utils.cloneArrayList(exprOrTypeArgsList);
		
		for(Pair<String, Object> name : nameMap){
			ret.addName(name.getA(), (Expression)(((Expression)name.getB()).copy()));
		}
		
		return ret;
	}
	
	public void addExpr(Expression expr)
	{
		exprOrTypeArgsList.add(expr);
	}
	
	public void addType(Type typ)
	{
		typ.setIsTypeInFuncref(true);
		exprOrTypeArgsList.add(typ);
	}
	
	public int unboundCount(){
		int n=0;
		for(Object o : exprOrTypeArgsList){
			if(o instanceof Type && !(o instanceof VarNull)){
				n++;
			}
		}
		return n;
	}
	
	public ArrayList<Type> getUnBoundArgs(){
		ArrayList<Type> ret = new ArrayList<Type>();
		
		for(Object o : exprOrTypeArgsList){
			if(o instanceof Type && !(o instanceof VarNull)){
				ret.add((Type)o);
			}
		}
		return ret;
	}
	
	public ArrayList<Expression> getBoundArgs(){
		ArrayList<Expression> ret = new ArrayList<Expression>();
		
		for(Object o : exprOrTypeArgsList){
			if(o instanceof Expression){
				ret.add((Expression)o);
			}
		}
		return ret;
	}
	
	public ArrayList<Type> getBoundArgsTypes(){
		ArrayList<Type> ret = new ArrayList<Type>();
		
		for(Object o : exprOrTypeArgsList){
			if(o instanceof Expression){
				ret.add( ((Expression)o).getTaggedType() );
			}
		}
		return ret;
	}
	
	@Override
	public Object accept(Visitor visitor) {
		visitor.setLastLineVisited(super.getLine());
		return visitor.visit(this);
	}
	
	@Override
	public String toString(){
		StringBuilder sb = new StringBuilder();
		
		for(Object o : exprOrTypeArgsList){
			sb.append(o);
			sb.append(", ");
		}
		
		return sb.toString();
	}
}
