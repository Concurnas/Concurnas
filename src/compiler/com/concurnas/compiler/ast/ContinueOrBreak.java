package com.concurnas.compiler.ast;

public interface ContinueOrBreak extends CanEndInReturnOrException {
	public boolean hasReturns();
	public boolean getIsValid();
	public void setIsValid(boolean val);
	public void setIsAsyncEarlyReturn(boolean val);
	public boolean getIsAsyncEarlyReturn();
}
