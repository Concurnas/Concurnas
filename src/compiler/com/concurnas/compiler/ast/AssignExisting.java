package com.concurnas.compiler.ast;

import java.util.ArrayList;
import java.util.List;

import com.concurnas.compiler.ast.interfaces.Expression;
import com.concurnas.compiler.visitors.ScopeAndTypeChecker;
import com.concurnas.compiler.visitors.Unskippable;
import com.concurnas.compiler.visitors.VectorizedRedirector;
import com.concurnas.compiler.visitors.Visitor;
import com.concurnas.runtime.Pair;

public class AssignExisting extends Assign implements CanBeInternallyVectorized, AutoVectorizableElements, REPLTopLevelComponent  {

	public Expression assignee;
	public AssignStyleEnum eq;
	public Expression expr;
	
	public boolean isReallyNew = false;
	public int localVarToCopyRefInto;
	//public NamedType isMapSetter;
	public Node astOverrideOperatorOverload;
	
	public AssignExisting(int line, int col, Expression assignee, AssignStyleEnum eq, Expression asignment) {
		super(line, col, true);
		this.assignee = assignee;
		this.eq = eq;
		
		this.expr = asignment;
	}

	public AssignExisting(int line, int col, String name, AssignStyleEnum eq, Expression asignment) {
		this(line, col, new RefName(line, col, name), eq, asignment);
	}

	public Annotations annotations;
	public boolean isAnnotationField=false;
	public boolean ignoreFinalCheck = false;
	public boolean whitelist = false;//e.g. if we are assigning to a module variable we have imported specifically (then this is ok, its not a redefinition of an imported type/thing)

	@Override
	public void setAnnotations(Annotations annotations) {
		this.annotations=annotations;
	}

	@Override
	public Annotations getAnnotations() {
		return annotations;
	}
	
	@Override
	public List<VectorizationConfig> getAllElements() {
		List<VectorizationConfig> allExprs = new ArrayList<VectorizationConfig>(2);
		
		if(!eq.isEquals()) {
			allExprs.add(new VectorizationConfig(this.assignee, false, eq.methodString, false, false, false));
			allExprs.add(new VectorizationConfig(this.expr, null, null, null, false, false));
		}/*else if(!this.isReallyNew) {
			allExprs.add(new VectorizationConfig(this.assignee, false, eq.methodString, false, false, false));
			allExprs.add(new VectorizationConfig(this.expr, null, null, null, false, false));
		}*/
		
		return allExprs;
	}
	
	@Override
	public void setAllElements(List<Expression> newones) {
		if(!eq.isEquals()) {
			assignee = newones.get(0);
			expr = newones.get(1);
		}
	}
	
	@Override
	public Object accept(Visitor visitor) {
		visitor.setLastLineVisited(super.getLine());
		
		if(null != vectorizedRedirect){
			if(!(visitor instanceof VectorizedRedirector)){
				return vectorizedRedirect.accept(visitor);
			}
		}
		
		if(astOverrideOperatorOverload != null && !(visitor instanceof ScopeAndTypeChecker)){
			return astOverrideOperatorOverload.accept(visitor);
		}
		

		if(this.canSkipIterativeCompilation && !(visitor instanceof Unskippable)) {
			return null;
		}

		if(visitor instanceof ScopeAndTypeChecker) {
			this.hasErrors = false;
		}
		visitor.pushErrorContext(this);
		Object ret = visitor.visit(this);
		visitor.popErrorContext();
		return ret;
	}

	@Override
	public void setInsistNew(boolean b) {
		throw new RuntimeException("setInsistNew not implemented on AssignExisting");
	}

	@Override
	public Node copyTypeSpecific() {
		AssignExisting ret = new AssignExisting(super.getLine(), super.getColumn(), (Expression)this.assignee.copy(), eq, expr==null?null:(Expression)expr.copy());
		ret.isReallyNew=isReallyNew; 
		ret.whitelist=whitelist; 
		ret.isAnnotationField=isAnnotationField; 
		ret.localVarToCopyRefInto=localVarToCopyRefInto; 
		ret.astOverrideOperatorOverload=(AssignExisting)(astOverrideOperatorOverload==null?null:astOverrideOperatorOverload.copy()); 
		ret.annotations = annotations==null?null:(Annotations)annotations.copy();
		ret.ignoreFinalCheck = ignoreFinalCheck;
		ret.isTransient = isTransient;
		ret.isShared = isShared;
		ret.isLazy = isLazy;
		ret.gpuVarQualifier = gpuVarQualifier;
		ret.vectorizedRedirect = vectorizedRedirect==null?null: (Block)vectorizedRedirect.copy();
		ret.isInjected = isInjected;
		//ret.isMapSetter=isMapSetter==null?null:isMapSetter.copyTypeSpecific(); 
		return ret;
	}
	
	@Override
	public boolean isInsistNew() {
		return isReallyNew;
	}
	
	public ArrayList<Pair<Boolean, NullStatus>> depth = null;
	private Block vectorizedRedirect=null;
	
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
	@Override
	public boolean hasErroredAlready() {
		return hasErrored;
	}
	@Override
	public void setHasErroredAlready(boolean hasError) {
		this.hasErrored=hasError;
	}

	@Override
	public Expression getRHSExpression() {
		return expr;
	}

	@Override
	public Expression setRHSExpression(Expression what) {
		Expression was = this.expr;
		this.expr = what;
		return was;
	}

	@Override
	public void setAssignStyleEnum(AssignStyleEnum to) {
		this.eq = to;
	}
	@Override
	public boolean getCanReturnAValue(){
		return false;
	}

	
	
	private boolean canSkipIterativeCompilation=false;
	@Override
	public boolean canSkip() {
		return canSkipIterativeCompilation;
	}

	@Override
	public void setSkippable(boolean skippable) {
		canSkipIterativeCompilation = skippable;
	}

	@Override
	public String getName() {
		if(this.assignee instanceof RefName) {
			RefName rn = (RefName)this.assignee;
			return rn.name;
		}
		return null;
	}

	@Override
	public boolean isNewComponent() {
		return /* this.isReallyNew && */ this.assignee instanceof RefName;
	}
	
	@Override
	public Type getFuncType() {
		return this.getTaggedType();
	}
	

	public boolean hasErrors = false;
	@Override
	public void setErrors(boolean hasErrors) {
		this.hasErrors = hasErrors;
	}
	@Override
	public boolean getErrors() {
		return hasErrors;
	}
	
	private boolean supressErrors = false;
	@Override
	public void setSupressErrors(boolean supressErrors) {
		this.supressErrors = supressErrors;
	}
	@Override
	public boolean getSupressErrors() {
		return supressErrors;
	}
}
