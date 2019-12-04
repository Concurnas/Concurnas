package com.concurnas.compiler.ast;

import com.concurnas.runtime.Pair;

public interface HasLambdaDetails {
	//private Tuple<String, String> lambdaDetails=null;
	public void setLambdaDetails(Pair<String, String> lambdaDetails );
	public Pair<String, String> getLambdaDetails();
	
}
