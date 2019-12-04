package com.concurnas.compiler.ast;

import com.concurnas.compiler.visitors.datastructs.TheScopeFrame;

public interface AttachedScopeFrame {
	public TheScopeFrame getScopeFrameGenIfMissing(TheScopeFrame parent, ClassDef cls);
}
