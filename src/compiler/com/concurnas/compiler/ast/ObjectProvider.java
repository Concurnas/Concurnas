package com.concurnas.compiler.ast;

import java.util.ArrayList;

import com.concurnas.compiler.ast.interfaces.Expression;
import com.concurnas.compiler.visitors.ScopeAndTypeChecker;
import com.concurnas.compiler.visitors.Visitor;
import com.concurnas.runtime.Pair;

public class ObjectProvider extends CompoundStatement implements HasAnnotations {
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
		
		return visitor.visit(this);
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
}
