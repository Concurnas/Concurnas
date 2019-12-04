package com.concurnas.bootstrap.runtime.transactions;

import java.util.Arrays;
import java.util.Collection;

import com.concurnas.bootstrap.lang.util.LinkedIdentityHashMap;
import com.concurnas.bootstrap.runtime.ref.LocalArray;
import com.concurnas.bootstrap.runtime.ref.Ref;

public class LocalTransaction implements Transaction {
	private final LinkedIdentityHashMap<Ref<?>, Object> localTostate;
	
	public LocalTransaction(){
		this.localTostate = new LinkedIdentityHashMap<Ref<?>, Object>();
	}
	
	public LocalTransaction(Collection<Ref<?>> refs){
		this();
		for(Ref<?> ref : refs){
			ref.addCurrentStateToTransaction(this);
		}
	}
	
	public LocalTransaction(LinkedIdentityHashMap<Ref<?>, Object> localTostate){
		this.localTostate = localTostate;
	}
	
	public LocalTransaction(Ref<?> ref, Object state){
		this();
		this.localTostate.put(ref, state);
	}
	
	
	@Override
	public Object getStateForRef(Ref<?> ref) {
		return localTostate.get(ref);
	}

	@Override
	public LocalArray<Ref<?>> getChanged() {
		LocalArray<Ref<?>> refArrayHolder = new LocalArray<Ref<?>>(  new Class<?>[]{null} );
		refArrayHolder.ar = localTostate.toArray(new Ref<?>[1]);
				
		return refArrayHolder;
	}

	@Override
	public void onNotify() {
		for(Ref<?> ref : localTostate.keySet()){
			ref.onNotify(this);
		}
	}

	@Override
	public void setStateForRef(Ref<?> ref, Object state) {
		this.localTostate.put(ref, state);
	}
}
