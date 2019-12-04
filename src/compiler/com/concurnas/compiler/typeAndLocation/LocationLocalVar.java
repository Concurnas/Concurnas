package com.concurnas.compiler.typeAndLocation;

import com.concurnas.compiler.ast.LambdaDef;
import com.concurnas.compiler.ast.Type;
import com.concurnas.compiler.visitors.datastructs.TheScopeFrame;

public class LocationLocalVar extends Location {

	private boolean isLambda = false;
	//private String lambdaOwner = "SETME";
	public TheScopeFrame scopeFramDefinedIn=null;
	private LambdaDef lamda; 
	
	@Override
	public Location copy() {
		LocationLocalVar ret = new LocationLocalVar(scopeFramDefinedIn);
		ret.isLambda = isLambda;
		ret.lambdaOwner = lambdaOwner;
		ret.lamda = lamda==null?null:(LambdaDef)lamda.copy();
		return ret;
	}
	
	public LambdaDef getLamda() {
		return lamda;
	}

	public void setLamda(LambdaDef lam) {
		this.lamda = lam;
	}

	public LocationLocalVar(TheScopeFrame scopeFramDefinedI){
		this.scopeFramDefinedIn = scopeFramDefinedI;
	}
	
	public boolean isLambda() {
		return isLambda;
	}
	public void setLambda(boolean isLambda) {
		this.isLambda = isLambda;
	}
}
