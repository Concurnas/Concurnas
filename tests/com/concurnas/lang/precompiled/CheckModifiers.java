package com.concurnas.lang.precompiled;

public class CheckModifiers {
	public static boolean isEnum(Class<?> cls){
		return (cls.getModifiers() & 0x00004000) != 0;
	}
	public static int Something = 99;
}
