package com.concurnas.compiler.ast;

import java.util.HashSet;
import java.util.Map;

import com.concurnas.runtime.Pair;

public interface AsyncInvokable extends HasExtraCapturedVars{
	
	
	public FuncDef getinitMethodNameFuncDef();
	public FuncDef getapplyMethodFuncDef();
	public FuncDef getcleanUpMethodFuncDef();
	public Pair<String, String> getonChangeDets();
	public String getFullnameSO();
	public HashSet<String> getnamesOverridedInInit();
	public Map<String, String> gettakeArgFromSO();
	public boolean getnoReturn();
	public AssignExisting gettheAssToStoreRefIn();
	public boolean getisModuleLevel();
	public NamedType getholderclass();
	
	
	
}
