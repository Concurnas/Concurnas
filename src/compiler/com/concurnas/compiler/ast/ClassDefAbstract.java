package com.concurnas.compiler.ast;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.concurnas.compiler.visitors.Visitor;
import com.concurnas.compiler.visitors.util.ErrorRaiseableSupressErrors;
import com.concurnas.runtime.Pair;

public abstract class ClassDefAbstract implements ClassDefI {

	public abstract String getPrettyName();
	public abstract boolean isChangeable();
	public abstract Object accept(Visitor visitor);
	
	public abstract boolean hasConstructor(List<Type> typeArgsToMatch, ErrorRaiseableSupressErrors invoker);
	public abstract boolean isBeingBeingPastTheParentOfMe(ClassDef lhsParent);
	public abstract ClassDef getSuperclass();
	public abstract HashSet<ClassDef> getTraitsIncTrans();
	
}
