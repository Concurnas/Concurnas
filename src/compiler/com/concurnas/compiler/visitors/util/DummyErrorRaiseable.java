package com.concurnas.compiler.visitors.util;

import java.util.ArrayList;
import java.util.Collections;

import com.concurnas.compiler.ast.ClassDef;
import com.concurnas.compiler.visitors.ErrorRaiseable;

public class DummyErrorRaiseable implements ErrorRaiseable {
	private final ArrayList<String> errs = new ArrayList<String>();
	public String getErrors()
	{
		Collections.sort(errs);
		StringBuilder sb = new StringBuilder();
		for(String i : errs)
		{
			sb.append(i);
			sb.append(';');
		}
		return sb.toString();
	}
	
	
	@Override
	public void raiseError(int line, int column, String error) {
		errs.add(String.format("Error on line: %s col: %s. Error: %s", line, column, error));
	}

	@Override
	public ClassDef getImportedOrDeclaredClassDef(String name) {
		return null;
	}

	@Override
	public ClassDef getImportedClassDef(String namedType) {
		return null;
	}


	@Override
	public ErrorRaiseable getErrorRaiseableSupression() {
		return this;
	}


	@Override
	public ClassDef getImportedClassDef(String namereftoresolve, boolean ingoreDotOp) {
		return null;
	}

}
