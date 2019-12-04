package com.concurnas.bootstrap.runtime.transactions;

import com.concurnas.bootstrap.runtime.ref.LocalArray;
import com.concurnas.bootstrap.runtime.ref.Ref;

public interface Transaction {
	public void setStateForRef(Ref<?> ref, Object state);
	public Object getStateForRef(Ref<?> ref);
	public LocalArray<Ref<?>> getChanged();
	public void onNotify();
}
