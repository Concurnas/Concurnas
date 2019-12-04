package com.concurnas.compiler.misc;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;

import org.junit.Test;

import com.concurnas.bootstrap.runtime.cps.Fiber;
import com.concurnas.bootstrap.runtime.ref.DefaultRef;
import com.concurnas.bootstrap.runtime.ref.Ref;
import com.concurnas.bootstrap.runtime.transactions.ChangesAndNotifies;
import com.concurnas.bootstrap.runtime.transactions.Transaction;
import com.concurnas.bootstrap.runtime.transactions.TransactionHandler;
import com.concurnas.compiler.ast.ClassDef;
import com.concurnas.compiler.ast.ClassDefJava;
import com.concurnas.compiler.ast.NamedType;
import com.concurnas.compiler.ast.NamedTypeMany;
import com.concurnas.compiler.ast.Type;
import com.concurnas.compiler.visitors.lca.ClassDefTree;
import com.concurnas.compiler.visitors.lca.LowestCommonAncestor;
import com.concurnas.runtime.ref.Local;

public class LowestCommonAncestorTests extends TestCase{

	private void genTestTH(Class<?> typ, String expect)
	{
		ClassDefTree tr = new ClassDefTree(new NamedType(new ClassDefJava(typ)));
		
		String sb = "";
		
		for(NamedType cls: tr.getTypeHierarchy())
		{
			sb += cls.getPrettyName() +", ";
		}
		
		assertEquals(expect, sb);
	}
	
	
	private NamedType LCACompaire(String expected, Class<?>... classes) {
		List<Type> toComp = new ArrayList<Type>();
		
        for (Class<?> n : classes) {
        	toComp.add(new NamedType(new ClassDefJava(n)));
        }
        
        NamedType ty = (NamedType)LowestCommonAncestor.getLCA(toComp, false);
		ClassDef d = ty.getSetClassDef();
		assertEquals(expected, d.getPrettyName());
		return ty;
    }
	
	private void  LCACompaire(String expected, String expectedMulti, Class<?>... classes) {
		String sb;
		NamedType ifNT = LCACompaire(expected, classes);
		if(ifNT instanceof NamedTypeMany){
			sb = "";
			NamedTypeMany dd = (NamedTypeMany)ifNT;
			for(NamedType cls : dd.getMany())
			{
				sb += cls.getPrettyName();
				sb += ", ";
			}
			
			sb = sb.substring(0, sb.length()-2);
		}
		else{
			sb = ifNT.getSetClassDef().getPrettyName();
		}

		assertEquals(expectedMulti, sb) ;	
	}
	
		
	class B{ }
	
	class A extends B implements I{ }
	
	class C extends B implements L, Y{}
	
	interface I{ }
	
	interface L extends I{	}
	
	interface Y extends I{}
	
	class X implements L{}

	private class Super<X>{}
	
	private class Kid1 extends Super<String>{}
	
	private class KidMatch extends Super<String>{}
	private class KidNoMatch extends Super<Integer>{}
	
	private class KidMatchSelf extends Super<KidMatchSelf>{}
	private class KidNoMatchSelf extends Super<KidNoMatchSelf>{}

	private interface SupInter<X>{}
	
	private class AClass implements SupInter<Integer>{}
	private class BClass implements SupInter<Integer>{}
	private class CClass implements SupInter<String>{}
	
	private class AClassSelfI implements SupInter<AClassSelfI>{}
	private class BClassSelfI implements SupInter<BClassSelfI>{}
	
	
	public void testComparableInterfaceTypeFixed1L()
	{
		LCACompaire("com.concurnas.compiler.misc.LowestCommonAncestorTests$SupInter", "com.concurnas.compiler.misc.LowestCommonAncestorTests$SupInter<java.lang.Integer>", AClass.class, BClass.class); //match
		LCACompaire("com.concurnas.compiler.misc.LowestCommonAncestorTests$SupInter", "com.concurnas.compiler.misc.LowestCommonAncestorTests$SupInter<? java.lang.Object>", AClass.class, CClass.class); //no match
		LCACompaire("com.concurnas.compiler.misc.LowestCommonAncestorTests$SupInter", "com.concurnas.compiler.misc.LowestCommonAncestorTests$SupInter<? java.lang.Object>", AClassSelfI.class, BClassSelfI.class); //no match
	}
	

	private class OfLocal extends Local<String>{
		public OfLocal(Class<?>[] type) {
			super(type);
		}}
	private class OfRef implements Ref<String>{

		@Override
		public boolean isSet() {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public void waitUntilSet() {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void close() {
			// TODO Auto-generated method stub
			
		}

		@Override
		public boolean isClosed() {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public void setException(Throwable e) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public boolean hasException() {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public boolean register() {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public void unregister() {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void onNotify(Transaction trans) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public DefaultRef<Integer> getListnerCount() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public void removeTransaction(TransactionHandler trans, boolean s) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void unlockAndSet() {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void addCurrentStateToTransaction(Transaction trans) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public Class<?>[] getType() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public ChangesAndNotifies lock(TransactionHandler trans, Transaction tt) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public long getNonChangeVersionId() {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public boolean register(Fiber toNotify, int a) {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public void unregister(Fiber toNotify, int a) {
			// TODO Auto-generated method stub
			
		}

	}
	
	public void testLocalExpansion()
	{
		LCACompaire("com.concurnas.bootstrap.runtime.ref.Ref", "java.lang.String:com.concurnas.bootstrap.runtime.ref.Ref, com.concurnas.bootstrap.runtime.ReifiedType", OfLocal.class, OfRef.class); //no match
	}
	
	@Test
	public void testComparableSuperTypeFixed1L() throws NoSuchMethodException, SecurityException, ClassNotFoundException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException
	{
		LCACompaire("com.concurnas.compiler.misc.LowestCommonAncestorTests$Super", "com.concurnas.compiler.misc.LowestCommonAncestorTests$Super<java.lang.String>", Kid1.class, KidMatch.class);
		LCACompaire("com.concurnas.compiler.misc.LowestCommonAncestorTests$Super", "com.concurnas.compiler.misc.LowestCommonAncestorTests$Super<? java.lang.Object>", Kid1.class, KidNoMatch.class); //no match
		LCACompaire("com.concurnas.compiler.misc.LowestCommonAncestorTests$Super", "com.concurnas.compiler.misc.LowestCommonAncestorTests$Super<? com.concurnas.compiler.misc.LowestCommonAncestorTests$Super>", KidMatchSelf.class, KidNoMatchSelf.class); //no match
	}
	
	
	//type and class relative
	//test kid[X] sup[x]
	


	
	@Test
	public void testTreeHierarchy() {
		//JPT: no idea what these tests do and frankly dont care anymore
		genTestTH(String.class, "java.lang.String, java.lang.Object, java.lang.CharSequence, java.lang.Comparable<java.lang.String>, java.io.Serializable, ");
		//genTestTH(String[].class, "java.lang.String[], java.io.Serializable[], java.lang.Comparable[], java.lang.CharSequence[], ");
		//genTestTH(Object[].class, "java.lang.Object[], ");
		//genTestTH(TestCase.class, "junit.framework.TestCase, junit.framework.Assert, junit.framework.Test, ");
		//genTestTH(HashMap.class, "java.util.HashMap, java.util.AbstractMap, java.util.Map, java.lang.Cloneable, java.io.Serializable, ");
		//genTestTH(C.class, "com.concurnas.compiler.misc.LowestCommonAncestorTests$C, com.concurnas.compiler.misc.LowestCommonAncestorTests$B, com.concurnas.compiler.misc.LowestCommonAncestorTests$L, com.concurnas.compiler.misc.LowestCommonAncestorTests$Y, com.concurnas.compiler.misc.LowestCommonAncestorTests$I, ");
		//genTestTH(C[].class, "com.concurnas.compiler.misc.LowestCommonAncestorTests$C[], com.concurnas.compiler.misc.LowestCommonAncestorTests$B[], com.concurnas.compiler.misc.LowestCommonAncestorTests$L[], com.concurnas.compiler.misc.LowestCommonAncestorTests$Y[], com.concurnas.compiler.misc.LowestCommonAncestorTests$I[], ");
	}
	@Test
	public void testLCA()
	{
		LCACompaire("com.concurnas.compiler.misc.LowestCommonAncestorTests$B", "com.concurnas.compiler.misc.LowestCommonAncestorTests$B, com.concurnas.compiler.misc.LowestCommonAncestorTests$I", A.class, C.class);
	}
	
	@Test
	public void testLCABoxed()
	{
		LCACompaire("java.lang.Number","java.lang.Number, java.lang.Comparable<? java.lang.Object>", Integer.class, Long.class);
	}
	
	@Test
	public void testLCAArray()
	{
		LCACompaire("java.lang.Object", Object.class, Object[].class); //just one result and that's just object
	}

	@Test
	public void testLCACLash()
	{
		LCACompaire("java.lang.Object", String[].class, Integer.class, Map.class); //just one result and that's just object
	}
	
	@Test
	public void testLCANoMatch()
	{//revert to our default
		NamedType d = LCACompaire("java.lang.Object", B.class, X.class);
		assertFalse(d instanceof NamedTypeMany);
	}
}
