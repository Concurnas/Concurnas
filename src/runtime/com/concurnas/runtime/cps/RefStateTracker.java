package com.concurnas.runtime.cps;

import java.util.WeakHashMap;

import com.concurnas.bootstrap.runtime.ref.Ref;

public class RefStateTracker {

	private WeakHashMap<Ref<?>, Long> reftoNoncloseVersionId = new WeakHashMap<Ref<?>, Long>();
	
	public boolean shouldProcess(Ref<?> maybeClosed) {
		long vid = maybeClosed.getNonChangeVersionId();
		if(!reftoNoncloseVersionId.containsKey(maybeClosed)) {
			if(vid == 0l) {//indicates that the ref has status set to close with no writes
				return false;
			}else {
				reftoNoncloseVersionId.put(maybeClosed, vid);
				return true;
			}
		}else {
			long prev = reftoNoncloseVersionId.get(maybeClosed);
			if(prev == vid) {//its been closed, no change, also do some tidy up
				reftoNoncloseVersionId.remove(maybeClosed);
				return false;
			}else {
				reftoNoncloseVersionId.put(maybeClosed, vid);
				return true;
			}
		}
	}
	
}
