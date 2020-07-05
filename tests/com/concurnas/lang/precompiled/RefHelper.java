package com.concurnas.lang.precompiled;


import com.concurnas.bootstrap.runtime.ref.LocalArray;
import com.concurnas.bootstrap.runtime.ref.Ref;
import com.concurnas.lang.NoNull;
import com.concurnas.runtime.ref.Local;
import com.concurnas.runtime.ref.RefArray;

public class RefHelper {

	public static String actonref(Local<?> acton){
		return acton.toString();//TODO: support for ? is needed in generics
	}
	public static String actonref2(Ref<Ref<?>> acton){
		return acton.toString();//TODO: support for ? is needed in generics
	}
	
	public static RefArray<Integer> getNullRefArray(){
		return null;
	}
	
	public static RefArray<Integer> getRealRefArray(int size) throws Throwable{
		
		RefArray<Integer> ret = new RefArray<Integer>(new Class<?>[]{Integer.class}, size);
		ret.put(0, 55);
		for(int n=1; n < size; n++){
			ret.put(n, 0);
		}
		
		return ret;
	}
	
	public static Local<?> getNullRef(){
		return null;//TODO: support for ? is needed in generics
	}
	
	@NoNull
	public static Local<Integer> getNullRef2(){ 
		return null;//TODO: support for ? is needed in generics
	}
	
	public static Local<Integer> getIntegerRef(int g) throws Throwable{
		Local<Integer> ret = new Local<Integer>(new Class<?>[]{Integer.class});
		ret.set(g);
		return ret;
	}
	
	public static Object getIntegerRefAsObject(int g) throws Throwable{
		Local<Integer> ret = new Local<Integer>(new Class<?>[]{Integer.class});
		ret.set(g);
		return ret;
	}
	
	public static Ref<Integer> getThingAsRef(int g) throws Throwable{
		Local<Integer> ret = new Local<Integer>(new Class<?>[]{Integer.class});
		ret.set(g);
		return ret;
	}
	
	public static Object getIntegerRefAsObjectNoSet(){
		Local<Integer> ret = new Local<Integer>(new Class<?>[]{Integer.class});
		return ret;
	}
	
	
	public static LocalArray<Local<Integer>> getRefArray() throws Throwable{
		Local<Integer> ref1 = new Local<Integer>(new Class<?>[]{Integer.class});
		ref1.set(44);
		
		Local<Integer>[] refArray = new Local[]{ref1};
		
		LocalArray<Local<Integer>> refArrayHolder = new LocalArray<Local<Integer>>(  new Class<?>[]{Local.class, Integer.class} );
		refArrayHolder.ar = refArray;
		
		return refArrayHolder;
	}
	
	public static LocalArray<Local<Integer>> getRefArrayNull(){
		return null;
	}
	
	public static LocalArray<Local<Integer>> getRefArrayNull2(){
		return new LocalArray<Local<Integer>>(  new Class<?>[]{Local.class, Integer.class} );
	}
	

	
}
