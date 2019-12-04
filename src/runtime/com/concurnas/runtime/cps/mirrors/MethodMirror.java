package com.concurnas.runtime.cps.mirrors;



public interface MethodMirror  {
    
    public abstract String getName();
	
	/** @see org.objectweb.asm.Type#getMethodDescriptor(java.lang.reflect.Method) */
	public abstract String getMethodDescriptor();
	
	public abstract String getMethodsignature();

	public abstract String[] getExceptionTypes() throws ClassMirrorNotFoundException;

	public abstract boolean isBridge();
	
	public abstract boolean isPublicAndNonStatic();
	
	public boolean hasBeenConced();

	public abstract boolean isFinal();


}
