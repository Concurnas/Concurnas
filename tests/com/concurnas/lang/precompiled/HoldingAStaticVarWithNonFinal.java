package com.concurnas.lang.precompiled;

public class HoldingAStaticVarWithNonFinal {
	public static final int CL_INVALID_VALUE  = -30;
	public static int CL_INVALID_VALUE2  = 100;
	public int getInvalidValue() {
		return HoldingAStaticVarWithNonFinal.CL_INVALID_VALUE;
	}
}
