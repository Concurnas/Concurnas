package com.concurnas.compiler.ast;

public interface IdxVarHaver {
	public AssignNew getIdxVariableCreator();
	public void setIdxVariableCreator(AssignNew xxx);
	public RefName getIdxVariableAssignment();
}
