package com.concurnas.compiler.ast;

public interface CanBeInternallyVectorized {
	public boolean hasVectorizedRedirect();
	public void setVectorizedRedirect(Node vectRedirect);
	
	public default boolean canBeNonSelfReferncingOnItsOwn() {
		return false;//e.g. a^ + 3, by itself on a line, cannot do this because it needs to return something
	}
	public boolean hasErroredAlready();
	public void setHasErroredAlready(boolean hasError);
}
