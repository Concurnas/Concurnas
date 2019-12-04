package com.concurnas.lang.precompiled;

import java.util.ArrayList;

public class WithALocalGenericType {
	public <TWAT> TWAT getaT(TWAT s, int gg){ return s; }
	
	public <TWAT> TWAT[] getarr(TWAT s, int gg){ return null; }
	
	public <TWAT> TWAT getaComplex(ArrayList<TWAT> s, int gg){ return s.get(0); }
}
