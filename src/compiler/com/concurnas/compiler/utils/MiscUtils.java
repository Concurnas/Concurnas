package com.concurnas.compiler.utils;

public class MiscUtils {

	public static void printCurrentStack(int bk) {
		System.out.println("Current Stack "+ Thread.currentThread().getStackTrace()[bk] + "\n");
	}

}
