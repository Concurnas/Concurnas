package com.concurnas.runtimeCache;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Handle;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import com.concurnas.runtime.ConcClassUtil;
import com.concurnas.runtime.Pair;
import com.concurnas.runtime.cps.mirrors.ClassMirror;
import com.concurnas.runtime.cps.mirrors.ClassMirrorNotFoundException;
import com.concurnas.runtime.cps.mirrors.Detector;
import com.concurnas.runtime.cps.mirrors.FieldMirror;
import com.concurnas.runtimeCache.StaticLambdaClassesFinder.ClassNode;

public class StaticLambdaAndGraphBuilder implements Opcodes {

	public class StaticLambdaFinderCls extends ClassVisitor {

		boolean hasLambdaInClinit = false;
		private ClassNode me;
		private String superName;
		private String[] interfaces;
		private Detector detector;

		public StaticLambdaFinderCls(Detector detector) {
			super(ASM7);
			this.detector = detector;
		}

		public void visit(final int version, final int access, final String name, final String signature, final String superName, final String[] interfaces) {
			this.me = new ClassNode(name);
			classNameToDependors.put(name, this.me);
			
			this.superName = superName;
			this.interfaces = interfaces;
			
			/*
			 * Pair<ClassNode, Boolean> instx = new Pair<ClassNode, Boolean>(me, true);
			 * 
			 * if(null != superName) {
			 * getNode(superName).nodesDependingOnMyStaticFieldsOrMethods.add(instx); }
			 * 
			 * if(interfaces != null) { for(int n=0; n < interfaces.length; n++) {
			 * getNode(interfaces[n]).nodesDependingOnMyStaticFieldsOrMethods.add(instx); }
			 * }
			 */
			
		}

		private ClassNode getNode(String of) {
			if (!classNameToDependors.containsKey(of)) {
				classNameToDependors.put(of, new ClassNode(of));
			}
			return classNameToDependors.get(of);
		}
		
		private HashSet<String> localfields = new HashSet<String>();
		
		public FieldVisitor visitField(final int access, final String name, final String descriptor, final String signature, final Object value) {
			localfields.add(name + descriptor);
			
			return super.visitField(access, name, descriptor, signature, value);
		}

		private class StaticLambdaFinderInMethod extends MethodVisitor {

			private boolean isClinit;
			private String nameAndSig;
			private Pair<ClassNode, Boolean> instx;

			public StaticLambdaFinderInMethod(MethodVisitor methodVisitor, boolean isClinit, String nameAndSig) {
				super(ASM7, methodVisitor);
				this.isClinit = isClinit;
				this.instx = new Pair<ClassNode, Boolean>(me, isClinit);
				this.nameAndSig = nameAndSig;
			}

			public void visitInvokeDynamicInsn(final String name, final String descriptor, final Handle bootstrapMethodHandle, final Object... bootstrapMethodArguments) {
				if(isClinit) {
					hasLambdaInClinit = true;
				}
				
				me.methodsHavingInvokeDynamic.add(this.nameAndSig);
				
			}
			
			private String findLocationOfField(String name, String desc, String supCls, String[] ifaces) {
				return findLocationOfField(name, desc, supCls, ifaces, new HashSet<String>());
			}
			
			private String findLocationOfField(String name, String desc, String supCls, String[] ifaces, HashSet<String> searchAlready) {
				String curSup = supCls;
				while(null != curSup && !curSup.equals("java/lang/Object") && !searchAlready.contains(curSup)) {
					try {
						ClassMirror sup = detector.classForName(curSup);
						
						if(sup == null) {
							int h=9;
						}
						
						FieldMirror fm = sup.getField(name);
						if(null != fm && fm.desc.equals(desc)) {
							return curSup;
						}
						searchAlready.add(curSup);
						for(String iface : sup.getInterfacesNoEx()) {
							String got = findLocationOfField(name, desc, iface, null, searchAlready);
							if(got != null) {
								return got;
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
						String got =findLocationOfField(name, desc, iface, null, searchAlready);
						if(got != null) {
							return got;
						}
					}
				}
				return null;
			}

			public void visitFieldInsn(final int opcode, final String owner, final String name, final String descriptor) {
				
				if (opcode == Opcodes.GETSTATIC || opcode == Opcodes.PUTSTATIC) {
					if(!owner.equals(me.myName)) {
						getNode(owner).nodesDependingOnMyStaticFields.add(this.instx);
					}else if(!localfields.contains(name + descriptor)){
						//if owner is myname but field is not defined in class, find where it is defined
						String origin = findLocationOfField(name, descriptor, superName, interfaces);
						if(null != origin) {//include source of field and intermediate classes for inclusion.. TODO: remove, not used
							Pair<ClassNode, Boolean> intermia = new Pair<ClassNode, Boolean>(this.instx.getA(), true);
							getNode(origin).nodesDependingOnMyStaticFields.add(intermia);
						}
					}
				}
			}

			public void visitMethodInsn(final int opcode, final String owner, final String name, final String descriptor, final boolean isInterface) {
				if(!owner.equals(me.myName)) {
					String key = name + descriptor;
					ClassNode clsNode = getNode(owner);
					
					if(!clsNode.callersOfMethod.containsKey(key)) {
						clsNode.callersOfMethod.put(key, new HashSet<Pair<ClassNode, String>>());
					}
					clsNode.callersOfMethod.get(key).add(new Pair<ClassNode, String>(me, this.nameAndSig));
				}
			}
		}

		
		//TODO: include invoke dynamic itself in analysis
		
		
		public MethodVisitor visitMethod(final int access, final String name, final String descriptor, final String signature, final String[] exceptions) {
			return new StaticLambdaFinderInMethod(super.visitMethod(access, name, descriptor, signature, exceptions), name.equals("<clinit>"), name + descriptor);
		}

	}

	private HashMap<String, ClassNode> classNameToDependors;

	public StaticLambdaAndGraphBuilder(HashMap<String, ClassNode> classNameToDependors) {
		this.classNameToDependors = classNameToDependors;
	}

	public boolean hasStaticLambda(byte[] code, ConcClassUtil ccutil) {

		ClassReader cra = new ClassReader(code);
		StaticLambdaFinderCls superConFinder = new StaticLambdaFinderCls(ccutil.getDetector());
		cra.accept(superConFinder, ClassReader.EXPAND_FRAMES);

		return superConFinder.hasLambdaInClinit;
	}

}
