package com.concurnas.compiler.ast;

import java.util.ArrayList;

import com.concurnas.compiler.ast.interfaces.Expression;
import com.concurnas.compiler.ast.interfaces.FuncDefI;
import com.concurnas.compiler.typeAndLocation.TypeAndLocation;
import com.concurnas.compiler.visitors.Visitor;
import com.concurnas.runtime.Pair;

public class LambdaDef extends FuncDefI implements HasExtraCapturedVars, HasAnnotations, AnonLambdaDefOrLambdaDef {
	public FuncParams params = new FuncParams(0,0);
	public Type returnType;
	public Block body;

	public FuncRef fakeFuncRef;
	public String methodName;
	private FuncParams extraCapturedLocalVars;
	private final boolean shouldInferFuncType;
	public Pair<String, String> lamDets; //MHA
	public Boolean forceNestFuncRepoint = null;
	public Annotations annotations;
	
	@Override
	public void setAnnotations(Annotations annotations) {
		this.annotations=annotations;
	}
	
	@Override
	public Annotations getAnnotations(){
		return annotations;
	}
	
	public boolean isFinal() {
		return false;
	}
	
	@Override
	public Node copyTypeSpecific() {
		LambdaDef lam = new LambdaDef(super.line, super.column, 
				(Annotations)(annotations==null?null:annotations.copy()),
				(FuncParams)params.copy(), 
				body == null?null:(Block)body.copy(), 
				returnType==null?null:(Type)returnType.copy(), 
				(ArrayList<Pair<String, NamedType>>) Utils.cloneArrayList(methodGenricList) );
		lam.extraCapturedLocalVars = this.extraCapturedLocalVars;
		lam.lamDets=lamDets;
		lam.methodName=methodName;
		lam.fakeFuncRef=fakeFuncRef==null?null:(FuncRef)fakeFuncRef.copy();
		lam.forceNestFuncRepoint = forceNestFuncRepoint;
		lam.preceedingExpression = preceedingExpression==null?null:(Expression)preceedingExpression.copy();
		lam.ignore = ignore;
		lam.implementSAM = implementSAM;
		lam.omitAnonLambdaSources = omitAnonLambdaSources;
		lam.isInjected = isInjected;
		lam.origSource = origSource;
		lam.supressTypeBoxing = supressTypeBoxing;

		return lam;
	}

	private Expression preceedingExpression;
	public boolean ignore;
	public Pair<NamedType, TypeAndLocation> implementSAM;
	public boolean omitAnonLambdaSources = false;
	public String origSource = null;
	private boolean supressTypeBoxing=false;
	
	
	public void setSupressTypeBoxing(boolean supressTypeBoxing) {
		this.supressTypeBoxing = supressTypeBoxing;
	}

	public boolean getSupressTypeBoxing() {
		return supressTypeBoxing || this.implementSAM != null || this.fakeFuncRef != null;
	}
	
	
	@Override
	public void setPreceedingExpression(Expression expr) {
		this.preceedingExpression = expr;
	}
	@Override
	public Expression getPreceedingExpression() {
		return preceedingExpression;
	}
	
	public LambdaDef(int line, int col, Annotations annotations, FuncParams params, Block body, Type t, ArrayList<Pair<String, NamedType>> methodGenricList) {
		super(line, col, false);
		this.annotations=annotations;
		if(null != params)
		{
			this.params = params;
		}
		this.returnType = t;
		this.body = body;
		if(null!= this.body){
			this.body.isMethodBlock=true;
		}
		
		/*if(null != returnType){
			returnType.setInOutGenModifier(InoutGenericModifier.OUT);
		}*/
		
		shouldInferFuncType = returnType==null;
		super.methodGenricList = methodGenricList;
	}
	
	public void setShouldBePresevedOnStack(boolean should)
	{
		super.setShouldBePresevedOnStack(should);
		if(this.body != null) {
			this.body.setShouldBePresevedOnStack(should);
		}
	}
	
	public AccessModifier getAccessModifier(){
		return AccessModifier.PUBLIC;
	}

	public FuncType getFunctType()
	{
		ArrayList<Type> inputs = new ArrayList<Type>();
		if(params != null)
		{
			for(FuncParam p : params.params)
			{
				inputs.add(p.getTaggedType());
			}
		}
		
		FuncType ft = new FuncType(0,0,inputs, returnType);
		ft.origonatingFuncDef = FuncDef.build(returnType, params);
		
		return ft;
	}
	

	public FuncType getFunctTypeIncExtraArgs() {
		ArrayList<Type> inputs = new ArrayList<Type>();
		if(params != null)
		{
			for(FuncParam p : params.params)
			{
				inputs.add(p.getTaggedType());
			}
		}
		
		FuncType ret = new FuncType(0,0,inputs, returnType);
		
		ArrayList<GenericType> gens = new ArrayList<GenericType>();
		
		for(Pair<String, NamedType> ss : this.methodGenricList){
			String name = ss.getA();
			NamedType nt = ss.getB();
			GenericType gt = new GenericType(name,0);
			if(nt != null) {
				gt.upperBound = nt;
				gt.setNullStatus(nt.getNullStatus());
			}
			
			gens.add(gt);
		}
		
		ret.setLocalGenerics(gens);
		
		return ret;
	}

	
	
	@Override
	public Object accept(Visitor visitor) {
		visitor.setLastLineVisited(super.getLine());
		
		
		return visitor.visit(this);
	}

	@Override
	public Type getRetuType() {
		return returnType;
	}

	@Override
	public FuncParams getParams() {
		FuncParams pars = new FuncParams(0,0);
		for(FuncParams p : new FuncParams[]{params, extraCapturedLocalVars}){
			if(p !=null){
				for(FuncParam pa : p.params){
					pars.add(pa);
				}
			}
			
		}
		//return  this.params;
		return pars;
	}

	@Override
	public Type getReturnType() {
		return this.returnType;
	}

	@Override
	public String getMethodName() {
		return methodName;
	}

	@Override
	public Block getBody() {
		return this.body;
	}
	public boolean isAbstract()
	{
		return false;
	}
	
	public void setAbstract(boolean asbtr) {
	}

	@Override
	public void setMethodName(String replace) {
		this.methodName  =replace;
	}

	@Override
	public boolean IsAutoGennerated() {
		return false;
	}

	@Override
	public FuncParams getExtraCapturedLocalVars() {
		return this.extraCapturedLocalVars;
	}

	@Override
	public void setExtraCapturedLocalVars(FuncParams extraCapturedLocalVars) {
		this.extraCapturedLocalVars = extraCapturedLocalVars;
	}
	
	@Override
	public String toString()
	{
		return String.format("lambda %s(%s) %s : %s -> %s", this.methodName, this.params, this.returnType, this.getLine(), this.lamDets);
	}

	@Override
	public boolean getShouldInferFuncType() {
		return shouldInferFuncType;
	}
}
