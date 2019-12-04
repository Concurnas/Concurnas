package com.concurnas.lang.precompiled;

public class CompiledSAMFunc {

	public static interface MySAM{
		public abstract int doer(int a); 
	}
	
	public static class MyClass{
		private int value;
		public MyClass(int value) {
			this.value = value;
		}
		
		public int apply(MySAM thing) {
			return thing.doer(value);
		}
		
		public int applytoSelf() {
			return this.apply(a -> a + 100);
		}
	}
	
	public static int runInstanceStatic(int ona) {
		MyClass mc = new MyClass(ona);
		return mc.apply(a -> a + 100);
	}
}
