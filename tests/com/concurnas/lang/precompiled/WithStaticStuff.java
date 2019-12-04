package com.concurnas.lang.precompiled;

public class WithStaticStuff {
	public int avar;
	public WithStaticStuff(int avar){
		this.avar=avar;
	}
	
	public static String name = "default name";
	public  String toString(){return name;}
	public static String astaticCall(){
		return "hi";
	}
}
