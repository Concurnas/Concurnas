package com.concurnas.compiler.misc;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.junit.Test;

import com.concurnas.bootstrap.lang.Lambda.Function0;
import com.concurnas.bootstrap.runtime.ref.DirectlyAssignable;
import com.concurnas.bootstrap.runtime.ref.LocalArray;
import com.concurnas.bootstrap.runtime.ref.Ref;
import com.concurnas.lang.TypedActor;
import com.concurnas.runtime.InstanceofGeneric;
import com.concurnas.runtime.ref.Local;

public class InstanceofGenericTests {
	
	public static class MyHash<To> extends HashMap<Integer, To>{
		private static final long serialVersionUID = 1L;
	}
	
	public static class Top<X, Y, Z>{}
	public static class Middle<X, Z> extends Top<X, String, Z>{}
	public static class Lower<X> extends Middle<Integer, X>{}
	public static class FullLow extends Lower<Float>{}
	
	
	public static class N<A, Y, Z> {}
	public static class X<Y, Z> extends N<String, Y, Z>{}
	public static class F<Z> extends X<Z, String>{}
	public static class F2<Z> extends F<Z>{}
	public static class Z extends F2<Local<Integer>>{}
	
	public static class Fella<X, Z> {}
	public static class ThHC<X> extends Fella<HashMap<X, X>, String>{}
	public static class Quali extends ThHC<Local<Integer>>{}
	
	public static class Supla<A, B, C, D> {}
	public static class Child<X, Y> extends Supla<X, Y, String, X>{}
	
	public static interface MyInterF<A, B, C, D> {}
	public static class ImplIfaceSup<A1, A2, A3> implements MyInterF<A1, Integer, A2, A3>{}
	public static class ImplIface<X, Y> extends ImplIfaceSup<String, Y, X>{}
	
	public static class MyClass<Axx>{}
	
	public static class MyActor<Axx> extends TypedActor<MyClass<Axx>>{

		public MyActor(Class<?>[] type, Function0<MyClass<Axx>> creator) {
			super(type, creator);
		}}
	
	
	@Test
	public void testActors() {
		assertTrue(InstanceofGeneric.isGenericInstnaceof(new Class<?>[]{MyActor.class, String.class}, new Class<?>[]{TypedActor.class, MyClass.class, String.class}));
	}
	
	
	@Test
	public void testArrayRef() {
		assertTrue(InstanceofGeneric.isGenericInstnaceof(new Class<?>[]{LocalArray.class, Local.class, Integer.class}, new Class<?>[]{LocalArray.class, Ref.class, Integer.class}));
	}
	
	
	@Test
	public void testtest() {
		
		assertTrue(InstanceofGeneric.isGenericInstnaceof(new Class<?>[]{Local.class, Integer.class}, new Class<?>[]{Local.class, Object.class}));
		assertTrue(InstanceofGeneric.isGenericInstnaceof(new Class<?>[]{Local.class, Integer.class}, new Class<?>[]{Local.class, Integer.class}));
		assertTrue(InstanceofGeneric.isGenericInstnaceof(new Class<?>[]{HashMap.class, Integer.class, Local.class, String.class}, new Class<?>[]{HashMap.class, Integer.class, Local.class, String.class}));
		assertTrue(InstanceofGeneric.isGenericInstnaceof(new Class<?>[]{HashMap.class, Integer.class, Local.class, String.class}, new Class<?>[]{HashMap.class, Integer.class, Local.class, Object.class}));
		
		assertTrue(InstanceofGeneric.isGenericInstnaceof(new Class<?>[]{F2.class, Local.class, Integer.class}, new Class<?>[]{X.class, Local.class, Integer.class, String.class}));
		
		assertTrue(InstanceofGeneric.isGenericInstnaceof(new Class<?>[]{Integer.class}, new Class<?>[]{Integer.class}));
		assertFalse(InstanceofGeneric.isGenericInstnaceof(new Class<?>[]{Object.class}, new Class<?>[]{Integer.class}));
		assertTrue(InstanceofGeneric.isGenericInstnaceof(new Class<?>[]{Integer.class}, new Class<?>[]{Object.class}));
		assertTrue(InstanceofGeneric.isGenericInstnaceof(new Class<?>[]{Local.class, Integer.class}, new Class<?>[]{Local.class, Integer.class}));
		assertTrue(InstanceofGeneric.isGenericInstnaceof(new Class<?>[]{Local.class, Integer.class}, new Class<?>[]{Local.class, Object.class}));
		assertFalse(InstanceofGeneric.isGenericInstnaceof(new Class<?>[]{Local.class, Object.class}, new Class<?>[]{Local.class, Integer.class}));
		assertTrue(InstanceofGeneric.isGenericInstnaceof(new Class<?>[]{HashMap.class, Integer.class, String.class}, new Class<?>[]{HashMap.class, Integer.class, String.class}));
		assertFalse(InstanceofGeneric.isGenericInstnaceof(new Class<?>[]{HashMap.class, Integer.class, String.class}, new Class<?>[]{HashMap.class, Integer.class, Object.class}));
		
		assertTrue(InstanceofGeneric.isGenericInstnaceof(new Class<?>[]{HashMap.class, Integer.class, Local.class, String.class}, new Class<?>[]{HashMap.class, Integer.class, Local.class, String.class}));
		assertTrue(InstanceofGeneric.isGenericInstnaceof(new Class<?>[]{HashMap.class, Integer.class, Local.class, String.class}, new Class<?>[]{HashMap.class, Integer.class, Local.class, Object.class}));
		
		//next one nice and complex, not that the first thing can be a subtype. e.g. HashMap < Map, but later args are not premitted UNLESS they are children of refs
		assertTrue(InstanceofGeneric.isGenericInstnaceof(new Class<?>[]{HashMap.class, Integer.class, Local.class, String.class}, new Class<?>[]{Map.class, Integer.class, Local.class, Object.class}));
		//so here, int <! Number as generic param, but it's of course ok when as a ref [ cos that carries the type information on it and guarantees correct setting]
		assertFalse(InstanceofGeneric.isGenericInstnaceof(new Class<?>[]{HashMap.class, Integer.class, Local.class, String.class}, new Class<?>[]{Map.class, Number.class, Local.class, Object.class}));
		
		
		assertTrue(InstanceofGeneric.isGenericInstnaceof(new Class<?>[]{F.class, Local.class, Integer.class}, new Class<?>[]{N.class, String.class, Local.class, Integer.class, String.class}));
		assertTrue(InstanceofGeneric.isGenericInstnaceof(new Class<?>[]{F2.class, Local.class, Integer.class}, new Class<?>[]{X.class, Local.class, Integer.class, String.class}));
		assertFalse(InstanceofGeneric.isGenericInstnaceof(new Class<?>[]{F2.class, Local.class, Integer.class, String.class}, new Class<?>[]{X.class, Local.class, Integer.class, String.class}));
		
		assertFalse(InstanceofGeneric.isGenericInstnaceof(new Class<?>[]{F2.class, Local.class, String.class}, new Class<?>[]{X.class, Local.class, Integer.class, String.class}));
		
		assertTrue(InstanceofGeneric.isGenericInstnaceof(new Class<?>[]{Local.class, Local.class, String.class}, new Class<?>[]{Local.class, Local.class, String.class}));
		
		assertFalse(InstanceofGeneric.isGenericInstnaceof(new Class<?>[]{F.class, Local.class, String.class, Integer.class}, new Class<?>[]{N.class}));//runs off the end
		assertFalse(InstanceofGeneric.isGenericInstnaceof(new Class<?>[]{N.class}, new Class<?>[]{Local.class, String.class, Integer.class}));//runs off the end
		
		assertTrue(InstanceofGeneric.isGenericInstnaceof(new Class<?>[]{FullLow.class}, new Class<?>[]{Top.class, Integer.class, String.class, Float.class}));
		
		//no subtyping permitted for generic arguments
		assertFalse(InstanceofGeneric.isGenericInstnaceof(new Class<?>[]{HashMap.class, Integer.class, FullLow.class}, new Class<?>[]{HashMap.class, Integer.class, Top.class, Integer.class, String.class, Float.class}));
		
		
		//bit more complex
		//assertTrue(InstanceofGeneric.isGenericInstnaceof(new Class<?>[]{Child.class, Local.class, String.class, String.class }, new Class<?>[]{Supla.class, Local.class, String.class, String.class, String.class, Local.class, String.class}));
		
		//next test... - (i.e. splice in more than one space)
		//a case of G[G] extends Sup[HashSet[G]]...
		assertTrue(InstanceofGeneric.isGenericInstnaceof(new Class<?>[]{Quali.class}, new Class<?>[]{Fella.class, HashMap.class, Local.class, Integer.class, Local.class, Integer.class, String.class}));
		assertFalse(InstanceofGeneric.isGenericInstnaceof(new Class<?>[]{Quali.class}, new Class<?>[]{Fella.class, HashSet.class, Local.class, Integer.class, Local.class, Integer.class, String.class}));
		
		//middler
		assertTrue(InstanceofGeneric.isGenericInstnaceof(new Class<?>[]{ThHC.class, Local.class, String.class}, new Class<?>[]{Fella.class, HashMap.class, Local.class, String.class, Local.class, String.class, String.class}));
		assertTrue(InstanceofGeneric.isGenericInstnaceof(new Class<?>[]{Middle.class, HashMap.class, Local.class, String.class, Local.class, String.class, Local.class, Object.class}, new Class<?>[]{Middle.class, HashMap.class, Local.class, String.class, Local.class, String.class, Local.class, Object.class}));
		assertFalse(InstanceofGeneric.isGenericInstnaceof(new Class<?>[]{Middle.class, HashMap.class, Local.class, Local.class, String.class, Local.class, Object.class}, new Class<?>[]{Middle.class, HashMap.class, Local.class, String.class, Local.class, String.class, Local.class, Object.class}));
		
		//interfaces...
		assertFalse(InstanceofGeneric.isGenericInstnaceof(new Class<?>[]{ImplIface.class, Local.class, String.class, HashSet.class, Integer.class}, new Class<?>[]{ImplIface.class, String.class, Integer.class, Local.class, String.class, HashSet.class, Integer.class}));
		assertTrue(InstanceofGeneric.isGenericInstnaceof(new Class<?>[]{ImplIface.class, HashSet.class, Integer.class, Local.class, String.class}, new Class<?>[]{MyInterF.class, String.class, Integer.class, Local.class, String.class, HashSet.class, Integer.class}));
	}
	
	
	@Test
	public void testComplexInheritance2() {
		/*assertTrue(InstanceofGeneric.isGenericInstnaceof(new Class<?>[]{FullLow.class}, new Class<?>[]{Top.class, Integer.class, String.class, Float.class}));
		
		assertFalse(InstanceofGeneric.isGenericInstnaceof(new Class<?>[]{Top.class, Integer.class, String.class, Float.class}, new Class<?>[]{FullLow.class}));
		//i think the above should actually be true....? since the two are kind of equivilent...
		
		assertTrue(InstanceofGeneric.isGenericInstnaceof(new Class<?>[]{Lower.class, Float.class}, new Class<?>[]{Top.class, Integer.class, String.class, Float.class}));
		
		assertTrue(InstanceofGeneric.isGenericInstnaceof(new Class<?>[]{Local.class, Integer.class}, new Class<?>[]{Local.class, Integer.class}));
		assertTrue(InstanceofGeneric.isGenericInstnaceof(new Class<?>[]{Local.class, Integer.class}, new Class<?>[]{Local.class, Number.class}));
		
		assertFalse(InstanceofGeneric.isGenericInstnaceof(new Class<?>[]{Lower.class, Integer.class}, new Class<?>[]{Top.class, Integer.class, String.class, Float.class}));
		*/
		assertFalse(InstanceofGeneric.isGenericInstnaceof(new Class<?>[]{Lower.class, Integer.class}, new Class<?>[]{Top.class, Integer.class, String.class, Number.class}));
	}
	
	
	
	@Test
	public void testEnsureOneForOneMatch() {
		//refs must match one for one type wise. i.e. a int: = 9; a as Number: <- fail; a as int: <-ok. int as a number is also ok
		assertTrue(InstanceofGeneric.isGenericInstnaceof(new Class<?>[]{Local.class, Integer.class}, new Class<?>[]{Local.class, Number.class}));
		assertTrue(InstanceofGeneric.isGenericInstnaceof(new Class<?>[]{Local.class, Integer.class}, new Class<?>[]{Local.class, Integer.class}));
		assertFalse(InstanceofGeneric.isGenericInstnaceof(new Class<?>[]{Local.class, Number.class}, new Class<?>[]{Local.class, Integer.class}));
	}
	
	@Test
	public void testDowncastOk() {
		//num:: cannot goto int:
		assertFalse(InstanceofGeneric.isGenericInstnaceof(new Class<?>[]{Local.class, Number.class}, new Class<?>[]{Local.class, Local.class, Integer.class}));
		assertFalse(InstanceofGeneric.isGenericInstnaceof(new Class<?>[]{Local.class, Local.class, Number.class}, new Class<?>[]{Local.class, Integer.class}));
		assertFalse(InstanceofGeneric.isGenericInstnaceof(new Class<?>[]{Local.class, Integer.class}, new Class<?>[]{Local.class, Local.class, Number.class}));
		assertFalse(InstanceofGeneric.isGenericInstnaceof(new Class<?>[]{Local.class, Local.class, Integer.class}, new Class<?>[]{Local.class, Number.class}));
		assertTrue(InstanceofGeneric.isGenericInstnaceof(new Class<?>[]{Local.class, Local.class, Integer.class}, new Class<?>[]{Local.class, Local.class, Integer.class}));
	}
	
	@Test
	public void testEnsureOneForOneMatchNotViaRefs() {
		//refs must match one for one type wise. i.e. a int: = 9; a as Number: <- fail; a as int: <-ok
		assertTrue(InstanceofGeneric.isGenericInstnaceof(new Class<?>[]{ String.class}, new Class<?>[]{String.class}));
	}
	
	@Test
	public void testSimple() {
		assertTrue(InstanceofGeneric.isGenericInstnaceof(new Class<?>[]{Integer.class}, new Class<?>[]{Integer.class}));
		assertTrue(InstanceofGeneric.isGenericInstnaceof(new Class<?>[]{Local.class, Integer.class}, new Class<?>[]{Local.class, Integer.class}));
		assertFalse(InstanceofGeneric.isGenericInstnaceof(new Class<?>[]{Local.class}, new Class<?>[]{Integer.class}));
	}
	
	@Test
	public void testRefIsAssignableInsatnceOf() {//used to incorrectly not include the DirectlyAssignable iface indirectly implemented by Local
		assertTrue(InstanceofGeneric.isGenericInstnaceof(new Class<?>[]{Local.class, Integer.class}, new Class<?>[]{DirectlyAssignable.class, Object.class}));
	}
	
	@Test
	public void testComplexInheritance() {
		assertTrue(InstanceofGeneric.isGenericInstnaceof(new Class<?>[]{MyHash.class, String.class}, new Class<?>[]{HashMap.class, Integer.class, String.class}));
	}
	@Test
	public void testJustOneRef() {
		assertTrue(InstanceofGeneric.isGenericInstnaceof(new Class<?>[]{Local.class, Integer.class}, new Class<?>[]{Local.class, Integer.class}));
	}
	

	
	@Test
	public void testLittleMoreAdvanced() {
		assertTrue(InstanceofGeneric.isGenericInstnaceof(new Class<?>[]{Local.class, Integer.class}, new Class<?>[]{Local.class, Integer.class}));
		assertFalse(InstanceofGeneric.isGenericInstnaceof(new Class<?>[]{Local.class, Integer.class}, new Class<?>[]{Local.class, Float.class}));
	}
	
	@Test
	public void testSimpleMap() {
		assertTrue(InstanceofGeneric.isGenericInstnaceof(new Class<?>[]{HashMap.class, Integer.class, String.class}, new Class<?>[]{HashMap.class, Integer.class, String.class}));
	}
	

	@Test
	public void testObjRulesAll() {
		//object is the parent of all
		//assertTrue(InstanceofGeneric.isGenericInstnaceof(new Class<?>[]{HashMap.class, Integer.class, String.class}, new Class<?>[]{Object.class}));
		//the local ref parts must match up
		assertTrue(InstanceofGeneric.isGenericInstnaceof(new Class<?>[]{HashMap.class, Integer.class, Local.class, String.class}, new Class<?>[]{HashMap.class, Integer.class, Local.class, String.class}));
		//precise after the Local
		//assertFalse(InstanceofGeneric.isGenericInstnaceof(new Class<?>[]{HashMap.class, Integer.class, Local.class, String.class}, new Class<?>[]{HashMap.class, Integer.class, Local.class, Object.class}));
	}
		
	@Test
	public void testRefObjRulesAll() {
		assertTrue(InstanceofGeneric.isGenericInstnaceof(new Class<?>[]{Local.class, Integer.class}, new Class<?>[]{Local.class, Object.class}));
		assertTrue(InstanceofGeneric.isGenericInstnaceof(new Class<?>[]{Local.class, Integer.class}, new Class<?>[]{Local.class, Integer.class}));
		assertFalse(InstanceofGeneric.isGenericInstnaceof(new Class<?>[]{Local.class, HashMap.class, Integer.class, String.class}, new Class<?>[]{Local.class, Object.class}));
		//doesnt have to be be 1:1 match
		assertTrue(InstanceofGeneric.isGenericInstnaceof(new Class<?>[]{Local.class,Local.class, Integer.class}, new Class<?>[]{Local.class,Local.class, Object.class}));
	}
	
	
	@Test
	public void testComplexInheritance3() {
		//generic args must match 1:1
		assertFalse(InstanceofGeneric.isGenericInstnaceof(new Class<?>[]{HashMap.class, Integer.class, FullLow.class}, new Class<?>[]{HashMap.class, Integer.class, Top.class, Integer.class, String.class, Float.class}));
		assertFalse(InstanceofGeneric.isGenericInstnaceof(new Class<?>[]{HashMap.class, FullLow.class, Integer.class}, new Class<?>[]{HashMap.class, Top.class, Integer.class, String.class, Float.class, Integer.class}));
	}
	
}
