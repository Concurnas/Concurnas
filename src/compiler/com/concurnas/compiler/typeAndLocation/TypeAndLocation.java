package com.concurnas.compiler.typeAndLocation;


import com.concurnas.compiler.ast.FuncType;
import com.concurnas.compiler.ast.Type;

public class TypeAndLocation {

	private Type type;
	private Location location;
	//public String nameRedirect;
	
	public TypeAndLocation(Type type, Location location)
	{
		//if(location==null){throw new RuntimeException("null location");}//TODO: remove this check
		//assert location !=null;//TODO: consider removing the cooment here and seeing if this assertion ever gets triggered
		this.type = type;
		this.location = location;
	}
	
	public void setLocation(Location location){
		this.location = location;
	}
	
	public void setType(Type xxx){
		this.type = xxx;
	}
	
	public Type getType()
	{
		return this.type;
	}
	
	public Location getLocation()
	{
		return this.location;
	}

	@Override
	public int hashCode()
	{
		return type.hashCode();
	}
	
	@Override
	public boolean equals(Object o)
	{
		if(o instanceof TypeAndLocation)
		{
			TypeAndLocation tal = (TypeAndLocation)o;
			boolean eq= tal.getType().equals(this.getType());//ignore location
			return eq;
		}
		return false;
	}
	
	
	public TypeAndLocation copy(){
		TypeAndLocation ret = new TypeAndLocation((Type)type.copy(), this.location.copy());
		//ret.nameRedirect = nameRedirect;
		return ret;
	}
	
	public TypeAndLocation cloneWithNoRetType(){
		FuncType ret = (FuncType)this.type.copy();
		ret.realReturnType=null;
		ret.retType=null;
		TypeAndLocation retx = new TypeAndLocation(ret, this.location);
		//retx.nameRedirect = nameRedirect;
		return retx;
	}
	
	public TypeAndLocation cloneWithRetFuncType(Type ret){
		TypeAndLocation retx = new TypeAndLocation(ret, this.location);
		//retx.nameRedirect = nameRedirect;
		return retx;
	}
	
	@Override
	public String toString()
	{
		return this.type.toString() + ":" + this.location;
	}
}
