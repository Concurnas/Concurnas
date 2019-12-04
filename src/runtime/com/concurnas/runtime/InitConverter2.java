package com.concurnas.runtime;

import java.lang.reflect.Modifier;
import java.util.HashMap;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Context;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.AnalyzerAdapter;

import com.concurnas.runtime.cps.analysis.ConcClassWriter;
import com.concurnas.runtime.cps.analysis.Fiberizer;

/**
 * convert <init> methods to an <init>(uncallable) and init(normal form,
 * uncallable) form
 * 
 * redirect all calls to <init> methods, so call empty <init> with uncallable
 * and also [except when calling super or this constructor]
 * 
 * for primordials, ensure that origonal init code is preserved
 * 
 * For Actors, we do something special...
 * 
 * @author Jason
 *
 */
public class InitConverter2 implements Opcodes {

	private static class NameAndDesc {
		public String name;
		public String desc;

		public NameAndDesc(String owner, String desc) {
			this.name = owner;
			this.desc = desc;
		}

		@Override
		public int hashCode() {
			return this.name.hashCode() + this.desc.hashCode();
		}

		@Override
		public boolean equals(Object o) {
			NameAndDesc asO = (NameAndDesc) o;
			return this.name.equals(asO.name) && this.desc.equals(asO.desc);
		}

		@Override
		public String toString() {
			return name + "->" + desc;
		}
	}

	private static String addInitU(String desc) {
		// "(II)V" => "(IILcom/concurnas/bootstrap/runtime/InitUncreatable;)V"
		return desc.substring(0, desc.length() - 2) + "Lcom/concurnas/bootstrap/runtime/InitUncreatable;)V";// sticking at the end permits the null addition to be easier
	}

	private class MethodAdjustor extends MethodVisitor {
		public HashMap<Integer, Boolean> initInstanceToIsSuperConstru = new HashMap<Integer, Boolean>();
		private int cnt = 0;
		private final int incArgCnt;
		private boolean ignoreIntrinsicAnnotation;

		public MethodAdjustor(MethodVisitor mv, HashMap<Integer, Boolean> initInstanceToIsSuperConstru, int incArgCnt, boolean ignoreIntrinsicAnnotation) {
			super(ASM7, mv);
			this.initInstanceToIsSuperConstru = initInstanceToIsSuperConstru;
			this.incArgCnt = incArgCnt;
			this.ignoreIntrinsicAnnotation = ignoreIntrinsicAnnotation;
		}

		public AnnotationVisitor visitAnnotation(final String descriptor, final boolean visible) {
			if(ignoreIntrinsicAnnotation) {
				if(descriptor.equals(Fiberizer.HotSpotIntrinsicCandidate_ANNOTATION)) {
					return null;
				}
				
				return super.visitAnnotation(descriptor, visible);
			}else {
				return super.visitAnnotation(descriptor, visible);
			}
		}

		public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
			if (name.equals("<init>")) {// skip for obj

				if (owner.equals("java/lang/Object")) {
					if (!inRuntimeCacheMode) {
						super.visitMethodInsn(opcode, owner, name, desc, itf);
					}
				}

				boolean callsSuper = initInstanceToIsSuperConstru.get(cnt++);
				name = "init";
				desc = addInitU(desc);
				mv.visitInsn(ACONST_NULL);

				if (!callsSuper) {
					// the init is invoked via a virtual call, unless we are calling super
					// constructor, in which case we need to go via INVOKESPECIAL
					opcode = Opcodes.INVOKEVIRTUAL;
				}

				boolean popAndExit;
				if (owner.equals("java/lang/Object")) {
					popAndExit = true;
				} else if (owner.equals("com/concurnas/bootstrap/runtime/cps/CObject")) {
					if (desc.equals("(Lcom/concurnas/bootstrap/runtime/CopyTracker;Lcom/concurnas/bootstrap/runtime/cps/CObject;Lcom/concurnas/bootstrap/runtime/CopyDefinition;Lcom/concurnas/bootstrap/runtime/InitUncreatable;)V")) {
						popAndExit = false;
					} else {
						popAndExit = true;
					}
				} else {
					popAndExit = false;
				}

				if (popAndExit) {// TODO: shouldnt we just check against
									// "com/concurnas/bootstrap/runtime/cps/CObject" ?
					mv.visitInsn(POP);
					mv.visitInsn(POP);
					return;
				}
			}
			super.visitMethodInsn(opcode, owner, name, desc, itf);
		}

		// NEW x, dup -> new X, dup, dup, null, invoke empty <init>
		public void visitTypeInsn(int opcode, String type) {
			super.visitTypeInsn(opcode, type);

			/*
			 * if(opcode == NEW && type.contains("Ext$ErrorOrWarning")) { return; }
			 */

			if (opcode == NEW && !type.equals("java/lang/Object")) {// skip for obj
				super.visitInsn(DUP);
				mv.visitInsn(ACONST_NULL);

				super.visitMethodInsn(INVOKESPECIAL, type, "<init>", "(Lcom/concurnas/bootstrap/runtime/InitUncreatable;)V", false);
			}
		}

		public void visitIincInsn(int var, int increment) {
			var += var >= incArgCnt ? 1 : 0;// extra args from fiber at end
			super.visitIincInsn(var, increment);
		}

		public void visitVarInsn(int opcode, int var) {
			var += var >= incArgCnt ? 1 : 0;// extra args from fiber at end
			super.visitVarInsn(opcode, var);
		}
	}

	class InitRedirector extends ClassVisitor {
		// private final HashMap<String, Integer> descToMaxLocals;
		public boolean preserveInit = false;
		public HashMap<NameAndDesc, HashMap<Integer, Boolean>> initInstanceToIsSuperConstru = new HashMap<NameAndDesc, HashMap<Integer, Boolean>>();

		public InitRedirector(ClassVisitor cv, HashMap<NameAndDesc, HashMap<Integer, Boolean>> initInstanceToIsSuperConstru) {
			super(ASM7, cv);
			this.initInstanceToIsSuperConstru = initInstanceToIsSuperConstru;
		}

		private boolean addedEmptyInit = false;
		private String superName;

		public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
			this.superName = superName;
			super.visit(version, access, name, signature, superName, interfaces);
		}

		public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {

			if (Modifier.isStatic(access) && !Modifier.isPublic(access)) {
				if (Modifier.isPrivate(access)) {
					access &= ~Opcodes.ACC_PRIVATE;
				} else if (Modifier.isProtected(access)) {
					access &= ~Opcodes.ACC_PROTECTED;
				}
				access += Opcodes.ACC_PUBLIC;

			}

			if (preserveInit) {
				if (name.equals("<init>")) {// only inits ignore others
					return super.visitMethod(access, name, desc, signature, exceptions);// dont touch
				}
				return null;
			} else {
				if (name.equals("<init>")) {
					if (!addedEmptyInit) {
						{// default...
							MethodVisitor mv = super.visitMethod(ACC_PUBLIC + ACC_SYNTHETIC, "<init>", "(Lcom/concurnas/bootstrap/runtime/InitUncreatable;)V", null, null);
							mv.visitCode();
							mv.visitVarInsn(ALOAD, 0);
							if (superName.equals("java/lang/Object") || superName.equals("com/concurnas/bootstrap/runtime/cps/CObject")) {
								mv.visitMethodInsn(INVOKESPECIAL, superName, "<init>", "()V", false);
							} else {
								mv.visitInsn(ACONST_NULL);
								mv.visitMethodInsn(INVOKESPECIAL, superName, "<init>", "(Lcom/concurnas/bootstrap/runtime/InitUncreatable;)V", false);
							}

							mv.visitInsn(RETURN);
							mv.visitMaxs(1, 1);
							mv.visitEnd();
						}

						addedEmptyInit = true;
					}

					// 1. if first for class, create standard init with call to superclass init
					name = "init";
					return new MethodAdjustor(super.visitMethod(ACC_PUBLIC, name, addInitU(desc), signature, exceptions), initInstanceToIsSuperConstru.get(new NameAndDesc("<init>", desc)), BcUtils.incArgPointsFromDesc(desc, access), true);
				}
			}

			if (name.startsWith("lambda$") && Modifier.isPrivate(access)) {
				access &= ~Opcodes.ACC_PRIVATE;// make lambdas public for purposes of access from 'global' instance
				access += Opcodes.ACC_PUBLIC;
			}

			return new MethodAdjustor(super.visitMethod(access, name, desc, signature, exceptions), initInstanceToIsSuperConstru.get(new NameAndDesc(name, desc)), BcUtils.incArgPointsFromDesc(desc, access), false);
		}

		// Also, convert non static final feilds to be non final so they can be written
		// to in init method
		@Override
		public FieldVisitor visitField(int access, final String name, final String descriptor, final String signature, final Object value) {
			if (Modifier.isFinal(access) && !Modifier.isStatic(access)) {
				access &= ~Opcodes.ACC_FINAL;
			}

			return cv.visitField(access, name, descriptor, signature, value);
		}

	}

	private class ClassReaderCanReReadMethod extends ClassReader {
		public ClassReaderCanReReadMethod(byte[] code) {
			super(code);
		}

		public InitRedirector myInit = null;

		protected int readMethod(final ClassVisitor classVisitor, final Context context, int u) {
			int newU = super.readMethod(classVisitor, context, u);
			if (myInit != null) {

				if (assumeNoPrimordials || true) {// TODO: restrict this to only primordials?
					// only for primordials, keep the inits
					// visit a primordial therefore add normal init method
					myInit.preserveInit = true;
					super.readMethod(classVisitor, context, u);
					myInit.preserveInit = false;
				}
			}

			return newU;
		}

	}

	private final byte[] inputClassBytes;
	private final CpsifierBCProvider cpsbcProvider;
	private final String name;
	// private final MaxLocalFinderCache mlf;
	private final boolean assumeNoPrimordials;
	private final boolean inRuntimeCacheMode;

	public InitConverter2(byte[] inputClassBytes, CpsifierBCProvider cpsbcProvider, String namea, boolean assumeNoPrimordials, boolean inRuntimeCacheMode) {
		this.inputClassBytes = inputClassBytes;
		this.cpsbcProvider = cpsbcProvider;
		this.name = namea;
		// this.mlf = mlf;
		this.assumeNoPrimordials = assumeNoPrimordials;
		this.inRuntimeCacheMode = inRuntimeCacheMode;
	}

	private class SuperConstructorInvokationMethodAdaptor extends AnalyzerAdapter {

		private HashMap<NameAndDesc, HashMap<Integer, Boolean>> initInstanceToIsSuperConstru;
		private NameAndDesc nandDesc;

		protected SuperConstructorInvokationMethodAdaptor(String owner, int access, String name, String desc, MethodVisitor mv, HashMap<NameAndDesc, HashMap<Integer, Boolean>> initInstanceToIsSuperConstru) {
			super(ASM7, owner, access, name, desc, mv);
			this.nandDesc = new NameAndDesc(name, desc);
			this.initInstanceToIsSuperConstru = initInstanceToIsSuperConstru;
		}

		private int cnt = 0;

		@Override
		public void visitMethodInsn(final int opcode, final String owner, final String name, final String desc, final boolean itf) {
			if (name.equals("<init>")) {
				HashMap<Integer, Boolean> got = initInstanceToIsSuperConstru.get(nandDesc);
				if (null == got) {
					got = new HashMap<Integer, Boolean>();
					initInstanceToIsSuperConstru.put(nandDesc, got);
				}
				got.put(cnt++, topOfStackIsThisUninit(desc));
			}

			super.visitMethodInsn(opcode, owner, name, desc, itf);
		}

		protected boolean topOfStackIsThisUninit(String desc) {
			// check to see if top of stack is UNITIZALIZED THIS reference, offset by
			// arguments consumed by this statement which -should be- already on the stack
			int offset = getSlotsConsumed(desc);

			if (null == super.stack) {
				return false;// TODO: dunno if this is the right solution...
			}

			int sz = super.stack.size();
			for (int n = sz - 1 - offset; n >= 0; n--) {
				Object top = super.stack.get(n);
				if (null != top) {

					/*
					 * if(top instanceof Label){//ununitialized object of a defined type return
					 * false; }
					 */
					if (top instanceof Integer && (((Integer) top) == UNINITIALIZED_THIS)) {// finally!
						return true;
					}
					return false;
					// any other type, assumed consumed by method - count this?
				}
			}
			return false;
		}

		private int getSlotsConsumed(String desc) {
			int ret = 0;
			for (Type t : Type.getArgumentTypes(desc)) {
				ret += t.getSize();
			}
			return ret;
		}

	}

	private class SuperConstructorInvokationFinder extends ClassVisitor {
		private String owner;
		public HashMap<NameAndDesc, HashMap<Integer, Boolean>> initInstanceToIsSuperConstru = new HashMap<NameAndDesc, HashMap<Integer, Boolean>>();

		public SuperConstructorInvokationFinder() {
			super(ASM7);
		}

		public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
			this.owner = name;
			super.visit(version, access, name, signature, superName, interfaces);
		}

		public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
			return new SuperConstructorInvokationMethodAdaptor(owner, access, name, desc, super.visitMethod(access, name, desc, signature, exceptions), initInstanceToIsSuperConstru);
		}

	}

	public byte[] transform() {

		if (this.name.equals("java/lang/Object")) {
			return inputClassBytes;
		}

		ClassReader cra = new ClassReader(this.inputClassBytes);
		SuperConstructorInvokationFinder superConFinder = new SuperConstructorInvokationFinder();
		cra.accept(superConFinder, ClassReader.EXPAND_FRAMES);

		ClassReaderCanReReadMethod cr = new ClassReaderCanReReadMethod(this.inputClassBytes);

		ClassWriter cw = new ConcClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS, cpsbcProvider.clloader.getDetector());// TODO: turn off compute frames? - see what happens
		InitRedirector initRedirector = new InitRedirector(cw, superConFinder.initInstanceToIsSuperConstru);
		cr.myInit = initRedirector;// some nice special magic going on here...
		cr.accept(initRedirector, 0);// ClassReader.EXPAND_FRAMES);

		return cw.toByteArray();
	}

}
