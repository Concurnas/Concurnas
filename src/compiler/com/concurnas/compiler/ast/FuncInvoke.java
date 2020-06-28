package com.concurnas.compiler.ast;

import java.util.ArrayList;

import com.concurnas.compiler.ast.interfaces.Expression;
import com.concurnas.compiler.ast.util.ExpressionListOrigin;
import com.concurnas.compiler.typeAndLocation.TypeAndLocation;
import com.concurnas.compiler.utils.Fourple;
import com.concurnas.compiler.visitors.ScopeAndTypeChecker;
import com.concurnas.compiler.visitors.VectorizedRedirector;
import com.concurnas.compiler.visitors.Visitor;
import com.concurnas.runtime.Pair;

public class FuncInvoke extends Node implements Expression, CanBeInternallyVectorized {
	//public Expression functo;
	public String funName;
	public String bytecodefunName;
	public FuncInvokeArgs args;
	public TypeAndLocation resolvedFuncTypeAndLocation;
	public boolean isReallyLambda = false;
	public boolean addCheckCastToRetType = false;
	public boolean isSynth = false;
	public Type refRepointOrigLhsType;
	public Node astRedirectforOnChangeNesting;
	public Node astRedirect;
	public Node primaryASTOverride;
	private Block vectorizedRedirect=null;
	public ArrayList<Type> genTypes;
	public boolean bcGenStopThingCalledOnBeingUnreffed = false;
	public DotOperator astOverrideOperatorOverload;
	//public boolean popOnEntry;
	public boolean refShouldBeDeletedOnUsusedReturn=false;
	//public boolean copyInArgs=false;
	
	public static FuncInvoke makeFuncInvoke(int line, int col, String funName, Expression... exprs){
		FuncInvokeArgs fia = new FuncInvokeArgs(line, col);
		for(Expression e: exprs){
			fia.add(e);
		}
		
		return new FuncInvoke(line, col, funName, fia);
	}
	
	public static FuncInvoke makeFuncInvoke(int line, int col, String funName, ArrayList<Expression> exprs){
		FuncInvokeArgs fia = new FuncInvokeArgs(line, col);
		for(Expression e: exprs){
			fia.add(e);
		}
		
		return new FuncInvoke(line, col, funName, fia);
	}
	
	public FuncInvoke(int line, int col, String funName, FuncInvokeArgs args) {
		super(line, col);
		this.funName = funName;
		this.args=args;
	}
	
	public FuncInvoke(int line, int col, String funName, FuncInvokeArgs args, ArrayList<Type> genTypes) {
		this(line, col, funName, args);
		this.genTypes=genTypes;
	}
	
	
	public FuncInvoke(int line, int col, String funName, Expression... oneArgs) {
		super(line, col);
		this.funName = funName;
		FuncInvokeArgs f = new FuncInvokeArgs(line, col);
		for(Expression oneArg : oneArgs) {
			f.add(oneArg);
		}
		
		this.args=f;
	}
	
	public FuncInvoke(int line, int col, String funName) {//no arg
		super(line, col);
		this.funName = funName;
		this.args=new FuncInvokeArgs(line, col);
	}
	
	
/*	public void setShouldBePresevedOnStack(boolean should)
	{
		super.setShouldBePresevedOnStack(should);
	}*/
	
	@Override
	public boolean getCanBeOnItsOwnLine(){
		return true;
	}
	
	//e.g. nestedFunc(a int) => NIF$0(a int, dep1 int, dep2 int)
	public void addNIFArg(Expression toAdd) {
		this.args.add(toAdd);
	}
	
	/*
	public FuncInvoke(Expression funto, FuncInvokeArgs args) {
		this.functo = funto;
		this.args = args;
	}
	*/
	
	@Override
	public Object accept(Visitor visitor) {
		visitor.setLastLineVisited(super.getLine());
		
		if(null != vectorizedRedirect){
			if(!(visitor instanceof VectorizedRedirector)){
				//if(visitor instanceof ScopeAndTypeChecker){
				//	visitor.visit(this);//visit self as normal in satc
				//}
				
				//if vectorizeRedirector, ignore
				return vectorizedRedirect.accept(visitor);
			}
		}
		
		if(null != primaryASTOverride && !(visitor instanceof ScopeAndTypeChecker)){
			return primaryASTOverride.accept(visitor);
		}
		if(null != astOverrideOperatorOverload && !(visitor instanceof ScopeAndTypeChecker)){
			return astOverrideOperatorOverload.accept(visitor);
		}
		
		if(null != astRedirect && null == astRedirectforOnChangeNesting){//onchange nesting dealt with below
			return astRedirect.accept(visitor);
		}
		
		return visitor.visit(this);
	}
	
	@Override
	public Type getTaggedType(){
		if(null != primaryASTOverride ){
			return primaryASTOverride.getTaggedType();
		}
		if(null != astOverrideOperatorOverload ){
			return astOverrideOperatorOverload.getTaggedType();
		}
		
		if(null != astRedirect && null == astRedirectforOnChangeNesting){//onchange nesting dealt with below
			return astRedirect.getTaggedType();
		}
		
		return super.getTaggedType();
	}

	@Override
	public boolean isPermissableToGoOnRHSOfADot(Node preceededBy){
		return true;
	}

	@Override
	public Node copyTypeSpecific() {
		FuncInvoke ret = new FuncInvoke(super.getLine(), super.getColumn(), funName, (FuncInvokeArgs)args.copy());
		ret.bytecodefunName = bytecodefunName;
		ret.resolvedFuncTypeAndLocation = resolvedFuncTypeAndLocation;
		ret.isReallyLambda = isReallyLambda;
		ret.addCheckCastToRetType = addCheckCastToRetType;
		ret.isSynth = isSynth;
		ret.refRepointOrigLhsType = refRepointOrigLhsType;
		ret.astRedirect = astRedirect;
		ret.primaryASTOverride = primaryASTOverride==null?null:primaryASTOverride.copy();
		ret.astRedirectforOnChangeNesting = null==astRedirectforOnChangeNesting?null:astRedirectforOnChangeNesting.copy();
		ret.genTypes = (ArrayList<Type>) Utils.cloneArrayList(genTypes) ;
		ret.bcGenStopThingCalledOnBeingUnreffed = bcGenStopThingCalledOnBeingUnreffed;
		ret. astOverrideOperatorOverload = (DotOperator)(astOverrideOperatorOverload==null?null:astOverrideOperatorOverload.copyTypeSpecific());
		ret.preceedingExpression = preceedingExpression==null?null:(Expression)preceedingExpression.copy();
		ret.waitForRefToReturnSomething = waitForRefToReturnSomething;
		ret.supressVectorization = supressVectorization;
		ret.requiresGenTypeInference = requiresGenTypeInference;
		ret.vectorizedRedirect = vectorizedRedirect==null?null: (Block)vectorizedRedirect.copy();
		ret.origName = origName==null?null:(RefName)origName.copy();
		ret.lhsOfAssignExisting = lhsOfAssignExisting;
		ret.expressionListOrigin = expressionListOrigin;
		ret.nameAndLocKey = nameAndLocKey;
		//ret.copyInArgs = copyInArgs;
		//ret.vectroizedDegreeAndArgs = vectroizedDegreeAndArgs;
		
		return ret;
	}
	private Expression preceedingExpression;
	public Fourple<ArrayList<Pair<Boolean, NullStatus>>, ArrayList<Integer>, Integer, Type> vectroizedDegreeAndArgs; 
	
	@Override
	public void setPreceedingExpression(Expression expr) {
		this.preceedingExpression = expr;
	}
	@Override
	public Expression getPreceedingExpression() {
		return preceedingExpression;
	}
	
	public void overrideFuncName(String with) {
		this.funName = with;
		if(origName != null) {
			origName.overrideFuncName(with);
		}
	}
	
	@Override
	public String toString() {
		return super.getLine() + ": " + this.funName + (args==null?"()":args) ;
	}
	
	@Override
	public boolean hasBeenVectorized(){
		return this.vectroizedDegreeAndArgs != null;
	}
	
	@Override
	public boolean hasVectorizedRedirect() {
		return this.vectorizedRedirect != null;
	}

	@Override
	public void setVectorizedRedirect(Node vectRedirect) {
		this.vectorizedRedirect = (Block)vectRedirect;
	}

	@Override
	public boolean canBeNonSelfReferncingOnItsOwn() {
		return true;
	}

	
	private boolean hasErrored=false;
	public boolean waitForRefToReturnSomething=false;
	public boolean supressVectorization=false;
	public boolean requiresGenTypeInference=false;
	public RefName origName;
	public boolean lhsOfAssignExisting=false;
	public ExpressionListOrigin expressionListOrigin;
	public Pair<String, Boolean> nameAndLocKey;
	
	@Override
	public boolean hasErroredAlready() {
		return hasErrored;
	}
	@Override
	public void setHasErroredAlready(boolean hasError) {
		this.hasErrored=hasError;
	}
	
	@Override 
	public void setShouldBePresevedOnStack(boolean should) {
		if(this.astRedirectforOnChangeNesting != null) {
			astRedirectforOnChangeNesting.setShouldBePresevedOnStack(should);
		}
		
		if(this.astRedirect != null) {
			((Node)astRedirect).setShouldBePresevedOnStack(should);
		}
		
		if(this.primaryASTOverride != null) {
			primaryASTOverride.setShouldBePresevedOnStack(should);
		}
		
		super.setShouldBePresevedOnStack(should);
	}
}
