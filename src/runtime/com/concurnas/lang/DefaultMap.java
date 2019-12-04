package com.concurnas.lang;

import java.util.HashMap;

import com.concurnas.bootstrap.lang.Lambda.Function1;

@SuppressWarnings("serial")
public class DefaultMap<K, V> extends HashMap<K, V> {
	private Function1<K, V> provider;
	
    public DefaultMap(Function1<K, V> provider) {
        this.provider = provider;    
    }
    
	@SuppressWarnings("unchecked")
	@Override
    public V get(Object key) {
		if(!super.containsKey(key)){
			super.put((K)key, provider.apply((K)key));
		}
		
		return super.get(key);
    }
	
	@Override
	public boolean containsKey(Object a){
		return true;
	}
}

