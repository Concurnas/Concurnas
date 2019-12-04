package com.concurnas.compiler.utils;

import com.concurnas.runtime.Pair;

public class NinePull<A, B, C, D, E, F, G, H, I> {
	private final A a;
	private final B b;
	private final C c;
	private final D d;
	private final E e;
	private final F f;
	private final G g;
	private final H h;
	private final I i;

	public NinePull(A a, B b, C c, D d, E e, F f, G g, H h, I i)
	{
		this.a = a;
		this.b = b;
		this.c = c;
		this.d = d;
		this.e = e;
		this.f = f;
		this.g = g;
		this.h = h;
		this.i = i;
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
	
	public G getG() {
		return g;
	}
	
	public H getH() {
		return h;
	}
	
	public I getI() {
		return i;
	}

	public NinePull<I, H, G, F, E, D,C, B, A> reverse()
	{
		return new NinePull<I, H, G, F, E, D, C, B, A>(i, h, g, f, e, d, c, b, a);
	}
	
	@Override
	public String toString()
	{
		return String.format("(%s, %s, %s, %s, %s, %s, %s, %s, %s)", this.a, this.b, this.c, this.d, this.e, this.f, this.g, this.h, this.i );
	}
	
	@Override
	public int hashCode()
	{
		return a==null?0:a.hashCode() 
				+ (b==null?0:b.hashCode())
				+ (c==null?0:c.hashCode())
				+ (d==null?0:d.hashCode())
				+ (e==null?0:e.hashCode())
				+ (f==null?0:f.hashCode())
				+ (g==null?0:g.hashCode())
				+ (h==null?0:h.hashCode())
				+ (i==null?0:i.hashCode());
	}
	
	
	@Override
	public boolean equals(Object o)
	{
		if(o instanceof NinePull){
			NinePull<?, ?, ?, ?, ?, ?, ?, ?, ?> other = (NinePull<?, ?, ?, ?, ?, ?, ?, ?, ?>)o;
			return Pair.tupleEEq(other.getA(), this.getA()) 
					&& Pair.tupleEEq(other.getB(), this.getB())
					&& Pair.tupleEEq(other.getC(), this.getC())
					&& Pair.tupleEEq(other.getD(), this.getD())
					&& Pair.tupleEEq(other.getE(), this.getE())
					&& Pair.tupleEEq(other.getF(), this.getF())
					&& Pair.tupleEEq(other.getG(), this.getG())
					&& Pair.tupleEEq(other.getH(), this.getH())
					&& Pair.tupleEEq(other.getI(), this.getI())
					;
		}
		
		return false;
	}
	
}
