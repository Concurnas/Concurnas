package com.concurnas.lang.precompiled;

import com.concurnas.runtime.ref.Local;

public class TestHelperClasses {//cheat because deful test classloader uses system one which incldues these defs
	public static class Sup{
		public String somethng() { return "aaa";}
	}
	
	public static abstract class Sup2{
		public abstract String somethng();
	}
	
	public static class Sup3{
		private String somethng() { return "aaa";}
		
		public void sdfsdf() { somethng();}
	}
	
	public static int countRefLevels(Object o) throws Throwable{
		int x=0;
		
		while(o instanceof Local<?>){
			o=((Local)o).get();
			x++;
		}
		
		return x;
	}
}
