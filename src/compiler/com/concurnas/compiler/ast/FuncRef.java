package com.concurnas.compiler.ast;

import java.util.ArrayList;

import com.concurnas.compiler.ast.interfaces.Expression;
import com.concurnas.compiler.typeAndLocation.TypeAndLocation;
import com.concurnas.compiler.utils.Fourple;
import com.concurnas.compiler.visitors.ScopeAndTypeChecker;
import com.concurnas.compiler.visitors.VectorizedRedirector;
import com.concurnas.compiler.visitors.Visitor;
import com.concurnas.compiler.visitors.Utils.CurriedVararg;
import com.concurnas.compiler.visitors.datastructs.TheScopeFrame;
import com.concurnas.runtime.Pair;

public class FuncRef extends AbstractExpression implements Expression, HasLambdaDetails, CanBeInternallyVectorized {

	public Expression functo;
	public FuncRefArgs args;
	public FuncRefArgs argsForNextCompCycle;// = new FuncRefArgs(0,0);
	public FuncRefArgs extraArgsForLambdaConst;
	public TypeAndLocation typeOperatedOn;
	public ArrayList<Type> argumentsThatDontNeedToBecurriedIn;
	public Type operatingOn;
	public String methodName;
	private Pair<String, String> lambdaDetails;
	public boolean shouldVisitFunctoInBytcodeGen = false;
	public FuncDef defaultEquals;
	public TheScopeFrame lambdaClassScopeFrame;
	public String superClassName = null;
	public ArrayList<Type> genTypes = null;
	public boolean isConstructor=false;
	public boolean unbounded=false;
	public Node functoFoBC;
	public Block vectorizedRedirect=null;
	public NamedType replaceFirstArgWithTypeArraOf;
	public Expression astRedirect;
	public DotOperator astOverrideOperatorOverload;
	public Node astRedirectnewOperatorOverloaded;
	
	
	public FuncRef(int line, int col, Expression functo, FuncRefArgs args) {
		super(line, col);
		this.functo = functo;
		if(functo != null){
			functo.setPreceedingExpression(this);
		}
		this.args = args; //a needs to resolve to a function
	}
	
	@Override
	public Node copyTypeSpecific() {
		FuncRef ret = new FuncRef(super.getLine(), super.getColumn());
		
		if(functo==null) {
			ret.functo = null;
		}else {
			if(functo.getPreceedingExpression() == this) {
				functo.setPreceedingExpression(null);
				ret.functo = (Expression)functo.copy();
				ret.functo.setPreceedingExpression(ret);
				functo.setPreceedingExpression(this);
			}else {
				ret.functo = (Expression)functo.copy();
			}
		}
		
		ret.args = args==null?args:(FuncRefArgs)args.copy();
		ret.argsForNextCompCycle = argsForNextCompCycle==null?argsForNextCompCycle:(FuncRefArgs)argsForNextCompCycle.copy();
		ret.extraArgsForLambdaConst = extraArgsForLambdaConst==null?extraArgsForLambdaConst:(FuncRefArgs)extraArgsForLambdaConst.copy();
		ret.typeOperatedOn = typeOperatedOn;
		ret.argumentsThatDontNeedToBecurriedIn = (ArrayList<Type>) Utils.cloneArrayList(argumentsThatDontNeedToBecurriedIn);
		ret.operatingOn = operatingOn==null?operatingOn:(Type)operatingOn.copy();
		ret.methodName = methodName;
		ret.lambdaDetails = lambdaDetails;
		ret.shouldVisitFunctoInBytcodeGen = shouldVisitFunctoInBytcodeGen;
		ret.defaultEquals =defaultEquals;
		ret.lambdaClassScopeFrame = lambdaClassScopeFrame;
		ret.superClassName = superClassName;
		ret.isConstructor = isConstructor;
		ret.unbounded = unbounded;
		ret.genTypes = (ArrayList<Type>) Utils.cloneArrayList(genTypes);
		ret.functoFoBC = functoFoBC;
		ret.replaceFirstArgWithTypeArraOf = replaceFirstArgWithTypeArraOf==null?null:replaceFirstArgWithTypeArraOf.copyTypeSpecific();
		ret.astRedirect = astRedirect;
		ret.curriedVararg=curriedVararg;
		ret.astOverrideOperatorOverload = (DotOperator)(astOverrideOperatorOverload==null?null:astOverrideOperatorOverload.copyTypeSpecific());
		ret.preceedingExpression = preceedingExpression==null?null:(Expression)preceedingExpression.copy();
		ret.astRedirectnewOperatorOverloaded = astRedirectnewOperatorOverloaded==null?null:(Node)astRedirectnewOperatorOverloaded.copy();
		ret.vectorizedRedirect = vectorizedRedirect==null?null: (Block)vectorizedRedirect.copy();
		ret.lhsHasNewOPOVerload = lhsHasNewOPOVerload;
		//Type tt = this.getTaggedType();
		//ret.setTaggedType(tt==null?null:(Type)tt.copy());
		
		return ret;
	}
	
	/**
	 * For use when making a fake FuncRef
	 */
	public FuncRef(int line, int col){
		super(line,col);
	}
	
	
	@Override
	public Object accept(Visitor visitor) {
		visitor.setLastLineVisited(super.getLine());
		
		if(null != vectorizedRedirect){
			if(!(visitor instanceof VectorizedRedirector)){
				return vectorizedRedirect.accept(visitor);
			}
		}
		
		if(null != astOverrideOperatorOverload /*&& !(visitor instanceof ScopeAndTypeChecker)*/){
			return astOverrideOperatorOverload.accept(visitor);
		}
		
		if(null != astRedirect){//onchange nesting dealt with below
			return astRedirect.accept(visitor);
		}
		
		if(null != astRedirectnewOperatorOverloaded && !(visitor instanceof ScopeAndTypeChecker)){
			return astRedirectnewOperatorOverloaded.accept(visitor);
		}
		
		return visitor.visit(this);
	}
	
	public boolean isPermissableToGoOnRHSOfADot(Node preceededBy){
		return true;
	}
	
	
	@Override
	public void setLambdaDetails(Pair<String, String> lambdaDetails) {
		//System.err.println("setLambdaDetails" + lambdaDetails + "to: " + this);
		this.lambdaDetails=lambdaDetails;
	}

	@Override
	public Pair<String, String> getLambdaDetails() {
		return lambdaDetails;
	}

	public FuncRefArgs getArgsForScopeAndTypeCheck() {
		/*if(null != argsForNextCompCycle){
			return argsForNextCompCycle;
		}*/
		return args;
	}
	
	public void setArgsForScopeAndTypeCheck(FuncRefArgs args) {
		this.args =  args;
	}

	public ArrayList<Expression> getArgsAndLambdaConsts() {
		ArrayList<Expression> gotArgs = this.argsForNextCompCycle.getBoundArgs();
		
		if(null != this.extraArgsForLambdaConst){
			for(Expression e : this.extraArgsForLambdaConst.getBoundArgs()){
				gotArgs.add(e);
			}
		}
		
		return gotArgs;
	}
	
	public FuncRefArgs getArgsAndLambdaConstsAsFuncRefArgs() {
		FuncRefArgs ret = new FuncRefArgs(0,0);
		for(FuncRefArgs fra: new FuncRefArgs[]{this.argsForNextCompCycle, this.extraArgsForLambdaConst}){
			if(null != fra ){
				for(Object o: fra.exprOrTypeArgsList){
					ret.exprOrTypeArgsList.add(o);
				}
			}
		}
		ret.curriedVararg = this.curriedVararg;
		return ret;
	}

	private Expression preceedingExpression;
	public CurriedVararg curriedVararg;
	public Fourple<ArrayList<Pair<Boolean, NullStatus>>, ArrayList<Integer>, Integer, Type> vectroizedDegreeAndArgs;
	@Override
	public void setPreceedingExpression(Expression expr) {
		this.preceedingExpression = expr;
	}
	@Override
	public Expression getPreceedingExpression() {
		return preceedingExpression;
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

	
	private boolean hasErrored=false;
	public boolean lhsHasNewOPOVerload=false;;
	@Override
	public boolean hasErroredAlready() {
		return hasErrored;
	}
	@Override
	public void setHasErroredAlready(boolean hasError) {
		this.hasErrored=hasError;
	}
	
}
