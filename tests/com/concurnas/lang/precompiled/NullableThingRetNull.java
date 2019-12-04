package com.concurnas.lang.precompiled;

import java.util.ArrayList;

import com.concurnas.lang.NoNull;
import com.concurnas.lang.NoNull.When;

public class NullableThingRetNull {
	public String maybeNull = null;
	public ArrayList<String> arMybeNull = null;
	
	public static class TakesNull<@NoNull(when = When.NEVER) X>{
		public TakesNull(X x) {
			
		}
	}
	
	public static class ItsNoNull<@NoNull(when = When.ALWAYS ) X>{
		public ItsNoNull(X x) {
			
		}
	}
	
	
	public static <@NoNull(when = When.NEVER ) X> String TakesNullMeth(X x) {
		return "ok";
	}
	
	public static <@NoNull(when = When.ALWAYS ) X> String ItsNoNullMeth(X x) {
		return "ok";
	}
	
	public static String retNull() {
		return null;
	}
	
}

