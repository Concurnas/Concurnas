package com.concurnas.repl;

import java.util.HashSet;

import com.concurnas.compiler.visitors.datastructs.TheScopeFrame;

public class REPLVariableState {
	//private HashSet<String> lastVars;
	private TheScopeFrame moduleLevelFrame;
	//TODO: remove this class and query modulelevelframe directly
	public REPLVariableState(TheScopeFrame moduleLevelFrame) {
		this.moduleLevelFrame = moduleLevelFrame;
	}

/*	private List<String> getAndFilterVars(){
		return moduleLevelFrame.getAllVars(null).keySet().stream().filter(a ->  a.startsWith("$") && a.lastIndexOf('$')==0 || a.lastIndexOf('$')<=0  ).collect(Collectors.toList());
	}*/
	
	public HashSet<String> getNewVars() {
		
		/*if(lastVars == null) {
			lastVars = new HashSet<String>(getAndFilterVars());
			return lastVars;
		}else {
			List<String> newVars = getAndFilterVars();
			HashSet<String> ret = new HashSet<String>();
			
			ret.addAll(moduleLevelFrame.replAssignedthisIteration);
			lastVars = new HashSet<String>(newVars);
			return ret;
		}*/
		return new HashSet<String>(moduleLevelFrame.replAssignedthisIteration);
	}
}
