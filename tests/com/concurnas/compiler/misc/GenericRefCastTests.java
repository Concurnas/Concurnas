package com.concurnas.compiler.misc;


import java.util.HashSet;

import junit.framework.TestCase;

import org.junit.Test;

import com.concurnas.bootstrap.runtime.ref.Ref;
import com.concurnas.compiler.bytecode.BytecodeTests;
import com.concurnas.lang.precompiled.RefHelper;
import com.concurnas.runtime.GenericRefCast;
import com.concurnas.runtime.ref.Local;

public class GenericRefCastTests extends TestCase{
	
	private static void assertNoGetCurFibExcept(Exception e){
		if(e.getMessage().equals("Fiber method: 'getCurrentFiber' must be invoked via concurnas")){
			fail("incorrect exception thrown, complains about fiber");
		}
	}
	
	public static void FromIntToObjFromConc() throws Throwable {
		Object asRef = RefHelper.getIntegerRefAsObjectNoSet();
		//GenericRefCast.genericRefCast(asRef, new Class<?>[]{Local.class, Object.class});//this should be ok
		//GenericRefCast.genericRefCast(asRef, new Class<?>[]{Local.class, Local.class, Object.class});//this also which is an upcast should be ok
		
		GenericRefCast.genericRefCast(asRef, new Class<?>[]{Local.class, Integer.class});//int -> number
		
		try{
			Object numRef = new Local<Number>(new Class<?>[]{Number.class});
			GenericRefCast.genericRefCast(numRef, new Class<?>[]{Local.class, Integer.class});//number -> int not ok
			fail();
		}
		catch(Exception e){
			assertNoGetCurFibExcept(e);
		}
		
		try{
			Object numRef = new Local<Number>(new Class<?>[]{Local.class, HashSet.class, Integer.class});//no match!
			GenericRefCast.genericRefCast(numRef, new Class<?>[]{Local.class, Local.class, Integer.class});//number -> int not ok
			fail();
		}
		catch(Exception e){
			assertNoGetCurFibExcept(e);
		}
		
		//but this is ok, cos match
		Object numRef = new Local<Number>(new Class<?>[]{Local.class, HashSet.class, Integer.class});//no match! HashSet[Integer]::
		GenericRefCast.genericRefCast(numRef, new Class<?>[]{Local.class,Local.class, HashSet.class, Integer.class});//finematch
		
	}
	

	public static void FromIntToObj2FromConc() throws Throwable {
		//number:: -> int: not ok
		try{
			Object numRef = new Local<Number>(new Class<?>[]{Local.class, Number.class});//num::
			GenericRefCast.genericRefCast(numRef, new Class<?>[]{Local.class, Integer.class});
			fail("should have failed");
		}
		catch(Exception e){
			assertNoGetCurFibExcept(e);
		}
	}
	
	
	public static void FromIntToObj3FromConc() throws Throwable {
		//number:: -> int: not ok
		try{
			Object numRef = new Local<Number>(new Class<?>[]{Integer.class});//num::
			GenericRefCast.genericRefCast(numRef, new Class<?>[]{Local.class, Local.class, Object.class});
		}
		catch(Exception e){
			assertNoGetCurFibExcept(e);
		}
	}
	
	public static void RefUpCastFromConc() throws Throwable {
		Object numRef = new Local<String>(new Class<?>[]{String.class});//Str::
		GenericRefCast.genericRefCast(numRef, new Class<?>[]{Local.class, Local.class, Object.class});
	}
	
	public static void RefUpCastFromConcWithRef() throws Throwable {
		//Str: -> Local Ref Obj
		Object numRef = new Local<String>(new Class<?>[]{String.class});//Str::
		GenericRefCast.genericRefCast(numRef, new Class<?>[]{Local.class, Ref.class, Object.class});//should be ok
	}
	
	
	@Test
	public void testFromIntToObj() throws Throwable {
		assertEquals("", BytecodeTests.microCompilation("import com.concurnas.compiler.misc.GenericRefCastTests as Gens\ndef doings() => Gens.FromIntToObjFromConc();  ''; \n"));
	
	}
	
	
	@Test
	public void testFromIntToObj2()  throws Throwable {
		assertEquals("", BytecodeTests.microCompilation("import com.concurnas.compiler.misc.GenericRefCastTests as Gens\ndef doings() => Gens.FromIntToObj2FromConc(); ''; \n"));
	}
	
	
	@Test
	public void testFromIntToObj3() throws Throwable {
		assertEquals("", BytecodeTests.microCompilation("import com.concurnas.compiler.misc.GenericRefCastTests as Gens\ndef doings() => Gens.FromIntToObj3FromConc(); ''; \n"));
	}
	
	@Test
	public void testRefUpCast() throws Throwable {
		assertEquals("", BytecodeTests.microCompilation("import com.concurnas.compiler.misc.GenericRefCastTests as Gens\ndef doings() => Gens.RefUpCastFromConc(); ''; \n"));
	}
	
	
	@Test
	public void testRefUpCastWithRef() throws Throwable {
		assertEquals("", BytecodeTests.microCompilation("import com.concurnas.compiler.misc.GenericRefCastTests as Gens\ndef doings() => Gens.RefUpCastFromConcWithRef(); ''; \n"));
	}
	
}
