package com.concurnas.compiler.typeAndLocation;

import com.concurnas.compiler.ast.AccessModifier;
import com.concurnas.compiler.ast.Annotations;
import com.concurnas.compiler.ast.FuncDef;
import com.concurnas.compiler.ast.FuncType;
import com.concurnas.compiler.ast.NamedType;
import com.concurnas.compiler.ast.Type;
import com.concurnas.compiler.utils.Thruple;
import com.concurnas.runtime.Pair;

public abstract class Location {

	private boolean isLambda = false;
	protected String lambdaOwner = "SETME";
	private boolean isFinal = false;
	private boolean isTransient = false;
	private boolean isShared = false;
	private AccessModifier accessModifier;//should be used for functions as well as variables, oops
	private Pair<String, String> privateStaticAccessorRedirectFuncGetter = null; // (accessorMethodName, origName)
	private Thruple<String, String, Type> privateStaticAccessorRedirectFuncSetter = null; // (accessorMethodName, origName)
	
	public Annotations annotations;//MHA: really doesnt belong here, oh well!
	
	public boolean localClassImportedField = false;
	
	public abstract Location copy();//TODO: i dont think this works, remove, since not used anywhere?
	
	private FuncType originatesFromConstructorRef = null;
	
	public FuncType getOriginatesFromConstructorRef(){
		return originatesFromConstructorRef;
	}
	
	public void setOriginatesFromConstructorRef(FuncType xxx){
		originatesFromConstructorRef = xxx;
	}
	
	public boolean isLambda() {
		return isLambda;
	}
	public void setLambda(boolean isLambda) {
		this.isLambda = isLambda;
	}
	public String getLambdaOwner() {
		if(null != originatesFromConstructorRef){
			String ret = originatesFromConstructorRef.getBytecodeType();
			return ret.substring(1, ret.length()-1);
		}
		return lambdaOwner;
	}
	public void setLambdaOwner(String owner) {
		this.lambdaOwner = owner;
	}

	public boolean isFinal() {
		return isFinal;
	}
	public void setFinal(boolean isFinal) {
		this.isFinal = isFinal;
	}
	
	public boolean isTransient() {
		return isTransient;
	}
	public void setTransient(boolean isTransient) {
		this.isTransient = isTransient;
	}
	
	public boolean isShared() {
		return isShared;
	}
	public void setShared(boolean isShared) {
		this.isShared = isShared;
	}
	
	//MHA: for fields and module level vars,can't think of anywhere else to stick this information....
	public AccessModifier getAccessModifier() {
		return accessModifier;
	}
	public void setAccessModifier(AccessModifier accessModifier) {
		this.accessModifier = accessModifier;
	}
	
	public Pair<String, String> getPrivateStaticAccessorRedirectFuncGetter() {
		return privateStaticAccessorRedirectFuncGetter;
	}
	public void setPrivateStaticAccessorRedirectFuncGetter(String accessorMethodName, String origName) {
		this.privateStaticAccessorRedirectFuncGetter = new Pair<String, String>(accessorMethodName, origName);
	}	
	
	
	
	public Thruple<String, String, Type> getPrivateStaticAccessorRedirectFuncSetter() {
		return privateStaticAccessorRedirectFuncSetter;
	}
	public void setPrivateStaticAccessorRedirectFuncSetter(String accessorMethodName, String origName, Type varType) {
		this.privateStaticAccessorRedirectFuncSetter = new Thruple<String, String, Type>(accessorMethodName, origName, varType);
	}	
	
	
	private FuncDef taggedFuncDef;
	public String redirectExtFuncOrWithExpr = null;
	
	public FuncDef getTaggedFuncDef() {
		return taggedFuncDef;
	}

	public void setTaggedFuncDef(FuncDef taggedFuncDef) {
		this.taggedFuncDef = taggedFuncDef;
	}
	
	public boolean isRHSOfTraitSuper = false;
	public Pair<NamedType, String> isRHSOfTraitSuperChainable = null;
	public boolean isInjected = false;
	
	public boolean islazyLambdaVar = false;
	
}
