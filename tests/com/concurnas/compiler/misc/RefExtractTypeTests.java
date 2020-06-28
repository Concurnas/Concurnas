package com.concurnas.compiler.misc;

import junit.framework.TestCase;

import org.junit.Test;

import com.concurnas.runtime.RefUtils;

public class RefExtractTypeTests extends TestCase{

	private String pprintArr(Class<?>[] arr){
		StringBuilder sb = new StringBuilder("[");
		for(int n=0; n < arr.length; n++){
			sb.append(arr[n]==null?"null":arr[n].getSimpleName());
			if(n != arr.length-1){
				sb.append(", ");
			}
		}
		sb.append("]");
		return sb.toString();
	}
	
	@Test
	public void testExtractTypeAndAugmentTypeArray() throws Throwable {
		//basic tests to fill in one null arg
		Class<?>[] got = RefUtils.extractTypeAndAugmentTypeArray(new Class<?>[]{Integer.class, Double.class, Integer.class, null, Double.class}, 1, 234.f, new Class<?>[]{Object.class});
		super.assertEquals("[Integer, Double, Integer, Float, Double]", pprintArr(got));
		
		got = RefUtils.extractTypeAndAugmentTypeArray(new Class<?>[]{Integer.class, Double.class, null, Integer.class, Double.class}, 2, 234.f, new Class<?>[]{Object.class});
		super.assertEquals("[Integer, Double, Float, Integer, Double]", pprintArr(got));
		
		got = RefUtils.extractTypeAndAugmentTypeArray(new Class<?>[]{Integer.class, Double.class, null, Integer.class, Double.class}, 2, null, new Class<?>[]{Object.class, String.class});
		super.assertEquals("[Integer, Double, Object, String, Integer, Double]", pprintArr(got));
		
		
	}

}
