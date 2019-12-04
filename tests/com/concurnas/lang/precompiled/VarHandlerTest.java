package com.concurnas.lang.precompiled;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;

public class VarHandlerTest {
    public volatile int value;
    
	public static String doIt() throws NoSuchFieldException, IllegalAccessException {
		VarHandle.storeStoreFence();

        MethodHandles.Lookup l = MethodHandles.lookup();
        VarHandle  VALUE = l.findVarHandle(VarHandlerTest.class, "value", int.class);
		
        VarHandlerTest vht = new VarHandlerTest();
        
        int somting = (int) VALUE.getAndSet(vht, 16);
        
		return "ok: " + somting;
	}
	
	public static void main(String[] args) throws NoSuchFieldException, IllegalAccessException {
		System.err.println(doIt());
	}
	
	/*
	 * public final int getAndSet(boolean newValue) { return
	 * (int)VALUE.getAndSet(this, (newValue ? 1 : 0)); }
	 */
	
}
