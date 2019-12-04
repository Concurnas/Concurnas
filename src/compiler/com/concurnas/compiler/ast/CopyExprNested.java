package com.concurnas.compiler.ast;

import java.util.HashSet;
import java.util.List;

public class CopyExprNested implements CopyExprItem {
	public String fname;
	public List<CopyExprItem> copyItems;
	public int line;
	public int col;
	public HashSet<String> incItems;
	public List<String> modifiers;
	public boolean nodefault;

	public CopyExprNested(int line, int col, String fname, List<CopyExprItem> copyItems, List<String> modifiers) {
		this.line = line;
		this.col = col;
		this.fname = fname;
		this.copyItems = copyItems;
		this.modifiers = modifiers;
	}

	@SuppressWarnings("unchecked")
	@Override
	public Copyable copy() {
		CopyExprNested ret = new CopyExprNested(line, col, fname, (List<CopyExprItem>)Utils.cloneArrayList(copyItems), modifiers);
		ret.nodefault = nodefault;
		return ret;
	}

}
