package com.concurnas.lang.precompiled;

public class PrivateClassHeirarchy {

	public static class ParentOne{
		
	}
	
	private static abstract class MyAbstract extends ParentOne{
		
	}
	
	public static class ClType1 extends MyAbstract{}
	public static class ClType2 extends MyAbstract{}
	
	public static MyAbstract getInstance() {
		return new ClType1();
	}
	
	
	private static abstract class MyAbstractB{
		
	}
	
	public static class ClTypeB1 extends MyAbstractB{}
	public static class ClTypeB2 extends MyAbstractB{}
	
}
