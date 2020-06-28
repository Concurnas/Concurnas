package com.concurnas.runtime.ref;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import com.concurnas.bootstrap.lang.Stringifier;
import com.concurnas.bootstrap.runtime.cps.Fiber;
import com.concurnas.bootstrap.runtime.ref.DirectlyArrayAssignable;
import com.concurnas.bootstrap.runtime.ref.DirectlyArrayGettable;
import com.concurnas.bootstrap.runtime.transactions.Transaction;
import com.concurnas.runtime.InstanceofGeneric;
import com.concurnas.runtime.Pair;
import com.concurnas.runtime.bootstrapCloner.Cloner;

public class RefArray<X> extends AbstractRef<X> implements DirectlyArrayAssignable<X>, DirectlyArrayGettable<X> {

	private volatile Object[] value;
	private volatile boolean[] isSet;
	private volatile int size;
	protected volatile HashMap<Integer, HashSet<Fiber>> waiters = new HashMap<Integer, HashSet<Fiber>>(); 
	
	public RefArray(Class<?>[] type, int size){//fix this
		super(type);
		this.size = size;
		this.value = (Object[]) Array.newInstance(type[0], size);
		this.isSet = new boolean[size];
		for(int nn=0; nn < size; nn++){
			this.isSet[nn] = false;
			//waiters.put(nn, new HashSet<Fiber>());
		}
	}
	
	public RefArray(Class<?>[] type){
		super(type);//differ size to initial call of set
	}
			
	
/*	public RefArray(Class<?>[] type, X[] x){//fix this
		super(type);
		this.size = null==x?0:x.length;
		this.value = (Object[]) Array.newInstance(type[0], this.size);
		this.isSet = new boolean[this.size];
		
		if(x != null){
			System.arraycopy(x, 0, this.value, 0, this.size);
			
			for(int nn=0; nn < this.size; nn++){
				this.isSet[nn] = true;
			}
			
		}
	}*/
	
	@Override
	public synchronized void set(X[] x) throws Throwable {
		int size = x==null?0:x.length;
		
		if(this.value == null){
			this.size = null==x?0:x.length;
			this.value = (Object[]) Array.newInstance(type[0], this.size);
			this.isSet = new boolean[this.size];
			
			if(x != null){
				/*System.arraycopy(x, 0, this.value, 0, this.size);
				
				for(int nn=0; nn < this.size; nn++){
					this.isSet[nn] = true;
				}*/
				
				for(int n=0; n < size; n++){
					this.put(n, x[n]);
				}
				
			}
		}else{
			if(size != this.size){
				throw new RuntimeException(String.format("Expected array of size: %s but was given size: %s", this.size, size));
			}
			
			for(int n=0; n < size; n++){
				this.put(n, x[n]);
			}
		}

		//super.incrementVersionId();
		super.isSet=true;//any set all set?
	}
	
	
	public int getSize(){
		return this.size;
	}
	
	@Override
	public boolean equals(Object obj){
		if(obj == null){
			return false;
		}
		
		if( obj instanceof RefArray){
			RefArray<?> objAsLocal = (RefArray<?>)obj;
			//Class<?>[] decTypes, Class<?> isInstanceOf
			try {
				return InstanceofGeneric.isGenericInstnaceof(objAsLocal.type, this.type) && this.get().equals(objAsLocal.get());
			} catch (Throwable e) {
				throw new RuntimeException(e);
			}
		}
		return false;
	}
	
	@Override
	public void finalize(){
		this.close();
	}
	
	
	
	private class RefArrayState extends AbstractRef.State{
		protected volatile LinkedHashMap<Integer, X> xOverride;
		
		public RefArrayState cloneSetX(Integer space, X x){
			RefArrayState ret = new RefArrayState();
			ret.xOverride = xOverride;
			ret.xOverride.put(space, x);
			return ret;
		}
		
		
		private boolean xEQ(Object[] compTo){
			Set<Integer> keys = this.xOverride.keySet();
			for(int n=0; n < compTo.length; n++){
				if(this.xOverride.containsKey(n)){
					if(this.xOverride.get(n) != compTo){
						return false;
					}
					keys.remove(n);
				}
			}
			
			if(!keys.isEmpty()){//shuldnt be any outside bounds...
				return false;
			}
			
			return true;
		}
		
		@Override
		public AbstractRef.State copy() {
			RefArrayState ret = new RefArrayState();
			ret.xOverride = (LinkedHashMap<Integer, X>)this.xOverride.clone();
			ret.exception = this.exception;
			ret.isclosed = this.isclosed;
			ret.nonClosedVersionx = this.nonClosedVersionx;
			
			return ret;
		}
		
		@Override
		public boolean stateEQ(AbstractRef comp){
			if(comp instanceof RefArray){
				RefArray asState = (RefArray)comp;
				
				return xEQ(asState.value) && super.stateEQ(comp);
			}
			return false;
			//lat.initial.x == this.x && lat.initial.isclosed == this.isclosed && lat.initial.exception == this.exception
			//return this.isclosed == comp.isclosed && this.exception == comp.exception;
		}
	}

	@Override
	public synchronized void onNotify(Transaction trans) {
		//called by fiber for which the notification queue applies
		//increments notificationi queues
		Fiber fib = Fiber.getCurrentFiber();
		//System.err.println("onNotify called for fiber: " + fib + " top value: " + this.x + " exceot: " + this.exception);
		RefArrayState ras = (RefArrayState)trans.getStateForRef(this);
		synchronized(getCache){
			getCache.put(fib, ras);
		}
		FiberStateQueue<X> fsq = fiberStateQuese.get(fib);
		if(null != fsq){
			HashMap<Integer, X> xOverride= ras.xOverride;
			if(null != xOverride){
				for(Integer k : xOverride.keySet()){
					fsq.remove(k, xOverride.get(k));
				}
			}
		}
	}
	
	@Override
	public String toString() {
		return Stringifier.stringify(this.value);
	}
	
	//so we can maintain transactionality across whole q...
	private HashMap<Fiber, FiberStateQueue<X>> fiberStateQuese = new HashMap<Fiber, FiberStateQueue<X>>();
	
	private static class FiberStateQueue<X>{
		
		private volatile HashMap<Integer, ArrayList<Pair<X, X>>> posToPending = new HashMap<Integer, ArrayList<Pair<X, X>>>();
		
		public void add(int pos, X old, X newa){
			ArrayList<Pair<X, X>> qq = posToPending.get(pos);
			if(qq == null){
				qq = new ArrayList<Pair<X, X>>();
				posToPending.put(pos, qq);
			}

			//System.err.println("add:  " + pos + " old: " + old + " newa: " + newa);
			qq.add(new Pair<X, X>(old, newa));
		}
		
		public void remove(int pos, X newa){
			//should be the first element
			ArrayList<Pair<X, X>> qq = posToPending.get(pos);
			
			//System.err.println("pos:  " + pos + " newa: " + newa);
			
			if(null != qq){
				
				while(!qq.isEmpty()){
					Pair<X, X> item = qq.remove(0);
					if(item.getB() == newa){
						if(qq.isEmpty()){
							posToPending.remove(pos);
						}
						return;
					}
					//pop off the head of the list until we get to the item in question
				}
				
				posToPending.remove(pos);
			}
		}
		
		public boolean hasPrevSTateIfIsOne(int pos){
			return null != posToPending.get(pos);
		}
		
		public X getPrevSTateIfIsOne(int pos){
			//only call if hasPrevSTateIfIsOnereturns true
			ArrayList<Pair<X, X>> qq = posToPending.get(pos);
			return qq.get(0).getA();
		}
		
	}
	
	@Override
	protected void onUnregister( ){
		Fiber currentFiber = Fiber.getCurrentFiber();//gets rewritten
		fiberStateQuese.remove(currentFiber);
	}
	
	@Override
	protected void onRegister( ){
		Fiber currentFiber = Fiber.getCurrentFiber();//gets rewritten
		fiberStateQuese.put(currentFiber, new FiberStateQueue<X>());
	}
	

	public synchronized void put(int i, X x) throws Throwable {
		if(x!=null){InstanceofGeneric.assertGenericInstnaceof(x, this.type);}
		put(i, x, true);
	}
	
	private synchronized void put(int i, X x, boolean doNotify){
		if(this.isclosed){//latest state
			throw new RuntimeException("Cannot set ref value as it is closed");
		}

		x = Cloner.cloner.clone(x);
		
		Fiber currentFiber = Fiber.getCurrentFiber();
		
		synchronized(getCache){//ensure that setter sees the result of its own change
			if(getCache.containsKey(currentFiber)){
				RefArrayState prev = (RefArrayState)getCache.get(currentFiber);
				if(null != prev){
					prev = prev.cloneSetX(i, x);
				}
				else{
					prev = (RefArrayState) currentState();
				}
				
				getCache.put(currentFiber, prev);
			}
		}
		//i guess this could be refactored even more...

		super.incrementVersionId();
		
		if(currentFiber.inTransaction()){//in transaction, we 'lock' in the value obtained for duration of innermost referant transaction
			TransStateInitialAndLatest tsiandLat = attributeTransStateManager();
			((RefArrayState)tsiandLat.latest).xOverride.put(i, x);
			tsiandLat.latest.versionx = onChange();
		}
		else{
			
			if(!fiberStateQuese.isEmpty()){
				for(Fiber notis : fiberStateQuese.keySet()){
					FiberStateQueue<X> fsq = fiberStateQuese.get(notis);
					fsq.add(i, (X)this.value[i], x);
				}
			}
			
			this.value[i] = x;
			isSet[i]=true;//first one, means set
			super.isSet=true;//any set all set?

			if(doNotify){
				RefArrayState state = new RefArrayState();
				state.isclosed=this.isclosed;
				state.exception=this.exception;
				state.xOverride=new LinkedHashMap<Integer, X>();
				state.xOverride.put(i, x);
				notifyAllReg(state);
			}
			
			wakeup(i);
		}
	}
	
	protected void wakeup(int i){
		synchronized(waiters){//i think this is susceptable to concuurent modificaiotn exception, cos we can add waiters outside the lock...
			HashSet<Fiber> towaiton =  waiters.get(i);
			if(towaiton != null){
				for(Fiber f :towaiton){//notify-all
					//System.err.println("wakeup cos is set: " + i + " :" + isSet[i]);
					Fiber.wakeup(f, this);
				}
			}
		}
	}

	@Override
	public X get(int i, boolean withNoWait) throws Throwable {
		if(withNoWait){
			return getNoWait(i); 
		}else{
			return get(i);
		}
	}
	@Override
	public X get(int i) throws Throwable  {
		Fiber currentFiber = Fiber.getCurrentFiber();//gets rewritten to fiber argument on command line
		if(!isSet[i]){
			while(!isSet[i]){
				//System.err.println("is set: " + i + " :" + isSet[i]);
				synchronized(waiters){
					HashSet<Fiber> oneswaiting = waiters.get(i);
					if(null == oneswaiting){
						oneswaiting = new HashSet<Fiber>();
						waiters.put(i, oneswaiting);
					}
					oneswaiting.add(currentFiber);
				}
				Fiber.pause();//go away come back later
			}
			synchronized(waiters){
				waiters.remove(i);//only called once
			}
		}
		
		//unless in notify list
		//X ret = Cloner.cloner.clone(getNoWait());
		X ret = getNoWait(i);//TODO: this should be cloned out...
		
		return ret;
	}
	
	@Override
	public synchronized X getNoWait(int i) throws Throwable{
		Fiber currentFiber = Fiber.getCurrentFiber();
		//System.err.println("get no wait: " + i);
		RefArrayState thestate;
		synchronized(getCache){//ensure that setter sees the result of its own change
			thestate = (RefArrayState)getCache.get(currentFiber);
		}
		
		if(null == thestate){
			if(currentFiber.inTransaction()){//in transaction, we 'lock' in the value obtained for duration of innermost referant transaction
				TransStateInitialAndLatest initalAndLatest = getFromTransStateManager();
				if(null != initalAndLatest){
					State lat = initalAndLatest.latest;
					if(lat.exception !=null){
						throw  lat.exception;
					}
					
					if(((RefArrayState)lat).xOverride.containsKey(i)){
						return ((RefArrayState)lat).xOverride.get(i);
					}//onyl if it has it
				}
			}
			
			if(exception !=null){
				throw exception;
			}
			
			FiberStateQueue<X> fsq = this.fiberStateQuese.get(currentFiber);
			if(fsq != null){
				if(fsq.hasPrevSTateIfIsOne(i)){
					return fsq.getPrevSTateIfIsOne(i);
				}
			}

			//System.err.println("get: " + i + " x: " + this.value[1]);
			return (X)this.value[i];
		}
		
		if(thestate.exception !=null){
			throw thestate.exception;
		}
		
		if(thestate.xOverride.containsKey(i)){
			//System.err.println("state: " + thestate.xOverride);
			return (thestate).xOverride.get(i);
		}//onyl if it has it
		
		FiberStateQueue<X> fsq = this.fiberStateQuese.get(currentFiber);
		if(fsq != null){
			if(fsq.hasPrevSTateIfIsOne(i)){
				return fsq.getPrevSTateIfIsOne(i);
			}
		}
		//System.err.println("get no state: " + i + " x: " + this.value[1]);
		return (X)this.value[i];
	}
	
	/**
	 * for using within onchange/every/await
	 * lists the indices of all items changed by this transaction and any that
	 * have been changed within the onchange/every/await block
	 * @return returns an empty list if there are no changes
	 */
	public synchronized List<Integer> modified(){
		//in order, consistnatly returned this way
		Fiber currentFiber = Fiber.getCurrentFiber();
		//System.err.println("get no wait: " + i);
		RefArrayState thestate;
		synchronized(getCache){//ensure that setter sees the result of its own change
			thestate = (RefArrayState)getCache.get(currentFiber);
			if(null == thestate || null == thestate.xOverride){
				return new ArrayList<Integer>();
			}
			else{
				Set<Entry<Integer, X>> entries = thestate.xOverride.entrySet();
				ArrayList<Integer> ret = new ArrayList<Integer>(entries.size());
				for(Entry<Integer, X> ee : entries){
					ret.add(ee.getKey());
				}
				return ret;
			}
		}
	}
	
	@Override
	public X[] get(boolean withNoWait) throws Throwable {
		if(withNoWait){
			return getNoWait();
		}else{
			return getReallyNoWait();
		}
	}
	
	private X[] getReallyNoWait() throws Throwable {
		X[] ret = (X[])Array.newInstance(this.type[0], this.size);
		
		for(int n = 0; n < ret.length; n++){
			ret[n] = get(n);
		}
		
		return ret;
	}
	
	@Override
	public X[] get() throws Throwable {//if missing a value -i.e. not set, return null in its place
		return getNoWait();//TODO: this seems like strange functionality, call getReallyNoWait?
	}

	@Override
	public X[] getNoWait() throws Throwable {
		X[] ret = (X[])Array.newInstance(this.type[0], this.size);
		
		for(int n = 0; n < ret.length; n++){
			ret[n] = getNoWait(n);
		}
		
		return ret;
	}
	
	
	@Override
	protected State currentState(){
		RefArrayState ret = new RefArrayState();
		ret.isclosed=this.isclosed;
		ret.exception=this.exception;
		ret.xOverride=new LinkedHashMap<Integer, X>();
		return ret;
	}

	@Override
	public void waitUntilSet() throws Throwable {
		int sz = this.size;
		for(int n = 0; n < sz; n++){
			get(n);
		}//until all set...
	}

	@Override
	protected void setOnUnlock(){
		HashMap<Integer, X> xOverride = ((RefArrayState)stateToSetOnUnlock).xOverride;
		for(Integer k : xOverride.keySet()){
			put(k, xOverride.get(k), false);
		}
	}
}
