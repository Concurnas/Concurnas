package com.concurnas.runtime.cps.analysis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Stack;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.VarInsnNode;


public class ANFTransform implements Opcodes{

	public static class InstuctionsAndPosToExtra
	{
        public InsnList inst;
        public HashMap<Integer, Integer> posToExtra;
        public  ArrayList<InsertRange> insertedCodeRanges;
	}

	
	/*
	 * convert from ssb  =new StringBuilder( v1() ) to
	 * x = v1()
	 * ssb  =new StringBuilder( x )
	 * 
	 * only applies to pausable constructor (i.e. all legacy and some , with pausable call in expr slot [optional dup]
	 * No applies to all constructors, the java ones which are pausable will have been extracted via the trick
	 * 
	 * new, dup?, |---|, invk <init> => |---| -> store x,y,z, new, dup?, oad x,y,z, invk <init>
	 * 
	 */
	public static InstuctionsAndPosToExtra anfConstructor(MethodFlow flow, InsnList instructions) {
        int size = instructions.size();
        InsnList instructionsRet = new InsnList();
        
        //stack in case of nested constructor invokation
        Stack<ConstruRange> construRanges = new Stack<ConstruRange>();
        
        int maxLocalSize = flow.maxLocals;//1 => method(this) argument etc - so inc from that onwards
       /* for (int pos=0; pos < size; pos++) {
        	AbstractInsnNode ain = (AbstractInsnNode) instructions.get(pos);
        	
        	if(ain instanceof VarInsnNode){
        		int v = ((VarInsnNode) ain).var;
        		if(v>maxLocalSize){
        			maxLocalSize = v;
        		}
        	}
        }
        */
        HashMap<Integer, Integer> posToExtraInstructions = new HashMap<Integer, Integer>(); 
        ArrayList<InsertRange> insertedCodeRanges = new ArrayList<InsertRange>();
        //int extraInstru=0;
        
        for (int pos=0; pos < size; pos++) {
        	AbstractInsnNode ain = (AbstractInsnNode) instructions.get(pos);
            int opcode = ain.getOpcode();
            switch (opcode) {
	            case INVOKEVIRTUAL:
	            case INVOKESTATIC:
	            case INVOKEINTERFACE:
	            case INVOKESPECIAL:
	            	if(!construRanges.isEmpty()){
	            		//if (flow.isPausableMethodInsn((MethodInsnNode) ain)) {
	            		MethodInsnNode min = ((MethodInsnNode) ain);
	            		String name = min.name;
	            		if(!name.endsWith("init>")) {//TODO: constructors not allowed
		                	construRanges.peek().nestedPauseInvokation=true;
		                }
		                
		                if(name.equals("<init>")){
		                	//it's the end!
		                	ConstruRange cr = construRanges.pop();
		                	if(cr.nestedPauseInvokation){
		                		if(!construRanges.isEmpty()){//set nestedPauseInvokation higher up
			                		construRanges.peek().nestedPauseInvokation = cr.nestedPauseInvokation;
			                	}
		                		//push to localvars, create object dup?, then locals to stack
		                		
		                		int nestingLevels = flow.getNestingLevel(min.owner);
		                		
		                		ArrayList<AbstractInsnNode> newAndDup = new ArrayList<AbstractInsnNode>(2);
		                		
		                		newAndDup.add(cr.codes.remove(0));//new
		                		
		                		int dupInstrConsumption = 1;//start with 1 because the NEW operation is an instruction
		                		
		                		boolean isCallingNestedConstr = false;
		                		int nextOne= cr.codes.get(0).getOpcode();//could it be a dup?
		                		boolean specialDup = nextOne == DUP_X1 ||  nextOne == DUP2_X1 || nextOne == DUP2_X2;//expect another swap operation
		                		if(nextOne == DUP ||  specialDup ||  nextOne == DUP_X2 ||  nextOne == DUP2 ){
		                			//dup?, dup_x1 etc all of the udps
		                			cr.codes.remove(0);
		                			dupInstrConsumption+=1;
		                			if(specialDup && cr.codes.get(0).getOpcode() == SWAP){
		                				//skip the next swap if there is one and dup is special
		                				cr.codes.remove(0);
		                				dupInstrConsumption+=1;
		                			}
		                			newAndDup.add(new InsnNode(DUP));//hack yuck - should really do a proper analysis
		                		}
		                		else if(nextOne == SWAP && nestingLevels>0){//for nested class constructors
		                			isCallingNestedConstr=true;
		                			//check its a nested consturctor?
		                			AbstractInsnNode code = cr.codes.remove(0);
		                			dupInstrConsumption+=1;
		                			newAndDup.add(code);
		                			code = cr.codes.remove(0);
		                			dupInstrConsumption+=1;
		                			while(code.getOpcode()!= SWAP){
		                				newAndDup.add(code);
		                				code = cr.codes.remove(0);
		                				dupInstrConsumption+=1;
		                			}
		                			newAndDup.add(code);
		                		}
		                		
		                		//if swap then consume until u get to another swap?
		                		
		                		//now we just have the stuff going onto the stack to deal with
		                		List<Character> charz = getPrimOrObj( min.desc);
		                		if(isCallingNestedConstr && nestingLevels > 0){//nested components will already be on the stack prior to calling xxx; new; ...; <init>
		                			charz = charz.subList(nestingLevels-1, charz.size()-nestingLevels);
		                		}
		                		
		                		for(AbstractInsnNode a : cr.codes){ instructionsRet.add(a); }//normal work
		                		
		                		for(int n=charz.size()-1; n >=0; n--){//store in vars
		                			char x = charz.get(n);
		                			instructionsRet.add(new VarInsnNode(getStoreOp(x), maxLocalSize++));
		                			if(x == 'D' || x=='J'){//doubles take up two var slots! (... jvm designers?)
		    							maxLocalSize++;
		    						}
		                		}
		                		flow.maxLocals = Math.max(flow.maxLocals, maxLocalSize);
		                		
		                		for(AbstractInsnNode a : newAndDup){ instructionsRet.add(a); }//new and dup
		                		
		                		for(char c : charz){//reload the stack
		                			if(c == 'D' || c == 'J'){
		                				--maxLocalSize;
		    						}
		                			instructionsRet.add(new VarInsnNode(getLoadOp(c), --maxLocalSize));
		                		}
		                		
		                		//extraInstru += charz.size() * 2;//x2 because we add a store instruction and a get instruction
		                		posToExtraInstructions.put(pos, charz.size() * 2);
		                		insertedCodeRanges.add(new InsertRange(cr.startPos, pos, dupInstrConsumption));
		                		//carry on as normal invoking the constructor as one would expect...
		                	}
		                	else{//easy, nothing to worry about just add to end
								for (AbstractInsnNode a : cr.codes) {
									if (construRanges.isEmpty()) {
										instructionsRet.add(a);
									} else {// push code block one level up if
											// approperiate
										construRanges.peek().codes.add(a);
									}
								}
		                	}
		                }
	            	}
	                
	                break;
	            case NEW:
	            	construRanges.push(new ConstruRange(size-pos, pos));
            }
            
        	if(construRanges.isEmpty()){
        		instructionsRet.add(ain);
        	}
        	else{
        		construRanges.peek().codes.add(ain);
        	}
        }
        
        if(!construRanges.isEmpty()){
        	throw new RuntimeException("Error on constructor ANF Transform, expected empty stack");
        }
		
        InstuctionsAndPosToExtra ret = new InstuctionsAndPosToExtra();
        ret.inst=instructionsRet;
        ret.posToExtra=posToExtraInstructions;
        ret.insertedCodeRanges=insertedCodeRanges;
        
		return ret;
	}
	
	public static class InsertRange{
		public final int startpos;
		public final int endpos;
		public final int backshift;
		
		public InsertRange(int startpos, int endpos, int backshift){
			this.startpos=startpos;
			this.endpos=endpos;
			this.backshift=backshift;
		}
		
		@Override
		public String toString(){
			return String.format("[%s - %s] : -%s", startpos, endpos, backshift);
		}
	}
	
	public static int getLoadOp(char c){ 
		switch(c){
			case 'Z': return Opcodes.ILOAD ;
			case 'B': return Opcodes.ILOAD ;
			case 'S': return Opcodes.ILOAD;
			case 'I': return Opcodes.ILOAD ;
			case 'J': return Opcodes.LLOAD ;
			case 'F': return Opcodes.FLOAD ;
			case 'D': return Opcodes.DLOAD ;
			case 'C': return Opcodes.ILOAD ;
			default:
				return Opcodes.ALOAD;
		}
	}
	
	public static int getReturnOp(char c){ 
		switch(c){
			case 'V': return RETURN ;
			case 'Z': return IRETURN ;
			case 'B': return IRETURN ;
			case 'S': return IRETURN;
			case 'I': return IRETURN ;
			case 'J': return LRETURN ;
			case 'F': return FRETURN ;
			case 'D': return DRETURN ;
			case 'C': return IRETURN ;
			default:
				return ARETURN;
		}
	}
	
	
	
	public static int getStoreOp(char c){
		switch(c){
			case 'Z': return Opcodes.ISTORE ;
			case 'B': return Opcodes.ISTORE ;
			case 'S': return Opcodes.ISTORE;
			case 'I': return Opcodes.ISTORE ;
			case 'J': return Opcodes.LSTORE ;
			case 'F': return Opcodes.FSTORE ;
			case 'D': return Opcodes.DSTORE ;
			case 'C': return Opcodes.ISTORE ;
			default:
				return Opcodes.ASTORE;
	}
	}
	
	public static List<Character> getPrimOrObj(String desc){
		
		
		Type[] xx = org.objectweb.asm.Type.getArgumentTypes(desc);
		
		//String d = xx[0].getDescriptor();

		ArrayList<Character> ret = new ArrayList<Character>();
		for(Type tt : xx) {
			char c  = tt.getDescriptor().charAt(0);
			switch(c){
				case 'Z': 
				case 'B': 
				case 'S': 
				case 'I': 
				case 'J': 
				case 'F': 
				case 'D': 
				case 'C': 
					ret.add(c);
					break;
				default: 
					ret.add('L');//object
					break;
			}
		}
		return ret;
		
		/*
		Z, B, S, I, J, F, D, C
		[.*;? = any non-primitives(Object)
		(IIDLString;[D[String;DD)LString;
		=>false, false, false, true, true, false, false
		 */
		
	/*	ArrayList<Character> ret = new ArrayList<Character>();
		
		int siz = desc.length();
		int n=0;
		boolean inobject = false;
		
		while( n <= siz){
			char c = desc.charAt(n);
			
			if(c==')'){
				break;
			}
			
			if(inobject){
				if(c==';'){
					inobject=false;
				}
				n++;
				continue;
			}
			
			switch(c){
				case '(':
					break;
				case 'Z': 
				case 'B': 
				case 'S': 
				case 'I': 
				case 'J': 
				case 'F': 
				case 'D': 
				case 'C': 
					ret.add(c);
					break;
				case '[':
					n++;
					char c2 = desc.charAt(n);
					inobject = c2=='L';
					ret.add('L');//object
					break;
				case 'L':
					inobject = true;
					ret.add('L');//object
			}
			
			n++;
		}
		
		return ret;*/
	}
	
	private static class ConstruRange{
		public boolean nestedPauseInvokation=false;
		public ArrayList<AbstractInsnNode> codes;
		public int startPos;
		
		public ConstruRange(int siz, int startPos){//guess size, save planet
			codes = new ArrayList<AbstractInsnNode>(siz);
			this.startPos = startPos;
		}
		
	}
}
