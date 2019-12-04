package com.concurnas.compiler.utils;

import com.concurnas.runtime.Pair;

public class Fourple<A, B, C, D> {
	private final A a;
	private final B b;
	private final C c;
	private final D d;

	public Fourple(A a, B b, C c, D d)
	{
		this.a = a;
		this.b = b;
		this.c = c;
		this.d = d;
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

	public D getD() {
		return d;
	}

	public Fourple<D,C, B, A> reverse()
	{
		return new Fourple<D, C, B, A>(d, c, b, a);
	}
	
	@Override
	public String toString()
	{
		return String.format("(%s, %s, %s, %s)", this.a, this.b, this.c, this.d );
	}
	
	@Override
	public int hashCode()
	{
		return a==null?0:a.hashCode() 
				+ (b==null?0:b.hashCode())
				+ (c==null?0:c.hashCode())
				+ (d==null?0:d.hashCode());
	}
	
	
	@Override
	public boolean equals(Object o)
	{
		if(o instanceof Fourple){
			Fourple<?, ?, ?, ?> other = (Fourple<?, ?, ?, ?>)o;
			return Pair.tupleEEq(other.getA(), this.getA()) 
					&& Pair.tupleEEq(other.getB(), this.getB())
					&& Pair.tupleEEq(other.getC(), this.getC())
					&& Pair.tupleEEq(other.getD(), this.getD())
					;
		}
		return false;
	}
	
}
