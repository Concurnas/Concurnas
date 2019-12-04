package com.concurnas.runtime;


public class Pair<A, B> {
	private A a;
	private B b;

	public Pair(A a, B b)
	{
		this.a = a;
		this.b = b;
	}

	public A getA() {
		return a;
	}

	public B getB() {
		return b;
	}
	
	public Pair<B, A> reverse()
	{
		return new Pair<B, A>(b, a);
	}
	
	@Override
	public String toString()
	{
		return String.format("(%s, %s)", this.a, this.b );
	}
	
	@Override
	public int hashCode()
	{
		return (this.a != null?this.a.hashCode():0) + (this.b != null?this.b.hashCode():0);
	}
	
	public static boolean tupleEEq(Object a, Object b) {
		if(a == null) {
			return b == null;
		}else {
			return a.equals(b);
		}
	}
	
	@Override
	public boolean equals(Object o)
	{
		if(o instanceof Pair){
			Pair<?, ?> other = (Pair<?, ?>)o;
			return tupleEEq(other.getA(), this.getA()) 
						&& tupleEEq(other.getB(), this.getB())
						;
		}
		return false;
	}
}
