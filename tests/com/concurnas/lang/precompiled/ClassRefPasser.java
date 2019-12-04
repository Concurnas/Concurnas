package com.concurnas.lang.precompiled;

import com.concurnas.bootstrap.lang.Lambda.ClassRef;
import com.concurnas.bootstrap.lang.Lambda.Function1;

public class ClassRefPasser {

	public static ClassRef<? extends String> pass(ClassRef<? extends String> what){
		return what;
	}
	
	public static Function1<Integer, String> passALambda(Function1<Integer, String> what ){
		return what;
	}
	
	public static ClassRef<?> illegalclassref(ClassRef<? extends String> what){
		return what;
	}
	
	public static ClassRef<?> illegalclassref2(ClassRef<?> what){
		return what;
	}
	
	public static class MyClass{
		private MyClass(int arg){}

		public MyClass(String arg){}
	
	}
	
}
