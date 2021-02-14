package com.concurnas.bootstrap.runtime;

/**
 * Used in interfaces with static variables to indicate whether
 * <clinit> equivilent has been invoked
 */
public final class ClinitOnceIfaceHolder {
	public boolean initOnce = false;
}
