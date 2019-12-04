package com.concurnas.lang;

import java.lang.reflect.Array;
import java.util.IdentityHashMap;

public class Hasher {
/*	
	public static boolean isCallerAlreadyInCallInStack(Object object){
		//if not deliberate then inf loop
		StackTraceElement[] stacktrace = Thread.currentThread().getStackTrace();
		
		int n=2;
		int maxN = stacktrace.length;
		StackTraceElement caller = stacktrace[2];
		
		while(++n < maxN){
			StackTraceElement e = stacktrace[n];
			if(e.equals(caller)){//loop
				return true;
			}
		}
		
		return false;
	}*/
	
	public static final IdentityHashMap<Object, Object> defVisitAlready = new IdentityHashMap<Object, Object>();
	//TODO: make me hidden in some way?
	
	public static int hashCode(Object a){
		if(null == a){ return 0; }
		
		Class<?> clsA = a.getClass();
		
		if( clsA.isArray() ){
			int ret = 0;
			int arDimA = Array.getLength(a);
			for(int n=0; n < arDimA; n++){
				ret += hashCode(Array.get(a, n));
			}
			return ret;
		}
		else{
			return a.hashCode();
		}
	}
	
	public static int hashCode(Object[] a){//slightly quicker with this variant copy pasted as well...
		if(null == a){ return 0; }
		int ret = 0;
		int arDimA = Array.getLength(a);
		for(int n=0; n < arDimA; n++){
			ret += hashCode(Array.get(a, n));
		}
		return ret;
	}
}
