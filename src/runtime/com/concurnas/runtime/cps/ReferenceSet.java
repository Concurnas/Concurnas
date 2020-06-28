package com.concurnas.runtime.cps;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.concurnas.bootstrap.runtime.ref.LocalArray;
import com.concurnas.bootstrap.runtime.ref.Ref;
import com.concurnas.bootstrap.runtime.transactions.Transaction;
import com.concurnas.lang.Shared;

@Shared
public class ReferenceSet{
	
	private Set<IsoRegistrationSet> masters = Collections.synchronizedSet(new HashSet<IsoRegistrationSet>());//JPT: should be weak so as to permit iso to die upon zero references?
	private volatile int masterCount =0;
	private Map<Ref<?>, Ref<?>> refs = Collections.synchronizedMap(new HashMap<Ref<?>, Ref<?>>());//hashset calls hashcode on fiber, which results in the value being obtained
	
	
	/**
	 * Returns refs which have already been set
	 * @throws Throwable 
	 */
	List<Ref<?>> addMaster(IsoRegistrationSet master) throws Throwable{
		Set<Ref<?>> refset = refs.keySet();
		ArrayList<Ref<?>> ret = new ArrayList<Ref<?>>(refs.size());
		masters.add(master);
		masterCount++;
		
		for(Ref<?> k : refset){
			if(master.register(k, this, true)){
				ret.add(k);
			}
		}
		
		return ret;
	}
	
	private void registerWithMasters(Ref<?> x) throws Throwable{
		for(IsoRegistrationSet master: masters){
			master.register(x, this, false);
		}
	}
	
	void removeMaster(IsoRegistrationSet master) throws Throwable{
		if(masters.remove(master)){
			for(Ref<?> x : refs.keySet()){
				master.unregister(x, this);
			}
			masterCount--;
			if(masterCount==0){
				refs.clear();
			}
		}//JPT: raize error if not present?
		else{
			throw new RuntimeException("Could not unbind registration set associated iso since it is not bound");
		}
	}

	
	public boolean add(Ref<?> x) throws Throwable{
		
		if(!refs.containsKey(x)){
			refs.put(x, x);
			if(masterCount>0){
				registerWithMasters(x);
			}
			return true;
		}
		return false;
	}
	
	/**
	 * @param xs returns idexes of those items having already been set
	 * @throws Throwable 
	 */
	public void addAll(Ref<?>[] xs) throws Throwable{
		//not stricly necisary to have these two?
		for(int n=0; n < xs.length; n++){
			add(xs[n]);
		}
	}
	
	public void addAll(Collection<? extends Ref<?>> xs) throws Throwable{
		//not stricly necisary to have these two?
		for(Ref<?> x : xs){
			add(x);
		}
	}
	
	public void addAll(LocalArray<Ref<?>> la) throws Throwable{
		Ref<?>[] xs = la.ar;
		for(int n=0; n < xs.length; n++){
			add(xs[n]);
		}
	}
	
	public void remove(Transaction trans) throws Throwable{
		for(Ref<?> change : trans.getChanged().ar){
			this.remove(change);
		}
	}
	
	public void remove(Ref<?> x) throws Throwable{
		//Sytesm.out.println("preform register");
		boolean removed = false;
		if(null!=refs.remove(x)){
			if(masterCount>0){
				for(IsoRegistrationSet master : masters){
					master.unregister(x, this);
					if(null != this.refs.remove(x)){
						removed=true;
					}
				}
			}
		}
		
		/*if(!removed){
			throw new RuntimeException("Cannot remove item as not registered");
		}*/
	}
	
	/**
	 * @param xs returns idexes of those items having already been set
	 * @throws Throwable 
	 */
	public void remove(Ref<?>[] xs) throws Throwable{
		//not stricly necisary to have these two?
		for(int n=0; n < xs.length; n++){
			remove(xs[n]);
		}
	}
	
	public void remove(LocalArray<Ref<?>> la) throws Throwable{
		Ref<?>[] xs = la.ar;
		for(int n=0; n < xs.length; n++){
			remove(xs[n]);
		}
	}

	public void removeAll() throws Throwable {
		Set<Ref<?>> ks = refs.keySet();
		
		for(Ref<?> r : ks.toArray(new Ref<?>[ks.size()])){
			remove(r);
		}
	}

	public int size() {
		return this.refs.size();
	}

	public boolean isEmpty() {
		return this.refs.isEmpty();
	}

	public boolean contains(Ref<?> o) {
		return this.refs.containsKey(o);
	}

	public Set<Ref<?>> toSet(){
		return this.refs.keySet();
	}
	
	public Object[] toArray() {
		return this.refs.keySet().toArray();
	}

	public <T> T[] toArray(T[] a) {
		return this.refs.keySet().toArray(a);
	}

	
}
