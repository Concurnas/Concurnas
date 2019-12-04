package com.concurnas.lang.precompiled;

import java.io.Closeable;
import java.io.IOException;
import java.io.UncheckedIOException;

public class TestInvokeDynamic {

	 private static Runnable asUncheckedRunnable(Closeable c) {
        return () -> {
            try {
                c.close();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        };
    }
	 
	 public static class MyClass implements Closeable{

		@Override
		public void close() throws IOException {
			
		}
		 
	 }
	 

@FunctionalInterface
public static interface IntPredicate {
    boolean check(int i);
}
public static class IntPredicatesChecker {
    // A static method for checking if  a number is positive
    public static boolean isPositive(int n) {
        return n > 0;
    }
    // A static method for checking if a number is even
    public static boolean isEven(int n) {
        return (n % 2) == 0;
    }
}
	 
	 public static void runIt() {
		 IntPredicate thing = IntPredicatesChecker::isEven;
		 
		 MyClass mc = new MyClass();
		 Runnable rn = asUncheckedRunnable(mc);
		 rn.run();
	 }
	
}


//HERE?