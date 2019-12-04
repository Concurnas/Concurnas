package com.concurnas.compiler.visitors.util;

import com.concurnas.compiler.CaseExpression;
import com.concurnas.compiler.ast.Block;
import com.concurnas.compiler.visitors.CasePatternConverter;

public class MactchCase {
	private CaseExpression ce;
	private Block block;

	public MactchCase(CaseExpression ce, Block block) {
		this.ce = ce;
		this.block = block;
	}
	
	public CaseExpression getA() {
		return this.ce;
	}
	
	public Block getB() {
		return this.block;
	}
}
