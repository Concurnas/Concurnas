package com.concurnas.lang.precompiled;

import com.concurnas.bootstrap.runtime.ref.LocalArray;
import com.concurnas.runtime.ref.Local;

public class Returner {

	public static LocalArray<Local<?>> getMeALocalArray() throws Throwable{
		Local<Integer> lll = new Local<Integer>(new Class[]{Integer.class});
		lll.set(99);
		
		LocalArray<Local<?>> refArrayHolder = new LocalArray<Local<?>>(  new Class<?>[]{Local.class, Integer.class} );
		refArrayHolder.ar = new Local<?>[]{lll};
		
		return refArrayHolder;
	}
	
	public static LocalArray< LocalArray<Local<?>>> getMeALocalArrayL2() throws Throwable{
		LocalArray<Local<?>> l1 = getMeALocalArray();
		
		LocalArray< LocalArray<Local<?>>> ret = new LocalArray< LocalArray<Local<?>>>(new Class[]{LocalArray.class, Local.class, Integer.class});
		ret.ar = new LocalArray[]{l1};
		
		return ret;
	}
	
	public static Local<Local<Integer>> getLocalL2() throws Throwable{
		Local<Local<Integer>> ret = new Local<Local<Integer>>(new Class<?>[]{Local.class, Integer.class});
		Local<Integer> retl2 = new Local<Integer>(new Class<?>[]{Integer.class});
		retl2.set(99);
		ret.set(retl2);
		
		return ret;
	}
	
}
