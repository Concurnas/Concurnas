package com.concurnas.runtime;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import com.concurnas.bootstrap.runtime.cps.Fiber;
import com.concurnas.runtime.cps.analysis.ANFTransform;
import com.concurnas.runtime.cps.analysis.ConcClassWriter;
import com.concurnas.runtime.cps.mirrors.ClassMirror;
import com.concurnas.runtime.cps.mirrors.ClassMirrorNotFoundException;
import com.concurnas.runtime.cps.mirrors.FieldMirror;

/**
 * Convert module level static fields and functions in class XYZ to XYZ$$Gloabls
 * - with singleton initalizer for accessing the Globals class contaiing the
 * items also reroute all access to static fields and methods to the respective
 * global instances
 * 
 * except for enums which require the values method to be decared for reflection
 * purposes
 * 
 * we don't map to global versions on if they originate from primordials -
 * unless they are in the staticLambdaClasses set
 * 
 * we don't map trait methods either
 * 
 */
public class Globalizer implements Opcodes {
	private final byte[] inputClassBytes;
	private final MaxLocalFinderCache mfl;
	private HashSet<String> staticLambdaClasses;
	private boolean isBootstrapClass;

	private static boolean isTraitMethod(String name) {
		return name.endsWith("$traitM");
	}

	public void copyFieldsFromTo(MethodVisitor mv, List<Pair<String, String>> staticFieldsToCopy, String fromName, String globName, boolean fromStaticToLocal) {

		int get;
		int put;
		if (fromStaticToLocal) {
			get = GETSTATIC;
			put = PUTFIELD;
		} else {
			get = GETFIELD;
			put = PUTSTATIC;
		}

		if (!staticFieldsToCopy.isEmpty()) {
			if (!isBootstrapClass && !fromName.equals("com/concurnas/runtime/bootstrapCloner/Cloner")) {// JPT: logic could be better structured
				mv.visitMethodInsn(INVOKESTATIC, "com/concurnas/runtime/bootstrapCloner/Cloner$Globals$", "getInstance?", "()Lcom/concurnas/runtime/bootstrapCloner/Cloner$Globals$;", false);
				mv.visitInsn(POP);
			}

			// for each copyable field...
			for (Pair<String, String> field : staticFieldsToCopy) {
				String fname = field.getA();
				String ftype = field.getB();
				if (fromStaticToLocal) {
					mv.visitVarInsn(ALOAD, 0);
				}

				if (!isBootstrapClass && (ftype.startsWith("L") || ftype.startsWith("["))) {// TODO: dont copy things marked as immutable
					mv.visitFieldInsn(GETSTATIC, "com/concurnas/runtime/bootstrapCloner/Cloner", "cloner", "Lcom/concurnas/runtime/bootstrapCloner/Cloner;");
					if (!fromStaticToLocal) {
						mv.visitVarInsn(ALOAD, 0);
					}

					mv.visitFieldInsn(get, fromName, fname, ftype);
					mv.visitMethodInsn(INVOKEVIRTUAL, "com/concurnas/runtime/bootstrapCloner/Cloner", !fromStaticToLocal?"cloneRepointTransient":"clone", "(Ljava/lang/Object;)Ljava/lang/Object;", false);
					mv.visitTypeInsn(CHECKCAST, (ftype.endsWith(";") && ftype.startsWith("L")) ? ftype.substring(1, ftype.length() - 1) : ftype);
				} else {
					if (!fromStaticToLocal) {
						mv.visitVarInsn(ALOAD, 0);
					}
					mv.visitFieldInsn(get, fromName, fname, ftype);
				}

				mv.visitFieldInsn(put, globName, fname, ftype);
			}
		}
	}

	private static boolean isNonGlobalerORPrimordial(String owner, String desc, String name, HashSet<String> staticLambdaClasses, ConcClassUtil clloader) {

		if (name.equals("metaBinary") && "()[Ljava/lang/String;".equals(desc)) {
			return true;
		}

		if (NoGlobalizationException(owner)) {
			return true;
		}

		if (null != staticLambdaClasses) {
			return !staticLambdaClasses.contains(owner);// primordial only if not in this list
		}

		try {// primordials have no global code
			HashSet<String> slcs = clloader.getStaticLambdaClasses();
			if (null != slcs && slcs.contains(owner)) {
				return false;// use the globalized version
			}

			Class<?> gt = clloader.loadClassFromPrimordial(owner.replace("/", "."));// no .replace("/", ".")
			return gt != null;
		} catch (ClassNotFoundException e) {
		}
		return false;
	}

	public static boolean NoGlobalizationException(String x) {
		x = x.replace(".", "/");
		if (x.startsWith("com/concurnas/bootstrap/runtime/cps/")) {
			return true;
		} else if (x.startsWith("com/concurnas/bootstrap/lang/Stringifier")) {// TODO: maybe there is a more elegant way to express this
			return true;
		} else if (x.startsWith("com/concurnas/lang/Hasher")) {// TODO: maybe there is a more elegant way to express this
			return true;
		} else if (x.startsWith("com/concurnas/repl/REPLRuntimeState")) {
			return true;
		}
		/*
		 * else if(x.equals("java/lang/System")){//TODO: maybe there is a more elegant
		 * way to express this return true; }
		 */
		return false;
	}

	public Globalizer(byte[] inputClassBytes, MaxLocalFinderCache mfl, HashSet<String> staticLambdaClasses, boolean isBootstrapClass) {
		this.inputClassBytes = inputClassBytes;
		this.mfl = mfl;
		this.staticLambdaClasses = staticLambdaClasses;
		this.isBootstrapClass = isBootstrapClass;
	}

	private class StaticAccessMethodRepoint extends MethodVisitor {
		private final String fromName;
		private final String classname;
		private int maxLocalSize;
		private final ConcClassUtil clloader;
		private final HashSet<OwnerNameDesc> methodsWhichWereForcedPublic;
		private HashSet<String> staticLambdaClasses;
		private HashSet<String> locallyDefinedFields;
		private Set<String> locallyDefinedMethods;
		private String superClass;
		private String[] ifaces;
		private String ignoreOwner;
		private boolean repointLocalStaticToGlobalWithoutgetInstanceCall;
		private ArrayList<FieldVisitorHolder> staticFinalVars;

		/**
		 * @param fromName - set this only on second run - i.e. when gennerating code in
		 *                 globalizer, so as to avoid claling getInstance on own stuff
		 */
		protected StaticAccessMethodRepoint(HashSet<String> staticLambdaClasses, MethodVisitor mv, int maxLocalSize, String fromName, ConcClassUtil clloader, HashSet<OwnerNameDesc> methodsWhichWereForcedPublic, HashSet<String> locallyDefinedFields, Set<String> locallyDefinedMethods, String superClass, String[] ifaces, String classname, String ignoreOwner, boolean repointLocalStaticToGlobalWithoutgetInstanceCall) {
			super(ASM7, mv);
			this.fromName = fromName;
			this.classname = classname;
			this.maxLocalSize = maxLocalSize;
			this.repointLocalStaticToGlobalWithoutgetInstanceCall = repointLocalStaticToGlobalWithoutgetInstanceCall && !isBootstrapClass;
			if (this.fromName != null || repointLocalStaticToGlobalWithoutgetInstanceCall) {// if fromName them indicates that we're making this a non static method when it
				// used to be static, thus we must shift the count of maxvars by 1
				this.maxLocalSize++;
			}
			this.clloader = clloader;
			this.methodsWhichWereForcedPublic = methodsWhichWereForcedPublic;
			this.staticLambdaClasses = staticLambdaClasses;
			this.locallyDefinedFields = locallyDefinedFields;
			this.locallyDefinedMethods = locallyDefinedMethods;

			this.superClass = superClass;
			this.ifaces = ifaces;
			this.ignoreOwner = ignoreOwner;
		}

		public StaticAccessMethodRepoint(HashSet<String> staticLambdaClasses, MethodVisitor mv, int maxLocalSize, ConcClassUtil clloader, HashSet<String> locallyDefinedFields, Set<String> locallyDefinedMethods, String superClass, String[] ifaces, String classname, String ignoreOwner, boolean repointLocalStaticToGlobalWithoutgetInstanceCall) {
			this(staticLambdaClasses, mv, maxLocalSize, null, clloader, null, locallyDefinedFields, locallyDefinedMethods, superClass, ifaces, classname, ignoreOwner, repointLocalStaticToGlobalWithoutgetInstanceCall);
		}

		private void genericSswap(String desc) {
			// caters for double, int on stack - to swap
			if (desc.equals("J") || desc.equals("D")) {// long and double take up two slots insteead of 1
				super.visitInsn(Opcodes.DUP_X2);
				super.visitInsn(Opcodes.POP);
			} else {// 1 slot each
				super.visitInsn(Opcodes.SWAP);
			}
		}

		private String findLocationOfFieldOrMethod(boolean isField, String name, String desc, String supCls, String[] ifaces) {
			return findLocationOfFieldOrMethod(isField, name, desc, supCls, ifaces, new HashSet<String>());
		}

		private String findLocationOfFieldOrMethod(boolean isField, String name, String desc, String supCls, String[] ifaces, HashSet<String> searchAlready) {
			String curSup = supCls;
			while (null != curSup && !curSup.equals("java/lang/Object") && !searchAlready.contains(curSup)) {
				try {
					ClassMirror sup = this.clloader.getDetector().classForName(curSup);

					if (isField) {
						FieldMirror fm = sup.getField(name);
						if (null != fm && fm.desc.equals(desc)) {
							return curSup;
						}
					} else {
						if (null != sup.getDeclaredMethod(name, desc)) {
							return curSup;
						}
					}

					searchAlready.add(curSup);
					for (String iface : sup.getInterfacesNoEx()) {
						String ret = findLocationOfFieldOrMethod(isField, name, desc, iface, null, searchAlready);
						if (null != ret) {
							return ret;
						}
					}
					curSup = sup.getSuperclass();
				} catch (ClassMirrorNotFoundException e) {
					curSup = null;
				}
			}

			if (ifaces != null) {
				for (int n = 0; n < ifaces.length; n++) {
					String iface = ifaces[n];
					String ret = findLocationOfFieldOrMethod(isField, name, desc, iface, null, searchAlready);
					if (ret != null) {
						return ret;
					}

				}
			}

			return null;
		}

		public void visitFieldInsn(int opcode, String owner, String name, String desc) {
			// repoint static field access
			if ((opcode == GETSTATIC || opcode == PUTSTATIC) && !isNonGlobalerORPrimordial(owner, desc, name, staticLambdaClasses, clloader)) {
				if (!owner.equals(ignoreOwner)) {

					boolean isShared = false;
					try {// shared fields are not converted into the Global class, but are kept static
						isShared = clloader.getDetector().classForName(owner).getField(name).isShared;
					} catch (Throwable e) {// is this possible?
						// throw new RuntimeException("Cannot find class: "+ owner + " as " +
						// e.getMessage(), e);
					}
					
					String globOwner = owner + "$Globals$";
					int opcodea = opcode == GETSTATIC ? GETFIELD : PUTFIELD;
					if (this.fromName != null && this.fromName.equals(owner)) {// ref self in constructor, etc
						if (null == this.locallyDefinedFields || this.locallyDefinedFields.contains(name + desc)) {// check self actually has feild in question
							super.visitVarInsn(ALOAD, 0);
							if(!isShared && !isBootstrapClass) {
								if (PUTFIELD == opcodea) {// only for put operation
									genericSswap(desc);
								}
								
								super.visitFieldInsn(opcodea, globOwner, name, desc);
								return;
							}
						} else {// , else map to that which does...
							String newglobOwner = findLocationOfFieldOrMethod(true, name, desc, this.superClass, this.ifaces);

							if (newglobOwner == null) {
								super.visitVarInsn(ALOAD, 0);
							} else {
								if (null == this.staticLambdaClasses || this.staticLambdaClasses.contains(newglobOwner)) {
									globOwner = newglobOwner + "$Globals$";
									super.visitMethodInsn(INVOKESTATIC, globOwner, "getInstance?", "()L" + globOwner + ";", false);
								} else {
									super.visitVarInsn(ALOAD, 0);
								}
							}
						}
					} else if (this.classname.equals(owner) && !this.locallyDefinedFields.contains(name + desc)) {
						// check self actually has feild in question
						String newglobOwner = findLocationOfFieldOrMethod(true, name, desc, this.superClass, this.ifaces);

						if (newglobOwner == null) {
							super.visitMethodInsn(INVOKESTATIC, globOwner, "getInstance?", "()L" + globOwner + ";", false);
						} else {
							if (null == this.staticLambdaClasses || this.staticLambdaClasses.contains(newglobOwner)) {
								globOwner = newglobOwner + "$Globals$";
							}
							super.visitMethodInsn(INVOKESTATIC, globOwner, "getInstance?", "()L" + globOwner + ";", false);
						}
					} else {
						super.visitMethodInsn(INVOKESTATIC, globOwner, "getInstance?", "()L" + globOwner + ";", false);
					}

					if (!isShared ) {
						if(null != staticLambdaClasses && staticLambdaClasses.contains(owner)) {
							//if bootstrapping and staticLambdaClasses doesnt contain instance, just call globalizer and ignor result
							mv.visitInsn(POP);
						}else {
							if (PUTFIELD == opcodea) {// only for put operation
								genericSswap(desc);
							}
							super.visitFieldInsn(opcodea, globOwner, name, desc);
							return;
						}
					} else {
						// shared, just call init, but dont extrat from globalizer (this calls clinit equvilents)
						mv.visitInsn(POP);
					}
				} else if (this.repointLocalStaticToGlobalWithoutgetInstanceCall) {

					boolean isShared = false;
					try {// shared fields are not converted into the Global class, but are kept static
						isShared = clloader.getDetector().classForName(owner).getField(name).isShared;
					} catch (Throwable e) {// is this possible?
						// throw new RuntimeException("Cannot find class: "+ owner + " as " +
						// e.getMessage(), e);
					}

					if (!isShared) {
						super.visitVarInsn(ALOAD, 0);
						if (opcode == PUTSTATIC) {
							genericSswap(desc);
						}

						super.visitFieldInsn(opcode == GETSTATIC ? GETFIELD : PUTFIELD, owner + "$Globals$", name, desc);
						return;
					}
				}
			}

			super.visitFieldInsn(opcode, owner, name, desc);
		}

		public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
			if (opcode == Opcodes.INVOKESTATIC && !isNonGlobalerORPrimordial(owner, null, name, staticLambdaClasses, clloader) && !owner.equals(ignoreOwner)) {

				if (isTraitMethod(name)) {
					super.visitMethodInsn(opcode, owner, name, desc, itf);
					return;
				}

				String globOwner = owner + "$Globals$";

				List<Character> charz = ANFTransform.getPrimOrObj(desc);
				int cnt = charz.size(); // cnt==0 do nothing!

				Stack<Integer> tmpVars = new Stack<Integer>();

				if (cnt >= 2) {// store as vars
					for (int n = charz.size() - 1; n >= 0; n--) {// store in vars
						char x = charz.get(n);
						tmpVars.add(maxLocalSize);
						super.visitVarInsn(ANFTransform.getStoreOp(x), maxLocalSize++);
						if (x == 'D' || x == 'J') {
							maxLocalSize++;
						}
					}
				}

				if (this.fromName != null && this.fromName.equals(owner)) {// oh, references itself, should use this reference in that case in constructor
					if (null == this.locallyDefinedMethods || this.locallyDefinedMethods.contains(name + desc)) {// check self actually has feild in question
						super.visitVarInsn(ALOAD, 0);
					} else {// , else map to that which does...
						String newglobOwner = findLocationOfFieldOrMethod(false, name, desc, this.superClass, this.ifaces);

						if (newglobOwner == null) {
							super.visitVarInsn(ALOAD, 0);
						} else {
							if (null == this.staticLambdaClasses || this.staticLambdaClasses.contains(newglobOwner)) {
								globOwner = newglobOwner + "$Globals$";
								super.visitMethodInsn(INVOKESTATIC, globOwner, "getInstance?", "()L" + globOwner + ";", false);
							} else {
								super.visitVarInsn(ALOAD, 0);
							}
						}
					}
				} else if (this.classname.equals(owner) && !this.locallyDefinedMethods.contains(name + desc)) {
					// check self actually has feild in question
					String newglobOwner = findLocationOfFieldOrMethod(false, name, desc, this.superClass, this.ifaces);

					if (newglobOwner == null) {
						super.visitMethodInsn(INVOKESTATIC, globOwner, "getInstance?", "()L" + globOwner + ";", false);
					} else {
						if (null == this.staticLambdaClasses || this.staticLambdaClasses.contains(newglobOwner)) {
							globOwner = newglobOwner + "$Globals$";
						}
						super.visitMethodInsn(INVOKESTATIC, globOwner, "getInstance?", "()L" + globOwner + ";", false);
					}
				} else {
					super.visitMethodInsn(INVOKESTATIC, globOwner, "getInstance?", "()L" + globOwner + ";", false);
				}

				if (cnt >= 2) {// unstore as vars
					for (char c : charz) {// reload the stack
						/*
						 * if(c == 'D' || c=='J'){ --maxLocalSize; }
						 * super.visitVarInsn(ANFTransform.getLoadOp(c), --maxLocalSize);
						 */
						super.visitVarInsn(ANFTransform.getLoadOp(c), tmpVars.pop());
					}
				} else if (cnt == 1) {
					genericSswap(charz.get(0).toString());
					// super.visitInsn(Opcodes.SWAP);
				}
				// if zero then do nothing, nothing to swap as no args
				super.visitMethodInsn(INVOKEVIRTUAL, globOwner, name, desc, false);
			} else {
				// so we make the thing INVOKEVIRTUAL instead of invokespecial (probably) iff
				// it's not an init and we override the origonal one to be public from private
				// (since not calling this/parent instance of private method anymore)
				super.visitMethodInsn((this.fromName != null && !name.equals("<init>") && this.methodsWhichWereForcedPublic != null && this.methodsWhichWereForcedPublic.contains(new OwnerNameDesc(owner, name, desc))) ? INVOKEVIRTUAL : opcode, owner, name, desc, itf);
			}
		}

		private boolean wasStaticNowVirtual() {
			return fromName != null || this.repointLocalStaticToGlobalWithoutgetInstanceCall;
		}

		// adding an extra '0' var [indicated by setting fromName for globals class]
		// (this ref) so point everything to the right...
		@Override
		public void visitVarInsn(int opcode, int var) {
			super.visitVarInsn(opcode, wasStaticNowVirtual() ? var + 1 : var);
		}

		@Override
		public void visitMaxs(int maxStack, int maxLocals) {
			super.visitMaxs(maxStack, wasStaticNowVirtual() ? maxLocals + 1 : maxLocals);
		}

		@Override
		public void visitIincInsn(int var, int increment) {
			super.visitIincInsn(wasStaticNowVirtual() ? var + 1 : var, increment);
		}

		@Override
		public void visitInsn(final int opcode) {
			if(opcode == RETURN && this.staticFinalVars != null) {
				//at the end of the clinitOnce method we assign initial values to any static final fields.
				String globOwner = isBootstrapClass?classname:classname + "$Globals$";
				for (FieldVisitorHolder sfv : staticFinalVars) {
					mv.visitLabel(new Label());
					if(!isBootstrapClass) {
						mv.visitVarInsn(ALOAD, 0);
					}

					switch (sfv.desc) {
						case "Ljava/lang/String;":
							mv.visitLdcInsn(sfv.value.toString());
							break;
						case "J":
							BytecodeGenUtils.longOpcode(mv, ((Long) sfv.value).longValue());
							break;
						case "F":
							BytecodeGenUtils.floatOpcode(mv, ((Float) sfv.value).floatValue());
							break;
						case "D":
							BytecodeGenUtils.doubleOpcode(mv, ((Double) sfv.value).doubleValue());
							break;
						default:
							BytecodeGenUtils.intOpcode(mv, ((Integer) sfv.value).intValue());
					}

					mv.visitFieldInsn(isBootstrapClass?PUTSTATIC:PUTFIELD, globOwner, sfv.name, sfv.desc);
				}
			}
			super.visitInsn(opcode);
		}
		
	}

	private static void addstaticFinalVars(MethodVisitor mv, ArrayList<FieldVisitorHolder> staticFinalVars, String putTo) {
		for (FieldVisitorHolder sfv : staticFinalVars) {//TODO: copy pasted from above, clean this up
			mv.visitLabel(new Label());
			mv.visitVarInsn(ALOAD, 0);

			switch (sfv.desc) {
				case "Ljava/lang/String;":
					mv.visitLdcInsn(sfv.value.toString());
					break;
				case "J":
					BytecodeGenUtils.longOpcode(mv, ((Long) sfv.value).longValue());
					break;
				case "F":
					BytecodeGenUtils.floatOpcode(mv, ((Float) sfv.value).floatValue());
					break;
				case "D":
					BytecodeGenUtils.doubleOpcode(mv, ((Double) sfv.value).doubleValue());
					break;
				default:
					BytecodeGenUtils.intOpcode(mv, ((Integer) sfv.value).intValue());
			}

			mv.visitFieldInsn(PUTFIELD, putTo, sfv.name, sfv.desc);
		}
	}

	/**
	 * Used to turn clinit method into an init method
	 *
	 */
	/*
	 * public void visitCode() { if(!staticFinalVars.isEmpty()) {//we have some
	 * variables declared as static final resolving to a constant, so we must add
	 * setters for these here addstaticFinalVars(this.mv, this.staticFinalVars,
	 * super.fromName+"$Globals$"); }
	 * 
	 * super.visitCode(); } }
	 */

	private static class OwnerNameDesc {
		private String owner;
		private String name;
		private String desc;

		public OwnerNameDesc(String owner, String name, String desc) {
			this.owner = owner;
			this.name = name;
			this.desc = desc;
		}

		public OwnerNameDesc(String owner, String name) {
			this(owner, name, "");
		}

		public boolean equals(Object o) {// assume
			OwnerNameDesc o2 = (OwnerNameDesc) o;
			return o2.owner.equals(owner) && o2.desc.equals(desc) && o2.name.equals(name);
		}

		public int hashCode() {
			return this.owner.hashCode() + this.desc.hashCode() + this.name.hashCode();
		}

	}

	private class TransformMainClass extends ClassVisitor {
		private HashMap<String, Integer> maxLocalSize;
		private final ConcClassUtil clloader;
		private final HashSet<OwnerNameDesc> shouldbeForcedPublic;
		private final HashSet<OwnerNameDesc> shouldbeForcedPublicField;
		public final HashSet<OwnerNameDesc> methodsWhichWereForcedPublic = new HashSet<OwnerNameDesc>();
		private HashSet<String> staticLambdaClasses;
		private List<Pair<String, String>> nonSharedStaticFieldsToCopy;
		private ArrayList<FieldVisitorHolder> staticFinalVars;
		private boolean isInterface;

		public TransformMainClass(HashSet<String> staticLambdaClasses, ClassVisitor cv, HashMap<String, Integer> maxLocalSize, ConcClassUtil clloader, 
				HashSet<OwnerNameDesc> shouldbeForcedPublic, HashSet<OwnerNameDesc> shouldbeForcedPublicField, List<Pair<String, String>> nonSharedStaticFieldsToCopy,
				ArrayList<FieldVisitorHolder> staticFinalVars) {
			super(ASM7, cv);
			this.maxLocalSize = maxLocalSize;
			this.clloader = clloader;
			this.shouldbeForcedPublic = shouldbeForcedPublic;
			this.shouldbeForcedPublicField = shouldbeForcedPublicField;
			this.staticLambdaClasses = staticLambdaClasses;
			this.nonSharedStaticFieldsToCopy = nonSharedStaticFieldsToCopy;
			this.staticFinalVars = staticFinalVars;
		}

		// private Stack<String> className = new Stack<String>();
		private String className;
		//private boolean isEnumCls = false;
		private String supername;
		private String[] interfaces;

		public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
			className = name;
			//isEnumCls = (access & Opcodes.ACC_ENUM) != 0 && superName.equals("java/lang/Enum");
			this.supername = superName;
			this.interfaces = interfaces;
			this.isInterface = Modifier.isInterface(access);
			super.visit(version, access, name, signature, superName, interfaces);
		}

		public void visitEnd() {
			super.visitEnd();
		}

		public HashMap<String, String> origClassClinitToNewOne = new HashMap<String, String>();

		private String getGlobName() {
			// return getGlobName(className.peek());
			return getGlobName(className);
		}

		private String getGlobName(String className) {

			String ret = origClassClinitToNewOne.get(className);

			if (null == ret) {
				ret = className + "$Globals$";
				origClassClinitToNewOne.put(className, ret);
			}
			return ret;
		}

		private HashSet<String> locallyDefinedFields = new HashSet<String>();

		public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
			locallyDefinedFields.add(name + desc);

			// String cClass = this.className.peek();

			if (className.contains("$")) {// if nested class method needs to be public
				access = makePublic(access);
			}

			if (staticLambdaClasses != null && isNonGlobalerORPrimordial(className, null, name, staticLambdaClasses, clloader)) {
				return super.visitField(access, name, desc, signature, value);
			}

			boolean shared = false;
			boolean isStatic = Modifier.isStatic(access);
			if (isStatic) {
				try {
					shared = this.clloader.getDetector().classForName(className).getField(name).isShared;
				} catch (Throwable e) {
					// throw new RuntimeException("Cannot find class: "+ cClass + " as " +
					// e.getMessage(), e);
				}

				if (Modifier.isPrivate(access)) {
					access &= ~ACC_PRIVATE;
					access += ACC_PUBLIC;
				} else if (Modifier.isProtected(access)) {
					access &= ~ACC_PROTECTED;
					access += ACC_PUBLIC;
				} else if (!Modifier.isPublic(access)) {// package
					access += ACC_PUBLIC;
				}

				if (Modifier.isFinal(access) && !this.isInterface) {//we keep fields static for interfaces
					access &= ~ACC_FINAL;
					value = null;
				}

				if (!shared) {// but dont skip if shared
					getGlobName();// may not have a clinit so we call it anyway

					/*if (isEnumCls) {
						if (name.equals("ENUM$VALUES")) {
							access = Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL | Opcodes.ACC_STATIC | Opcodes.ACC_SYNTHETIC;
						}
						return super.visitField(access, name, desc, signature, value);
					}*/
				}
			}

			OwnerNameDesc me = new OwnerNameDesc(className, name);
			if (this.shouldbeForcedPublicField.contains(me) || className.contains("AtomicBoolean")) {
				// if the field is used in subclass/function which is static, then force it
				// public (bit of a hack, should use psar)
				/*
				 * if(isStatic) { access = ACC_PUBLIC + ACC_STATIC; }
				 */
				access = makePublic(access);
			}

			return super.visitField(access, name, desc, signature, value);
		}

		private int makePublic(int access) {
			if (!Modifier.isPublic(access)) {
				if (Modifier.isPrivate(access)) {
					access &= ~ACC_PRIVATE;
				} else if (Modifier.isProtected(access)) {
					access &= ~ACC_PROTECTED;
				}
				access += ACC_PUBLIC;
			}
			return access;
		}

		/**
		 * We use a String Object for clinitOnce$
		 */
		private void addClinitOnce() {
			FieldVisitor fieldVisitor;
			if(this.isInterface) {
				//add <clinit> to initialize the holder
				{
					MethodVisitor clinit = super.visitMethod(ACC_STATIC, "<clinit>", "()V", null, null);
					clinit.visitCode();
					clinit.visitLabel(new Label());
					clinit.visitTypeInsn(NEW, "com/concurnas/bootstrap/runtime/ClinitOnceIfaceHolder");
					clinit.visitInsn(DUP);
					clinit.visitMethodInsn(INVOKESPECIAL, "com/concurnas/bootstrap/runtime/ClinitOnceIfaceHolder", "<init>", "()V", false);
					clinit.visitFieldInsn(PUTSTATIC, className, "clinitOnce$", "Lcom/concurnas/bootstrap/runtime/ClinitOnceIfaceHolder;");
					clinit.visitInsn(RETURN);
					clinit.visitMaxs(2, 0);
					clinit.visitEnd();
				}
				
				fieldVisitor = super.visitField(ACC_PUBLIC | ACC_STATIC | ACC_FINAL, "clinitOnce$", "Lcom/concurnas/bootstrap/runtime/ClinitOnceIfaceHolder;", null, null);
			}else {
				fieldVisitor = super.visitField(ACC_PRIVATE | ACC_STATIC, "clinitOnce$", "Ljava/lang/String;", null, null);
			}
			fieldVisitor.visitEnd();

			String globname = "L" + this.getGlobName() + ";";
			MethodVisitor methodVisitor;
			Label notnull = new Label();
			if(this.isInterface) {
				methodVisitor = super.visitMethod(ACC_PUBLIC | ACC_STATIC, "clinitOnce$", String.format("(%s)Z", globname), null, null);
				methodVisitor.visitCode();
				Label label0 = new Label();
				Label label1 = new Label();
				Label label2 = new Label();
				methodVisitor.visitTryCatchBlock(label0, label1, label2, null);
				Label label3 = new Label();
				Label label4 = new Label();
				methodVisitor.visitTryCatchBlock(label3, label4, label2, null);
				Label label5 = new Label();
				methodVisitor.visitTryCatchBlock(label2, label5, label2, null);
				methodVisitor.visitLabel(new Label());
				methodVisitor.visitFieldInsn(GETSTATIC, className, "clinitOnce$", "Lcom/concurnas/bootstrap/runtime/ClinitOnceIfaceHolder;");
				methodVisitor.visitInsn(DUP);
				methodVisitor.visitVarInsn(ASTORE, 1);
				methodVisitor.visitInsn(MONITORENTER);
				methodVisitor.visitLabel(label0);
				methodVisitor.visitFieldInsn(GETSTATIC, className, "clinitOnce$", "Lcom/concurnas/bootstrap/runtime/ClinitOnceIfaceHolder;");
				methodVisitor.visitFieldInsn(GETFIELD, "com/concurnas/bootstrap/runtime/ClinitOnceIfaceHolder", "initOnce", "Z");
				methodVisitor.visitJumpInsn(IFNE, label3);
				methodVisitor.visitLabel(new Label());
				
				addClinitOnceCommon(methodVisitor, globname);
				
				if (null == staticLambdaClasses || !staticLambdaClasses.contains(className)) {// TODO: running clinit for every isolate may be inefficient for these classes
					// but we need to do this such that top level lambdas with invoke dynamic have
					// fiberizers when they are initalized in
					// non fiberized code (i.e. at bootstrap)
					methodVisitor.visitLabel(new Label());
					methodVisitor.visitFieldInsn(GETSTATIC, className, "clinitOnce$", "Lcom/concurnas/bootstrap/runtime/ClinitOnceIfaceHolder;");
					methodVisitor.visitInsn(ICONST_1);
					methodVisitor.visitFieldInsn(PUTFIELD, "com/concurnas/bootstrap/runtime/ClinitOnceIfaceHolder", "initOnce", "Z");
				}
				
				methodVisitor.visitLabel(new Label());
				methodVisitor.visitVarInsn(ALOAD, 1);
				methodVisitor.visitInsn(MONITOREXIT);
				methodVisitor.visitLabel(label1);
				methodVisitor.visitInsn(ICONST_1);
				methodVisitor.visitInsn(IRETURN);
				methodVisitor.visitLabel(label3);
				methodVisitor.visitVarInsn(ALOAD, 1);
				methodVisitor.visitInsn(MONITOREXIT);
				methodVisitor.visitLabel(label4);
				Label label10 = new Label();
				methodVisitor.visitJumpInsn(GOTO, label10);
				methodVisitor.visitLabel(label2);
				methodVisitor.visitVarInsn(ALOAD, 1);
				methodVisitor.visitInsn(MONITOREXIT);
				methodVisitor.visitLabel(label5);
				methodVisitor.visitInsn(ATHROW);
				methodVisitor.visitLabel(label10);
				methodVisitor.visitInsn(ICONST_0);
				methodVisitor.visitInsn(IRETURN);
				Label label11 = new Label();
				methodVisitor.visitLabel(label11);
				methodVisitor.visitMaxs(2, 3);
				methodVisitor.visitEnd();
			}else {
				methodVisitor = super.visitMethod(ACC_PUBLIC | ACC_STATIC | ACC_SYNCHRONIZED, "clinitOnce$", String.format("(%s)Z", globname), null, null);
				methodVisitor.visitCode();

				methodVisitor.visitFieldInsn(GETSTATIC, className, "clinitOnce$", "Ljava/lang/String;");

				methodVisitor.visitJumpInsn(IFNONNULL, notnull);
				addClinitOnceCommon(methodVisitor, globname);
				
				if (null == staticLambdaClasses || !staticLambdaClasses.contains(className)) {// TODO: running clinit for every isolate may be inefficient for these classes
					// but we need to do this such that top level lambdas with invoke dynamic have
					// fiberizers when they are initalized in
					// non fiberized code (i.e. at bootstrap)
					methodVisitor.visitLdcInsn("initialized");
					methodVisitor.visitFieldInsn(PUTSTATIC, className, "clinitOnce$", "Ljava/lang/String;");
				}
				
				methodVisitor.visitInsn(ICONST_1);// true created and set state in global

				Label lastLab = new Label();
				methodVisitor.visitJumpInsn(GOTO, lastLab);

				methodVisitor.visitLabel(notnull);
				methodVisitor.visitInsn(ICONST_0);// false, no creation

				methodVisitor.visitLabel(lastLab);
				methodVisitor.visitInsn(IRETURN);

				methodVisitor.visitMaxs(1, 0);
				methodVisitor.visitEnd();
			}
		}
		
		private void addClinitOnceCommon(MethodVisitor methodVisitor, String globname) {
			methodVisitor.visitLabel(new Label());
			methodVisitor.visitVarInsn(ALOAD, 0);
			/*if(isBootstrapClass) {
				//call with fake fiber:
				methodVisitor.visitMethodInsn(INVOKESTATIC, "com/concurnas/bootstrap/runtime/cps/Fiber", "getCurrentFiberWithCreate", "()Lcom/concurnas/bootstrap/runtime/cps/Fiber;", false);
				methodVisitor.visitMethodInsn(INVOKESTATIC, className, "clinit$", String.format("(%sLcom/concurnas/bootstrap/runtime/cps/Fiber;)V", globname), this.isInterface);
			}
			else {*/
				methodVisitor.visitMethodInsn(INVOKESTATIC, className, "clinit$", String.format("(%s)V", globname), this.isInterface);
			//}

			// copy state from global instance to localized.
			if (!isBootstrapClass) {
				copyFieldsFromTo(methodVisitor, nonSharedStaticFieldsToCopy, this.getGlobName(), className, false);
			}
		}

		public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
			if (className.contains("$")) {// if nested class method needs to be public
				access = makePublic(access);
			}

			if (staticLambdaClasses != null && isNonGlobalerORPrimordial(className, null, name, staticLambdaClasses, clloader)) {
				return new StaticAccessMethodRepoint(this.staticLambdaClasses, super.visitMethod(access, name, desc, signature, exceptions), maxLocalSize.get(name + desc), clloader, locallyDefinedFields, maxLocalSize.keySet(), supername, interfaces, className, null, false);
			}

			if (name.equals("<clinit>")) {
				addClinitOnce();
				MethodVisitor mv = super.visitMethod(access, "clinit$", String.format("(L%s;)V", this.getGlobName()), null, exceptions);
				StaticAccessMethodRepoint clinitRepointer = new StaticAccessMethodRepoint(this.staticLambdaClasses, mv, maxLocalSize.get(name + desc), clloader, locallyDefinedFields, maxLocalSize.keySet(), supername, interfaces, className, className, true);// repoint all static field/method access
				clinitRepointer.staticFinalVars = this.staticFinalVars;
				return clinitRepointer;
			}

			if (Modifier.isStatic(access) /* && !isEnumCls */) {// && !name.equals("doings")){ //TODO: the doings hack
				// skip static module methods
				getGlobName();// may not have a clinit so we call it anyway

				if (Modifier.isNative(access) || isTraitMethod(name)) {
					access = makePublic(access);
					return super.visitMethod(access, name, desc, signature, exceptions);
				}

				MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
				redirectStaticMethodProxytoGlobal(mv, name, desc);
				return mv;
			} else {
				OwnerNameDesc me = new OwnerNameDesc(className, name, desc);
				if (this.shouldbeForcedPublic.contains(me)) {
					// if the thing is used in subclass/function which is static, then force it
					// public (bit of a hack, should use psar but cant be arsed)
					access = ACC_PUBLIC;
					methodsWhichWereForcedPublic.add(me);
				}
				MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
				return new StaticAccessMethodRepoint(this.staticLambdaClasses, mv, maxLocalSize.get(name + desc), clloader, locallyDefinedFields, maxLocalSize.keySet(), supername, interfaces, className, null, false);// repoint all static field/method access
			}

		}

		private void redirectStaticMethodProxytoGlobal(MethodVisitor mv, String name, String desc) {
			String globName = getGlobName(className);
			// return new StaticMethodProxytoGlobal(mv);//+ $Globals$
			mv.visitCode();
			mv.visitLabel(new Label());

			mv.visitMethodInsn(INVOKESTATIC, globName, "getInstance?", "()L" + globName + ";", false);

			int argSpace = 0;
			for (Type arg : Type.getArgumentTypes(desc)) {
				mv.visitVarInsn(arg.getOpcode(Opcodes.ILOAD), argSpace);
				argSpace += arg.getSize();
			}

			mv.visitMethodInsn(INVOKEVIRTUAL, globName, name, desc, false);
			Type retType = Type.getReturnType(desc);

			int opcode = retType.getOpcode(Opcodes.IRETURN); // Opcodes.ARETURN;
			/*
			 * if(retType.getDimensions() == 1){
			 * 
			 * }
			 */

			mv.visitInsn(opcode);
			mv.visitMaxs(1, 0);
			mv.visitEnd();
		}

	}

	/*
	 * private static class StaticMethodProxytoGlobal extends MethodVisitor { public
	 * StaticMethodProxytoGlobal(MethodVisitor mv) { super(ASM7, mv); } }
	 */

	/*private static class EnumValuesFieldRepointer extends MethodVisitor {
		public EnumValuesFieldRepointer(MethodVisitor mv) {
			super(ASM7, mv);
		}

		public void visitFieldInsn(int opcode, String owner, String name, String desc) {
			// ->
			// INVOKESTATIC bytecodeSandbox$MYE$Globals$.getInstance?
			// ()LbytecodeSandbox$MYE$Globals$;
			// GETFIELD TestClass$MyEnum.ENUM$VALUES : TestClass$MyEnum[] -> GETSTATIC
			// TestClass$MyEnum$Globals$.ENUM$VALUES : TestClass$MyEnum[]
			if (opcode == Opcodes.GETSTATIC && name.equals("ENUM$VALUES")) {
				owner += "$Globals$";
				mv.visitMethodInsn(INVOKESTATIC, owner, "getInstance?", "()L" + owner + ";", false);
				opcode = Opcodes.GETFIELD;
			}

			super.visitFieldInsn(opcode, owner, name, desc);
		}
	}*/

	private class GlobalClassGennerator extends ClassVisitor {

		private final String fromName;
		private final String globName;
		//private boolean isEnumCls;
		private final ConcClassUtil clloader;
		private final HashMap<String, Integer> maxLocalSize;
		private final HashSet<OwnerNameDesc> methodsWhichWereForcedPublic;
		private final ArrayList<FieldVisitorHolder> staticFinalVars;
		private HashSet<String> staticLambdaClasses;
		private String[] ifaces;
		private String superClass;
		private List<Pair<String, String>> nonSharedStaticFieldsToCopy;
		private boolean isActingOnInterface;

		public GlobalClassGennerator(HashSet<String> staticLambdaClasses, 
				ClassVisitor cv, String fromName, String globName, 
				HashMap<String, Integer> maxLocalSize, ConcClassUtil clloader, HashSet<OwnerNameDesc> methodsWhichWereForcedPublic, 
				ArrayList<FieldVisitorHolder> staticFinalVars, 
				List<Pair<String, String>> nonSharedStaticFieldsToCopy, boolean isActingOnInterface) {
			super(ASM7, cv);
			this.fromName = fromName;
			this.globName = globName;
			this.maxLocalSize = maxLocalSize;
			this.clloader = clloader;
			this.methodsWhichWereForcedPublic = methodsWhichWereForcedPublic;
			this.staticFinalVars = staticFinalVars;
			this.staticLambdaClasses = staticLambdaClasses;
			this.nonSharedStaticFieldsToCopy = nonSharedStaticFieldsToCopy;
			this.isActingOnInterface = isActingOnInterface;
		}

		public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
			//isEnumCls = (access & Opcodes.ACC_ENUM) != 0 && superName.equals("java/lang/Enum");

			if (fromName.equals(name)) {// TODO: remove this check
				super.visit(version, ACC_PUBLIC + ACC_SUPER + ACC_STATIC, globName, null, "com/concurnas/bootstrap/runtime/cps/CObject", null);
			}

			this.superClass = superName;
			this.ifaces = interfaces;

			return;
		}

		// cw.visitInnerClass("Akimbo$Globals$", "Akimbo", "Globals$", ACC_PUBLIC +
		// ACC_STATIC);

		public void visitInnerClass(String name, String outerName, String innerName, int access) {
			super.visitInnerClass(name, outerName, innerName, access);
		}

		private void createInializerAndSupportMethods(boolean visitInit) {
			// only visitInit - true: when there is something to actually init
			String instance = "instance?";

			String globNameInPoshForm = "L" + globName + ";";

			{
				FieldVisitor fv = super.visitField(ACC_PRIVATE + ACC_STATIC, instance, "Ljava/util/WeakHashMap;", "Ljava/util/WeakHashMap<Lcom/concurnas/bootstrap/runtime/cps/Fiber;" + globNameInPoshForm + ">;", null);
				fv.visitEnd();
			}

			{
				MethodVisitor mv = super.visitMethod(ACC_STATIC, "<clinit>", "()V", null, null);
				mv.visitCode();
				Label l0 = new Label();
				mv.visitLabel(l0);
				mv.visitTypeInsn(NEW, "java/util/WeakHashMap");
				mv.visitInsn(DUP);
				mv.visitMethodInsn(INVOKESPECIAL, "java/util/WeakHashMap", "<init>", "()V", false);
				mv.visitFieldInsn(PUTSTATIC, this.globName, instance, "Ljava/util/WeakHashMap;");
				mv.visitInsn(RETURN);
				mv.visitMaxs(2, 0);
				mv.visitEnd();
			}

			{
				MethodVisitor mv = super.visitMethod(ACC_PUBLIC + ACC_STATIC + ACC_SYNTHETIC + ACC_SYNCHRONIZED, "getInstance?", "()" + globNameInPoshForm, null, null);
				mv.visitCode();
				mv.visitLabel(new Label());
				mv.visitMethodInsn(INVOKESTATIC, "com/concurnas/bootstrap/runtime/cps/Fiber", "getCurrentFiber", "()Lcom/concurnas/bootstrap/runtime/cps/Fiber;", false);
				mv.visitVarInsn(ASTORE, 0);
				Label l1 = new Label();
				mv.visitLabel(l1);
				mv.visitFieldInsn(GETSTATIC, this.globName, instance, "Ljava/util/WeakHashMap;");
				mv.visitVarInsn(ALOAD, 0);
				mv.visitMethodInsn(INVOKEVIRTUAL, "java/util/WeakHashMap", "get", "(Ljava/lang/Object;)Ljava/lang/Object;", false);
				mv.visitTypeInsn(CHECKCAST, this.globName);
				mv.visitVarInsn(ASTORE, 1);

				Label l2 = new Label();
				mv.visitLabel(l2);
				mv.visitVarInsn(ALOAD, 1);
				Label l3 = new Label();
				mv.visitJumpInsn(IFNONNULL, l3);
				Label l4 = new Label();
				mv.visitLabel(l4);
				mv.visitTypeInsn(NEW, this.globName);
				mv.visitInsn(DUP);
				mv.visitMethodInsn(INVOKESPECIAL, this.globName, "<init>", "()V", false);
				mv.visitVarInsn(ASTORE, 1);

				Label l5 = new Label();
				mv.visitLabel(l5);
				mv.visitFieldInsn(GETSTATIC, this.globName, instance, "Ljava/util/WeakHashMap;");
				mv.visitVarInsn(ALOAD, 0);
				mv.visitVarInsn(ALOAD, 1);
				mv.visitMethodInsn(INVOKEVIRTUAL, "java/util/WeakHashMap", "put", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", false);
				mv.visitInsn(POP);
				if (visitInit) {
					mv.visitVarInsn(ALOAD, 1);
					mv.visitMethodInsn(INVOKESPECIAL, this.globName, "init", "()V", false);
				}
				mv.visitLabel(l3);
				mv.visitFrame(Opcodes.F_APPEND, 2, new Object[] { "com/concurnas/bootstrap/runtime/cps/Fiber", this.globName }, 0, null);

				mv.visitVarInsn(ALOAD, 1);
				mv.visitInsn(ARETURN);
				Label l6 = new Label();
				mv.visitLabel(l6);
				mv.visitMaxs(3, 2);
				mv.visitEnd();
			}

			{
				MethodVisitor mv = super.visitMethod(ACC_PUBLIC + ACC_STATIC + ACC_SYNTHETIC + ACC_SYNCHRONIZED, "copyInstance?", "(Lcom/concurnas/bootstrap/runtime/CopyTracker;)" + globNameInPoshForm, null, null);
				mv.visitCode();
				mv.visitLabel(new Label());

				mv.visitMethodInsn(INVOKESTATIC, "com/concurnas/bootstrap/runtime/cps/Fiber", "getCurrentFiber", "()Lcom/concurnas/bootstrap/runtime/cps/Fiber;", false);
				mv.visitVarInsn(ASTORE, 1);
				Label l1 = new Label();
				mv.visitLabel(l1);
				mv.visitFieldInsn(GETSTATIC, this.globName, instance, "Ljava/util/WeakHashMap;");
				mv.visitVarInsn(ALOAD, 1);
				mv.visitMethodInsn(INVOKEVIRTUAL, "java/util/WeakHashMap", "get", "(Ljava/lang/Object;)Ljava/lang/Object;", false);
				mv.visitTypeInsn(CHECKCAST, this.globName);
				mv.visitVarInsn(ASTORE, 2);
				Label l2 = new Label();
				mv.visitLabel(l2);

				mv.visitVarInsn(ALOAD, 2);
				Label l3 = new Label();
				mv.visitJumpInsn(IFNONNULL, l3);
				mv.visitInsn(ACONST_NULL);
				Label l4 = new Label();
				mv.visitJumpInsn(GOTO, l4);
				mv.visitLabel(l3);

				mv.visitMethodInsn(INVOKESTATIC, "com/concurnas/runtime/bootstrapCloner/Cloner$Globals$", "getInstance?", "()Lcom/concurnas/runtime/bootstrapCloner/Cloner$Globals$;", false);
				mv.visitInsn(POP);

				Label la1 = new Label();
				mv.visitLabel(la1);

				mv.visitFieldInsn(GETSTATIC, "com/concurnas/runtime/bootstrapCloner/Cloner", "cloner", "Lcom/concurnas/runtime/bootstrapCloner/Cloner;");

				Label la2 = new Label();
				mv.visitLabel(la2);

				// cloner is not doing a clone of itself?
				mv.visitVarInsn(ALOAD, 0);
				mv.visitVarInsn(ALOAD, 2);
				mv.visitInsn(ACONST_NULL);
				mv.visitMethodInsn(INVOKEVIRTUAL, "com/concurnas/runtime/bootstrapCloner/Cloner", "clone", "(Lcom/concurnas/bootstrap/runtime/CopyTracker;Ljava/lang/Object;Lcom/concurnas/bootstrap/runtime/CopyDefinition;)Ljava/lang/Object;", false);
				mv.visitTypeInsn(CHECKCAST, this.globName);
				mv.visitLabel(l4);

				mv.visitInsn(ARETURN);
				mv.visitMaxs(3, 2);
				mv.visitEnd();
			}

			{
				MethodVisitor mv = super.visitMethod(ACC_PUBLIC + ACC_STATIC + ACC_SYNTHETIC + ACC_SYNCHRONIZED, "setInstance?", "(Lcom/concurnas/bootstrap/runtime/cps/Fiber;" + globNameInPoshForm + ")V", null, null);
				mv.visitCode();
				Label l0 = new Label();
				mv.visitLabel(l0);

				mv.visitFieldInsn(GETSTATIC, this.globName, instance, "Ljava/util/WeakHashMap;");
				mv.visitVarInsn(ALOAD, 0);
				mv.visitVarInsn(ALOAD, 1);
				mv.visitMethodInsn(INVOKEVIRTUAL, "java/util/WeakHashMap", "put", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", false);
				mv.visitInsn(POP);
				Label l1 = new Label();
				mv.visitLabel(l1);
				mv.visitInsn(RETURN);
				mv.visitMaxs(3, 2);
				mv.visitEnd();
			}

			{
				MethodVisitor mv = super.visitMethod(ACC_PUBLIC + ACC_STATIC + ACC_SYNTHETIC + ACC_SYNCHRONIZED, "removeInstance?", "()V", null, null);
				mv.visitCode();
				Label l0 = new Label();
				mv.visitLabel(l0);
				mv.visitFieldInsn(GETSTATIC, this.globName, instance, "Ljava/util/WeakHashMap;");
				mv.visitMethodInsn(INVOKESTATIC, "com/concurnas/bootstrap/runtime/cps/Fiber", "getCurrentFiber", "()Lcom/concurnas/bootstrap/runtime/cps/Fiber;", false);
				mv.visitMethodInsn(INVOKEVIRTUAL, "java/util/WeakHashMap", "remove", "(Ljava/lang/Object;)Ljava/lang/Object;", false);
				mv.visitInsn(POP);
				Label l1 = new Label();
				mv.visitLabel(l1);
				mv.visitInsn(RETURN);
				mv.visitMaxs(2, 1);
				mv.visitEnd();
			}

		}
		/*
		private void createInializerAndSupportMethodsEnum() {
			// only visitInit - true: when there is something to actually init

			String instance = "singleEnuminstance?";

			String globNameInPoshForm = "L" + globName + ";";

			{
				FieldVisitor fv = super.visitField(ACC_PRIVATE + ACC_STATIC, instance, globNameInPoshForm, globNameInPoshForm, null);
				fv.visitEnd();
			}

			{
				MethodVisitor mv = super.visitMethod(ACC_STATIC, "<clinit>", "()V", null, null);
				mv.visitCode();
				mv.visitLabel(new Label());
				mv.visitInsn(RETURN);
				mv.visitMaxs(2, 0);
				mv.visitEnd();
			}

			{
				MethodVisitor mv = super.visitMethod(ACC_PUBLIC + ACC_STATIC + ACC_SYNTHETIC + ACC_SYNCHRONIZED, "getInstance?", "()" + globNameInPoshForm, null, null);
				mv.visitCode();
				mv.visitLabel(new Label());

				mv.visitFieldInsn(GETSTATIC, this.globName, instance, globNameInPoshForm);
				Label end = new Label();
				mv.visitJumpInsn(IFNONNULL, end);
				mv.visitLabel(new Label());
				mv.visitTypeInsn(NEW, this.globName);
				mv.visitInsn(DUP);
				mv.visitInsn(DUP);
				mv.visitMethodInsn(INVOKESPECIAL, this.globName, "<init>", "()V", false);
				mv.visitMethodInsn(INVOKESPECIAL, this.globName, "init", "()V", false);
				mv.visitFieldInsn(PUTSTATIC, this.globName, instance, globNameInPoshForm);

				mv.visitLabel(end);

				mv.visitFieldInsn(GETSTATIC, this.globName, instance, globNameInPoshForm);

				mv.visitInsn(ARETURN);
				mv.visitMaxs(3, 2);
				mv.visitEnd();
			}

			{
				MethodVisitor mv = super.visitMethod(ACC_PUBLIC + ACC_STATIC + ACC_SYNTHETIC + ACC_SYNCHRONIZED, "copyInstance?", "(Lcom/concurnas/bootstrap/runtime/CopyTracker;)" + globNameInPoshForm, null, null);
				mv.visitCode();
				mv.visitLabel(new Label());
				mv.visitMethodInsn(INVOKESTATIC, this.globName, "getInstance?", "()" + globNameInPoshForm, false);
				mv.visitInsn(ARETURN);
				mv.visitMaxs(3, 2);
				mv.visitEnd();
			}

			{// nop
				MethodVisitor mv = super.visitMethod(ACC_PUBLIC + ACC_STATIC + ACC_SYNTHETIC + ACC_SYNCHRONIZED, "setInstance?", "(Lcom/concurnas/bootstrap/runtime/cps/Fiber;" + globNameInPoshForm + ")V", null, null);
				mv.visitCode();
				mv.visitLabel(new Label());
				mv.visitInsn(RETURN);
				mv.visitMaxs(3, 2);
				mv.visitEnd();
			}

			{// nop
				MethodVisitor mv = super.visitMethod(ACC_PUBLIC + ACC_STATIC + ACC_SYNTHETIC + ACC_SYNCHRONIZED, "removeInstance?", "()V", null, null);
				mv.visitCode();
				mv.visitLabel(new Label());
				mv.visitInsn(RETURN);
				mv.visitMaxs(3, 2);
				mv.visitEnd();
			}

		}*/

		private boolean visitedClinit = false;

		private void addEmptyInit() {// super simple
			MethodVisitor mv = super.visitMethod(ACC_PRIVATE, "<init>", "()V", null, null);
			mv.visitCode();
			mv.visitVarInsn(ALOAD, 0);
			mv.visitMethodInsn(INVOKESPECIAL, "com/concurnas/bootstrap/runtime/cps/CObject", "<init>", "()V", false);
			mv.visitInsn(RETURN);
			mv.visitMaxs(1, 1);
			mv.visitEnd();
		}

		private void addSyncInitMethod() {
			/*
			 * inst$Gloabl -> <init> inst$Gloabl -> inst.initOnce (if not called before) =
			 * contents of clinit inst$Gloabl -> copy static from inst instance of field to
			 * inst$Gloabl unless primative non array type or shared
			 * 
			 * shared things stay as static fields on origonal non instance version of cls
			 */

			MethodVisitor mv = super.visitMethod(ACC_PUBLIC, "init", "()V", null, null);
			mv.visitCode();

			mv.visitVarInsn(ALOAD, 0);
			
			
			/*if(isBootstrapClass && staticLambdaClasses != null && staticLambdaClasses.contains(fromName) && fromName.contains("FindOps")) {
				//call with fake fiber:
				mv.visitMethodInsn(INVOKESTATIC, "com/concurnas/bootstrap/runtime/cps/Fiber", "getCurrentFiberWithCreate", "()Lcom/concurnas/bootstrap/runtime/cps/Fiber;", false);
				mv.visitMethodInsn(INVOKESTATIC, fromName, "clinitOnce$", String.format("(L%s;Lcom/concurnas/bootstrap/runtime/cps/Fiber;)Z", globName), this.isActingOnInterface);
			}
			else {*/
				mv.visitMethodInsn(INVOKESTATIC, fromName, "clinitOnce$", String.format("(L%s;)Z", globName), this.isActingOnInterface);
			//}
			
			

			if (!isBootstrapClass) {
				Label firstCall = new Label();
				mv.visitJumpInsn(IFNE, firstCall);
				// we need to copy to globals if not the first call

				copyFieldsFromTo(mv, nonSharedStaticFieldsToCopy, fromName, globName, true);
				mv.visitLabel(firstCall);// on first call data has already been copied into global
			} else {
				mv.visitInsn(POP);
			}

			mv.visitInsn(RETURN);
			mv.visitMaxs(3, 1);
			mv.visitEnd();

		}

		public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {

			if (name.equals("<clinit>")) {
				visitedClinit = true;
				// steal the contents! - use this in the init method
				//if (isEnumCls) {
				//	createInializerAndSupportMethodsEnum();
				//} else {
					createInializerAndSupportMethods(true);
				//}
				addEmptyInit();

				addSyncInitMethod();
				// keep clinit method in origonal class
				return null;

			} else {
				if (Modifier.isStatic(access)) {

					if (Modifier.isNative(access)) {
						return new RepointNativeStaticMethodBackToNonGlobal(super.visitMethod(ACC_PUBLIC, name, desc, signature, exceptions), name, desc, fromName);
					} else if (isTraitMethod(name)) {
						return null;
					}

					if (/* name.equals("metaBinary") && */ desc.equals("(Lcom/concurnas/bootstrap/lang/offheap/Decoder;)V")) {
						return null;
					}

					// route to static instance, but also route all local static field and method
					// invokations to local instance (ALOAD 0; etc)
					return new StaticAccessMethodRepoint(this.staticLambdaClasses, super.visitMethod(ACC_PUBLIC, name, desc, signature, exceptions), maxLocalSize.get(name + desc), fromName, this.clloader, methodsWhichWereForcedPublic, this.locallyDefinedFields, maxLocalSize.keySet(), superClass, ifaces, this.fromName, null, false);// no longer static - cheat and make public i.e. ignore: access &=
					// ~Modifier.STATIC
				}
			}

			return null;
		}

		private HashSet<String> locallyDefinedFields = new HashSet<String>();
		// private List<Pair<String, String>> nonSharedStaticFieldsToCopy = new
		// ArrayList<Pair<String, String>>();

		public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
			locallyDefinedFields.add(name + desc);
			if (Modifier.isStatic(access)) {
				boolean shared = false;
				try {
					shared = this.clloader.getDetector().classForName(fromName).getField(name).isShared;
				} catch (Throwable e) {
					// throw new RuntimeException("Cannot find class: "+ fromName + " as " +
					// e.getMessage(), e);
				}

				if (!shared) {// shared are not mapped
					// nonSharedStaticFieldsToCopy.add(new Pair<>(name, desc));

					int accessp = ACC_PUBLIC;

					if (Modifier.isFinal(access)) {
						accessp += ACC_FINAL;
					}

					return super.visitField(accessp, name, desc, signature, value);// TODO: treat as local field, non static, cheat and make public [nasty]
				}
			}

			return null;
		}

		private void addInitforStaticFinalVars() {// super simple
			MethodVisitor mv = super.visitMethod(ACC_PUBLIC, "init", "()V", null, null);
			mv.visitCode();
			addstaticFinalVars(mv, this.staticFinalVars, this.fromName + "$Globals$");
			mv.visitInsn(RETURN);
			mv.visitMaxs(1, 1);
			mv.visitEnd();
		}

		public void visitEnd() {
			if (!visitedClinit) {// didnt have a clinit, so create a simple init and getter now...
				if (!this.staticFinalVars.isEmpty()) {
					addInitforStaticFinalVars();
					createInializerAndSupportMethods(true);
				} else {
					createInializerAndSupportMethods(false);
				}
				// TODO: this code is only really useful in case where u do this class X{
				// fromouter = 99 class Y{ y = 1 + fromouter }} //since fromouter is from psar
				// which is static...
				addEmptyInit();
			}

			super.visitEnd();
		}
	}

	private static class RepointNativeStaticMethodBackToNonGlobal extends MethodVisitor {

		private String methodName;
		private String desc;
		private String origClsName;

		protected RepointNativeStaticMethodBackToNonGlobal(MethodVisitor mv, String name, String desc, String origClsName) {
			super(ASM7, mv);
			this.methodName = name;
			this.desc = desc;
			this.origClsName = origClsName;
		}

		@Override
		public void visitEnd() {// normal wew'd use visitCode, but there is no code to visit!
			int locVar = 1;

			for (char c : ANFTransform.getPrimOrObj(desc)) {// reload the stack from vars
				mv.visitVarInsn(ANFTransform.getLoadOp(c), locVar++);
				if (c == 'D' || c == 'J') {
					locVar++;
				}
			}

			mv.visitMethodInsn(INVOKESTATIC, origClsName, methodName, desc, false);

			String dd = Type.getMethodType(desc).getReturnType().getDescriptor();
			mv.visitInsn(dd.startsWith("[") ? ARETURN : ANFTransform.getReturnOp(desc.charAt(desc.length() - 1)));// return
		}
	}

	private HashMap<String, byte[]> globalizerClasses = new HashMap<String, byte[]>();

	/**
	 * Find all constructors OR Fields refered visited inside all nested static
	 * method/classes. If one of these is created in parent class, then force it
	 * public. Since it will now be called from static globalizer class [which wont
	 * have access without a psar - which is an ugly java hack anyway imho...]
	 */
	private static class UsedPrivConstruFinder extends ClassVisitor {

		public UsedPrivConstruFinder() {
			super(ASM7);
		}

		private Stack<Boolean> isCurrentStatic = new Stack<Boolean>();
		public final HashSet<OwnerNameDesc> shouldbeForcedPublic = new HashSet<OwnerNameDesc>();// esnetially just a list of all constructors method visited during static
																								// method/class
		public final HashSet<OwnerNameDesc> shouldbeForcedPublicField = new HashSet<OwnerNameDesc>();

		private class MethodV extends MethodVisitor {
			public MethodV(MethodVisitor mv) {
				super(ASM7, mv);
			}

			public void visitEnd() {
				super.visitEnd();
				isCurrentStatic.pop();// bit dirty to stick this here oh well
			}

			public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
				if (isCurrentStatic.peek()) {
					shouldbeForcedPublic.add(new OwnerNameDesc(owner, name, desc));
				}
				super.visitMethodInsn(opcode, owner, name, desc, itf);
			}

			public void visitFieldInsn(int opcode, String owner, String name, String desc) {
				if (isCurrentStatic.peek()) {
					shouldbeForcedPublicField.add(new OwnerNameDesc(owner, name));
				}
				super.visitFieldInsn(opcode, owner, name, desc);
			}

		}

		private void goIn(int access) {
			boolean isStatic = Modifier.isStatic(access);
			if (isCurrentStatic.isEmpty()) {
				isCurrentStatic.push(isStatic);
			} else {
				isCurrentStatic.push(isCurrentStatic.peek() || isStatic);
			}
		}

		public void visitInnerClass(String name, String outerName, String innerName, int access) {
			goIn(access);
			super.visitInnerClass(name, outerName, innerName, access);
			isCurrentStatic.pop();
		}

		public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
			goIn(access);
			return new MethodV(super.visitMethod(access, name, desc, signature, exceptions));
		}
	}

	private static class FieldVisitorHolder {
		public int access;
		public String name;
		public String desc;
		public String signature;
		public Object value;

		public FieldVisitorHolder(int access, String name, String desc, String signature, Object value) {
			this.access = access;
			this.name = name;
			this.desc = desc;
			this.signature = signature;
			this.value = value;
		}
	}

	private static class StaticFieldFinder extends ClassVisitor {
		private final ConcClassUtil clloader;
		private ClassMirror cm;

		public List<Pair<String, String>> nonSharedStaticFieldsToCopy = new ArrayList<Pair<String, String>>();

		public StaticFieldFinder(ConcClassUtil clloader) {
			super(ASM7);
			this.clloader = clloader;
		}

		public void visit(final int version, final int access, final String name, final String signature, final String superName, final String[] interfaces) {
			super.visit(version, access, name, signature, superName, interfaces);
			try {
				cm = this.clloader.getDetector().classForName(name);
			} catch (Throwable e) {
				// throw new RuntimeException("Cannot find class: "+ fromName + " as " +
				// e.getMessage(), e);
			}
		}

		public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
			if (Modifier.isStatic(access)) {
				boolean shared = null != cm ? cm.getField(name).isShared : false;
				if (!shared) {// shared are not mapped
					nonSharedStaticFieldsToCopy.add(new Pair<>(name, desc));
				}
			}
			return null;
		}
	}

	private static class StaticFinalVariableFinder extends ClassVisitor {

		public StaticFinalVariableFinder() {
			super(ASM7);
		}

		public ArrayList<FieldVisitorHolder> staticFinalVars = new ArrayList<FieldVisitorHolder>();

		public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
			if (Modifier.isStatic(access) && Modifier.isFinal(access) && null != value) {
				staticFinalVars.add(new FieldVisitorHolder(access, name, desc, signature, value));
			}

			return super.visitField(access, name, desc, signature, value);
		}

	}

	public HashMap<String, byte[]> transform(String name, ConcClassUtil clloader) {
		ClassReader cr = new ClassReader(this.inputClassBytes);
		// calculate the maxlocals for each method
		HashMap<String, Integer> maxlocalMap = this.mfl.getMaxlocalMap(name, cr);

		// find private constructors used within nested static functions or classes
		UsedPrivConstruFinder upcf = new UsedPrivConstruFinder();
		cr.accept(upcf, 0);
		HashSet<OwnerNameDesc> shouldbeForcedPublic = upcf.shouldbeForcedPublic;
		HashSet<OwnerNameDesc> shouldbeForcedPublicField = upcf.shouldbeForcedPublicField;

		ClassWriter cw = new ConcClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS, clloader.getDetector());// TODO: turn off compute frames?
		// cv forwards all events to cw

		StaticFieldFinder staticFieldFinder = new StaticFieldFinder(clloader);
		cr.accept(staticFieldFinder, 0);

		StaticFinalVariableFinder sfvf = new StaticFinalVariableFinder();
		cr.accept(sfvf, 0);
		ArrayList<FieldVisitorHolder> staticFinalVars = sfvf.staticFinalVars;
		
		TransformMainClass mainTransformation = new TransformMainClass(this.staticLambdaClasses, cw, maxlocalMap, clloader, shouldbeForcedPublic, shouldbeForcedPublicField, staticFieldFinder.nonSharedStaticFieldsToCopy, staticFinalVars);
		cr.accept(mainTransformation, 0);
		globalizerClasses.put(name, cw.toByteArray());

		// TODO: only redirect if in staticLambdaClasses

		if (staticLambdaClasses == null || staticLambdaClasses.contains(name)) {
			HashSet<OwnerNameDesc> methodsWhichWereForcedPublic = mainTransformation.methodsWhichWereForcedPublic;

			if (!mainTransformation.origClassClinitToNewOne.isEmpty()) {
				for (String clsName : mainTransformation.origClassClinitToNewOne.keySet()) {
					String globName = mainTransformation.origClassClinitToNewOne.get(clsName);

					ClassWriter gcw = new ConcClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS, clloader.getDetector());// TODO: turn off compute frames?

					GlobalClassGennerator gcg = new GlobalClassGennerator(this.staticLambdaClasses, gcw, clsName, globName, maxlocalMap, clloader, methodsWhichWereForcedPublic, staticFinalVars, staticFieldFinder.nonSharedStaticFieldsToCopy, mainTransformation.isInterface);

					cr.accept(gcg, 0);
					globalizerClasses.put(globName, gcw.toByteArray());
				}
			}
		}

		return globalizerClasses; // b2 represents the same class as b1
	}

}
