package com.concurnas.compiler.utils;

import junit.framework.TestCase;
import org.junit.Test;
import com.concurnas.lang.ArrayUtils;

public class ArraysTest extends TestCase {

	@Test
	public void testArrays1D() {
		Object[] obj = new Object[]{1,2,3};
		//converted
		Integer[] got = ArrayUtils.cast(obj, Integer[].class);
		
		assertTrue(got instanceof Integer[]);
		assertTrue(got[0] instanceof Integer);
		assertTrue(obj != got);//diff object
	}
	
	@Test
	public void testArrays2D() {
		Object[][] obj = new Object[][]{new Object[]{1,2,3},new Object[]{1,2,3},new Object[]{1,2,3}}; 
		//converted
		Integer[][] got = ArrayUtils.cast(obj, Integer[][].class);
		
		assertTrue(got instanceof Integer[][]);
		assertTrue(got[0][0] instanceof Integer);
		assertTrue(obj != got);//diff object
	}
	
	@Test
	public void testArrays1DSameObj() {
		Object[] obj = new Integer[]{1,2,3};
		//converted
		Integer[] got = ArrayUtils.cast(obj, Integer[].class);

		assertTrue(got instanceof Integer[]);
		assertTrue(got[0] instanceof Integer);
		assertTrue(obj == got);//same object - as nothing done as already of desired type - no copy made!
		//since we are operating on a generic array this normally triggers a cast - which causes a copy sadly, however, the array is already of the desired type, so no casting is needed and so no copy is made
	}
	@Test
	public void testArraysleNull() {
		Integer[] got = ArrayUtils.cast(null, Integer[].class);
		
		assertTrue(got == null);
	}

}
