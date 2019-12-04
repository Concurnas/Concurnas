package com.concurnas.compiler.visitors.util;

import com.concurnas.compiler.ast.ClassDef;
import com.concurnas.compiler.visitors.ErrorRaiseable;

public class ErrorRaiseableSupressErrorsAndLogProblem implements ErrorRaiseable {

	private ErrorRaiseable parent;
	private boolean hasErrored = false;

	
	public ErrorRaiseableSupressErrorsAndLogProblem(ErrorRaiseable parent){
		this.parent = parent;
	}
	
	
	public boolean isHasErrored() {
		return hasErrored;
	}

	@Override
	public void raiseError(int line, int column, String error) {
		this.hasErrored = true;
		//ignore
	}

	@Override
	public ClassDef getImportedOrDeclaredClassDef(String name) {
		return this.parent.getImportedOrDeclaredClassDef(name);
	}

	@Override
	public ClassDef getImportedClassDef(String namedType) {
		return this.parent.getImportedClassDef(namedType);
	}

	@Override
	public ErrorRaiseable getErrorRaiseableSupression() {
		return this;
	}

	@Override
	public ClassDef getImportedClassDef(String namereftoresolve, boolean ingoreDotOp) {
		return this.parent.getImportedClassDef(namereftoresolve, ingoreDotOp);
	}

}
