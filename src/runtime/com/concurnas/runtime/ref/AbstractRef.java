package com.concurnas.runtime.ref;

import java.util.HashSet;
import java.util.WeakHashMap;

import com.concurnas.bootstrap.lang.offheap.Decoder;
import com.concurnas.bootstrap.lang.offheap.Encoder;
import com.concurnas.bootstrap.runtime.cps.CObject;
import com.concurnas.bootstrap.runtime.cps.Fiber;
import com.concurnas.bootstrap.runtime.ref.DefaultRef;
import com.concurnas.bootstrap.runtime.ref.Ref;
import com.concurnas.bootstrap.runtime.transactions.ChangesAndNotifies;
import com.concurnas.bootstrap.runtime.transactions.LocalTransaction;
import com.concurnas.bootstrap.runtime.transactions.Transaction;
import com.concurnas.bootstrap.runtime.transactions.TransactionHandler;


public abstract class AbstractRef<X> extends CObject implements Ref<X> {

	public Class<?>[] type;//TOOD: make private
	//util...
	protected transient Local<Integer> listnerCount = null;
	protected transient HashSet<Fiber> toNotifyOnChange = new HashSet<Fiber>();
	
	protected transient WeakHashMap<Fiber, State> getCache = new  WeakHashMap<Fiber, State>();
	protected transient WeakHashMap<Fiber, State> prevCache = new  WeakHashMap<Fiber, State>();
	
	protected boolean isSet=false;//no need to be volitile since only accessed via sync block
	
	protected transient WeakHashMap<TransactionHandler, TransStateInitialAndLatest> transToTaggedValue = new WeakHashMap<TransactionHandler, TransStateInitialAndLatest>();

	protected transient HashSet<Fiber> waiters = new HashSet<Fiber>(); 

	protected volatile boolean isclosed = false;
	protected volatile Throwable exception=null;
	private volatile long version=0l;
	private volatile long nonClosedVersion=0l;//for tracking changes to the object that dont affect its closed status (i.e. real material changes)

	public synchronized long getNonChangeVersionId() {
		Fiber currentFiber = Fiber.getCurrentFiber();
		State thestate;
		synchronized(getCache){//ensure that setter sees the result of its own change
			thestate = getCache.get(currentFiber);
			if(null != thestate){
				return thestate.nonClosedVersionx;
			}
		}
		
		return this.nonClosedVersion;
	}
	
	protected synchronized void incrementVersionId() {
		++this.nonClosedVersion;
	}
	
	public void toBinary(Encoder offheap){
		offheap.putObjectArray(type, 1);
		offheap.put(isSet);
		offheap.put(isclosed);
		offheap.put(exception);
		offheap.put(version);
		offheap.put(nonClosedVersion);
	}
	
	public void fromBinary(Decoder offheap){
		this.type = (Class<?>[])offheap.getObjectArray(1);
		this.isSet = offheap.getBoolean();
		this.isclosed = offheap.getBoolean();
		this.exception = (Throwable)offheap.getObject();
		this.version = offheap.getLong();
		this.nonClosedVersion = offheap.getLong();
		
		toNotifyOnChange = new HashSet<Fiber>();
		getCache = new  WeakHashMap<Fiber, State>();
		prevCache = new  WeakHashMap<Fiber, State>();
		transToTaggedValue = new WeakHashMap<TransactionHandler, TransStateInitialAndLatest>();
		waiters = new HashSet<Fiber>(); 
	}
	
	
	public AbstractRef(Class<?>[] type){
		Class<?>[] ret=  new Class<?>[type.length];
		System.arraycopy(type, 0, ret, 0, type.length);
		this.type = ret;
	}
	
	@Override
	public final Class<?>[] getType() {
		//type itself is not immutable
		Class<?>[] ret=  new Class<?>[type.length];
		System.arraycopy(type, 0, ret, 0, type.length);
		return ret;
	}
	
	protected long onChange(){
		return ++this.version;
	}
	
	
	protected class State{
		protected volatile Throwable exception;
		protected volatile boolean isclosed;
		protected volatile long versionx;
		protected volatile long nonClosedVersionx;
		
		protected State(){
			this.versionx = version;
			this.nonClosedVersionx = nonClosedVersion;
		}
		
		public State cloneSetException(Throwable exception){
			State ret = new State();
			ret.exception=exception;
			ret.isclosed=isclosed;
			ret.nonClosedVersionx=nonClosedVersion;
			return ret;
		}
		
		public State cloneSetClosed(boolean closed){
			State ret = new State();
			ret.isclosed=closed;
			ret.exception=exception;
			ret.nonClosedVersionx=nonClosedVersion;
			return ret;
		}
		
		public boolean pntEq(State asState){
			return this.versionx == asState.versionx && this.nonClosedVersionx == asState.nonClosedVersionx;//asState.isclosed == this.isclosed && asState.exception == this.exception;
		}

		public State copy() {
			State ret = new State();
			ret.exception = this.exception;
			ret.isclosed = this.isclosed;
			ret.nonClosedVersionx=this.nonClosedVersionx;
			
			return ret;
		}
		
		public boolean stateEQ(AbstractRef comp){
			//lat.initial.x == this.x && lat.initial.isclosed == this.isclosed && lat.initial.exception == this.exception
			return this.isclosed == comp.isclosed && this.exception == comp.exception;
		}
	}
	
	
	@Override
	public void close(){
		close(true);
	}
	
	protected abstract State currentState();//{
/*		State ret = new State();
		ret.isclosed=this.isclosed;
		ret.exception=this.exception;
		return ret;
	}*/
	
	protected void setPrevState(){
		Fiber fib = Fiber.getCurrentFiber();
		State prev = getCache.get(fib);
		if(null == prev){
			prev = this.currentState();
		}
		prevCache.put(fib, prev);
	}
	
	protected synchronized void notiClosed(){
		notifyAllReg();
	}
	
	//this code sucks...
	protected synchronized void notifyAllReg(){
		notifyAllReg(null);
	}
	protected synchronized void notifyAllReg(State state){
		try {
			synchronized(toNotifyOnChange){//TODO: round robin notifications?
				if(!toNotifyOnChange.isEmpty()){
					for(Fiber theFib : toNotifyOnChange){
						LocalTransaction trans = new LocalTransaction(this, state==null?currentState():state);		
						Fiber.notif(theFib, trans, false);
					}
				}
			}
			
		}catch(Exception e) {
			e.printStackTrace();
		}
		
	}
	
	protected class TransStateInitialAndLatest{
		
		public TransStateInitialAndLatest(State both){
			this.initial = both;
			this.latest = both.copy();
		}
		
		public volatile State initial;
		public volatile State latest;
		
	}
	
	protected final TransStateInitialAndLatest attributeTransStateManager(){
		Fiber currentFiber = Fiber.getCurrentFiber();
		
		TransactionHandler topTransLayer = currentFiber.transactions.getLast();
		TransStateInitialAndLatest tsiandLat = transToTaggedValue.get(topTransLayer);
		
		if(null == tsiandLat){
			tsiandLat = new TransStateInitialAndLatest(currentState());
			transToTaggedValue.put(topTransLayer, tsiandLat);
			topTransLayer.trackRef(this);
		}
		
		return tsiandLat;
	}
	
	protected final TransStateInitialAndLatest getFromTransStateManager(){
		Fiber currentFiber = Fiber.getCurrentFiber();
		
		TransactionHandler firstOne = currentFiber.transactions.peek();
		if(transToTaggedValue.containsKey(firstOne)){
			return transToTaggedValue.get(firstOne);//take from top transaction if already locked - 90% of the time
		}
		
		for(int n = transToTaggedValue.size()-2; n >=0; n--){
			TransactionHandler candidate = currentFiber.transactions.peek();
			if(transToTaggedValue.containsKey(candidate)){
				return transToTaggedValue.get(candidate);//take from parent locking transaction
			}
			
		}
		//tag most inner transaction with value
		transToTaggedValue.put(firstOne, new TransStateInitialAndLatest(currentState()) );
		firstOne.trackRef(this);
		return null;
		
	}
	
	protected void onUnregister(){}
	protected void onRegister(){}
	
	@Override
	public boolean register(){
		return register(Fiber.getCurrentFiber(), 1);//gets rewritten
	}
	
	@Override
	public void unregister(){
		unregister(Fiber.getCurrentFiber(), 1 );//gets rewritten
	}
	

	@Override
	public boolean register(Fiber toNotify, int a) {
		synchronized(toNotifyOnChange){
			toNotifyOnChange.add(toNotify);
			onRegister();
			if(null != listnerCount){
				listnerCount.set(listnerCount.get()+1);
			}
		}
		
		return isSet;
	}

	@Override
	public void unregister(Fiber toNotify, int a) {
		synchronized(toNotifyOnChange){
			toNotifyOnChange.remove(toNotify);
			onUnregister();
			if(null != listnerCount){
				listnerCount.set(listnerCount.get()-1);
			}
		}
	}
	
	
	protected void close(boolean doNotify){
		if(!isclosed){//only set once
			Fiber currentFiber = Fiber.getCurrentFiber();
			synchronized(getCache){//ensure that setter sees the result of its own change
				synchronized(prevCache){
					setPrevState();
				}
				if(getCache.containsKey(currentFiber)){
					State prev = getCache.get(currentFiber);
					if(null != prev){
						prev = prev.cloneSetClosed(true);
					}
					else{
						prev = (State) currentState();
					}
					
					getCache.put(currentFiber, prev);
				}
			}
			
			if(currentFiber.inTransaction()){//in transaction, we 'lock' in the value obtained for duration of innermost referant transaction
				TransStateInitialAndLatest tsiandLat = attributeTransStateManager();
				tsiandLat.latest.isclosed = true;
				tsiandLat.latest.versionx = onChange();
			}
			else{
				this.isclosed = true;
				
				if(doNotify){
					notiClosed();
				}
			}
		}
	}
	
	protected void wakeup(){
		synchronized(waiters){//i think this is susceptable to concuurent modificaiotn exception, cos we can add waiters outside the lock...
			for(Fiber f : waiters){//notify-all
				/*if(!waiters.isEmpty()){
					System.err.println(String.format("%s do wakeup on: %s", System.identityHashCode(this), System.identityHashCode(f)));
				}
				*/
				Fiber.wakeup(f, this);
			}
		}
	}

	@Override
	public void setException(Throwable e){
		setException(e, true);
	}
	
	protected void setException(Throwable theE, boolean doNotify){
		Fiber currentFiber = Fiber.getCurrentFiber();//this can be null if we are setting this outside of a fiber
		
		synchronized(getCache){//ensure that setter sees the result of its own change
			synchronized(prevCache){
				setPrevState();
			}
			if(getCache.containsKey(currentFiber)){
				State prev = getCache.get(currentFiber);
				if(null != prev){
					prev = prev.cloneSetException(theE);
				}
				else{
					prev = (State) currentState();
				}
				
				getCache.put(currentFiber, prev);
			}
		}

		this.incrementVersionId();
		if(null != currentFiber && currentFiber.inTransaction()){//in transaction, we 'lock' in the value obtained for duration of innermost referant transaction
			TransStateInitialAndLatest tsiandLat = attributeTransStateManager();
			tsiandLat.latest.exception = theE;
			tsiandLat.latest.isclosed = true;
			tsiandLat.latest.versionx = onChange();
		}
		else{
			this.exception = theE;
			onChange();
			if(doNotify){
				this.isSet=true;
				this.notiExcep();

				this.isclosed = true;
				notiClosed();
				
				wakeup();
			}
			
			//wakeup(); - should wakeup on close?
		}
		
	}
	
	private synchronized void notiExcep(){
		notifyAllReg();
	}
	
	
	
	
	public synchronized boolean isSet(){
		return isSet;
	}

	@Override
	public abstract void waitUntilSet();

	@Override
	public boolean isClosed(){
		Fiber currentFiber = Fiber.getCurrentFiber();//gets rewritten
		
		State thestate;
		synchronized(getCache){//ensure that setter sees the result of its own change
			thestate = getCache.get(currentFiber);
		}
		
		if(null == thestate){
			return this.isclosed;
		}
		else{
			if(currentFiber.inTransaction()){//in transaction, we 'lock' in the value obtained for duration of innermost referant transaction
				TransStateInitialAndLatest initalAndLatest = getFromTransStateManager();
				if(null != initalAndLatest){
					return initalAndLatest.latest.isclosed;
				}
			}
		}
		
		return thestate.isclosed;
	}

	
	@Override
	public synchronized  boolean hasException(){
		Fiber currentFiber = Fiber.getCurrentFiber();
		
		State thestate;
		synchronized(getCache){//ensure that setter sees the result of its own change
			thestate = getCache.get(currentFiber);
		}
		
		if(null == thestate){
			if(currentFiber.inTransaction()){//in transaction, we 'lock' in the value obtained for duration of innermost referant transaction
				TransStateInitialAndLatest initalAndLatest = getFromTransStateManager();
				if(null != initalAndLatest){
					return initalAndLatest.latest.exception != null;
				}
			}
			
			return this.exception != null;
		}
		
		return thestate.exception != null;
		
	}

	@Override
	public synchronized void onNotify(Transaction trans) {
		//called by fiber for which the notification queue applies
		//increments notificationi queues
		Fiber fib = Fiber.getCurrentFiber();
		//System.err.println("onNotify called for fiber: " + fib + " top value:  exceot: " + this.exception);
		synchronized(getCache){
			State pre = getCache.get(fib);
			synchronized(prevCache){
				prevCache.put(fib, pre);
			}
			
			getCache.put(fib, (State)trans.getStateForRef(this));
		}
	}

	@Override
	public DefaultRef<Integer> getListnerCount() {
		//not everyone cares about this, so only create upon demand
		if(null == listnerCount){
			synchronized(toNotifyOnChange){
				if(null == listnerCount){//hmm
					listnerCount =  new Local<Integer>(new Class<?>[]{Integer.class});
					listnerCount.set(toNotifyOnChange.size());
				}
			}
		}
		return listnerCount;
	}
	
	protected synchronized void onTransactionEnd(boolean sucsess){
		
	}

	@Override
	public synchronized void removeTransaction(TransactionHandler trans, boolean sucsess){
		transToTaggedValue.remove(trans);
		if(isTransLocked == trans){
			isTransLocked=null;
		}
		onTransactionEnd(sucsess);
	}
	
	
	@Override
	public synchronized void addCurrentStateToTransaction(Transaction trans) {
		trans.setStateForRef(this,  this.currentState());
	}
	
	@Override
	public void finalize(){
		this.close();
	}

	protected State stateToSetOnUnlock;
	protected TransactionHandler isTransLocked = null;
	
	/*
	 * returns true: if can lock and operating on latest
	 * TODO: hide this method and others
	 */
	@Override
	public synchronized ChangesAndNotifies lock(TransactionHandler transHandler, Transaction trans){
		TransStateInitialAndLatest lat = this.transToTaggedValue.get(transHandler);
		//System.err.println("enter lock: " + x);
		if(null == lat){
			throw new RuntimeException("Attempting to lock on unknown transaction");
		}
		
		if(lat.initial.pntEq(lat.latest)){
			//i think you dont need to have the ref on things which are not being changed...
			//no changes to write
			return new ChangesAndNotifies(false, null);
		}
		else{
			if(null==isTransLocked && lat.initial.stateEQ(this)){
				isTransLocked=transHandler;
				
				trans.setStateForRef(this, lat.latest);
				stateToSetOnUnlock=lat.latest;
				
				return new ChangesAndNotifies(true, toNotifyOnChange);
			}
			return null;//cannot aquire lock
		}
	}

	protected abstract void setOnUnlock();
	
	@Override
	public synchronized void unlockAndSet( ){
		isTransLocked=null;
		
		setOnUnlock();
		
		if(stateToSetOnUnlock.isclosed || stateToSetOnUnlock.exception != null){
			close(false);
		}
		
		if(stateToSetOnUnlock.exception != null){
			setException(stateToSetOnUnlock.exception, false);
		}
		
		stateToSetOnUnlock=null;
	}
}
