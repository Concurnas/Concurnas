package com.concurnas.runtime.cps.mirrors;

public class ClassMirrorNotFoundException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 56969696969694L;

	public ClassMirrorNotFoundException (String msg) {
	    super(msg);
	}
	public ClassMirrorNotFoundException(Throwable cause) {
		super(cause);
	}
	public ClassMirrorNotFoundException(String className,
			ClassNotFoundException e) {
		super(className, e);
	}

}
