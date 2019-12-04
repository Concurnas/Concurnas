package com.concurnas.repl;

import java.util.concurrent.ConcurrentHashMap;

import com.concurnas.lang.Shared;

public class REPLRuntimeState {
	@Shared
	public static ConcurrentHashMap<String, Object> vars = new ConcurrentHashMap<String, Object>();
	
}
