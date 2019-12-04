package com.concurnas.compiler.ast;

public interface HasExtraCapturedVars {
	public FuncParams getExtraCapturedLocalVars();
	public void setExtraCapturedLocalVars(FuncParams extraCapturedLocalVars);
}
