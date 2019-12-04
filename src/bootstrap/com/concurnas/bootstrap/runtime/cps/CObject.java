package com.concurnas.bootstrap.runtime.cps;

import java.util.ArrayList;

import com.concurnas.bootstrap.lang.offheap.Decoder;
import com.concurnas.bootstrap.lang.offheap.Encoder;
import com.concurnas.bootstrap.runtime.InitUncreatable;

public class CObject {

	public CObject( ){}
	
	public String toString(){
		StringBuilder sb = new StringBuilder();
		sb.append(getClass().getName());
		sb.append("@");
		String h = Integer.toHexString(hashCode());//going via super would mean that the conc information is lost
		sb.append(h);
		return sb.toString();
	}
	
	public int hashCode(){
		return super.hashCode();
	}
	
	public boolean equals(Object o){
		return super.equals(o);
	}
	
	public Object clone() throws CloneNotSupportedException{
		return super.clone();
	}
	
	public boolean toBoolean(){
		return true;
	}
	
	public ArrayList<String> getGlobalDependancies$() {
		return null;
	}
	
	/**
	 * Called when object is removed from local scope using the del command
	 */
	public void delete(){
	}
	
	protected void finalize() {
	}
	
	public void defaultFieldInit$(InitUncreatable x, boolean[] y){
		//used to instantiate transient fields with defaults - unused for classes from primorial classloader
		return;
	}
	
	public void toBinary(Encoder offheap){
		Class<?> calledon = this.getClass();
		if(calledon != CObject.class){
			throw new RuntimeException("toBinary(Encoder) has not been implemented for: " + calledon.getSimpleName());
		}
	}
	
	public void fromBinary(Decoder offheap){
		Class<?> calledon = this.getClass();
		if(calledon != CObject.class){
			throw new RuntimeException("fromBinary(Decoder) has not been implemented for: " + calledon.getSimpleName());
		}
	}
}
