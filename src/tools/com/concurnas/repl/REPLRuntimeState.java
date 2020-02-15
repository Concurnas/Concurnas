package com.concurnas.repl;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

import com.concurnas.bootstrap.runtime.cps.Fiber;

public class REPLRuntimeState {
	//@Shared
	private static Map<Fiber, Map<String, Object>> fibToVarHolder = Collections.synchronizedMap(new WeakHashMap<Fiber, Map<String, Object>>());
	private static Map<String, Object> master = null;
	private static Fiber masterFiber = null;
	
	public static void reset() {
		fibToVarHolder = new ConcurrentHashMap<Fiber, Map<String, Object>>();
		master = null;
		masterFiber = null;
	}
	

	public static void resetMaster() {
		masterFiber = null;
	}
	
	private static Map<String, Object> getMappingForFiber(Fiber fib){
		if(fib == null) {
			return master;
		}else {
			if(masterFiber == null) {
				if(null == master) {
					master = new HashMap<String, Object>();
				}
				masterFiber = fib;
				return master;
			}else if(masterFiber == fib) {
				return master;
			}
			
			if(fibToVarHolder.containsKey(fib)) {
				return fibToVarHolder.get(fib);
			}else {
				//copy from master
				HashMap<String, Object> child = new HashMap<String, Object>(master);
				fibToVarHolder.put(fib, child);
				return child;
			}
		}
	}
	
	public static Object get(String varname) {
		return get(varname, Fiber.getCurrentFiber());
	}
	
	public static Object get(String varname, Fiber fib) {
		Map<String, Object> child = getMappingForFiber(fib);
		if(child.containsKey(varname)) {
			return child.get(varname);
		}
		
		return  master.get(varname);
	}
	
	public static boolean contains(String varname) {
		return contains(varname, Fiber.getCurrentFiber());
	}
	
	public static boolean contains(String varname, Fiber fib) {
		return master != null && master.containsKey(varname);
	}
	
	public static void put(String varname, Object value) {
		put(varname, value, Fiber.getCurrentFiber());
	}
	public static void put(String varname, Object value, Fiber fib) {
		Map<String, Object> child = getMappingForFiber(fib);
		child.put(varname, value);
		if(!master.containsKey(varname)) {
			master.put(varname, value);
		}
	}
	
	public static void remove(String varname) {
		if(master.containsKey(varname)) {
			master.remove(varname);
		}
	}
}
