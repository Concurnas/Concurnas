package com.concurnas.runtime;


import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.AnalyzerAdapter;


public abstract class AnalyzerAdapterInitialConstructorTracker extends AnalyzerAdapter implements Opcodes {
	protected AnalyzerAdapterInitialConstructorTracker(final int api, final String owner, final int access, final String name, final String desc, final MethodVisitor mv) {
		super(api, owner, access, name, desc, mv);
	}
	
	protected boolean topOfStackIsThisUninit(String desc){
		//check to see if top of stack is UNITIZALIZED THIS reference, offset by arguments consumed by this statement which -should be- already on the stack
		int offset = getSlotsConsumed(desc);
		
		if(null == super.stack){
			return false;//TODO: dunno if this is the right solution...
		}
		
		int sz = super.stack.size();
		for(int n = sz-1 - offset; n >= 0; n--){
			Object top = super.stack.get(n);
			if(null != top){
				
				/*if(top instanceof Label){//ununitialized object of a defined type
					return false;
				}*/
				if(top instanceof Integer && (((Integer)top) == UNINITIALIZED_THIS)){//finally!
					return true;
				}
				return false;
				//any other type, assumed consumed by method - count this?
			}
		}
		return false;
	}
	
	private Label getTopOfStack(int offset){
		if(null == super.stack){
			return null;
		}
		int sz = super.stack.size();
		for(int n = sz-1-offset; n >= 0; n--){
			Object top = super.stack.get(n);
			if(null != top){
				if(top instanceof Label){
					return (Label)top;
				}
				break;
			}
		}
		return null;
	}
	
	protected Label getTopOfStack(){
		return this.getTopOfStack(0);
	}
	
	private int getSlotsConsumed(String desc){
		int ret=0;
		for(Type t : Type.getArgumentTypes(desc)){
			ret += t.getSize();
		}
		return ret;
	}
	
	protected Label getTopOfStack(String desc){
		return getTopOfStack(getSlotsConsumed(desc));
	}
	
	/*
	 	protected Label getTopOfStack(){
		int sz = super.stack.size();
		for(int n = sz-1; n >= 0; n--){
			Object top = super.stack.get(n);
			if(null != top){
				if(top instanceof Label){
					return (Label)top;
				}
			}
		}
		return null;
	}
	 */
	
}
