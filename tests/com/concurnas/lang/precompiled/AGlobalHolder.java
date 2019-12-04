package com.concurnas.lang.precompiled;

import java.util.ArrayList;

public class AGlobalHolder {

	public static ArrayList<String> theList = new ArrayList<String>();
	static{
		theList.add("firstItem");
	}
	
}
