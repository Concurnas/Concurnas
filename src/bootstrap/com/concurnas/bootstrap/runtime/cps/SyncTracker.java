package com.concurnas.bootstrap.runtime.cps;

import java.util.ArrayList;

import com.concurnas.bootstrap.runtime.ref.Ref;

public class SyncTracker {

	private ArrayList<Ref<Boolean>> toAwait = new ArrayList<Ref<Boolean>>();
	
	public void awaitAll() throws Throwable {
		for(Ref<Boolean> loc : toAwait){
			loc.waitUntilSet();
		}
	}

	public void awaitFor(Ref<Boolean> onDone) {
		toAwait.add(onDone);
	}
}
