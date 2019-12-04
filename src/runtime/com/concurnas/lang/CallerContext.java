package com.concurnas.lang;

public class CallerContext extends SecurityManager {
	private static final CallerContext INSTANCE = new CallerContext();
    private CallerContext() {}
    
    public static CallerContext getInstnce() {
    	return INSTANCE;
    }

    public Class<?>[] getCallerStack() {
    	return getClassContext();
    }
}