package com.concurnas.bootstrap.runtime.ref;

import com.concurnas.bootstrap.runtime.ReifiedType;
import com.concurnas.bootstrap.runtime.cps.Fiber;
import com.concurnas.bootstrap.runtime.transactions.ChangesAndNotifies;
import com.concurnas.bootstrap.runtime.transactions.Transaction;
import com.concurnas.bootstrap.runtime.transactions.TransactionHandler;

//minimal requirements for support of transactioins etc
public interface Ref<X> extends ReifiedType  {
	/**
	 * @return returns if a value for the ref has already been assigned
	 */
	public boolean isSet();//set in some shape or form
	public void waitUntilSet() throws Throwable;
	
	public void close();
	public boolean isClosed();
	
	public void setException(Throwable e);
	public boolean hasException();
	
	public boolean register() throws Throwable;
	public void unregister() throws Throwable;
	
	public boolean register(Fiber toNotify, int dummy) throws Throwable;//bit ugly that we need to place a dummy arg to permit fiberization
	public void unregister(Fiber toNotify, int dummy) throws Throwable;
	
	public void onNotify(Transaction trans);
	public DefaultRef<Integer> getListnerCount() throws Throwable;
	
	/* transactional functionality */
	public void removeTransaction(TransactionHandler trans, boolean sucsess);
	/**
	 *	returns Tuple<Boolean, HashSet<Fiber>>, A - indicates if any changes were recorded in ref, B - indicates notifees
	 */
	public ChangesAndNotifies lock(TransactionHandler trans, Transaction tt);
	public void unlockAndSet( ) throws Throwable;
	public void addCurrentStateToTransaction(Transaction trans);

	public long getNonChangeVersionId();
}
