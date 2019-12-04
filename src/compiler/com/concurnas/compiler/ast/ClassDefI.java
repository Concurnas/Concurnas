package com.concurnas.compiler.ast;

import java.util.HashSet;

import com.concurnas.compiler.visitors.ScopeAndTypeChecker;
import com.concurnas.compiler.visitors.Visitor;
import com.concurnas.compiler.visitors.util.ErrorRaiseableSupressErrors;

public interface ClassDefI{

	public abstract String getPrettyName();
	public abstract boolean isChangeable();
	public abstract Object accept(Visitor visitor);
	
	public abstract HashSet<FuncType> getConstructor(int typeArgsToMatch, ErrorRaiseableSupressErrors invoker);
	public abstract boolean isBeingBeingPastTheParentOfMe(ClassDef lhsParent);
	public abstract ClassDef getSuperclass();
	public abstract HashSet<ClassDef> getTraitsIncTrans();
	public abstract HashSet<ClassDef> getTraits();
	public abstract boolean hasNoArgConstructor(ScopeAndTypeChecker sac);
}
