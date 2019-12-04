package com.concurnas.compiler.utils;

import com.concurnas.runtime.Pair;

public class Fiveple<A, B, C, D, E> {
	private final A a;
	private final B b;
	private final C c;
	private final D d;
	private final E e;

	public Fiveple(A a, B b, C c, D d, E e)
	{
		this.a = a;
		this.b = b;
		this.c = c;
		this.d = d;
		this.e = e;
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
	
	public E getE() {
		return e;
	}

	public Fiveple<E, D,C, B, A> reverse()
	{
		return new Fiveple<E, D, C, B, A>(e, d, c, b, a);
	}
	
	@Override
	public String toString()
	{
		return String.format("(%s, %s, %s, %s, %s)", this.a, this.b, this.c, this.d, this.e );
	}
	
	@Override
	public int hashCode()
	{
		return a==null?0:a.hashCode() 
				+ (b==null?0:b.hashCode())
				+ (c==null?0:c.hashCode())
				+ (d==null?0:d.hashCode())
				+ (e==null?0:e.hashCode());
	}
	
	
	@Override
	public boolean equals(Object o)
	{
		if(o instanceof Fiveple){
			Fiveple<?, ?, ?, ?, ?> other = (Fiveple<?, ?, ?, ?, ?>)o;
			return Pair.tupleEEq(other.getA(), this.getA()) 
					&& Pair.tupleEEq(other.getB(), this.getB())
					&& Pair.tupleEEq(other.getC(), this.getC())
					&& Pair.tupleEEq(other.getD(), this.getD())
					&& Pair.tupleEEq(other.getE(), this.getE())
					;
		}
		return false;
	}
	
}
