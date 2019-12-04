package com.concurnas.lang.precompiled;

public class Sleepers {
	public static void sleepFunc(int cnt){
		try {
			Thread.sleep(cnt*1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}
