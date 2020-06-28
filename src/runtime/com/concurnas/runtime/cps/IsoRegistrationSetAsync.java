package com.concurnas.runtime.cps;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import com.concurnas.bootstrap.runtime.ref.Ref;
import com.concurnas.bootstrap.runtime.transactions.Transaction;

public class IsoRegistrationSetAsync {

	private static class IsoRegistrationSetAsyncChildCompatior implements Comparator<IsoRegistrationSetAsyncChild> {
		@Override
		public int compare(IsoRegistrationSetAsyncChild o1, IsoRegistrationSetAsyncChild o2) {
			return o1.order - o2.order;
		}
	}

	private Map<Ref<?>, TreeSet<IsoRegistrationSetAsyncChild>> refToChildren = new HashMap<Ref<?>, TreeSet<IsoRegistrationSetAsyncChild>>();
	private Set<IsoRegistrationSetAsyncChild> children = new HashSet<IsoRegistrationSetAsyncChild>();

	public void register(IsoRegistrationSetAsyncChild child, Ref<?> x) {
		children.add(child);
		TreeSet<IsoRegistrationSetAsyncChild> sset = refToChildren.get(x);
		if (null == sset) {
			sset = new TreeSet<IsoRegistrationSetAsyncChild>(new IsoRegistrationSetAsyncChildCompatior() );
			refToChildren.put(x, sset);
		}

		sset.add(child);
		
	}

	public void unregisterAll() throws Throwable {
		// children - iterate through all children and
		for (IsoRegistrationSetAsyncChild child : children) {
			child.unregisterAll();
		}
		children.clear();
		refToChildren.clear();
	}

	/**
	 * returns consistently ordered TreeSet based on declaration order of child onchange/every instances
	 */
	public TreeSet<IsoRegistrationSetAsyncChild> getMapping(Ref<?> x) {
		TreeSet<IsoRegistrationSetAsyncChild> ret = refToChildren.get(x);
		if (ret == null) {
			throw new RuntimeException("Ref is not used within onchange/every blocks within async block");
		} else if (ret.isEmpty()) {
			throw new RuntimeException("No onchange/every blocks associated with ref");
		}

		return ret;
	}
	
	/**
	 * returns consistently ordered TreeSet based on declaration order of child onchange/every instances
	 */
	public IsoRegistrationSetAsyncChild[] getMapping(Transaction trans) {
		TreeSet<IsoRegistrationSetAsyncChild> ret = null;
		
		for(Ref<?> x : trans.getChanged().ar){
			if(null == ret){
				ret = getMapping(x);
			}
			else{
				ret.addAll(getMapping(x));
			}
		}

		return (IsoRegistrationSetAsyncChild[])ret.toArray(new IsoRegistrationSetAsyncChild[0]);
	}

	public void unregister(IsoRegistrationSetAsyncChild child, Ref<?> x) {
		
		
		TreeSet<IsoRegistrationSetAsyncChild> sset = refToChildren.get(x);
		if (null == sset) {
			throw new RuntimeException("onchange/every block was not registered with ref");
		}

		sset.remove(child);
		if(sset.isEmpty()){
			refToChildren.remove(x);
			children.remove(x);
		}
	}
	
	
	public boolean hasRegistrations(){//true if any still have regs - seems a bit inefficient
		if(!children.isEmpty()){
			for(IsoRegistrationSetAsyncChild child : children){
				if(child.hasRegistrations()){
					return true;
				}
			}
		}
		return false;
	}
	
}
