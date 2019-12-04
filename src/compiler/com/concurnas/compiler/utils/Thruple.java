package com.concurnas.compiler.utils;

import com.concurnas.runtime.Pair;

public class Thruple<A, B, C> {
	private final A a;
	private final B b;
	private final C c;

	public Thruple(A a, B b, C c)
	{
		this.a = a;
		this.b = b;
		this.c = c;
	}

	public A getA() {
		return a;
	}

	public B getB() {
		return b;
	}
	
	public C getC() {
		return c;
	}

	public Thruple< C, B, A> reverse()
	{
		return new Thruple<C, B, A>(c, b, a);
	}
	
	@Override
	public String toString()
	{
		return String.format("(%s, %s, %s)", this.a, this.b, this.c );
	}
	
	@Override
	public int hashCode()
	{
		return (a==null?0:this.a.hashCode() )
				+ (b==null?0:this.b.hashCode() )
				+ (c==null?0:this.c.hashCode());
	}
	
	
	@Override
	public boolean equals(Object o)
	{
		if(o instanceof Thruple){
			Thruple<?, ?, ?> other = (Thruple<?, ?, ?>)o;
			return Pair.tupleEEq(other.getA(), this.getA()) 
					&& Pair.tupleEEq(other.getB(), this.getB())
					&& Pair.tupleEEq(other.getC(), this.getC())
					;
		}
		return false;
	}
	
}
