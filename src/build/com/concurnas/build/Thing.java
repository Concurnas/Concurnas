package com.concurnas.build;

import com.concurnas.runtime.cps.Constants;

public class Thing {
	public static void main(String[] rags) {
		int h=9;
		
		try {
			System.err.println("cl: " + Constants.class.getClassLoader());
		}catch(NoClassDefFoundError ndcf) {
			ndcf.getCause().printStackTrace();
		}
	}
}
