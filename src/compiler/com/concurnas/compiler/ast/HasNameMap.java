package com.concurnas.compiler.ast;

import java.util.List;

import com.concurnas.runtime.Pair;

public interface HasNameMap {
	public List<Pair<String, Object>> getNameMap();
}
