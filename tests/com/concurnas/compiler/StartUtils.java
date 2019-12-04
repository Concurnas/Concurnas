package com.concurnas.compiler;

public class StartUtils {
	
	public static boolean isRunningInEclipse() {
		for(StackTraceElement ele : new Exception().getStackTrace()) {
			if(ele.getClassName().contains("TestRunner")) {
				return false;
			}
		}
		return true; 
	}
	
	
}
