package com.concurnas.compiler.ast;

import java.util.HashSet;
import java.util.List;

public class CopyExprSuper implements CopyExprItem {
	public List<CopyExprItem> copyItems;
	public int line;
	public int col;
	public HashSet<String> incItems;
	public List<String> modifiers;
	public boolean nodefault;

	public CopyExprSuper(int line, int col, List<CopyExprItem> copyItems, List<String> modifiers) {
		this.line = line;
		this.col = col;
		this.copyItems = copyItems;
		this.modifiers = modifiers;
	}

	@SuppressWarnings("unchecked")
	@Override
	public Copyable copy() {
		CopyExprSuper ret = new CopyExprSuper(line, col, (List<CopyExprItem>)Utils.cloneArrayList(copyItems), modifiers);
		ret.nodefault = nodefault;
		return ret;
	}

}
