package com.concurnas.lang.precompiled;

public class JustASuperClass {

	protected int protectedMethod() {
		return 232;
	}
	
	
	private static class Intermediate extends JustASuperClass{}
	
	private static class Child extends Intermediate{
		public int some(JustASuperClass inst) {
			return inst.protectedMethod();
		}
	}
	
	public static void main(String[] args) {
		Child c = new Child();
		
		
		System.err.println("" + c.some(c));
	}
	
}
