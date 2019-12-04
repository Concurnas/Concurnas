package com.concurnas.compiler.ast;

public abstract class Assign extends Statement implements HasAnnotations, AssignWithRHSExpression{
	public boolean isTempVariableAssignment = false;//used to mark temp vars so as t not create them as static var if this is at module level
	public int refCnt = 0;
	public boolean isTransient = false;
	public boolean isShared = false;
	public boolean isLazy = false;
	public boolean isModuleLevelShared = false;
	
	public Assign(int line, int column, boolean validAtClassLevel) {
		super(line, column, validAtClassLevel);
	}
	public abstract void setInsistNew(boolean b);
	public abstract boolean isInsistNew();

	public abstract void setAssignStyleEnum(AssignStyleEnum to);
	
	/*public void setTransient(boolean isTransient) {
		this.isTransient = isTransient;
	}*/

	public GPUVarQualifier gpuVarQualifier = null;
	//public boolean isActor = false;
	public boolean isOverride = false;
	public boolean isInjected = false;
	
}
