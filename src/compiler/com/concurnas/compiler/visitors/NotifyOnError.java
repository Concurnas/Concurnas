package com.concurnas.compiler.visitors;

public interface NotifyOnError {
	public void initNotifyOnError(int maskingLevels);
	public void notifyOfError(int curMaskingLevel);
}
