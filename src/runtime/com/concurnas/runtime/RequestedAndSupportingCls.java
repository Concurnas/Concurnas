package com.concurnas.runtime;

import java.util.ArrayList;
import java.util.HashSet;

import com.concurnas.compiler.utils.Thruple;

public class RequestedAndSupportingCls {

	public Thruple<Boolean, byte[], HashSet<String>> requested;
	public ArrayList<Pair<String, Thruple< Boolean, byte[], HashSet<String>>>> supporting;
	
}
