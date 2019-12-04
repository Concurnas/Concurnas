package com.concurnas.lang.offheap.storage;

import java.util.Arrays;

public class ClassAndMeta {
	public String[] meta;
	public Class<?> objClass;

	public ClassAndMeta(Class<?> objClass, String[] meta){
		this.objClass=objClass;
		this.meta=meta;
	}
	
	
	public int hashCode(){
		int ret = objClass.hashCode();
		if(null != meta){
			for(String x : meta){
				ret += x.hashCode();
			}
		}
		
		return ret;
	}
	
	public boolean equals(Object anobj){
		if(anobj instanceof ClassAndMeta){
			ClassAndMeta anotherCam = (ClassAndMeta)anobj;
			return anotherCam.objClass == objClass && Arrays.equals(anotherCam.meta, meta);
		}
		return false;
	}
	
}
