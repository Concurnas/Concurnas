/* Copyright (c) 2006, Sriram Srinivasan
 *
 * You may distribute this software under the terms of the license 
 * specified in the file "License"
 */

package com.concurnas.runtime.cps.tools;
import static com.concurnas.runtime.cps.analysis.Utils.dedent;
import static com.concurnas.runtime.cps.analysis.Utils.indent;
import static com.concurnas.runtime.cps.analysis.Utils.pn;
import static com.concurnas.runtime.cps.analysis.Utils.resetIndentation;
import static org.objectweb.asm.Opcodes.INVOKESTATIC;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;


import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;

import com.concurnas.runtime.FixedSizeFrame;
import com.concurnas.runtime.Value;
import com.concurnas.runtime.cps.analysis.BasicBlock;
import com.concurnas.runtime.cps.analysis.ClassFlow;
import com.concurnas.runtime.cps.analysis.MethodFlow;
import com.concurnas.runtime.cps.analysis.TypeDesc;
import com.concurnas.runtime.cps.analysis.Usage;
import com.concurnas.runtime.cps.mirrors.Detector;

/**
 * Used to dump the stack and locals at the beginning of each basic block
 * @author ram
 */
public class FlowAnalyzer {
    
    private static void stackTrace(Throwable t) {
        PrintStream ps = new PrintStream(System.out);
        t.printStackTrace(ps);
    }
    
    private static void reportFlow(MethodFlow method, String className) {
        resetIndentation();
        pn("Method : "+  className + '.' + method.name);
        
        pn("MaxStack: " + method.maxStack);
        pn("MaxLocals: " + method.maxLocals);
        ArrayList<BasicBlock> bbs = method.getBasicBlocks();
        Collections.sort(bbs);
        indent(2);
        for (BasicBlock bb: bbs) {
            AbstractInsnNode ainode = bb.getInstruction(bb.startPos);
            if (ainode instanceof MethodInsnNode) {
                MethodInsnNode m = (MethodInsnNode)ainode;
                int n = getNumArgs(m); // This many will get consumed from stack
                pn("Call(" + n + "): " + m.owner + "." + m.name + m.desc);
                indent(2);
                pn("Inframe: ");
                indent(2);
                FixedSizeFrame f = bb.startFrame;
                pn(f.toString());
                dedent(2);
                pn("Live locals:");
                indent(2);
                Usage u = bb.getVarUsage();
                pn(u.toString());
                dedent(2);
                pn("Actual usage: " + uniqueItems(bb, f, u, n));
                dedent(2);
            }
        }
        dedent(2);
    }
    
    private static String uniqueItems(BasicBlock bb, FixedSizeFrame f, Usage u, int nStack) {
        StringBuffer sb = new StringBuffer(80);
        int numNonConstants = 0;
        int numLive = 0;
        ArrayList<Value> set = new ArrayList<Value>(10);
        for (int i = 0; i < f.getMaxLocals(); i++) {
            if (u.isLiveIn(i)) {
                numLive++;
                Value v = f.getLocal(i);
                if (!set.contains(v)) set.add(v);
            }
        }
        nStack = f.getStackLen() - nStack;
        for (int i = 0; i < nStack; i++) {
            Value v = f.getStack(i);
            if (!set.contains(v)) set.add(v);
        }
        char[] sig = new char[set.size()];
        // create canonical sig. Convert types to one of 'O', 'I', 'F', 'L', 'D' and
        // put in sorted order
        // Also count non constants while we are iterating anyway.
        for (int i = 0; i < set.size(); i++) {
            Value v = set.get(i);
            char c = v.getTypeDesc().charAt(0);
            switch (c) {
                case 'L': case '[': case 'N': 
                    c = 'O'; break;
                case 'I': case 'B': case 'S': case 'Z': case 'C': 
                    c = 'I'; break;
                case 'J':
                    c = 'J'; break;
                case 'F': 
                    c = 'F'; break;
                case 'U': 
                    default: {
                    c = 'U';
                    System.err.println("***************************************");
                    System.err.println("Undefined/unrecognized value " + v);
                    System.err.println("BasicBlock:\n" + bb);
                    break;
                }
            }
            sig[i] = c;
            if (v.getConstVal() == Value.NO_VAL) {
                numNonConstants++;
            }
        }
        Arrays.sort(sig);
        numLive += nStack;
        sb.append("avail: ").append(nStack + f.getMaxLocals());
        sb.append(", live: " + numLive);
        sb.append(", unique: ").append(set.size());
        sb.append(", unique non-const: ").append(numNonConstants);
        sb.append("\nState signature: ").append(set.size() == 0 ? "None" : new String(sig));
        return sb.toString();
    }
    
    private static int getNumArgs(MethodInsnNode m) {
        int ret = TypeDesc.getNumArgumentTypes(m.desc);
        if (m.getOpcode() != INVOKESTATIC) ret++; 
        return ret;
    }
    
}
