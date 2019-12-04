package com.concurnas.compiler.visitors;

import java.util.Collection;
import java.util.HashSet;

import com.concurnas.compiler.ErrorHolder;
import com.concurnas.compiler.ast.RefName;
import com.concurnas.compiler.ast.interfaces.Expression;

/**
 * Outputs all raw name references made in AST
 */
public class RefNameTrackerVisitor extends AbstractVisitor {
	HashSet<String> namesReferencedInTree = new HashSet<String>();
	
	@Override
	public Object visit(RefName refName) {
		namesReferencedInTree.add(refName.name);
		return null;
	}

	public void visit(Expression e) {
		e.accept(this);
	}

	@Override
	public Collection<? extends ErrorHolder> getErrors() {
		return null;
	}
}
