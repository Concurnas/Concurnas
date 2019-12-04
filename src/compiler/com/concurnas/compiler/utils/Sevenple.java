package com.concurnas.compiler.utils;

public class Sevenple<A, B, C, D, E, F, G> {
	private final A a;
	private final B b;
	private final C c;
	private final D d;
	private final E e;
	private final F f;
	private final G g;

	public Sevenple(A a, B b, C c, D d, E e, F f, G g)
	{
		this.a = a;
		this.b = b;
		this.c = c;
		this.d = d;
		this.e = e;
		this.f = f;
		this.g = g;
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

	public Sevenple<G, F, E, D,C, B, A> reverse()
	{
		return new Sevenple<G, F, E, D, C, B, A>(g, f, e, d, c, b, a);
	}
	
	@Override
	public String toString()
	{
		return String.format("(%s, %s, %s, %s, %s, %s, %s)", this.a, this.b, this.c, this.d, this.e, this.f, this.g );
	}
	
	@Override
	public int hashCode()
	{
		return this.a.hashCode() + this.b.hashCode()+ this.c.hashCode()+ this.d.hashCode()+ this.e.hashCode()+ this.f.hashCode()+ this.g.hashCode();
	}
	
	
	@Override
	public boolean equals(Object o)
	{
		if(o instanceof Sevenple)
		{
			return ((Sevenple<?, ?, ?, ?, ?, ?, ?>)o).getA().equals(this.getA()) 
					&& ((Sevenple<?, ?, ?, ?, ?, ?, ?>)o).getB().equals(this.getB())
					&& ((Sevenple<?, ?, ?, ?, ?, ?, ?>)o).getC().equals(this.getC())
					&& ((Sevenple<?, ?, ?, ?, ?, ?, ?>)o).getD().equals(this.getD())
					&& ((Sevenple<?, ?, ?, ?, ?, ?, ?>)o).getE().equals(this.getE())
					&& ((Sevenple<?, ?, ?, ?, ?, ?, ?>)o).getE().equals(this.getF())
			        && ((Sevenple<?, ?, ?, ?, ?, ?, ?>)o).getE().equals(this.getG());
		}
		return false;
	}
	
}
