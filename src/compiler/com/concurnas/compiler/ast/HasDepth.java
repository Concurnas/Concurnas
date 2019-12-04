package com.concurnas.compiler.ast;

import java.util.ArrayList;

import com.concurnas.runtime.Pair;

public interface HasDepth {
	public ArrayList<Pair<Boolean, NullStatus>> getDepth();
	public void setDepth(ArrayList<Pair<Boolean, NullStatus>> depth);
}
