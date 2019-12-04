package com.concurnas.compiler.visitors.util;

import com.concurnas.compiler.ast.AccessModifier;
import com.concurnas.compiler.ast.Annotations;
import com.concurnas.compiler.ast.Type;

public class VarAtScopeLevel {
	private String varname;
	private Type type;
	private Boolean isFinal;
	private AccessModifier accessModifier;
	private Integer extraModifiers;
	private Annotations annotations;
	private Boolean assignedOnCreationAndFinal;
	private Boolean assignedOnCreation;
	private Boolean isInjected;
	private Boolean isShared;

	public String getVarName() {
		return varname;
	}

	public Type getType() {
		return type;
	}
	
	public Boolean isFinal() {
		return isFinal;
	}

	public AccessModifier getAccessModifier() {
		return accessModifier;
	}
	
	public Integer getExtraModifiers() {
		return extraModifiers;
	}
	
	public Annotations getAnnotations() {
		return annotations;
	}
	
	public Boolean getAssignedOnCreationAndFinal() {
		return assignedOnCreationAndFinal;
	}
	
	public Boolean getAssignedOnCreation() {
		return assignedOnCreation;
	}
	
	public Boolean isInjected() {
		return isInjected;
	}
	
	public Boolean isShared() {
		return isShared;
	}
	
	public VarAtScopeLevel(String varname, Type type, Boolean isFinal, AccessModifier accessModifier, Integer extraModifiers, Annotations annotations, 
			Boolean assignedOnCreationAndFinal, Boolean assignedOnCreation, Boolean isInjected, Boolean isShared) {
		this.varname = varname;
		this.type = type;
		this.isFinal = isFinal;
		this.accessModifier = accessModifier;
		this.extraModifiers = extraModifiers;
		this.annotations = annotations;
		this.assignedOnCreationAndFinal = assignedOnCreationAndFinal;
		this.assignedOnCreation = assignedOnCreation;
		this.isInjected = isInjected;
		this.isShared = isShared;
	}
}
