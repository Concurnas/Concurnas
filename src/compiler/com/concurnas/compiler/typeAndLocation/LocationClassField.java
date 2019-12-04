package com.concurnas.compiler.typeAndLocation;

import com.concurnas.compiler.ast.Type;

public class LocationClassField extends Location {
	
	private final String owner;
	public final Type ownerType;
	public final boolean isArrayLength;//MHA! - super hack such that ARRAYLENGTH is called instead
	public final boolean assignedOnCreation;
	private boolean isLambda = false;
	public boolean isOverride;
	//private String lambdaOwner = "SETME";
	
	public LocationClassField(String owner, Type ownerType){
		this(owner, ownerType, false, false, false);
	}
	
	public LocationClassField(String owner, Type ownerType, boolean isArrayLength, boolean assignedOnCreation, boolean isOverride)
	{
		this.owner = owner;
		this.ownerType = ownerType;
		this.isArrayLength = isArrayLength;
		this.assignedOnCreation = assignedOnCreation;
		this.isOverride = isOverride;
		
	}
	
	@Override
	public Location copy() {
		LocationClassField ret = new LocationClassField(owner, (Type)ownerType.copy(), isArrayLength, assignedOnCreation, isOverride);
		ret.isLambda = isLambda;
		ret.lambdaOwner = lambdaOwner;
		return ret;
	}
	
	public String getOwner(){
		return owner;
	}
	
	public boolean isLambda() {
		return isLambda;
	}
	public void setLambda(boolean isLambda) {
		this.isLambda = isLambda;
	}
	/*public String getLambdaOwner() {
		return lambdaOwner;
	}
	public void setLambdaOwner(String owner) {
		this.lambdaOwner = owner;
	}*/
}
