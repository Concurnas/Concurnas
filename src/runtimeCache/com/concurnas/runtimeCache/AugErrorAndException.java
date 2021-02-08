package com.concurnas.runtimeCache;

public class AugErrorAndException {
	public String module;
	public String className;
	public Throwable err;

	public AugErrorAndException(String module, Throwable err) {
		this(module, null, err);
	}
	
	public AugErrorAndException(String module, String className, Throwable err) {
		this.module = module;
		this.className = className;
		this.err = err;
	}
}