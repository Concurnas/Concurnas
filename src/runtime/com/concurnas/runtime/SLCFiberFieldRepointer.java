package com.concurnas.runtime;

import java.lang.reflect.Modifier;
import java.util.HashSet;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import com.concurnas.runtime.cps.analysis.ConcClassWriter;
import com.concurnas.runtime.cps.analysis.Fiberizer;

/**
 * Bootstrap classes having static fields (SLC classes) pointing to lambda's
 * (method references) are invoked without reference to a fiber at bootstrap.
 * Here we duplicate these fields as +"$?Fiberized" only for the SLC classes and
 * repoint to them as necessary. We then repoint to these methods within fiberized methods
 * only. 
 * 
 * @author jason
 *
 */
public class SLCFiberFieldRepointer extends ClassVisitor implements Opcodes {
	private HashSet<String> staticLambdaClasses;
	private boolean isBootstrap;
	private boolean isSLCClass;

	private static final String fiberFieldPostfix = "$?Fiberized";

	public SLCFiberFieldRepointer(ClassVisitor classVisitor, HashSet<String> staticLambdaClasses, boolean isBootstrap) {
		super(ASM7, classVisitor);
		this.staticLambdaClasses = staticLambdaClasses;
		this.isBootstrap = isBootstrap;
	}

	public void visit(final int version, final int access, final String name, final String signature, final String superName, final String[] interfaces) {
		isSLCClass = isBootstrap && staticLambdaClasses.contains(name);
		super.visit(version, access, name, signature, superName, interfaces);
	}

	public FieldVisitor visitField(final int access, final String name, final String descriptor, final String signature, final Object value) {

		if (this.isSLCClass && Modifier.isStatic(access)) {
			FieldVisitor fibVersion = super.visitField(access, name + fiberFieldPostfix, descriptor, signature, value);
			fibVersion.visitEnd();
		}

		return super.visitField(access, name, descriptor, signature, value);
	}

	private class FieldUseRepointer extends MethodVisitor {
		public FieldUseRepointer(MethodVisitor methodVisitor) {
			super(ASM7, methodVisitor);
		}

		@Override
		public void visitFieldInsn(final int opcode, final String owner, String name, final String descriptor) {
			if((opcode == GETSTATIC || opcode == PUTSTATIC) && staticLambdaClasses.contains(owner)){
				name += fiberFieldPostfix;
			}
			super.visitFieldInsn(opcode, owner, name, descriptor);
		}
	}

	public MethodVisitor visitMethod(final int access, final String name, final String descriptor, final String signature, final String[] exceptions) {
		MethodVisitor ret = super.visitMethod(access, name, descriptor, signature, exceptions);
		// if ends with Fiber then remap all GETSTATIC, PUTSTATIC calls to owners in
		// SLCSet to fiberized postfix name
		if (descriptor.contains(Fiberizer.FIBER_SUFFIX)) {
			ret = new FieldUseRepointer(ret);
		}

		return ret;
	}

	public static byte[] transform(byte[] code, ConcClassUtil loader, HashSet<String> staticLambdaClasses, boolean isBootstrap) {
		ClassWriter cw = new ConcClassWriter(0, loader.getDetector());
		new ClassReader(code).accept(new SLCFiberFieldRepointer(cw, staticLambdaClasses, isBootstrap), 0);

		return cw.toByteArray();
	}

}
