package com.concurnas.runtime.ref;


import com.concurnas.bootstrap.lang.Stringifier;
import com.concurnas.bootstrap.runtime.ReifiedType;
import com.concurnas.bootstrap.runtime.cps.Fiber;
import com.concurnas.bootstrap.runtime.ref.DefaultRef;
import com.concurnas.runtime.InstanceofGeneric;
import com.concurnas.runtime.bootstrapCloner.Cloner;

/**
 * get function maps to last value unless acting within an iso that is being notified upon in which case value is fixed
 */
public  class Local<X> extends AbstractRef<X> implements DefaultRef<X>, ReifiedType {
	private volatile X x;
	
	private class LocalState extends AbstractRef.State{
		protected volatile X x;
		
		public LocalState cloneSetX(X x){
			LocalState ret = new LocalState();
			ret.x=x;
			ret.exception = super.exception;
			ret.isclosed = super.isclosed;
			ret.nonClosedVersionx = super.nonClosedVersionx;
			return ret;
		}
		
		@Override
		public AbstractRef.State copy() {
			LocalState ret = new LocalState();
			ret.x = this.x;
			ret.exception = this.exception;
			ret.isclosed = this.isclosed;
			ret.nonClosedVersionx = this.nonClosedVersionx;
			
			return ret;
		}
		
		@Override
		public boolean stateEQ(AbstractRef comp){
			if(comp instanceof Local){
				Local asState = (Local)comp;
				return this.x == asState.x && super.stateEQ(comp);
			}
			return false;
			//lat.initial.x == this.x && lat.initial.isclosed == this.isclosed && lat.initial.exception == this.exception
			//return this.isclosed == comp.isclosed && this.exception == comp.exception;
		}
	}
	
	public Local(Class<?>[] type){
		super(type);
		//System.err.println(String.format("%s create local: %s", System.identityHashCode(this), this.type[0]));
	}
	
	
	
	@Override
	protected State currentState(){
		LocalState ret = new LocalState();
		ret.isclosed=this.isclosed;
		ret.x=x;
		ret.exception=this.exception;
		return ret;
	}
	
	private synchronized void set(X x, boolean doNotify) throws Exception{
		if(this.isclosed){//latest state
			throw new Exception("Cannot set ref value as it is closed");
		}
		

		Fiber currentFiber = Fiber.getCurrentFiberWithCreate();
		
		/*System.err.println(String.format("%s set something: %s cf: %s", System.identityHashCode(this), x.getClass(), System.identityHashCode(currentFiber)));
		if(x.getClass() == Integer.class){
			System.err.println(String.format("%s set resolves to: %s cf: %s", System.identityHashCode(this), x, System.identityHashCode(currentFiber)));
		}*/
		
				
		synchronized(getCache){//ensure that setter sees the result of its own change
			synchronized(prevCache){
				setPrevState();
			}
			if(getCache.containsKey(currentFiber)){
				State got = getCache.get(currentFiber);
				if(got == null || got instanceof Local.LocalState) {
					LocalState prev = (LocalState)got;
					if(null != prev){
						prev = prev.cloneSetX(x);
					}
					else{
						prev = (LocalState) currentState();
					}
					getCache.put(currentFiber, prev);
				}
				
				
			}
		}

		super.incrementVersionId();
		if( currentFiber.inTransaction()){//in transaction, we 'lock' in the value obtained for duration of innermost referant transaction
			TransStateInitialAndLatest tsiandLat = attributeTransStateManager();
			((LocalState)tsiandLat.latest).x = x;
			tsiandLat.latest.versionx = onChange();

			//System.err.println(String.format("%s TRANS trigger normal wakup on set of: %s on : %s", System.identityHashCode(this), System.identityHashCode(x), this.type[0]));
		}
		else{
			this.x = x;
			
			isSet=true;

			if(doNotify){
				notifyAllReg();
			}
			//System.err.println(String.format("%s trigger normal wakup on set of: %s  on : %s", System.identityHashCode(this), System.identityHashCode(x), this.type[0]));
			wakeup();
		}
	}
	
	@Override
	public void set(X x) throws Throwable{
		//System.err.println(String.format("%s call set: %s of: %s", System.identityHashCode(this), this.type[0], x));
		//System.err.println("set: " + (x==null?"null":x.getClass().getName()) +" on: " + System.identityHashCode(this) + " item address: " + System.identityHashCode(x));
		if(x != null) {
			//if(x.getClass() == Integer.class) {
			//	System.err.println(String.format("set: %s -> %s onto: %s", x.getClass().getName(), x, System.identityHashCode(this)));
			//}
		}
		
		if(x!=null){InstanceofGeneric.assertGenericInstnaceof(x, this.type);}//TODO: does checking this on every call slow things down?
		set(Cloner.cloner.clone(x), true);//why clone as we go in?
	}
	
	
	
	@Override
	public synchronized void waitUntilSet() throws Throwable{
		get();
	}
		
	@Override
	public  X get() throws Throwable  {
		if(!isSet){
			Fiber currentFiber = Fiber.getCurrentFiberWithCreate();//gets rewritten to fiber argument on command line
			while(!isSet){
				synchronized(waiters){
					//System.out.println(String.format("%s ADD waiter, fiber: %s for type: %s", System.identityHashCode(this), System.identityHashCode(currentFiber), this.type[0]));
					//(new Exception("")).printStackTrace(System.out);
					waiters.add(currentFiber);
				}
				Fiber.pause();//go away come back later
			}
			synchronized(waiters){
				waiters.remove(currentFiber);//only called once
			}
		}

		//Fiber.clearPausing();
		//unless in notify list
		X ret = Cloner.cloner.clone(getNoWait());
		//X ret = getNoWait();//TODO: this should be cloned out...
		//System.err.println(String.format("%s WOKE UP waiter, fiber: %s for type: %s", System.identityHashCode(this), System.identityHashCode(currentFiber), this.type[0]));
		
		return ret;
	}
	
	@Override
	public X get(boolean withNoWait) throws Throwable  {
		return withNoWait?getNoWait():get();
	}
	
	@Override
	protected void setOnUnlock() throws Throwable{
		set(((LocalState)stateToSetOnUnlock).x, false);
	}
	
/*	@Override
	protected synchronized void onRegister( ){
		Fiber currentFiber = Fiber.getCurrentFiber();
		getCache.put(currentFiber, (LocalState) currentState());
	}*/
	
	@Override
	public synchronized X getNoWait() throws Throwable{
		Fiber currentFiber = Fiber.getCurrentFiberWithCreate();
		
		State thestate;
		synchronized(getCache){//ensure that setter sees the result of its own change
			thestate = getCache.get(currentFiber);
		}
		
		if(null == thestate){
			if(currentFiber.inTransaction()){//in transaction, we 'lock' in the value obtained for duration of innermost referant transaction
				TransStateInitialAndLatest initalAndLatest = getFromTransStateManager();
				if(null != initalAndLatest){
					State lat = initalAndLatest.latest;
					if(lat.exception !=null){
						throw lat.exception;
					}
					
					return ((LocalState)lat).x;
				}
			}
			
			if(exception !=null){
				throw exception;
			}
			return x;
		}
		
		if(thestate.exception !=null){
			throw thestate.exception;
		}
		X ret = ((LocalState)thestate).x;
		//System.err.println("loc state: " + ret);
		return ret;
	}
	
	@Override
	public String toString(){
		try {
			return Stringifier.stringify(get())+ ":";
		} catch (Throwable e) {
			throw new RuntimeException(e);
		}
	}
	
	
	@Override
	public X getPrevious(){
		Fiber currentFiber = Fiber.getCurrentFiberWithCreate();
		synchronized(prevCache){
			return ((LocalState)prevCache.get(currentFiber)).x;//could be null
		}
	}
	
	//remove below...
	@Override
	public synchronized X last() throws Throwable{
		return get();
	}
	
	@Override
	public synchronized X lastNoWait() throws Throwable{
		return getNoWait();
	}
	
	@Override
	public X last(boolean withNoWait) throws Throwable{
		return get(withNoWait);
	}
}
