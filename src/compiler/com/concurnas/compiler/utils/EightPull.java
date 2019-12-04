package com.concurnas.compiler.utils;

import com.concurnas.runtime.Pair;

public class EightPull<A, B, C, D, E, F, G, H> {
	private final A a;
	private final B b;
	private final C c;
	private final D d;
	private final E e;
	private final F f;
	private final G g;
	private final H h;

	public EightPull(A a, B b, C c, D d, E e, F f, G g, H h)
	{
		this.a = a;
		this.b = b;
		this.c = c;
		this.d = d;
		this.e = e;
		this.f = f;
		this.g = g;
		this.h = h;
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
	

	public EightPull<H, G, F, E, D,C, B, A> reverse()
	{
		return new EightPull<H, G, F, E, D, C, B, A>(h, g, f, e, d, c, b, a);
	}
	
	@Override
	public String toString()
	{
		return String.format("(%s, %s, %s, %s, %s, %s, %s, %s)", this.a, this.b, this.c, this.d, this.e, this.f, this.g, this.h );
	}
	
	@Override
	public int hashCode()
	{
		return a==null?0:this.a.hashCode() 
				+ (b==null?0:this.b.hashCode())
				+ (c==null?0:this.c.hashCode())
				+ (d==null?0:this.d.hashCode())
				+ (e==null?0:this.e.hashCode())
				+ (f==null?0:this.f.hashCode())
				+ (g==null?0:this.g.hashCode())
				+ (h==null?0:this.h.hashCode());
	}
	
	
	@Override
	public boolean equals(Object o)
	{
		if(o instanceof EightPull){
			EightPull<?, ?, ?, ?, ?, ?, ?, ?> other = (EightPull<?, ?, ?, ?, ?, ?, ?, ?>)o;
			return Pair.tupleEEq(other.getA(), this.getA()) 
					&& Pair.tupleEEq(other.getB(), this.getB())
					&& Pair.tupleEEq(other.getC(), this.getC())
					&& Pair.tupleEEq(other.getD(), this.getD())
					&& Pair.tupleEEq(other.getE(), this.getE())
					&& Pair.tupleEEq(other.getF(), this.getF())
					&& Pair.tupleEEq(other.getG(), this.getG())
					&& Pair.tupleEEq(other.getH(), this.getH())
					;
		}
		return false;
	}
	
}
