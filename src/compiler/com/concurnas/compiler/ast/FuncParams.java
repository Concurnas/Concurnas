package com.concurnas.compiler.ast;

import java.util.ArrayList;

import com.concurnas.compiler.utils.Fiveple;
import com.concurnas.compiler.utils.Sixple;
import com.concurnas.compiler.visitors.Visitor;

public class FuncParams extends Node {

	public ArrayList<FuncParam> params = new ArrayList<FuncParam>();
	
	public void add(FuncParam funcParam) {
		this.params.add(funcParam);
	}
	
	public void add(int x, FuncParam funcParam) {
		this.params.add(x, funcParam);
	}
	
	public FuncParams(int line, int col)
	{
		super(line, col);
	}
	
	public FuncParams(int line, int col, FuncParam funcParam)
	{
		this(line, col);
		this.add(funcParam);
	}
	
	@Override
	public Node copyTypeSpecific() {
		//return this;
		FuncParams fps = new FuncParams(line, super.column);
		fps.params = (ArrayList<FuncParam>) Utils.cloneArrayList(params) ;
		return fps;
	}
	
	@Override
	public Object accept(Visitor visitor) {
		visitor.setLastLineVisited(super.getLine());
		return visitor.visit(this);
	}
	
	@Override
	/**
	 * checks types only
	 */
	public boolean equals(Object comp){
		if(comp instanceof FuncParams)
		{
			FuncParams compod = ((FuncParams)comp);
			
			return this.params.equals(compod.params);
		}
		return false;
	}
	
	public boolean isEmpty(){
		return params.isEmpty();
	}
	
	public ArrayList<Sixple<Type, String, Annotations, Boolean, Boolean, Boolean>> getAsTypesAndNames()
	{
		ArrayList<Sixple<Type, String, Annotations, Boolean, Boolean, Boolean>> ret = new ArrayList<Sixple<Type, String, Annotations, Boolean, Boolean, Boolean>>();
		for(FuncParam par: params)
		{
			Type tt = par.getTaggedType();
			/*
			 * if(tt instanceof MultiType) { tt = tt.getTaggedType(); }
			 */
			ret.add(new Sixple<Type, String, Annotations, Boolean, Boolean, Boolean>(tt, par.name, par.annotations, null !=par.defaultValue, par.isVararg, par.isShared));
		}
		return ret;
	}
	
	public ArrayList<Type> getAsTypes()
	{
		ArrayList<Type> ret = new ArrayList<Type>();
		for(FuncParam par: params)
		{
			ret.add(par.getTaggedType());
		}
		
		return ret;
	}
	
	public ArrayList<String> getAsNames()
	{
		ArrayList<String> ret = new ArrayList<String>(params.size());
		for(FuncParam par: params)
		{
			ret.add(par.name);
		}
		
		return ret;
	}
	
	@Override
	public int hashCode()
	{
		int ret = 0;
		
		for(FuncParam p : params)
		{
			ret += p.hashCode();
		}
		return ret;
	}
	
	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		for(int n = 0; n < params.size(); n++)
		{
			sb.append(params.get(n));
			if(n != params.size()-1)
			{
				sb.append(", ");
			}
		}
		return sb.toString();
	}

	public boolean hasParams(){
		return !this.params.isEmpty();
	}
	
	public FuncParam getFirst() {
		return this.params.get(0);
	}
	
	public void remove(String name){
		//slighlty stupid to avoid conc exeption AND equals by type only...
		ArrayList<Integer> torm = new ArrayList<Integer>(params.size());
		int n=0;
		for(FuncParam par: params){
			if(par.name.equals(name)){
				torm.add(n);
			}
			n++;
		}
		n=0;
		for(int na : torm){
			params.remove(na-n);
			n++;
		}
		
	}
	
}
