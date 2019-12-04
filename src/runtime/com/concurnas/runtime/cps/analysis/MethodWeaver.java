/* Copyright (c) 2006, Sriram Srinivasan
 *
 * You may distribute this software under the terms of the license 
 * specified in the file "License"
 */

package com.concurnas.runtime.cps.analysis;
import static com.concurnas.runtime.cps.Constants.D_FIBER;
import static com.concurnas.runtime.cps.Constants.D_INT;
import static com.concurnas.runtime.cps.Constants.FIBER_CLASS;
import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.ASTORE;
import static org.objectweb.asm.Opcodes.DUP;
import static org.objectweb.asm.Opcodes.GETFIELD;
import static org.objectweb.asm.Opcodes.GOTO;
import static org.objectweb.asm.Opcodes.INVOKEVIRTUAL;

import java.util.ArrayList;
import java.util.List;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Handle;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.tree.LookupSwitchInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.TableSwitchInsnNode;
import org.objectweb.asm.tree.TryCatchBlockNode;

import com.concurnas.runtime.cps.mirrors.Detector;

/**
 * This class takes the basic blocks from a MethodFlow and generates 
 * all the extra code to support continuations.  
 */

public class MethodWeaver {

    private Fiberizer           classWeaver;

    private MethodFlow            methodFlow;

    private boolean               isPausable;

    private int                   maxVars;

    private int                   maxStack;

    /**
     * The last parameter to a pausable method is a Fiber ref. The rest of the
     * code doesn't know this because we do local surgery, and so is likely to
     * stomp on the corresponding local var. We need to save this in a slot
     * beyond (the original) maxLocals that is a safe haven for keeping the
     * fiberVar.
     */
    private int                   fiberVar;
    private int                   numWordsInSig;
    private ArrayList<MethodInvokationWeaver> callWeavers = new ArrayList<MethodInvokationWeaver>(5);


	private final boolean augmentJdk;

	public boolean failed=false;
	
    MethodWeaver(Fiberizer cw, MethodFlow mf, boolean augmentJdk) {
        this.classWeaver = cw;
        this.methodFlow = mf;
        isPausable = mf.isPausable();
        fiberVar =  methodFlow.maxLocals;
        maxVars = fiberVar + 1;
        maxStack = methodFlow.maxStack + 1; // plus Fiber 
        if (!mf.isAbstract()) {
            createCallWeavers();
        }
        this.augmentJdk = augmentJdk;
    }

    
    public void accept(ClassVisitor cv) {
        MethodFlow mf = methodFlow;
        String[] exceptions = Fiberizer.toStringArray(mf.exceptions);
        String desc = mf.desc;
        String sig = mf.signature;
        if (mf.isPausable() ) {//!fiberFromStatic - means that we obtan the fiber from arguments
            desc = desc.replace(")", D_FIBER + ')');
            if (sig != null)
                sig = sig.replace(")", D_FIBER + ')');
        }
        MethodVisitor mv = cv.visitMethod(mf.access, mf.name, desc, sig, exceptions);
        //System.err.println("ana: " + mf.name + " -> " + desc);
        
        if (!mf.isAbstract()) {
            if (mf.isPausable()) {
                accept(mv);
            } else {
                mf.accept(mv);
            }
        	//mf.accept(cv.visitMethod(mf.access, mf.name, mf.desc, mf.signature, exceptions));
        } else {
        	mv.visitEnd();
        }
    }
    
    void accept(MethodVisitor mv) {
        visitAttrs(mv);
        visitCode(mv);
        mv.visitEnd();
    }

    private void visitAttrs(MethodVisitor mv) {
        MethodFlow mf = methodFlow;
        // visits the method attributes
        int i, j, n;
        if (mf.annotationDefault != null) {
            AnnotationVisitor av = mv.visitAnnotationDefault();
            MethodFlow.acceptAnnotation(av, null, mf.annotationDefault);
            av.visitEnd();
        }
        n = mf.visibleAnnotations == null ? 0 : mf.visibleAnnotations.size();
        for (i = 0; i < n; ++i) {
            AnnotationNode an = (AnnotationNode) mf.visibleAnnotations.get(i);
            an.accept(mv.visitAnnotation(an.desc, true));
        }
        n = mf.invisibleAnnotations == null ? 0 : mf.invisibleAnnotations.size();
        for (i = 0; i < n; ++i) {
            AnnotationNode an = (AnnotationNode) mf.invisibleAnnotations.get(i);
            an.accept(mv.visitAnnotation(an.desc, false));
        }
        n = mf.visibleParameterAnnotations == null ? 0
                : mf.visibleParameterAnnotations.length;
        for (i = 0; i < n; ++i) {
            List<?> l = mf.visibleParameterAnnotations[i];
            if (l == null) {
                continue;
            }
            for (j = 0; j < l.size(); ++j) {
                AnnotationNode an = (AnnotationNode) l.get(j);
                an.accept(mv.visitParameterAnnotation(i, an.desc, true));
            }
        }
        n = mf.invisibleParameterAnnotations == null ? 0
                : mf.invisibleParameterAnnotations.length;
        for (i = 0; i < n; ++i) {
            List<?> l = mf.invisibleParameterAnnotations[i];
            if (l == null) {
                continue;
            }
            for (j = 0; j < l.size(); ++j) {
                AnnotationNode an = (AnnotationNode) l.get(j);
                an.accept(mv.visitParameterAnnotation(i, an.desc, false));
            }
        }
        n = mf.attrs == null ? 0 : mf.attrs.size();
        for (i = 0; i < n; ++i) {
            mv.visitAttribute((Attribute) mf.attrs.get(i));
        }
    }

    private void visitCode(MethodVisitor mv) {
        mv.visitCode();
        methodFlow.resetLabels();
        visitTryCatchBlocks(mv);
        visitInstructions(mv);
        visitLocals(mv);
        visitLineNumbers(mv);
        
        try{
        	mv.visitMaxs(maxStack, maxVars);
        }
        catch(ArrayIndexOutOfBoundsException e){//TODO: is this ever thrown from here?
        	if(e.toString().equals("java.lang.ArrayIndexOutOfBoundsException: 236")){
        		if(augmentJdk){
        			//simple copy normal method code and add fiber at end
        			//System.err.println("todo copy method code -" + methodFlow.classFlow.getClassName() + " size: ");
        			failed = true;
        		}
        		else{
        			throw new RuntimeException(String.format("Method: '%s' of '%s' is too large (>64kb post concurnification). Split into smaller submethods", this.methodFlow.name, this.methodFlow.classFlow.name));
        		}
        	}
        }
        
    }

    private void visitLineNumbers(MethodVisitor mv) {
        methodFlow.visitLineNumbers(mv);
    }
  
    private void visitLocals(MethodVisitor mv) {
        for (Object l: methodFlow.localVariables) {
        	LocalVariableNode lvn = (LocalVariableNode)l;
        	lvn.signature = lvn.signature==null?null:lvn.signature;
        	lvn.accept(mv);
        }
    }

    private void visitInstructions(MethodVisitor mv) {
        MethodFlow mf = methodFlow;
        genPrelude(mv);
        BasicBlock lastBB = null;
        
        for (BasicBlock bb : mf.getBasicBlocks()) {
            int from = bb.startPos;
            
            //if (bb.isPausable() && bb.startFrame != null) {
            if (bb.isPausable() && bb.startFrame != null) {
                boolean repoint = redirectToPausableMethodInvoke(mv, bb);
                if(repoint){
                	from = bb.startPos + 1; // first instruction is consumed
                }
            } else if (bb.isCatchHandler()) {
                List<MethodInvokationWeaver> cwList = getCallsUnderCatchBlock(bb);
                if (cwList != null) {
                    genException(mv, bb, cwList);
                    from = bb.startPos + 1; // first instruction is consumed
                } // else no different from any other block
            }
            int to = bb.endPos;
            for (int i = from; i <= to; i++) {
                LabelNode l = mf.getLabelAt(i);
                if (l != null) {
                    l.accept(mv);
                }
                
                AbstractInsnNode ain = bb.getInstruction(i);
                if (ain.getOpcode() == Opcodes.INVOKEDYNAMIC) {
                    invokeDynamicOnLambda(mv, ain);
                } else {
                    ain.accept(mv);
                }
                
                //((AbstractInsnNode)bb.getInstruction(i)).accept(mv);
            }
            lastBB = bb;
        }
        if (lastBB != null) {
            LabelNode l = methodFlow.getLabelAt(lastBB.endPos+1);
            if (l != null) {
                l.accept(mv);
            }
        }
    }
    
   /* private String replaceDesc(String desc){
    	return desc;
    }
    
    private void acceptInstruction(AbstractInsnNode  inst, final MethodVisitor mv){
    	//remap Object references as necisary
    	if(inst instanceof TypeInsnNode){
    		TypeInsnNode asTypeInsnNode = (TypeInsnNode)inst;
    		asTypeInsnNode.desc = replaceDesc(asTypeInsnNode.desc);
    		inst = asTypeInsnNode;
    	}
    	else if(inst instanceof MultiANewArrayInsnNode){
    		MultiANewArrayInsnNode asTypeInsnNode = (MultiANewArrayInsnNode)inst;
    		asTypeInsnNode.desc = replaceDesc(asTypeInsnNode.desc);
    		inst = asTypeInsnNode;
    	}//ignore visitMethodInsn
    	
    	inst.accept(mv);
    }*/

    private List<MethodInvokationWeaver> getCallsUnderCatchBlock(BasicBlock catchBB) {
        List<MethodInvokationWeaver> cwList = null; // create it lazily
        for (MethodInvokationWeaver cw: callWeavers) {
            for (Handler h: cw.bb.handlers) {
                if (h.catchBB == catchBB) {
                    if (cwList == null) {
                        cwList = new ArrayList<MethodInvokationWeaver>(callWeavers.size()); 
                    }
                    if (!cwList.contains(cw)) {
                    cwList.add(cw);
                }
            }
        }
        }
        return cwList;
    }

    /**
     * For a method invocation f(...), this method assumes that the arguments to
     * the call have already been pushed in. We need to push in the Fiber as the
     * final argument, make the call, then add the code for post-calls, then
     * leave it to visitInstructions() to resume visiting the remaining
     * instructions in the block
     * 
     * <pre>
     *  F_CALL:
     *    aload &lt;fiberVar&gt;
     *    invokevirtual fiber.down() ;; returns Fiber
     *    ... invoke ....
     *    aload &lt;fiberVar&gt;
     *    ... post call code
     *  F_RESUME: 
     * </pre>
     * 
     * @param bb
     * The BasicBlock that contains the pausable method invocation as the first
     * instruction
     * @param mv
     */
    private boolean redirectToPausableMethodInvoke(MethodVisitor mv, BasicBlock bb) {
        MethodInvokationWeaver caw = null;
        //if (bb.isGetCurrentTask()) {
        //    genGetCurrentTask(mv, bb);
        //    return true;
        //}
        for (MethodInvokationWeaver cw : callWeavers) {
            if (cw.getBasicBlock() == bb) {
                caw = cw;
                break;
            }
        }
        boolean redirect = null != caw;
        if(redirect){
        	  caw.genCall(mv);
              caw.genPostCall(mv);
        }
        return redirect;
    }
    
    /*
     * The Task.getCurrentTask() method is marked pausable to force
     * the caller to be pausable too. But the method doesn't really
     * pause; it merely looks up the task from the fiber. This is a
     * special case where the call to getCurrentTask is replaced by
     * <pre>
     *   load fiberVar
     *   getfield task
     * @param mv
     */
/*    void genGetCurrentTask(MethodVisitor mv, BasicBlock bb) {
        bb.startLabel.accept(mv);
        loadVar(mv, TOBJECT, getFiberVar());
        mv.visitFieldInsn(GETFIELD, FIBER_CLASS, "task", Constants.D_TASK);
    }*/

    private boolean hasGetCurrentTask() {//TODO: remove this code, we dont use this
        MethodFlow mf = methodFlow;
        for (BasicBlock bb : mf.getBasicBlocks()) {
            if (!bb.isPausable() || bb.startFrame==null) continue;
            if (bb.isGetCurrentTask()) return true;
        }
        return false;
    }
    private void createCallWeavers() {
        MethodFlow mf = methodFlow;
        for (BasicBlock bb : mf.getBasicBlocks()) {
        	//if (!bb.isPausable() || bb.startFrame==null) continue;
            if (!bb.isMethodCall() || bb.startFrame==null) continue;
        	if(bb.getInstruction(bb.startPos) instanceof InvokeDynamicInsnNode) {
        		continue;
        	}
            //if (!(bb.getInstruction(bb.startPos) instanceof MethodInsnNode) || bb.startFrame==null) continue;
            
            // No prelude needed for Task.getCurrentTask(). 
            if (bb.isGetCurrentTask()) continue; 
            MethodInvokationWeaver cw = new MethodInvokationWeaver(this, bb);
            callWeavers.add(cw);
        }
    }
    
    public boolean noFibizationToBeAdded(){
	    if (callWeavers.size() == 0){
	        // Method has been marked pausable, but does not call any pausable methods, nor Task.getCurrentTask.  
	        // Prelude is not needed at all.
	    	return true;
	    }
	    return false;
    }
    
    /**
     * 
     * Say there are two invocations to two pausable methods obj.f(int)
     * (virtual) and fs(double) (a static call) ; load fiber from last arg, and
     * save it in a fresh register ; lest it gets stomped on. This is because we
     * only patch locally, and don't change the other instructions.
     * 
     * <pre>
     *     aload lastVar
     *     dup
     *     astore fiberVar 
     *     switch (fiber.pc) { 
     *       default: 0: START 
     *       1: F_PASS_DOWN 
     *       2: FS_PASS_DOWN 
     *     }
     * </pre>
     */
    private void genPrelude(MethodVisitor mv) {
        assert isPausable : "MethodWeaver.genPrelude called for nonPausable method";
        if (callWeavers.size() == 0 && (!hasGetCurrentTask())) {
            // Method has been marked pausable, but does not call any pausable methods, nor Task.getCurrentTask.  
            // Prelude is not needed at all.
        	//shouldnt be possible to get here
        	throw new RuntimeException("genPrelude should not reach this block");//TODO: remove this
        }
        
        MethodFlow mf = methodFlow;
        // load fiber from last var
        int lastVar = getFiberArgVar();
       
        mv.visitVarInsn(ALOAD, lastVar);
        if (lastVar < fiberVar) {
            if (callWeavers.size() > 0) {
                mv.visitInsn(DUP); // for storing into fiberVar
            }
            mv.visitVarInsn(ASTORE, getFiberVar());
        }
        
        if (callWeavers.size() == 0) {
          // No pausable method calls, but Task.getCurrentTask() is present. 
          // We don't need the rest of the prelude.
           return; 
        }

        mv.visitFieldInsn(GETFIELD, FIBER_CLASS, "pc", D_INT);
        // The prelude doesn't need more than two words in the stack.
        // The callweaver gen* methods may need more. 
        ensureMaxStack(2);

        // switch stmt
        LabelNode startLabel = mf.getOrCreateLabelAtPos(0);
        LabelNode errLabel = new LabelNode();
        
        LabelNode[] labels = new LabelNode[callWeavers.size() + 1];
        labels[0] = startLabel;
        for (int i = 0; i < callWeavers.size(); i++) {
            labels[i + 1] = new LabelNode();
        }
        new TableSwitchInsnNode(0, callWeavers.size(), errLabel, labels).accept(mv);
        
        errLabel.accept(mv);
        mv.visitVarInsn(ALOAD, getFiberVar());
        mv.visitLdcInsn(mf.name);
        mv.visitMethodInsn(INVOKEVIRTUAL, FIBER_CLASS, "wrongPC", "(Ljava/lang/String;)V", false);
        // Generate pass through down code, one for each pausable method
        // invocation
        int last = callWeavers.size() - 1;
        for (int i = 0; i <= last; i++) {
            MethodInvokationWeaver cw = callWeavers.get(i);
            labels[i+1].accept(mv);
            cw.genRewind(mv);
        }
        startLabel.accept(mv);
    }

    boolean isStatic() {
        return methodFlow.isStatic();
    }

    int getFiberArgVar() {
        int lastVar = getNumWordsInSig();
        if (!isStatic()) {
            lastVar++;
        }
        return lastVar;
    }

    /*
     * The number of words in the argument; doubles/longs occupy
     * two local vars.
     */
    int getNumWordsInSig() {
        if (numWordsInSig != -1) {
            String[]args = TypeDesc.getArgumentTypes(methodFlow.desc);
            int size = 0;
            for (int i = 0; i < args.length; i++) {
                size += TypeDesc.isDoubleWord(args[i]) ? 2 : 1;
            }
            numWordsInSig = size;
        }
        return numWordsInSig;
    }

    /**
     * Generate code for only those catch blocks that are reachable
     * from one or more pausable blocks. fiber.pc tells us which
     * nested call possibly caused an exception, fiber.status tells us
     * whether there is any state that needs to be restored, and
     * fiber.curState gives us access to that state. 
     * 
     * ; Figure out which pausable method could have caused this.
     * 
     * switch (fiber.upEx()) {
     *    0: goto NORMAL_EXCEPTION_HANDLING;
     *    2: goto RESTORE_F
     * }
     * RESTORE_F:
     *   if (fiber.curStatus == HAS_STATE) {
     *      restore variables from the state. don't restore stack
     *      goto NORMAL_EXCEPTION_HANDLING
     *   }
     * ... other RESTOREs
     * 
     * NORMAL_EXCEPTION_HANDLING:
     */
    private void genException(MethodVisitor mv, BasicBlock bb, List<MethodInvokationWeaver> cwList) {
        bb.startLabel.accept(mv);
        LabelNode resumeLabel = new LabelNode();
        VMType.loadVar(mv, VMType.TOBJECT, getFiberVar());
        //mv.visitLdcInsn(bb.flow.name);
        // mv.visitMethodInsn(INVOKEVIRTUAL, FIBER_CLASS, "upEx", "(Ljava/lang/String;)I", false);
        mv.visitMethodInsn(INVOKEVIRTUAL, FIBER_CLASS, "upEx", "()I", false);
        // fiber.pc is on stack
        LabelNode[] labels = new LabelNode[cwList.size()];
        int[] keys = new int[cwList.size()];
        for (int i = 0; i < cwList.size(); i++) {
            labels[i] = new LabelNode();
            keys[i] = callWeavers.indexOf(cwList.get(i)) + 1;
        }
        
        new LookupSwitchInsnNode(resumeLabel, keys, labels).accept(mv);
        int i = 0;
        for (MethodInvokationWeaver cw: cwList) {
            if (i > 0) {
                // This is the jump (to normal exception handling) for the previous
                // switch case.
                mv.visitJumpInsn(GOTO, resumeLabel.getLabel());
            }
            labels[i].accept(mv);
            cw.genRestoreEx(mv, labels[i]);
            i++;
        }
        
        // Consume the first instruction because we have already consumed the
        // corresponding label. (The standard visitInstructions code does a 
        // visitLabel before visiting the instruction itself)
        resumeLabel.accept(mv);
        bb.getInstruction(bb.startPos).accept(mv);
    }
    
    int getFiberVar() {
        return fiberVar; // The first available slot
    }

    void visitTryCatchBlocks(MethodVisitor mv) {
        MethodFlow mf = methodFlow;
        ArrayList<BasicBlock> bbs = mf.getBasicBlocks();
        ArrayList<Handler> allHandlers = new ArrayList<Handler>(bbs.size() * 2);
        for (BasicBlock bb : bbs) {
            allHandlers.addAll(bb.handlers);
        }
        //allHandlers = Handler.consolidate(allHandlers);
        //trying to be too clever above
        for (Handler h : allHandlers) {
            new TryCatchBlockNode(mf.getLabelAt(h.from), mf.getOrCreateLabelAtPos(h.to+1), h.catchBB.startLabel, h.type).accept(mv);
        }
    }

    void ensureMaxVars(int numVars) {
        if (numVars > maxVars) {
            maxVars = numVars;
        }
    }

    void ensureMaxStack(int numStack) {
        if (numStack > maxStack) {
            maxStack = numStack;
        }
    }

    int getPC(MethodInvokationWeaver weaver) {
        for (int i = 0; i < callWeavers.size(); i++) {
            if (callWeavers.get(i) == weaver)
                return i + 1;
        }
        assert false : " No weaver found";
        return 0;
    }

    public String createStateClass(ValInfoList valInfoList) {
        return classWeaver.createStateClass(valInfoList);
    }
    
    private void invokeDynamicOnLambda(MethodVisitor mv, AbstractInsnNode ain) {
        InvokeDynamicInsnNode indy = (InvokeDynamicInsnNode)ain;
        Object[]bsmArgs = indy.bsmArgs;
        // Is it a lambda conversion
        if (indy.bsm.getOwner().equals("java/lang/invoke/LambdaMetafactory")) {
            Handle lambdaBody = (Handle)bsmArgs[1];
            Detector detector = this.methodFlow.detector();
            String desc = lambdaBody.getDesc();
            //if (detector.isPausable(lambdaBody.getOwner(), lambdaBody.getName(), desc)) {
            //if(!lambdaBody.getName().equals("<init>")) {
            	bsmArgs[0] = addFiberType((org.objectweb.asm.Type)bsmArgs[0]);
                bsmArgs[1] = new Handle(lambdaBody.getTag(), 
                                        lambdaBody.getOwner(), 
                                        lambdaBody.getName(), 
                                        desc.replace(")", D_FIBER + ")"));
                bsmArgs[2] = addFiberType((org.objectweb.asm.Type)bsmArgs[2]);
            //}
                
            //}
        }
        ain.accept(mv);
    }
    
    private static org.objectweb.asm.Type addFiberType(org.objectweb.asm.Type type) {
        String typeDesc = type.toString().replace(")", D_FIBER + ")");
        return org.objectweb.asm.Type.getType(typeDesc);
    }
    
}


