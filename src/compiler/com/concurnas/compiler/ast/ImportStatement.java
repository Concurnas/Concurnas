package com.concurnas.compiler.ast;


public abstract class ImportStatement extends Statement {
	public boolean normalImport;

	public ImportStatement(int line, int col, boolean isNormalImport)
	{
		super(line, col);
		this.normalImport = isNormalImport;
	}
	

	
}
