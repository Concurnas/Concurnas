package com.concurnas.compiler.visitors;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

public enum WarningVariant {
	ENUMMATCHNONEXHAUSTIVE("enum-match-non-exhaustive"),
	TYPEDEFARGUSAGE("typedef-arg-use"), 
	GENERICCAST("generic-cast"), 
	REDEFINE_IMPORT("redefine-import"), 
	TYPEDEF_OVERRIDE("typedef-override"),
	ALL("all"/*, true*/);//all is special cannot be declated
	
	public String variant;
	//public boolean declarable;
	private WarningVariant(String variant/*, boolean declarable*/){
		this.variant = variant;
		//this.declarable=declarable;
	}
	
	/*private WarningVariant(String variant){
		this(variant, true);
	}*/
	
	public static final HashMap<String, WarningVariant> WarningVariantMap = new HashMap<String, WarningVariant>();
	public static final String WarningVariantList;
	static{
		ArrayList<String> itesm = new ArrayList<String>();
		for(WarningVariant wv : WarningVariant.values()){
			String vari = wv.variant;
			//if(wv.declarable){
				WarningVariantMap.put(vari, wv);
			//}
			if(!"all".equals(vari)){
				itesm.add(vari);
			}
		}
		Collections.sort(itesm);
		WarningVariantList=Utils.justJoin(itesm);
	}
}
