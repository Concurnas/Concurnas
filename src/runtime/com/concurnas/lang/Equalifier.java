package com.concurnas.lang;

import java.lang.reflect.Array;
import java.util.IdentityHashMap;

public class Equalifier {
	public static final IdentityHashMap<Object, Object> defEQVisitAlready = new IdentityHashMap<Object, Object>();
	//TODO: make me hidden in some way?
	
	public static boolean equals(Object a, Object b){
		if(null == a || null == b){
			if(null == a && null == b){
				return true;
			}
			return false;
		}
		
		if(a==b){ return true; }//save time
		
		Class<?> clsA = a.getClass();
		Class<?> clsB = b.getClass();
		
		if( clsA.isArray() && clsB.isArray()){
			int arDimA = Array.getLength(a);
			
			if(arDimA != Array.getLength(b)){
				return false;
			}
			else{
				
				for(int n=0; n < arDimA; n++){
					if(!equals(Array.get(a, n), Array.get(b, n))){
						return false;
					}
				}
				return true;
			}
		}
		else{
			return a.equals(b);
		}
	}
	
	public static boolean equals(Object[] a, Object[] b){//slightly quicker with this variant copy pasted as well...
		if(null == a || null == b){
			if(null == a && null == b){
				return true;
			}
			return false;
		}
		
		if(a==b){ return true; }//save time
		
		if(a.length != b.length){
			return false;
		}
		else{			
			for (int n = 0; n < a.length; n++) {
				if (!equals(a[n], b[n])) {
					return false;
				}
			}
		}
		
		return true;
	}
}
