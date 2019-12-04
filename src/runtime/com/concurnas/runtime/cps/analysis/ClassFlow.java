/* Copyright (c) 2006, Sriram Srinivasan
 *
 * You may distribute this software under the terms of the license 
 * specified in the file "License"
 */
package com.concurnas.runtime.cps.analysis;

import java.util.ArrayList;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;


import com.concurnas.runtime.cps.CPSException;
import com.concurnas.runtime.cps.analysis.Fiberizer.OrigCode;
import com.concurnas.runtime.cps.mirrors.Detector;

/**
 * This class reads a .class file (or stream), wraps each method with a MethodFlow object and optionally analyzes it.
 * 
 */
public class ClassFlow extends ClassNode {
    ArrayList<MethodFlow> methodFlows;
    ClassReader           cr;
    ClassReader           crOrig;
    String                classDesc;
    /**
     * true if any of the methods contained in the class file is pausable. ClassWeaver uses it later to avoid weaving if
     * isPausable isn't true.
     */
    private boolean       isPausable;

    /**
     * true if the .class being read is already woven.
     */
    public boolean        isWoven = false;
    private Detector      detector;

    private final OrigCode origCode;
    private final OrigCode origCode2;
    private final OrigCode origCode3;

    public ClassFlow(byte[] data, Detector detector, OrigCode origCode, OrigCode origCode2, OrigCode origCode3) {
    	super(Opcodes.ASM7);
    	cr = new ClassReader(data);
        //crOrig = new ClassReader(data);
    	this.detector = detector;
        this.origCode = origCode;
        this.origCode2 = origCode2;
        this.origCode3 = origCode3;
    }


    @Override
    @SuppressWarnings( { "unchecked" })
    public MethodVisitor visitMethod(
            final int access,
            final String name,
            final String desc,
            final String signature,
            final String[] exceptions)
    {
        MethodFlow mn = new MethodFlow( this, access, name,  desc, signature,
                exceptions, detector);
        
        super.methods.add(mn);
        return mn;
    }

    public ArrayList<MethodFlow> getMethodFlows() {
        assert (methodFlows != null) : "ClassFlow.analyze not called";
        return methodFlows;
    }
    
    
    public ArrayList<MethodFlow> analyze(boolean forceAnalysis) throws CPSException {
        // cr.accept(this, ClassReader.SKIP_DEBUG);

        Detector save = Detector.setDetector(detector);
        try {
        	
        	cr.accept(origCode, ClassReader.SKIP_FRAMES);
        	
        	
            cr.accept(origCode, ClassReader.SKIP_FRAMES);
            cr.accept(origCode2, ClassReader.SKIP_FRAMES);//JPT:wow so lazy...
            cr.accept(origCode3, ClassReader.SKIP_FRAMES);//JPT:wow so lazy...
        	cr.accept(this, /*flags*/ClassReader.SKIP_FRAMES); //sets instructions of methods
        	
            if (isWoven && !forceAnalysis) 
                return new ArrayList<MethodFlow>(); // This is a hack. 


            //cr = null; // We don't need this any more.
            classDesc = TypeDesc.getInterned("L" + name + ';');
            ArrayList<MethodFlow> flows = new ArrayList<MethodFlow>(methods.size());
            String msg = "";
            for (Object o : methods) {
                try {
                    MethodFlow mf = (MethodFlow) o;
                    
                    if (mf.isBridge()) {
                        MethodFlow mmf = getOrigWithSameSig(mf);
                        if (mmf != null)
                            mf.setPausable(mmf.isPausable());
                    }
                    mf.verifyPausables();
                    if (mf.isPausable())
                        isPausable = true;
                    if ((mf.isPausable() || forceAnalysis) && (!mf.isAbstract())) {
                        mf.analyze();
                    }
                    flows.add(mf);
                } catch (CPSException ke) {
                    msg = msg + ke.getMessage() + "\n-------------------------------------------------\n";
                    throw ke;
                }
            }
            if (msg.length() > 0) {
                throw new CPSException(msg);
            }
            methodFlows = flows;
            return flows;

        } finally {
            Detector.setDetector(save);
        }
    }

    private MethodFlow getOrigWithSameSig(MethodFlow bridgeMethod) {
        for (Object o : methods) {
            MethodFlow mf = (MethodFlow) o;
            if (mf == bridgeMethod)
                continue;
            if (mf.name.equals(bridgeMethod.name)) {
                String mfArgs = mf.desc.substring(0, mf.desc.indexOf(')'));
                String bmArgs = bridgeMethod.desc.substring(0, bridgeMethod.desc.indexOf(')'));
                if (mfArgs.equals(bmArgs))
                    return mf;
            }
        }
        return null;
        // throw new AssertionError("Bridge method found, but original method does not exist\nBridge method:" +
        // this.name + "::" + bridgeMethod.name + bridgeMethod.desc);
    }

    public String getClassDescriptor() {
        return classDesc;
    }

    public String getClassName() {
        return super.name.replace('/', '.');
    }

    public boolean isPausable() {
        getMethodFlows(); // check analyze has been run.
        return isPausable;
    }

    boolean isInterface() {
        return (this.access & Opcodes.ACC_INTERFACE) != 0;
    }

    public Detector detector() {
        return detector;
    }
}
