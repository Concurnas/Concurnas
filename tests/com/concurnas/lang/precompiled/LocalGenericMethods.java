package com.concurnas.lang.precompiled;

public class LocalGenericMethods<F> {
	public static   int alocalGeneric2(int a){
		return 12;
	}
	public   <Type> int alocalGeneric(Type a, F u){
		return 12;
	}
}
