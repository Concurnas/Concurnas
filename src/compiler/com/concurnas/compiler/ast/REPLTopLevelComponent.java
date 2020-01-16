package com.concurnas.compiler.ast;

import java.util.ArrayList;

/**
 * Indicates item can be a component of the repl graph
 * Also indicates that for compilation stages other than bytecode generation analysis can be skipped if flag is set
 */
public interface REPLTopLevelComponent {
	public boolean canSkip();
	public void setSkippable(boolean skippable);
	public String getName();
	
	public default ArrayList<String> getNames(){
		ArrayList<String> ret = new ArrayList<String>();
		ret.add(getName());
		return ret;
	}
	
	public Type getFuncType();
	public boolean isNewComponent();
	public default boolean persistant() { return false;}
	public void setErrors(boolean b);
	public boolean getErrors();
	public void setSupressErrors(boolean supressErrors);
	public boolean getSupressErrors();
}
