package com.concurnas.compiler.bytecode;

import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Stack;

import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.IincInsnNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MultiANewArrayInsnNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

import com.concurnas.runtime.BytecodeStackFrameModifier;
import com.concurnas.runtime.OnCompileFrame;
import com.concurnas.runtime.Value;

/**
 * Wrapper around method visitor, adds frame state tracking
 * 
 * @author Jason
 * 
 */
public class FrameSateTrackingMethodVisitor extends MethodVisitor implements Opcodes {

	public OnCompileFrame currentFrame = new OnCompileFrame();

	public FrameSateTrackingMethodVisitor(MethodVisitor mv, int access, String desc, String classname) {
		super(Opcodes.ASM7, mv);
		
		boolean isStatic = Modifier.isStatic(access);
		int space = 0;
		if(!isStatic){
			currentFrame.setLocal(space, Value.make(space, classname));
			space++;
		}
		
		Type[] types = Type.getArgumentTypes(desc);
		for(Type t: types){
			currentFrame.setLocal(space, Value.make(space, t.toString()));
			space+=t.getSize();
		}
		
	}

	public void visitParameter(String name, int access) {
		super.visitParameter(name, access);
	}

	/*public AnnotationVisitor visitAnnotationDefault() {
		return super.visitAnnotationDefault();
	}

	public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
		return super.visitAnnotation(desc, visible);
	}

	public AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath, String desc, boolean visible) {
		return super.visitTypeAnnotation(typeRef, typePath, desc, visible);
	}

	public AnnotationVisitor visitParameterAnnotation(int parameter, String desc, boolean visible) {
		return super.visitParameterAnnotation(parameter, desc, visible);
	}

	public void visitAttribute(Attribute attr) {
		super.visitAttribute(attr);
	}

	public void visitCode() {
		super.visitCode();
	}

	public void visitFrame(int type, int nLocal, Object[] local, int nStack, Object[] stack) {
		super.visitFrame(type, nLocal, local, nStack, stack);
	}*/

	public void visitInsn(int opcode) {
		BytecodeStackFrameModifier.intepret(0, opcode, new InsnNode(opcode), currentFrame, "");
		super.visitInsn(opcode);
	}

	public void visitIntInsn(int opcode, int operand) {
		BytecodeStackFrameModifier.intepret(0, opcode, new IntInsnNode(opcode, operand), currentFrame, "");
		super.visitIntInsn(opcode, operand);
	}

	public void visitVarInsn(int opcode, int var) {
		BytecodeStackFrameModifier.intepret(0, opcode, new VarInsnNode(opcode, var), currentFrame, "");
		super.visitVarInsn(opcode, var);
	}

	public void visitTypeInsn(int opcode, String type) {
		BytecodeStackFrameModifier.intepret(0, opcode, new TypeInsnNode(opcode, type), currentFrame, "");
		super.visitTypeInsn(opcode, type);
	}

	public void visitFieldInsn(int opcode, String owner, String name, String desc) {
		BytecodeStackFrameModifier.intepret(0, opcode, new FieldInsnNode(opcode,  owner,  name,  desc), currentFrame, "");
		super.visitFieldInsn(opcode, owner, name, desc);
	}

	/*@Deprecated
	public void visitMethodInsn(int opcode, String owner, String name, String desc) {
		BytecodeStackFrameModifier.intepret(0, opcode, new MethodInsnNode(opcode, owner, name, desc), currentFrame, "");
		super.visitMethodInsn(opcode, owner, name, desc);
	}*/

	public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
		BytecodeStackFrameModifier.intepret(0, opcode, new MethodInsnNode(opcode, owner, name, desc, itf), currentFrame, "");
		super.visitMethodInsn(opcode, owner, name, desc, itf);
	}

	public void visitInvokeDynamicInsn(String name, String desc, Handle bsm, Object... bsmArgs) {
		super.visitInvokeDynamicInsn(name, desc, bsm, bsmArgs);
	}

	//lableToFrameState - on GOTO and IF_JUMP statements we must restore the current state of the stack prior to triggering the goto
	//on entry to the destination of the goto
	private HashMap<Label, Stack<Value>> lableToFrameState = new HashMap<Label, Stack<Value>>();
	
	public void visitJumpInsn(int opcode, Label label) {
		BytecodeStackFrameModifier.intepret(0, opcode, new JumpInsnNode(opcode, new LabelNode(label)), currentFrame, "");
		
		lableToFrameState.put(label, this.currentFrame.copyStack());
		
		super.visitJumpInsn(opcode, label);
	}
	
	//restore state here
	public void visitLabel(Label label) {
		super.visitLabel(label);
		Stack<Value> stackStateToRestore = lableToFrameState.get(label);
		if(null != stackStateToRestore){
			this.currentFrame.stack = stackStateToRestore;
		}
	}

	public void visitLdcInsn(Object cst) {
		BytecodeStackFrameModifier.intepret(0, LDC, new LdcInsnNode(cst), currentFrame, "");
		super.visitLdcInsn(cst);
	}

	public void visitIincInsn(int var, int increment) {
		BytecodeStackFrameModifier.intepret(0, IINC, new IincInsnNode(var, increment), currentFrame, "");
		super.visitIincInsn(var, increment);
	}

	public void visitTableSwitchInsn(int min, int max, Label dflt, Label... labels) {
		BytecodeStackFrameModifier.intepret(0, TABLESWITCH, null, currentFrame, "");//TODO - what happens to state here?
		super.visitTableSwitchInsn(min, max, dflt, labels);
	}

	public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
		BytecodeStackFrameModifier.intepret(0, LOOKUPSWITCH, null, currentFrame, "");
		super.visitLookupSwitchInsn(dflt, keys, labels);
	}

	public void visitMultiANewArrayInsn(String desc, int dims) {
		BytecodeStackFrameModifier.intepret(0, MULTIANEWARRAY, new MultiANewArrayInsnNode(desc, dims), currentFrame, "");
		super.visitMultiANewArrayInsn(desc, dims);
	}

	/*public AnnotationVisitor visitInsnAnnotation(int typeRef, TypePath typePath, String desc, boolean visible) {
		return super.visitInsnAnnotation(typeRef, typePath, desc, visible);
	}*/

	/*public void visitTryCatchBlock(Label start, Label end, Label handler, String type) {
		super.visitTryCatchBlock(start, end, handler, type);
	}*/

	/*public AnnotationVisitor visitTryCatchAnnotation(int typeRef, TypePath typePath, String desc, boolean visible) {
		return super.visitTryCatchAnnotation(typeRef, typePath, desc, visible);
	}

	public void visitLocalVariable(String name, String desc, String signature, Label start, Label end, int index) {
		super.visitLocalVariable(name, desc, signature, start, end, index);
	}

	public AnnotationVisitor visitLocalVariableAnnotation(int typeRef, TypePath typePath, Label[] start, Label[] end, int[] index, String desc, boolean visible) {
		return super.visitLocalVariableAnnotation(typeRef, typePath, start, end, index, desc, visible);
	}*/

	/*public void visitLineNumber(int line, Label start) {
		super.visitLineNumber(line, start);
	}

	public void visitMaxs(int maxStack, int maxLocals) {
		super.visitMaxs(maxStack, maxLocals);
	}

	public void visitEnd() {
		super.visitEnd();
	}*/
}
