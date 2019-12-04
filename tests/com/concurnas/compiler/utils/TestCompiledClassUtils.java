package com.concurnas.compiler.utils;

import java.util.ArrayList;

import junit.framework.TestCase;

import org.junit.Test;

import com.concurnas.compiler.utils.CompiledClassUtils;

public class TestCompiledClassUtils extends TestCase {

	@Test
	public void testTypes() {
		assertEquals("java.lang.String", CompiledClassUtils.ConvertCompiledClassToType(String.class).getPrettyName());
		assertEquals("java.lang.String[][]", CompiledClassUtils.ConvertCompiledClassToType(String[][].class).getPrettyName());
		assertEquals("boolean", CompiledClassUtils.ConvertCompiledClassToType(java.lang.Boolean.TYPE).getPrettyName());
		assertEquals("java.lang.Boolean", CompiledClassUtils.ConvertCompiledClassToType(Boolean.class).getPrettyName());
		assertEquals("java.util.ArrayList<E>", CompiledClassUtils.ConvertCompiledClassToType(ArrayList.class).getPrettyName());
		
		ArrayList<String> inst = new ArrayList<String>();
		
		assertEquals("java.util.ArrayList<E>", CompiledClassUtils.ConvertCompiledClassToType(inst.getClass()).getPrettyName());
		
	}

}
