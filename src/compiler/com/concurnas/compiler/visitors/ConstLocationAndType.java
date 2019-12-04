package com.concurnas.compiler.visitors;

import com.concurnas.compiler.ast.Type;

public class ConstLocationAndType{
	
	public int line;
	public int col;
	public Type type;

	public ConstLocationAndType(int line, int col, Type type)
	{
		this.line = line;
		this.col = col;
		this.type = type;
	}
	
	@Override
	public int hashCode()
	{
		return this.type.hashCode() + line + col;
	}
	
	@Override
	public boolean equals(Object obj)
	{
		if(obj instanceof ConstLocationAndType)
		{
			ConstLocationAndType comp = (ConstLocationAndType)obj;
			return type.equals(comp.type) && this.line == comp.line && this.col == comp.col;
		}
		return false;
	}
}
