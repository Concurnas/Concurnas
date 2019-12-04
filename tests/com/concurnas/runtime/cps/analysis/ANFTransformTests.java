package com.concurnas.runtime.cps.analysis;

import java.util.List;

import junit.framework.TestCase;

import org.junit.Test;

public class ANFTransformTests extends TestCase{

	@Test
	public void testgetPrimOrObj() {
		assertEquals("[I, D, I]", ""+ANFTransform.getPrimOrObj("(IDI)V"));
		assertEquals("[I, D, I, L, D]", ""+ANFTransform.getPrimOrObj("(IDILString;D)V"));
		assertEquals("[I, I, D, L, L, L, D, D]", ""+ANFTransform.getPrimOrObj("(IIDLString;[D[LString;DD)LString;"));
		assertEquals("[L, L, L, D, D]", ""+ANFTransform.getPrimOrObj("(LD;LD;LD;DD)"));//a class called D?
		//ANFTransform.getPrimOrObj("([I[I[Ljava/lang/String;)I");
	}
}
