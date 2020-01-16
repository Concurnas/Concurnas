package com.concurnas.compiler.ast;

import java.util.ArrayList;

import com.concurnas.compiler.ast.interfaces.Expression;
import com.concurnas.compiler.visitors.ScopeAndTypeChecker;
import com.concurnas.compiler.visitors.Unskippable;
import com.concurnas.compiler.visitors.Visitor;
import com.concurnas.runtime.Pair;

public class ObjectProvider extends CompoundStatement implements HasAnnotations, REPLTopLevelComponent {
	public AccessModifier accessModifier;
	public String providerName;
	public ClassDefArgs classDefArgs;
	public boolean isTransient;
	public boolean isShared;
	public Annotations annotations;
	public ObjectProviderBlock objectProviderBlock;
	public ArrayList<Pair<String, NamedType>> classGenricList;
	
	public ClassDef astRedirect;
	public ClassDef astRedirectPrev;
	
	public boolean failedGennerateClass=true;
	
	public ObjectProvider(int line, int col, AccessModifier accessModifier, 
			String providerName, ClassDefArgs classDefArgs, boolean isTransient, boolean isShared, 
			ObjectProviderBlock objectProviderBlock,
			ArrayList<Pair<String, NamedType>> classGenricList) {
		super(line, col);
		this.accessModifier = accessModifier;
		this.providerName = providerName;
		this.classDefArgs = classDefArgs;
		this.isTransient = isTransient;
		this.isShared = isShared;
		this.objectProviderBlock = objectProviderBlock;
		this.classGenricList = classGenricList;
	}

	@Override
	public Node copyTypeSpecific() {
		return this;
	}

	@Override
	public Object accept(Visitor visitor) {
		visitor.setLastLineVisited(super.getLine());
		
		if(astRedirect != null && !(visitor instanceof ScopeAndTypeChecker)) {
			return astRedirect.accept(visitor);
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
	
	
	private Expression preceedingExpression;
	@Override
	public void setPreceedingExpression(Expression expr) {
		this.preceedingExpression = expr;
	}
	@Override
	public Expression getPreceedingExpression() {
		return preceedingExpression;
	}

	@Override
	public void setAnnotations(Annotations annotations) {
		this.annotations=annotations;
	}
	
	@Override
	public Annotations getAnnotations(){
		return annotations;
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
		return this.providerName;
	}

	@Override
	public Type getFuncType() {
		//tuple of provided?
		return null;
	}

	@Override
	public boolean isNewComponent() {
		return true;
	}
	
	@Override
	public boolean persistant() { 
		return true;
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