package com.concurnas.compiler.utils;

import com.concurnas.runtime.Pair;

public class Sixple<A, B, C, D, E, F> {
	private final A a;
	private final B b;
	private final C c;
	private final D d;
	private final E e;
	private final F f;

	public Sixple(A a, B b, C c, D d, E e, F f)
	{
		this.a = a;
		this.b = b;
		this.c = c;
		this.d = d;
		this.e = e;
		this.f = f;
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
	
	public F getF() {
		return f;
	}

	public Sixple<F, E, D,C, B, A> reverse()
	{
		return new Sixple<F, E, D, C, B, A>(f, e, d, c, b, a);
	}
	
	@Override
	public String toString()
	{
		return String.format("(%s, %s, %s, %s, %s, %s)", this.a, this.b, this.c, this.d, this.e, this.f );
	}
	
	@Override
	public int hashCode()
	{
		return a==null?0:a.hashCode() 
				+ (b==null?0:b.hashCode())
				+ (c==null?0:c.hashCode())
				+ (d==null?0:d.hashCode())
				+ (e==null?0:e.hashCode())
				+ (f==null?0:f.hashCode());
	}
	
	
	@Override
	public boolean equals(Object o)
	{
		if(o instanceof Sixple){
			Sixple<?, ?, ?, ?, ?, ?> other = (Sixple<?, ?, ?, ?, ?, ?>)o;
			return Pair.tupleEEq(other.getA(), this.getA()) 
					&& Pair.tupleEEq(other.getB(), this.getB())
					&& Pair.tupleEEq(other.getC(), this.getC())
					&& Pair.tupleEEq(other.getD(), this.getD())
					&& Pair.tupleEEq(other.getE(), this.getE())
					&& Pair.tupleEEq(other.getF(), this.getF())
					;
		}
		return false;
	}
	
}
