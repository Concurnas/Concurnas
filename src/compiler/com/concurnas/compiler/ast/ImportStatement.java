package com.concurnas.compiler.ast;

import java.util.ArrayList;

public abstract class ImportStatement extends Statement implements REPLTopLevelComponent {
	public boolean normalImport;

	public ImportStatement(int line, int col, boolean isNormalImport)
	{
		super(line, col);
		this.normalImport = isNormalImport;
	}
	

	@Override
	public void setErrors(boolean hasErrors) {
	}
	@Override
	public boolean getErrors() {
		return false;
	}
	
	@Override
	public void setSupressErrors(boolean supressErrors) {
	}
	@Override
	public boolean getSupressErrors() {
		return false;
	}

	@Override
	public boolean canSkip() {
		return false;
	}

	@Override
	public void setSkippable(boolean skippable) {
	}

	@Override
	public String getName() {
		return null;
	}


	@Override
	public boolean isNewComponent() {
		return true;
	}
	
	@Override
	public Type getFuncType() {
		return null;
	}
	
}
