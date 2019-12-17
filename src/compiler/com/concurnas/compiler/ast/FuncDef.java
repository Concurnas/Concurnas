package com.concurnas.compiler.ast;

import java.util.ArrayList;

import com.concurnas.compiler.ast.interfaces.Expression;
import com.concurnas.compiler.ast.interfaces.FuncDefI;
import com.concurnas.compiler.ast.util.GPUKernelFuncDetails;
import com.concurnas.compiler.visitors.ScopeAndTypeChecker;
import com.concurnas.compiler.visitors.Visitor;
import com.concurnas.runtime.Pair;

public class FuncDef extends FuncDefI implements HasAnnotations, Comparable<FuncDef>  {

	public String funcName;
	private String origfuncName;
	public FuncParams params = new FuncParams(0,0);
	public Type retType;// = new PrimativeType(PrimativeTypeEnum.VOID);
	public boolean isOverride = false;
	public Block funcblock;
	private boolean aabstract = false;
	
	public ClassDef origin = null;
	
	public boolean isEnumconstru = false;//for constructors but easier to put in here cos his has the copy consturcotr etc
	public boolean isEnumconstruSubClass = false;
	
	public boolean isAutoGennerated = false; //can be overwritten
	
	public ConstructorDef callsThisConstructor = null;
	public boolean callsAnotherConstructor = false;
	public boolean isFinal=false;
	public AccessModifier accessModifier = null;//AccessModifier.PUBLIC;
	public boolean shouldInferFuncType;
	public boolean alreadyNested = false;
	public Annotations annotations; 
	public boolean definedAtClassLevel = false;
	public boolean definedAtLocalClassLevel = false;

	@Override
	public void setAnnotations(Annotations annotations) {
		this.annotations=annotations;
	}
	
	@Override
	public Annotations getAnnotations(){
		return annotations;
	}
	
	public boolean doithCallsAnotherConstructor(){
		return callsAnotherConstructor;
	}
	
	public boolean isFinal() {
		return isFinal;
	}

	@Override
	public Type getRetuType() {
		return retType;
	}
	
	public AccessModifier getAccessModifier(){
		return accessModifier;
	}
	
	public void setShouldBePresevedOnStack(boolean should)
	{
		super.setShouldBePresevedOnStack(should);
		if(null!= this.funcblock){
			this.funcblock.setShouldBePresevedOnStack(should);
		}
	}
	
	public FuncDef(int line, int col, Annotations annotations, AccessModifier accessModifier, String funcName, FuncParams params, Block funcblock, boolean isOverride, boolean aabstract, boolean isFinal) {
		super(line, col, true);
		this.annotations = annotations;
		this.funcName = this.origfuncName = funcName;
		
		this.params = params !=null ? params : new FuncParams(line,col);
		this.funcblock = funcblock;
		if(null != this.funcblock){
			this.funcblock.isMethodBlock = true;
		}
		this.aabstract = aabstract;
		//assert null != this.funcName;
		this.isOverride =isOverride;
		this.isFinal =isFinal;
		this.accessModifier =accessModifier;
		shouldInferFuncType = true;
	}
	
	public FuncDef(int line, int col, Annotations annotations, AccessModifier accessModifier, String funcName, FuncParams params, Block funcblock, Type t, boolean isOverride, boolean aabstract, boolean isFinal, ArrayList<Pair<String, NamedType>> methodGenricList) {
		this(line, col, annotations, accessModifier, funcName, params, funcblock, isOverride, aabstract, isFinal);
		this.retType = t;
		shouldInferFuncType=null==t;
		this.methodGenricList = methodGenricList;
	}
	
	public static FuncDef build(Type returnType, Type... inputs){
		FuncParams fps = new FuncParams(0,0);
		for(int n=0; n< inputs.length; n++){
			fps.add(new FuncParam(0,0, "x" + n, inputs[n], false)); 
		}
		
		FuncDef ret = new FuncDef(0,0, null, AccessModifier.PUBLIC, "fname", fps, new Block(0,0), false, false, false);
		ret.retType = returnType;
		return ret;
	}
	public static FuncDef build(Type returnType, FuncParams fps){
		FuncDef ret = new FuncDef(0,0, null, AccessModifier.PUBLIC, null, fps, new Block(0,0), false, false, false);
		ret.retType = returnType;
		return ret;
	}
	
	@Override
	public Node copyTypeSpecific() {
		FuncDef ret = new FuncDef(super.line, super.column, (Annotations)(annotations==null?null:annotations.copy()), accessModifier==null?null:accessModifier.copy(), this.funcName, params==null?null:(FuncParams)params.copy(), funcblock==null?null:(Block)funcblock.copy(), retType==null?null:(Type)retType.copy(),  this.isOverride,  this.aabstract,  this.isFinal, methodGenricList==null?null:new ArrayList<Pair<String, NamedType>>(methodGenricList));
		copySpecifics(ret);

		return ret;
	}
	
	protected void copySpecifics(FuncDef ret) {
		ret.isNestedFunc=isNestedFunc;
		ret.isEnumconstru = isEnumconstru;
		ret.isEnumconstruSubClass = isEnumconstruSubClass;
		ret.preceedingExpression = preceedingExpression==null?null:(Expression)preceedingExpression.copy();
		ret.definedAtClassLevel = definedAtClassLevel;
		ret.definedAtLocalClassLevel = definedAtLocalClassLevel;
		ret.extFunOn = extFunOn==null?null:(Type)extFunOn.copy();
		ret.checkForFDVariants = checkForFDVariants;
		ret.funcDefVariants = funcDefVariants==null?null: (ArrayList<FuncDef>) Utils.cloneArrayList(funcDefVariants);
		ret.ignore = ignore;
		ret.shouldInferFuncType = shouldInferFuncType;
		ret.requiresBridgeMethodTo = requiresBridgeMethodTo==null?null:(FuncType)extFunOn.copy();
		ret.getShouldBeDeletedOnUsusedReturnCache = getShouldBeDeletedOnUsusedReturnCache;
		ret.isGPUKernalOrFunction = isGPUKernalOrFunction;
		ret.kernelDim = kernelDim == null?null: (Expression)kernelDim.copy();
		ret.gpuKernelFuncDetails = gpuKernelFuncDetails;
		ret.origin=origin;
		ret.createTraitStaticMethod=createTraitStaticMethod;
		ret.isInjected=isInjected;
		ret.origfuncName = origfuncName;
		ret.supressErrors = supressErrors;
		ret.isNestedFuncionDef = isNestedFuncionDef;
	}

	
	public Expression kernelDim = null;
	
	private Expression preceedingExpression;
	public Type extFunOn;
	public ArrayList<FuncDef> funcDefVariants;
	public boolean checkForFDVariants=true;
	public boolean ignore;
	public FuncType requiresBridgeMethodTo;
	
	@Override
	public void setPreceedingExpression(Expression expr) {
		this.preceedingExpression = expr;
	}
	@Override
	public Expression getPreceedingExpression() {
		return preceedingExpression;
	}
	
	public boolean isAbstract()
	{
		return this.aabstract;
	}

	public void setAbstract(boolean asbtr) {
		aabstract = asbtr;
	}
	
	@Override
	public Object accept(Visitor visitor) {
		visitor.setLastLineVisited(super.getLine());
		
		if(funcDefVariants != null && !funcDefVariants.isEmpty() && !(visitor instanceof ScopeAndTypeChecker)) {
			Object last = null;
			for(FuncDef fd : funcDefVariants) {
				last = visitor.visit(fd);
			}
			return last;
		}
		
		/*if(this.isNestedFunc){
			System.err.println(new Exception().getStackTrace()[1].getClassName());	
		}*/
		
		if(this.isGPUKernalFuncOrStub()) {
			return visitor.visit(this);
		}else {
			if(visitor instanceof ScopeAndTypeChecker) {
				this.hasErrors = false;
			}
			visitor.pushErrorContext(this);
			Object ret = visitor.visit(this);
			visitor.popErrorContext();
			return ret;
		}
	}

	public FuncType getFuncType()
	{
		ArrayList<Type> iinputs = new ArrayList<Type>();
		for(FuncParam par : this.params.params) { iinputs.add(par.getTaggedType()); }
		FuncType ret = new FuncType(0,0,  iinputs , retType);
		ret.setAbstarct(this.isAbstract());
		ret.setFinal(this.isFinal());
		ret.setAutoGenenrated(this.isAutoGennerated);
		ret.origin = this.origin;
		ret.origonatingFuncDef = this;
		
		if(this.methodGenricList != null && !this.methodGenricList.isEmpty()){
			ArrayList<GenericType> localGenerics = new ArrayList<GenericType>();
			
			int n=0;
			for(Pair<String, NamedType> gg : this.methodGenricList){
				String name = gg.getA();
				NamedType nt = gg.getB();
				
				GenericType gt = new GenericType(name, n++);
				if(null != nt) {
					gt.upperBound = nt;
					gt.setNullStatus(nt.getNullStatus());
				}
				
				localGenerics.add(gt);
			}
			
			ret.setLocalGenerics(localGenerics);
		}
		
		if(this.extFunOn != null){
			ret.extFuncOn = true;
			iinputs.add(0, this.extFunOn);
		}
		
		
		
		return ret;
	}
	
	@Override
	public boolean equals(Object comp)
	{
		if(comp instanceof FuncDef)
		{//check closure only
			FuncDef compod = ((FuncDef)comp);
			return compod.params.equals(this.params);// && compod.retType.getNonGenericPrettyName().equals(this.retType.getNonGenericPrettyName());
		}
		return false;
	}
	
	@Override
	public int hashCode()
	{
		return this.params.hashCode();// + this.retType.getNonGenericPrettyName().hashCode();
	}
	
	@Override
	public String toString()
	{
		String gens = this.methodGenricList !=null && !this.methodGenricList.isEmpty() ? ""+this.methodGenricList : "";
		return String.format("%sfun %s%s%s(%s) %s", this.isInjected?"inject ":"",this.extFunOn!=null?(this.extFunOn+" ") : "", this.funcName, gens, this.params, this.retType);
	}

	@Override
	public FuncParams getParams() {
		return this.params;
	}

	@Override
	public Type getReturnType() {
		return this.retType;
	}
	
	@Override
	public String getMethodName() {
		return this.funcName;
	}
	
	public String getMethodNameIgnoreNIF() {
		return this.origfuncName;
	}

	@Override
	public Block getBody() {
		return this.funcblock;
	}

	@Override
	public void setMethodName(String replace) {
		this.funcName  =replace;
	}

	@Override
	public boolean IsAutoGennerated() {
		return this.isAutoGennerated;
	}
	
	@Override
	public boolean getShouldInferFuncType() {
		return shouldInferFuncType;
	}

	@Override
	public int compareTo(FuncDef o) {
		return this.toString().compareTo(o.toString());
	}

	public GPUKernelFuncDetails gpuKernelFuncDetails;
	
	private Boolean getShouldBeDeletedOnUsusedReturnCache = null;
	public boolean createTraitStaticMethod;
	public boolean supressErrors=false;
	public boolean isNestedFuncionDef=false;
	
	private boolean hasAnnotation(NamedType wantannot) {
		Annotations annots = getAnnotations();
		if(annots != null) {
			for(Annotation annot : annots.annotations) {
				if(wantannot.equals(annot.getTaggedType())) {
					return true;
				}
			}
		}
		return false;
	}
	
	public boolean isGPUStubFunction() {
		return hasAnnotation(ScopeAndTypeChecker.const_Annotation_GPUStubFunction);
	}
	
	public boolean isGPUKernalFuncOrStub() {
		return isGPUKernalOrFunction != null || isGPUStubFunction();
	}
	
	
	public boolean getShouldBeDeletedOnUsusedReturn() {
		if(getShouldBeDeletedOnUsusedReturnCache == null) {
			getShouldBeDeletedOnUsusedReturnCache=hasAnnotation(ScopeAndTypeChecker.const_Annotation_DeleteOnUnusedReturn);
		}
		return getShouldBeDeletedOnUsusedReturnCache;
	}
}
