package com.concurnas.bootstrap.runtime;

import java.util.IdentityHashMap;
import java.util.Map;

import com.concurnas.bootstrap.runtime.cps.CObject;

public final class CopyTracker extends CObject{
	public Map<Object, Object> clonedAlready = new IdentityHashMap<Object, Object>(16);
}
