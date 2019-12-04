package com.concurnas.compiler.visitors;

import com.concurnas.compiler.ast.ClassDef;

public interface ErrorRaiseable {
	public void raiseError(int line, int column, String error);
	public ClassDef getImportedOrDeclaredClassDef(String name);
	public ClassDef getImportedClassDef(String namereftoresolve, boolean ingoreDotOp);
	public ClassDef getImportedClassDef(String namedType);
	public ErrorRaiseable getErrorRaiseableSupression();
	
}
