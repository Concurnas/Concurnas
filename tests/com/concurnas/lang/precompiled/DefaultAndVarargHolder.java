package com.concurnas.lang.precompiled;

import com.concurnas.lang.ParamName;
import com.concurnas.runtime.DefaultParamUncreatable;

public class DefaultAndVarargHolder {

	public static String exe(@ParamName(name="a")String a, 
			@ParamName(name="b") int[] b, 
			@ParamName(name="c", hasDefaultValue=true) int[] c, 
			@ParamName(name="d", isVararg=true) String[] d) {
		return String.format("exe: %s %s %s %s", a, b[0], c, d.length);
	}
	
	public static String exe(@ParamName(name="a")String a, 
			@ParamName(name="b") int[] b, 
			@ParamName(name="c") int[] c, 
			@ParamName(name="c$defaultNull") DefaultParamUncreatable uncr,
			@ParamName(name="d") String[] d) {
		return exe(a, b, null, d);
	}
	
}
