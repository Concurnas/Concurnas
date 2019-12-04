/* Copyright (c) 2006, Sriram Srinivasan
 *
 * You may distribute this software under the terms of the license 
 * specified in the file "License"
 */

package com.concurnas.runtime.cps.analysis;
import static com.concurnas.runtime.cps.analysis.BasicBlock.COALESCED;
import static com.concurnas.runtime.cps.analysis.BasicBlock.ENQUEUED;
import static com.concurnas.runtime.cps.analysis.BasicBlock.INLINE_CHECKED;
import static org.objectweb.asm.Opcodes.ACC_STATIC;
import static org.objectweb.asm.Opcodes.ACC_VOLATILE;
import static org.objectweb.asm.Opcodes.JSR;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.PriorityQueue;
import java.util.TreeMap;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.FrameNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LineNumberNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TryCatchBlockNode;

import com.concurnas.runtime.FixedSizeFrame;
import com.concurnas.runtime.cps.CPSException;
import com.concurnas.runtime.cps.mirrors.ClassMirror;
import com.concurnas.runtime.cps.mirrors.ClassMirrorNotFoundException;
import com.concurnas.runtime.cps.mirrors.Detector;


/** 
 * This represents all the basic blocks of a method. 
 */
public class MethodFlow extends MethodNode {
    
	
    /**
     * The classFlow to which this methodFlow belongs
     */
    
    ClassFlow                  classFlow;
    
    /**
     * Maps instructions[i] to LabelNode or null (if no label). Note that
     * LabelInsnNodes are not accounted for here because they themselves are not
     * labelled.
     */
    
    private ArrayList<LabelNode>           posToLabelList;
    
    /**
     * Reverse map of posToLabelMap. Maps Labels to index within
     * method.instructions.
     */
    private HashMap<LabelNode, Integer>    labelToPosMap;
    
    /**
     * Maps labels to BasicBlocks
     */
    private HashMap<LabelNode, BasicBlock> labelToBBMap;
    
    /**
     * The list of basic blocks, in the order in which they occur in the class file.
     * Maintaining this order is important, because we'll use it to drive duplication (in case
     * of JSRs) and also while writing out the class file.
     */
    private BBList      basicBlocks;
    
    private PriorityQueue<BasicBlock>          workset;
    
    private boolean hasPausableAnnotation;
    private boolean suppressPausableCheck;

    private HashSet<MethodInsnNode> pausableMethods = new HashSet<MethodInsnNode>();
    
	private final Detector detector;

    private TreeMap<Integer, LineNumberNode> lineNumberNodes = new TreeMap<Integer, LineNumberNode>();
    private HashMap<Integer, FrameNode> frameNodes = new HashMap<Integer, FrameNode>();
    
    private Boolean overridePrimarch =null;
    private InsnList origonalInstructions;
    
    public MethodFlow(
            ClassFlow classFlow,
            final int access,
            final String name,
            final String desc,
            final String signature,
            final String[] exceptions,
            final Detector detector) {
        super(Opcodes.ASM7, access, name, desc, signature, exceptions);
        this.classFlow = classFlow;
        this.detector = detector;
        posToLabelList = new ArrayList<LabelNode>();
        labelToPosMap = new HashMap<LabelNode, Integer>();
        labelToBBMap = new HashMap<LabelNode, BasicBlock>();

        /*if (exceptions != null && exceptions.length > 0) {
            for (String e: exceptions) { 
                if (e.equals(PAUSABLE_CLASS)) {
                    hasPausableAnnotation = true;
                    break;
                } else if (e.equals(NOT_PAUSABLE_CLASS)) {
                    suppressPausableCheck = true;
                }
            }
        }*/
        hasPausableAnnotation = true;
    }

    public void restoreNonInstructionNodes() {
        InsnList newinsns = new InsnList();
        
        InsnList iii = origonalInstructions!=null?origonalInstructions:instructions;
        //InsnList iii = instructions;
        
        int sz = iii.size();
        for (int i = 0; i < sz; i++) {
            LabelNode l = getLabelAt(i);
            if (l != null) {
                newinsns.add(l);
            }
            LineNumberNode ln = lineNumberNodes.get(i);
            if (ln != null) {
                newinsns.add(ln);
            }
            AbstractInsnNode ain = iii.get(i);
            newinsns.add(ain);
        }
        
        LabelNode l = getLabelAt(sz);
        if (l != null) {
            newinsns.add(l);
        }
        LineNumberNode ln = lineNumberNodes.get(sz);
        if (ln != null) {
            newinsns.add(ln);
        }
        super.instructions = newinsns;
    }
    
    public boolean getOverridesPrimordialMethod(){//i.e. doings(x) => doings(x, Fiber)
    	//if not then one must extract the Fiber via Static invokation Slower)
    	//if(null == overridePrimarch){
    	//	overridePrimarch=detector.overridesPrimordialMethod(classFlow.superName, name, desc);    		
    	//}
    	//return overridePrimarch;
    	return false;
    }
    
    
    public void analyze() throws CPSException {
    	this.origonalInstructions = this.instructions;
    	
        buildBasicBlocks();
        if (basicBlocks.size() == 0) return;
        consolidateBasicBlocks();
        assignCatchHandlers();
        inlineSubroutines();
        doLiveVarAnalysis();
        dataFlow();
        this.labelToBBMap = null; // we don't need this mapping anymore
    }

    public void verifyPausables() throws CPSException {
        // If we are looking at a woven file, we don't need to verify
        // anything
    	//TODO: seems a bit OTT
        if (classFlow.isWoven || suppressPausableCheck) return;
        
        if (!hasPausableAnnotation && !pausableMethods.isEmpty()) {
            String msg;
            String name = toString(classFlow.getClassName(),this.name,this.desc);   
            if (this.name.endsWith("init>")) {
                msg = "Constructor " + name + " calls pausable methods:\n";
            } else { 
                msg = name + " should be marked pausable. It calls pausable methods\n";
            }
            for (MethodInsnNode min: pausableMethods) {
                msg += toString(min.owner, min.name, min.desc) + '\n';
            }
            throw new CPSException(msg);
        }
        if (classFlow.superName != null) {
            //checkStatus(classFlow.superName, name, desc);
        }
        if (classFlow.interfaces != null) {
            for (Object ifc: classFlow.interfaces) {
               // checkStatus((String) ifc, name, desc);
            }
        }
    }

/*    private void checkStatus(String superClassName, String methodName, String desc) throws CPSException {
        int status = detector.getPausableStatus(superClassName, methodName, desc);
        if ((status == Detector.PAUSABLE_METHOD_FOUND && !hasPausableAnnotation)) {
            throw new CPSException("Base class method is pausable, derived class is not: " +
                    "\nBase class = " + superClassName +
                    "\nDerived class = " + this.classFlow.name +
                    "\nMethod = " + methodName + desc);
        } 
        if (status == Detector.METHOD_NOT_PAUSABLE && hasPausableAnnotation) {
            throw new CPSException("Base class method is not pausable, but derived class is: " +
                    "\nBase class = " + superClassName +
                    "\nDerived class = " + this.classFlow.name +
                    "\nMethod = " + methodName + desc);           
        }
    }*/

    private String toString(String className, String methName, String desc) {
        return className.replace('/', '.') + '.' + methName + desc;
    }
    
    private HashMap<String, Integer> classToNestingLevels = new HashMap<String, Integer>();

	public boolean hasInvokeDynamicCall;
    
    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String desc) {
        super.visitMethodInsn(opcode, owner, name, desc);
        // The only reason for adding to pausableMethods is to create a BB for pausable
        // method call sites. If the class is already woven, we don't need this 
        // functionality.
        
        if (!classFlow.isWoven) {
            boolean invokedCallIsStackPausable = detector.invokedCallIsStackPausable(owner, name, desc);
            //if (methodStatus == Detector.PAUSABLE_METHOD_FOUND) {
            if (invokedCallIsStackPausable) {
                MethodInsnNode min = (MethodInsnNode)instructions.get(instructions.size()-1);
                pausableMethods.add(min);
            }
            
            if(!classToNestingLevels.containsKey(owner)){
            	classToNestingLevels.put(owner, detector.getNestingLevels(owner));
            }
        }
    }
    
    public int getNestingLevel(String name){
    	try{
    		//next line is a bit of a hack...
    		if(!classToNestingLevels.containsKey(name)){return detector.getNestingLevels(name);}
    		
    		return classToNestingLevels.get(name);
    	}
    	catch(Exception e){
    		throw e;
    	}
    }
    
    
    @Override
    public void visitLabel(Label label) {
        setLabel(instructions.size(), super.getLabelNode(label));
    }
    
    @Override
    public void visitLineNumber(int line, Label start) {
        LabelNode ln = getLabelNode(start);
        lineNumberNodes.put(instructions.size(), new LineNumberNode(line, ln));
    }

    void visitLineNumbers(MethodVisitor mv) {
        for (LineNumberNode node : lineNumberNodes.values()) {
            mv.visitLineNumber(node.line, node.start.getLabel());
        }
    }

    
    @Override
    public void visitFrame(int type, int nLocal, Object[] local, int nStack,
            Object[] stack) {
        frameNodes.put(instructions.size(), new FrameNode(type, nLocal, local, nStack, stack));
    }
        
    private void inlineSubroutines() throws CPSException {
        markPausableJSRs();
        while (true) {
            ArrayList<BasicBlock> newBBs = null;
            for (BasicBlock bb: basicBlocks) {
                if (bb.hasFlag(INLINE_CHECKED)) continue;
                bb.setFlag(INLINE_CHECKED);
                if (bb.lastInstruction() == JSR) {
                    newBBs = bb.inline();
                    if (newBBs != null) {
                        break;
                    }
                }
            }
            if (newBBs == null) { 
                break;
            }
            int id = basicBlocks.size();
            for (BasicBlock bb: newBBs) {
                bb.setId(id++);
                basicBlocks.add(bb);
            }
        }
        // If there are any pausable subroutines, modify the JSRs/RETs to
        // GOTOs
        for (BasicBlock bb: basicBlocks) {
            bb.changeJSR_RET_toGOTOs();
        }
        
    }
    
    private void markPausableJSRs() throws CPSException {
        for (BasicBlock bb: basicBlocks) {
            bb.checkPausableJSR();
        }
    }
    
    private static final HashMap<String, HashSet<String>> excludeCorouteenExcept = new HashMap<String, HashSet<String>>();
    static{//things of this type of subclassse of this are not corouteen elegiable (no fiberization)
    	excludeCorouteenExcept.put("java/io/InputStream", new HashSet<String>());
    	excludeCorouteenExcept.put("java/lang/reflect/Array", new HashSet<String>());
    	excludeCorouteenExcept.put("java/io/OutputStream", new HashSet<String>());
    	excludeCorouteenExcept.put("java/lang/annotation/Annotation", new HashSet<String>());
    	excludeCorouteenExcept.put("com/sun/imageio/plugins/tiff/TIFFT6Compressor", new HashSet<String>());//err in transformation
    	excludeCorouteenExcept.put("java/lang/System", new HashSet<String>());
    	HashSet<String> except = new HashSet<String>();
    	except.add("enumConstantDirectory");
    	except.add("getEnumConstantsShared");
    	excludeCorouteenExcept.put("java/lang/Class", except);
    	excludeCorouteenExcept.put("sun/reflect/Reflection", new HashSet<String>());
    	
    	excludeCorouteenExcept.put("com/concurnas/runtime/ConcClassUtil", new HashSet<String>());
    	excludeCorouteenExcept.put("com/concurnas/runtime/ConcurnasClassLoader", new HashSet<String>());
    	
    	HashSet<String> strexcept = new HashSet<String>();//these static methods of String just pass though
    	strexcept.add("valueOf");
    	strexcept.add("join");
    	strexcept.add("format");
    	strexcept.add("copyValueOf");
    	excludeCorouteenExcept.put("java/lang/String", strexcept);
    	
    	excludeCorouteenExcept.put("java/lang/Number", new HashSet<String>());
    	excludeCorouteenExcept.put("java/lang/Integer", new HashSet<String>());
    	excludeCorouteenExcept.put("java/lang/Long", new HashSet<String>());
    	excludeCorouteenExcept.put("java/lang/Double", new HashSet<String>());
    	excludeCorouteenExcept.put("java/lang/Float", new HashSet<String>());
    	excludeCorouteenExcept.put("java/lang/Character", new HashSet<String>());
    	excludeCorouteenExcept.put("java/lang/Math", new HashSet<String>());
    	excludeCorouteenExcept.put("java/lang/Short", new HashSet<String>());
    	excludeCorouteenExcept.put("java/lang/Byte", new HashSet<String>());
    	excludeCorouteenExcept.put("java/lang/Boolean", new HashSet<String>());
    	//excludeCorouteenExcept.put("java/lang/Thread", new HashSet<String>());
    	excludeCorouteenExcept.put("java/lang/ref/Reference", new HashSet<String>());
    	excludeCorouteenExcept.put("jdk/internal/misc/Unsafe", new HashSet<String>());
    }
    
    private static final ArrayList<String> excludeCorouteenAll = new ArrayList<String>();
    static {
    	excludeCorouteenAll.add("java/net");
    	excludeCorouteenAll.add("jdk/internal/reflect");
    	excludeCorouteenAll.add("java/lang/Throwable");
    	excludeCorouteenAll.add("com/concurnas/lang/LangExt");
    	//VarHandle etc we don't want to add a fiber to:
    	excludeCorouteenAll.add("java/lang/invoke");
    	
    	//these classes are used by lambdas which are declared within platform level static initalizers...
    	excludeCorouteenAll.add("java/util/regex/Pattern$CharPredicate");
    	excludeCorouteenAll.add("java/util/regex/Pattern");
    }
    

   /* private static final HashMap<String, HashSet<String>> excludeCorouteenAllExcept = new HashMap<String, HashSet<String>>();
    static {
    	HashSet<String> strexcept = new HashSet<String>();
    	strexcept.add("valueOf");
    	strexcept.add("join");
    	strexcept.add("format");
    	strexcept.add("copyValueOf");
    	excludeCorouteenAllExcept.put("java/lang/String", strexcept);
    }*/
    
    
    boolean isPausableMethodInsn(String name, String owner) {
    	//exclusions go here
    	if(owner.equals("java/lang/Object")){
    		return name.equals("toString") || name.equals("equals") || name.equals("hashCode");//these get conced only, the power 3
		}

    	if(this.classFlow.name.equals("com/concurnas/bootstrap/runtime/cps/CObject")){//don't call conc variant of stringbuilder in CObject
    		if( owner.equals("java/lang/StringBuilder") || owner.equals("java/lang/AbstractStringBuilder") ){
    			return false;
    		}
    	}
    	
    	if(name.equals("<init>")){
    		return false;
    	}
    	
		final String key = owner.replace('.', '/');
		if(excludeCorouteenAll.stream().anyMatch(x -> key.startsWith(x))) {
			return false;
		}
		
		try {
    		//anti fiberization list
    		//we're doing this one wrong: java.lang.Class.getRawAnnotations(Lcom/concurnas/bootstrap/runtime/cps/Fiber;)[B+1
    		//TODO: core classes should probably be skipped above in addition to object and throwable
			
			ArrayList<String> scs = detector.getSuperClasses(owner);
			
			for(String supCls : scs){
				if(supCls.equals("com/concurnas/bootstrap/runtime/cps/CObject")){//we know that everything higher than this is permissable
					break;
				}
				else {
					String keyx = supCls.replace('.', '/');
					if(excludeCorouteenExcept.containsKey(keyx)){
						HashSet<String> except = excludeCorouteenExcept.get(keyx);
						return except.contains(name);
					}
				}
			}
			
		} catch (ClassMirrorNotFoundException e) {
			throw new RuntimeException(e);
		}
    	
    	try {//dont fiberize annotations
			ClassMirror theMirror = this.detector.classForName(owner);
			if(null != theMirror){
				for(String iface : theMirror.getInterfaces()){
					if(iface.equals("java/lang/annotation/Annotation")){
						return false;
					}
				}
			}
		} catch (ClassMirrorNotFoundException e) {
			
		}
    	
    	if( owner.equals("java/security/ProtectionDomain")){
    		return false;
    	}
    	
    	//special exceptions just for Fiber etc
    	return true;
        //return pausableMethods.contains(min);
    }
    
    @Override
    public String toString() {
        ArrayList<BasicBlock> ret = getBasicBlocks();
        Collections.sort(ret);
        return ret.toString();
    }
    
    public BBList getBasicBlocks() {
        return basicBlocks;
    }
    
	public String getUniqueName() {
		if(null == basicBlocks || basicBlocks.isEmpty()){
			//no code?
			return "";
		}
		BasicBlock header = basicBlocks.get(0);
		return header.flow.name + header.flow.desc;
	}
	
	public String getDesc(){
		return basicBlocks.get(0).flow.desc;
	}
	
	public String getMethodName(){
		return basicBlocks.get(0).flow.name;
	}
    
    private void assignCatchHandlers() {
        @SuppressWarnings("unchecked")
        ArrayList<TryCatchBlockNode> tcbs = (ArrayList<TryCatchBlockNode>) tryCatchBlocks;
        /// aargh. I'd love to create an array of Handler objects, but generics
        // doesn't care for it.
        if (tcbs.size() == 0) return;
        ArrayList<Handler> handlers= new ArrayList<Handler>(tcbs.size());
        
        for (int i = 0; i < tcbs.size(); i++) {
            TryCatchBlockNode tcb = tcbs.get(i);
            handlers.add(new Handler(
                    getLabelPosition(tcb.start),
                    getLabelPosition(tcb.end) - 1, // end is inclusive
                    tcb.type, 
                    getOrCreateBasicBlock(tcb.handler)));
        }
        for (BasicBlock bb : basicBlocks) {
            bb.chooseCatchHandlers(handlers);
        }
    }
    
    void buildBasicBlocks() {
        // preparatory phase
        int numInstructions = instructions.size(); 
        
        basicBlocks = new BBList();
        // Note: i modified within the loop
        //String added = "";
        for (int i = 0; i < numInstructions; i++) {
            LabelNode l = getOrCreateLabelAtPos(i);
            BasicBlock bb = getOrCreateBasicBlock(l);
            i = bb.initialize(i); // i now points to the last instruction in bb. 
            if(bb.hasInvokeDynamicCall) {
            	this.hasInvokeDynamicCall = true;
            }
            basicBlocks.add(bb);
            //added += String.format(" [%s -> %s]\n", bb.startPos, bb.endPos);
        }
        //System.err.println("name: " + this.name + " instru: " + numInstructions + "::" + added);
    }
    
    /**
     * In live var analysis a BB asks its successor (in essence) about which
     * vars are live, mixes it with its own uses and defs and passes on a
     * new list of live vars to its predecessors. Since the information
     * bubbles up the chain, we iterate the list in reverse order, for
     * efficiency. We could order the list topologically or do a depth-first
     * spanning tree, but it seems like overkill for most bytecode
     * procedures. The order of computation doesn't affect the correctness;
     * it merely changes the number of iterations to reach a fixpoint.
     */
    private void doLiveVarAnalysis() {
        ArrayList<BasicBlock> bbs = getBasicBlocks();
        Collections.sort(bbs); // sorts in increasing startPos order
        
        boolean changed;
        do {
            changed = false;
            for (int i = bbs.size() - 1; i >= 0; i--) {
                changed = bbs.get(i).flowVarUsage() || changed;
            }
        } while (changed);
    }
    
    /**
     * In the first pass (buildBasicBlocks()), we create BBs whenever we
     * encounter a label. We don't really know until we are done with that
     * pass whether a label is the target of a branch instruction or it is
     * there because of an exception handler. See coalesceWithFollowingBlock()
     * for more detail.  
     */
    private void consolidateBasicBlocks() {
        BBList newBBs = new BBList(basicBlocks.size());
        int pos = 0;
        for (BasicBlock bb: basicBlocks) {
            if (!bb.hasFlag(COALESCED)) {
                bb.coalesceTrivialFollowers();
                // The original bb's followers should have been marked as processed.
                bb.setId(pos++);  
                newBBs.add(bb);
            }
        }
        basicBlocks = newBBs;
        assert checkNoBasicBlockLeftBehind();
    }
    
    private boolean checkNoBasicBlockLeftBehind() { // like "no child left behind"
        ArrayList<BasicBlock> bbs = basicBlocks;
        HashSet<BasicBlock> hs = new HashSet<BasicBlock>(bbs.size() * 2);
        hs.addAll(bbs);
        int prevBBend = -1;
        for (BasicBlock bb: bbs) {
            assert bb.isInitialized() : "BB not inited: " + bb;
            assert bb.startPos == prevBBend + 1;
            for (BasicBlock succ: bb.successors) {
                assert succ.isInitialized() : "Basic block not inited: " + succ +"\nSuccessor of " + bb;
                assert hs.contains(succ) : 
                    "BB not found:\n" + succ; 
            }
            prevBBend = bb.endPos;
        }
        assert bbs.get(bbs.size()-1).endPos == instructions.size()-1;
        return true;
    }
    
    private void dataFlow() {
        workset = new PriorityQueue<BasicBlock>(instructions.size(), new BBComparator());
        //System.out.println("Method: " + this.name);
        BasicBlock startBB = getBasicBlocks().get(0);
        assert startBB != null : "Null starting block in flowTypes()";
        startBB.startFrame = new FixedSizeFrame(classFlow.getClassDescriptor(), this);
        enqueue(startBB);
        
        while (!workset.isEmpty()) {
            BasicBlock bb = dequeue();
            bb.interpret();
        }
    }
    
    void setLabel(int pos, LabelNode l) {
        for (int i = pos - posToLabelList.size() + 1; i >= 0; i--) {
            // pad with nulls ala perl
            posToLabelList.add(null);
        }
        posToLabelList.set(pos, l);
        labelToPosMap.put(l, pos);
    }
    
    LabelNode getOrCreateLabelAtPos(int pos) {
        LabelNode ret = null;
        if (pos < posToLabelList.size()) {
            ret = posToLabelList.get(pos);
        }
        if (ret == null) {
            ret = new LabelNode();
            setLabel(pos, ret);
        }
        return ret;
    }
    
    int getLabelPosition(LabelNode l) {
        return labelToPosMap.get(l);
    }
    
    BasicBlock getOrCreateBasicBlock(LabelNode l) {
        BasicBlock ret = labelToBBMap.get(l);
        if (ret == null) {
        	//anfTransform
        	
            ret = new BasicBlock(this, l);
            BasicBlock oldVal = labelToBBMap.put(l, ret);
            assert oldVal == null : "Duplicate BB created at label";
        }
        return ret;
    }

    BasicBlock getBasicBlock(LabelNode l) { 
        return labelToBBMap.get(l);
    }

    private BasicBlock dequeue() {
        BasicBlock bb = workset.poll();
        bb.unsetFlag(ENQUEUED);
        return bb;
    }
    
    void enqueue(BasicBlock bb) {
        assert bb.startFrame != null : "Enqueued null start frame";
        if (!bb.hasFlag(ENQUEUED)) {
            workset.add(bb);
            bb.setFlag(ENQUEUED);
        }
    }

    public LabelNode getLabelAt(int pos) {
        return  (pos < posToLabelList.size()) ? posToLabelList.get(pos) : null;
    }

    void addInlinedBlock(BasicBlock bb) {
        bb.setId(basicBlocks.size());
        basicBlocks.add(bb);
    }

    public int getNumArgs() {
        int ret = TypeDesc.getNumArgumentTypes(desc);
        if (!isStatic()) ret++;
        return ret;
    }
    
    public boolean isPausable() {
    	//TODO: exclude hashcode and equals
    	//if(name.equals("copy") || name.equals("<init>")){
    	if( name.equals("<init>")){
    		return false;
    		//return !this.desc.startsWith("(Lcom/concurnas/runtime/ConcurnificationTracker;");
    	}
    	/*if(name.equals("toString")){
    		System.err.println("this is pasuable:" + this.name);
    		
    		//cannot be paused in conventional manner if inherits from method of baseclasses (Object, String etc).
    	}*/
    	
    	return true;//getOverridesPrimordialMethod();
        //return hasPausableAnnotation;
    }
    
    public void setPausable(boolean isPausable) {
        hasPausableAnnotation = isPausable;
    }

    public static void acceptAnnotation(final AnnotationVisitor av, final String name,
            final Object value) {
        if (value instanceof String[]) {
            String[] typeconst = (String[]) value;
            av.visitEnum(name, typeconst[0], typeconst[1]);
        } else if (value instanceof AnnotationNode) {
            AnnotationNode an = (AnnotationNode) value;
            an.accept(av.visitAnnotation(name, an.desc));
        } else if (value instanceof List<?>) {
            AnnotationVisitor v = av.visitArray(name);
            List<?> array = (List<?>) value;
            for (int j = 0; j < array.size(); ++j) {
                acceptAnnotation(v, null, array.get(j));
            }
            v.visitEnd();
        } else {
            av.visit(name, value);
        }
    }

    public boolean isAbstract() {
        //return ((this.access & Opcodes.ACC_ABSTRACT) != 0);
    	return false;
    }
    public boolean isStatic() {
        return ((this.access & ACC_STATIC) != 0);
    }

    public boolean isBridge() {
        return ((this.access & ACC_VOLATILE) != 0);
    }

	public Detector detector() {
		return this.classFlow.detector();
}

    public void resetLabels() {
        for (int i = 0; i < posToLabelList.size(); i++) {
            LabelNode ln = posToLabelList.get(i);
            if (ln != null) {
                ln.resetLabel();
            }
        }
        
    }
}


