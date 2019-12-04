package com.concurnas.compiler.typeAndLocation;

import com.concurnas.compiler.ast.Type;

public class LocationStaticField extends Location {
	
	public String owner;
	public Type type;
	private boolean isLambda = false;
	//private String lambdaOwner = "SETME";
	public boolean enumValue = false;
	
	public LocationStaticField(String owner, Type type)
	{
		//mv.visitFieldInsn(GETSTATIC, "A/Child", "a", "I");
		this.owner = owner;
		this.type = type;
	}
	public LocationStaticField(String owner, Type type, boolean enumValue)	{
		this(owner, type);
		this.enumValue = enumValue;
	}
	
	
	@Override
	public Location copy() {
		LocationStaticField ret = new LocationStaticField(owner, (Type)type.copy());
		ret.isLambda = isLambda;
		ret.lambdaOwner = super.lambdaOwner;
		return ret;
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
