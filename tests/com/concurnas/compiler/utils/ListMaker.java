package com.concurnas.compiler.utils;

import java.util.ArrayList;
import java.util.List;

public class ListMaker {

	public static <X> List<X> make(X... args){
		List<X> ret = new ArrayList<X>(args.length);
		for(X a : args) {
			ret.add(a);
		}
		return ret;
	}
	
}
