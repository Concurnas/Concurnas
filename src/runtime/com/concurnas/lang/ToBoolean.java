package com.concurnas.lang;

public class ToBoolean {
	public static boolean toBoolean(float dd) {
		return !Float.isNaN(dd) && Math.round(dd) != 0; 
	}
	
	public static boolean toBoolean(double dd) {
		return !Double.isNaN(dd) && Math.round(dd) != 0; 
	}
	
	public static boolean toBoolean(int dd) {
		return dd != 0; 
	}
	
	public static boolean toBoolean(long dd) {
		return dd != 0; 
	}
	
	public static boolean toBoolean(short dd) {
		return dd != 0; 
	}
	
	public static boolean toBoolean(byte dd) {
		return dd != 0; 
	}
	
	public static boolean toBoolean(char dd) {
		return dd != 0; 
	}
}
