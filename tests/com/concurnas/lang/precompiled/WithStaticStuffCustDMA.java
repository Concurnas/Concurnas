package com.concurnas.lang.precompiled;

import com.concurnas.bootstrap.lang.offheap.Decoder;
import com.concurnas.bootstrap.lang.offheap.Encoder;

public class WithStaticStuffCustDMA {
	public int avar;
	public WithStaticStuffCustDMA(int avar){
		this.avar=avar;
	}
	
	public static String name = "default name";
	public  String toString(){return name;}
	
	public void toBinary(Encoder enc){
		enc.put(avar);
		enc.put(name);
		
	}
	
	public void  fromBinary(Decoder dec ){
		avar = dec.getInt();
		name = (String)dec.getObject();
	}
	
}
