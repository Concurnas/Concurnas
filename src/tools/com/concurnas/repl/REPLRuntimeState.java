package com.concurnas.repl;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.concurnas.lang.Shared;

public class REPLRuntimeState {
	@Shared
	public static Map<String, Object> vars = Collections.synchronizedMap(new HashMap<String, Object>());
	
	public static void reset() {
		vars = Collections.synchronizedMap(new HashMap<String, Object>());
	}
}
