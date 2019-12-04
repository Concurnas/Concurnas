package com.concurnas.runtime;

import java.util.HashMap;
import java.util.List;
import java.util.Stack;
import java.util.stream.Collectors;


/**
 * Doesn't have fixed size unliked FixedSizeFrame, also minimal functionality
 * @author Jason
 *
 */
public class OnCompileFrame extends AbstractFrame {

	public Stack<Value> stack = new Stack<Value>();
	private HashMap<Integer, Value> locals = new HashMap<Integer, Value>();
	
	private static final boolean DEBUG=false;
	
	/*
	 * copy frame for use when frame is restored, e.g. arriving at the destination of a jump reference
	 * note we just copy the stack because local stay regardless + we dont really care for stak management
	 * which is the pruposoe of this code
	 */
	public Stack<Value> copyStack(){
		Stack<Value> ret = new Stack<Value>();
		//soft copy is fine
		ret.addAll(stack);
		
		return ret;
	}
	
	
	@Override
	public Value push(Value make) {
		if(DEBUG){
			System.out.println(">>Stack pre push: " + stack + "|pushing: " + make);
		}
		stack.add(make);
		return make;
	}

	@Override
	public Value popWord() {
		return pop();
	}

	@Override
	public Value pop() {
		if(DEBUG){
			System.out.println(">>Stack pre pop: " + stack);
		}
		
		return stack.pop();
	}

	@Override
	public Value getLocal(int local, int opcode) {
		return locals.get(local);
	}

	@Override
	public void popn(int i) {
		for(int n=0; n < i; n++){
			pop();
		}
	}

	@Override
	public int setLocal(int var, Value v1) {
		locals.put(var, v1);
		return 0;
	}

	@Override
	public void clearStack() {
		stack.clear();
	}


	@Override
	public int getStackLen() {
		return this.stack.size();
	}
	
    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer(100);
        int numDefined = 0;
        sb.append("): ");
        
        List<Integer> slots = locals.keySet().stream().sorted().collect(Collectors.toList());
        
        for (int i : slots) {
            Value v = locals.get(i);
            if (v != Value.V_UNDEFINED) {
                numDefined++;
                sb.append(i).append(':').append(v).append(" ");
            }
        }
        
        sb.insert(0, numDefined);
        sb.insert(0, "Locals(");
        int stsize = getStackLen();
        sb.append("\n").append("Stack(").append(stsize).append("): ");
        for (int i = 0; i < stsize; i++) {
            sb.append(this.stack.get(i)).append(" ");
        }
        return sb.toString();
    }

    @Override
    public String stacktoString() {
        int stsize = getStackLen();
        StringBuffer sb = new StringBuffer(100);
    	sb.append("(").append(stsize).append("): ");
        for (int i = 0; i < stsize; i++) {
            sb.append(this.stack.get(i).getTypeDesc()).append(" ");
        }
        return sb.toString();
    }
}
