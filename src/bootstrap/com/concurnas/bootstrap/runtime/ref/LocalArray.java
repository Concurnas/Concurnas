package com.concurnas.bootstrap.runtime.ref;

import java.util.Iterator;
import java.util.function.Consumer;

import com.concurnas.bootstrap.lang.Stringifier;
import com.concurnas.bootstrap.runtime.ReifiedType;
import com.concurnas.lang.Equalifier;
import com.concurnas.lang.Hasher;

public class LocalArray<X> implements ReifiedType {
	public X[] ar;
	public final Class<?>[] type;//TODO: shouldnt this be private?

	public LocalArray(Class<?>[] type){
		this.type = type;
	}
	
	@Override
	public Class<?>[] getType() {
		return type;
	}
	
	@Override
	public String toString(){
		return Stringifier.stringify(ar);
	}
	
	@Override
	public boolean equals(Object obj){
		return Equalifier.equals(obj, this);
	}
	
	@Override
	public int hashCode(){
		return Hasher.hashCode(ar);
	}
	
	private class LAIterator implements Iterator<X>{

		private int pos =0;
		
		@Override
		public boolean hasNext() {
			return pos < ar.length;
		}

		@Override
		public X next() {
			return ar[pos++];
		}

		@Override
		public void remove() {
		}

		@Override
		public void forEachRemaining(Consumer<? super X> action) {
		}
		
	}
	
	public Iterator<X> iterator(){
		return new LAIterator();
	}
	
}
