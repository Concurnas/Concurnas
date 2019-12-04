package com.concurnas.compiler.visitors.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class PermutorWithCache {
	
	private static HashMap<Integer, List<Boolean[]>> cache = new HashMap<Integer, List<Boolean[]>>();
	
	/**
	 * Provide permutations whilst ignoring skipping null case (i.e. binary representation of 0) 
	 */
	public static List<Boolean[]> permutationsWithoutNullCase(int n) {
		
		List<Boolean[]> got = cache.get(n);
		if(got == null){
			got = new ArrayList<Boolean[]>();
			
			for (int i = 1; i < (Math.pow(2, n)); i++) {
				Boolean[] itm = new Boolean[n];
				String bString = Integer.toBinaryString(i);
				while(bString.length() < n){
					bString = "0" + bString;
				}
				
				for(int m=0; m < bString.length(); m++){
					if(bString.charAt(m)=='1'){
						itm[m]=true;
					}
				}
				got.add(itm);
			}
			
			cache.put(n, got);
		}

		return got;
	}
	
}
