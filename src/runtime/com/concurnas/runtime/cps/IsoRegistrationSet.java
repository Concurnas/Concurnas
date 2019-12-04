package com.concurnas.runtime.cps;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

import com.concurnas.bootstrap.runtime.cps.Fiber;
import com.concurnas.bootstrap.runtime.ref.LocalArray;
import com.concurnas.bootstrap.runtime.ref.Ref;
import com.concurnas.bootstrap.runtime.transactions.LocalTransaction;
import com.concurnas.bootstrap.runtime.transactions.Transaction;
import com.concurnas.lang.Shared;

@Shared
public class IsoRegistrationSet {
	//mast registration set used internally to an Iso

	private Set<ReferenceSet> children = new HashSet<ReferenceSet>();
	//TODO: since using HashMap, we better hope that hashcode has been implemented correctly... for the ref (Local or otherwise). - figure out the implications of this...
	private IdentityHashMap<Ref<?>, Ref<?>> refs = new IdentityHashMap<Ref<?>, Ref<?>>();//hashset calls hashcode on fiber, which results in the value being obtained
	private IdentityHashMap<Ref<?>, Set<ReferenceSet>> childsubscriptions = new IdentityHashMap<Ref<?>, Set<ReferenceSet>>();//hashset calls hashcode on fiber, which results in the value being obtained
	private int childRefCount = 0;
	
	private Fiber toNotify;
	private boolean forEvery;
	private boolean forOnlyCloseds;
	
	public IsoRegistrationSet(boolean forEvery, boolean forOnlyCloseds) {
		this.forEvery = forEvery;
		this.forOnlyCloseds = forOnlyCloseds;
	}
	
	public void setFiberToNotify() {
		toNotify = Fiber.getCurrentFiber();
	}
	
	public Fiber getToNotify() {
		if(null == toNotify) {
			setFiberToNotify();
		}
		return toNotify;
	}
	
	public boolean isRegistered(Ref<?> x){
		//System.err.println("isRegistered:" + x);
		return refs.containsKey(x) || childsubscriptions.containsKey(x);
	}
	
	public boolean register(Ref<?> x){
		if(childsubscriptions.containsKey(x)){
			return x.isSet();//already subscribed
		}
		else{
			refs.put(x, x);
			return x.register(getToNotify(), 1);
		}
	}
	
	public boolean register(Ref<?> x, ReferenceSet child, boolean isInitialsubscription){
		if(refs.containsKey(x)){
			return x.isSet();
		}
		else{
			childRefCount++;
			Set<ReferenceSet> kids = childsubscriptions.get(x);
			if(null == kids){
				kids = new HashSet<ReferenceSet>();
				childsubscriptions.put(x, kids);
				kids.add(child);
				boolean regState = x.register(getToNotify(), 1);
				
				if(!isInitialsubscription && this.forEvery){
					if((this.forOnlyCloseds && x.isClosed()) || regState) {//closed and closed or regular instance
						ArrayList<Ref<?>> toProc = new ArrayList<Ref<?>>(1);
						toProc.add(x);
						Transaction transx = new LocalTransaction(toProc);
						Fiber.notif(getToNotify(), transx, true);
					}
				}
				
				return regState;
			}
			else{
				kids.add(child);
				return x.isSet();
			}
		}
	}
	
	/**
	 * @param xs returns idexes of those items having already been set
	 */
	public ArrayList<Integer> register(Ref<?>[] xs){
		//not stricly necisary to have these two?
		ArrayList<Integer> ret = new ArrayList<Integer>(xs.length);
		for(int n=0; n < xs.length; n++){
			Ref<?> x = xs[n];
			if(register(x)){
				ret.add(n);
			}
		}
		return ret;
	}
	
	public ArrayList<Integer> register(LocalArray<Ref<?>> la){
		Ref<?>[] xs = la.ar;
		ArrayList<Integer> ret = new ArrayList<Integer>(xs.length);
		for(int n=0; n < xs.length; n++){
			Ref<?> x = xs[n];
			if(register(x)){
				ret.add(n);
			}
		}
		return ret;
	}
	
	
	
	
	public boolean hasRegistrations(){
		if(!refs.isEmpty() || childRefCount > 0){
			return true;
		}
		return false;
	}

	public List<Ref<?>> register(ReferenceSet child){
		if(!this.children.contains(child)){
			this.children.add(child);
			return child.addMaster(this);
		}
		return null;
	}
	
	
	/**
	 * ok to be unregistered twice
	 * @param x
	 */
	public void unregister(Ref<?> x){
		if(refs.containsKey(x)) {
			childRefCount--;
			x.unregister(getToNotify(), 1);
			refs.remove(x);
		}
	}
	
	public void unregister(Ref<?> x, ReferenceSet child){
		Set<ReferenceSet> kids = this.childsubscriptions.get(x);
		if(null != kids && kids.contains(child)){
			kids.remove(child);
			if(kids.isEmpty()){
				this.childsubscriptions.remove(x);
				x.unregister(getToNotify(), 1);
			}
		}
	}
	
	public void unregisterAll(){
		for(ReferenceSet child : children){
			child.removeMaster(this);
			children.remove(child);
		}
		
		//local registrations:
		Set<Ref<?>> kz = this.refs.keySet();
		Ref<?>[] locs = kz.toArray(new Ref<?>[kz.size()]);
		
		for(Object x : locs){
			unregister((Ref<?>)x);
		}
	}
	
}
