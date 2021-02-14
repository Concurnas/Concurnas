package com.concurnas.lang.precompiled;

public interface IfaceToGlobalize {

	int SOMEVAR = Thing.helper();
	
	public static int thing() {
		return 23;
	}
	
	
	public static class Thing {
		public static int helper() {
			return 23;
		}
	}
	
}
