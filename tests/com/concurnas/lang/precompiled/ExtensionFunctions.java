package com.concurnas.lang.precompiled;

import java.util.Collections;

import com.concurnas.lang.ExtensionFunction;

public class ExtensionFunctions {
	@ExtensionFunction
	public static String repeater(String str, int rep){
		return String.join("", Collections.nCopies(rep, str));
	}
	
	public static String notAnExtFunction(String str, int rep){
		return String.join("", Collections.nCopies(rep, str));
	}
	
}
