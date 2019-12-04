package com.concurnas.compiler.bytecode;

import com.concurnas.compiler.ast.Type;
import com.concurnas.compiler.typeAndLocation.LocationClassField;
import com.concurnas.compiler.typeAndLocation.LocationStaticField;
import com.concurnas.runtime.Pair;

public class TopOfStack {

	public static class VarLocal extends TopOfStack
	{
		public Type type;
		public int slot;
		
		public VarLocal(Pair<Type, Integer> got)
		{
			type = got.getA();
			slot=got.getB();
		}
	}

	public static class VarStatic extends TopOfStack
	{
		public Type varType;
		public LocationStaticField loc;
		
		public VarStatic(Type varType, LocationStaticField loc)
		{
			this.varType = varType;
			this.loc = loc;
		}
		
	}
	
	public static class VarClassField extends TopOfStack
	{
		public Type varType;
		public LocationClassField loc;
		
		public VarClassField(Type varType, LocationClassField loc)
		{
			this.varType = varType;
			this.loc = loc;
		}
	}
	
}
