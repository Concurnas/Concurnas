package com.concurnas.repl;

import java.util.HashSet;

import com.concurnas.compiler.visitors.datastructs.TheScopeFrame;

public class REPLVariableState {
	private TheScopeFrame moduleLevelFrame;
	
	public REPLVariableState(TheScopeFrame moduleLevelFrame) {
		this.moduleLevelFrame = moduleLevelFrame;
	}
	
	public HashSet<String> getNewVars() {
		return new HashSet<String>(moduleLevelFrame.replAssignedthisIteration);
	}
}
