package com.concurnas.runtime.tests;

import org.junit.Test;

import com.concurnas.runtime.ConcurnasClassLoader;
import com.concurnas.runtime.Concurnifier.ActorAugmentor;

import junit.framework.TestCase;

public class TestTypeDescForRefGennerator  extends TestCase{


	@Test
	public void testypeDescForRef() {
		ConcurnasClassLoader ccl = new ConcurnasClassLoader();
		super.assertEquals("[java/util/ArrayList, java/lang/Comparable, java/lang/Object]",
				""+ ActorAugmentor.typeDescForRef(ccl, "Ljava/util/ArrayList<Ljava/lang/Comparable<Ljava/lang/Object;>;>;", "()Ljava/util/ArrayList;"));
		
		super.assertEquals("[java/util/ArrayList, java/lang/Object]",
				""+ ActorAugmentor.typeDescForRef(ccl, "Ljava/util/ArrayList<Ljava/lang/Object;>;", "()Ljava/util/ArrayList;"));
		
		
		super.assertEquals("[[Ljava/lang/Class;, java/lang/Object]",
				""+ ActorAugmentor.typeDescForRef(ccl, "()[Ljava/lang/Class<*>;", "()[Ljava/lang/Class;"));
		
		super.assertEquals("[java/lang/Class, java/lang/Object]",
				""+ ActorAugmentor.typeDescForRef(ccl, "()Ljava/lang/Class<*>;", "()[Ljava/lang/Class;"));
		
	}
	

}
