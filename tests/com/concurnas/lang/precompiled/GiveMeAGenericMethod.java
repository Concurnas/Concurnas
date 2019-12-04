package com.concurnas.lang.precompiled;

import java.util.HashMap;

public class GiveMeAGenericMethod<X> {

	public <Y>String proc(X x, Y y, HashMap<X,Y> kid){
		kid.put(x, y);
		return "" + kid;
	}
	
}
