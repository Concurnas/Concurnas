package com.concurnas.runtime;

/**
 * null or not object used to mark generated default argument methods/constructors
 */
public class DefaultParamUncreatable {
	public DefaultParamUncreatable(){}//TODO: should be private
	
	private static DefaultParamUncreatable sharedInstance = new DefaultParamUncreatable();
	
	public static DefaultParamUncreatable getInstance(){
		return sharedInstance;
	}
	
}
