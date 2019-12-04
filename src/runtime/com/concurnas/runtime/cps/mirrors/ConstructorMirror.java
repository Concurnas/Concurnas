package com.concurnas.runtime.cps.mirrors;



public interface ConstructorMirror  {
	/** @see org.objectweb.asm.Type#getMethodDescriptor(java.lang.reflect.Method) */
	public abstract String getMethodDescriptor();
	public abstract boolean isPublic();
}
