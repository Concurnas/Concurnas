package com.concurnas.compiler.ast;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.concurnas.compiler.ast.interfaces.Expression;
import com.concurnas.compiler.visitors.Visitor;
import com.concurnas.runtime.Pair;

public class FuncInvokeArgs extends Node implements HasNameMap{

	public FuncInvokeArgs(int line, int column) {
		super(line, column);
	}

	public ArrayList<Expression> asnames = new ArrayList<Expression>();
	public List<Expression> argsWithNamedParams;
	public ArrayList<Pair<String, Object>> nameMap = new ArrayList<Pair<String, Object>>();
	public List<Boolean> assertNonNull;
	
	public static FuncInvokeArgs singleFIA(int line, int column, Expression expr){
		FuncInvokeArgs ret = new FuncInvokeArgs(line, column);
		ret.add(expr);
		return ret;
	}
	
	public static FuncInvokeArgs manyargs(int line, int column, Expression... exprs){
		FuncInvokeArgs ret = new FuncInvokeArgs(line, column);
		for(Expression expr : exprs){
			ret.add(expr);
		}
		return ret;
	}
	
	public static FuncInvokeArgs manyargs(int line, int column, ArrayList<Expression> exprs){
		FuncInvokeArgs ret = new FuncInvokeArgs(line, column);
		for(Expression expr : exprs){
			ret.add(expr);
		}
		return ret;
	}
	
	public FuncInvokeArgs(FuncRefArgs args) {
		//create from a FuncInvokeArgs
		super(args.getLine(), args.getColumn());
		
		asnames.addAll(args.getBoundArgs());
		nameMap.addAll(args.getNameMap());
	}
	
	public List<Pair<String, Object>> getNameMap(){
		return nameMap;
	}
	
	
	public ArrayList<Expression> getOrigArguments(){
		return asnames;
	}
	
	public List<Expression> getArgumentsWNPs(){
		return argsWithNamedParams!=null?argsWithNamedParams:asnames;
	}
	
	public int size(){
		return asnames.size(); 
	}
	
	public boolean isEmpty(){
		return asnames.isEmpty(); 
	}
	
	public void add(Expression expression) {
		if(null!= expression)
		{
			asnames.add(expression);
		}
	}
	public void addAll(Collection<Expression> expression) {
		if(null!= expression)
		{
			asnames.addAll(expression);
		}
	}
	
	public void prefixadd(Expression expression) {
		if(null!= expression)
		{
			asnames.add(0, expression);
		}
	}
	
	@Override
	public Object accept(Visitor visitor) {
		visitor.setLastLineVisited(super.getLine());
		return visitor.visit(this);
	}

	@Override
	public Node copyTypeSpecific() {
		FuncInvokeArgs ret = new FuncInvokeArgs(super.getLine(), super.getColumn());
		
		for(Expression a: asnames){
			ret.add((Expression)a.copy());
		}
		
		for(Pair<String, Object> name : nameMap){
			ret.addName(name.getA(), (Expression)(((Expression)name.getB()).copy()));
		}
		
		if(argsWithNamedParams != null) {
			
			ret.argsWithNamedParams = new ArrayList<Expression>(argsWithNamedParams.size());
			
			for(Expression expr : argsWithNamedParams) {
				ret.argsWithNamedParams.add(expr==null?null:(Expression) expr.copy());
			}
		}
		
		ret.assertNonNull = assertNonNull;
		
		return ret;
		
	}

	public void addName(String name, Expression e) {
		nameMap.add(new Pair<String, Object>(name, (Expression)e));
	}
}
