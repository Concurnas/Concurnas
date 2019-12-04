package com.concurnas.runtime;

@SuppressWarnings("serial")
public class MethodTransformationFail extends RuntimeException {

	public String uniqueName;
	
	public MethodTransformationFail(String uniqueName, String message){
		super(message);
		this.uniqueName = uniqueName;
	}
	
}
