package com.concurnas.compiler.bytecode;

import java.util.Collection;

import com.concurnas.compiler.ErrorHolder;
import com.concurnas.compiler.ast.AssignExisting;
import com.concurnas.compiler.ast.AssignNew;
import com.concurnas.compiler.visitors.AbstractVisitor;
import com.concurnas.compiler.visitors.Unskippable;

public class BytecodeVisitorCinitFieldGen extends AbstractVisitor implements Unskippable{

	private BytecodeGennerator mainVistitor;
	
	public BytecodeVisitorCinitFieldGen(BytecodeGennerator mainVistitor)
	{
		this.mainVistitor = mainVistitor;
	}
	
	@Override
	public Object visit(AssignNew assignNew) {
		return mainVistitor.visit(assignNew);
	}

	@Override
	public Object visit(AssignExisting assignExisting) {
		return mainVistitor.visit(assignExisting);
	}

	@Override
	public Collection<? extends ErrorHolder> getErrors() {
		return null;
	}
}
