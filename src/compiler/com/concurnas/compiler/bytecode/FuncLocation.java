package com.concurnas.compiler.bytecode;

import com.concurnas.compiler.ast.NamedType;
import com.concurnas.compiler.ast.Type;
import com.concurnas.compiler.typeAndLocation.Location;

public class FuncLocation extends Location {
	
	public Type ownerType;
	
	public FuncLocation(Type ownerType){
		this.ownerType = ownerType;
	}

	public Type getOwnerType() {
		return ownerType;
	}
	
	@Override
	public Location copy() {
		return new FuncLocation((Type)ownerType.copy());
	}

	public static class StaticFuncLocation extends FuncLocation
	{
		public StaticFuncLocation(NamedType ownerType){
			super(ownerType);
		}
	}
	
	public static class OuterClassLocation extends FuncLocation
	{
		//inner class calls function in outerclass
		public OuterClassLocation(Type ownerType){
			super(ownerType);
		}
	}
	
	public static class ClassFunctionLocation extends FuncLocation
	{
		public String owner;
		public boolean isInterface;
		public boolean castToCOBject=false;

		public ClassFunctionLocation(String owner, Type ownerType, boolean isInterface)
		{
			super(ownerType);
			
			this.owner = owner;
			this.isInterface = isInterface;
		}
		
		public ClassFunctionLocation(String owner, Type ownerType)
		{
			this(owner, ownerType, false );
		}
		
		@Override
		public Location copy() {
			return new ClassFunctionLocation(owner, (Type)super.ownerType.copy(), isInterface);
		}
		
	}
	
	public static class NestedSuperClassFuncLocation extends FuncLocation
	{//inner class calls function in outerclass
		public NestedSuperClassFuncLocation(Type ownerType){
			super(ownerType);
		}
	}
	
	
	public static class DummyFuncLocation extends FuncLocation
	{
		public DummyFuncLocation(Type ownerType){
			super(ownerType);
		}
	}
	
	@Override
	public String toString()
	{
		return this.getClass().getName();
	}
	
	@Override
	public int hashCode()
	{
		return this.toString().hashCode();
	}
	
	@Override
	public boolean equals(Object o)
	{
		if(o instanceof FuncLocation){
			return this.toString().equals(o.toString());
		}
		return false;
	}
}
