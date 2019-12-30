package com.concurnas.compiler.ast;

/**
 * Indicates item can be a component of the repl graph
 * Also indicates that for compilation stages other than bytecode generation analysis can be skipped if flag is set
 */
public interface REPLDepGraphComponent {
	public boolean canSkip();
	public void setSkippable(boolean skippable);
	public String getName();
	public Type getFuncType();
	public boolean isNewComponent();
}
