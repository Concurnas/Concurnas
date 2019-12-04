package com.concurnas.runtime;

import java.util.HashMap;
import java.util.HashSet;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import com.concurnas.runtime.cps.analysis.ConcClassWriter;
import com.concurnas.runtime.cps.mirrors.ClassMirror;
import com.concurnas.runtime.cps.mirrors.ClassMirrorNotFoundException;
import com.concurnas.runtime.cps.mirrors.FieldMirror;

/**
 * converts non ref fields to WeakHadhMap<Fiber, Object>... (+callers and initilizer)
 *
 */
public class RefFieldMapper implements Opcodes {
	private static ClassMirror RefCM = null;
	private static final HashSet<String> excludedRefs = new HashSet<String>();
	static {
		excludedRefs.add("com/concurnas/bootstrap/runtime/ref/Ref");
		excludedRefs.add("com/concurnas/runtime/ref/AbstractRef");
		excludedRefs.add("com/concurnas/bootstrap/runtime/ref/DefaultRef");
		excludedRefs.add("com/concurnas/bootstrap/runtime/ref/DirectlyArrayAssignable");
		excludedRefs.add("com/concurnas/bootstrap/runtime/ref/DirectlyArrayGettable");
		excludedRefs.add("com/concurnas/bootstrap/runtime/ref/DirectlyAssignable");
		excludedRefs.add("com/concurnas/bootstrap/runtime/ref/DirectlyGettable");
		excludedRefs.add("com/concurnas/runtime/ref/Local");
		excludedRefs.add("com/concurnas/bootstrap/runtime/ref/LocalArray");
		excludedRefs.add("com/concurnas/runtime/ref/RefArray");
		
	}
	
	private static boolean classHasRefFields(ConcurnasClassLoader loader, ClassMirror cm, String className) throws ClassMirrorNotFoundException {
		if(excludedRefs.contains(className)) {
			return false;
		}
		
		if(null == RefCM) {
			RefCM = loader.getDetector().classForName("com/concurnas/bootstrap/runtime/ref/Ref");
		}
		
		return RefCM.isAssignableFrom(cm);
	}
	
	public static byte[] doMapping(byte[] inputcode, String className, ConcurnasClassLoader loader){
		HashMap<String, FieldMirror> hasFields = null;
		if(!excludedRefs.contains(className)) {//not a predefined ref already (we dont want to remap the fields for these)
			try {
				ClassMirror cm = loader.getDetector().classForName(className);
				
				
				{
					//remap get and put to fields

					ClassReader origReader = new ClassReader(inputcode);
					MethodLocalVarsSizeFinder methodLocalVarsExtent = new MethodLocalVarsSizeFinder();
					origReader.accept(methodLocalVarsExtent, 0);
					
					origReader = new ClassReader(inputcode);
					ConcClassWriter cw = new ConcClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS, loader.getDetector());//TODO: consider removing COMPUTE_MAXS | COMPUTE_FRAMES this to make load time quicker

					NormalClassFieldMapper refMapper =  new NormalClassFieldMapper(cw, loader, methodLocalVarsExtent.methNameAndDescToSAL);
					origReader.accept(refMapper, 0 );
					
					inputcode = cw.toByteArray();
					
				}
				
				
				{//change field tpye in ref class and add initalizer
					if(classHasRefFields(loader, cm, className)) {//it's a ref class, remap fields
						ClassReader origReader = new ClassReader(inputcode);
						ConcClassWriter cw = new ConcClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS, loader.getDetector());//TODO: consider removing COMPUTE_MAXS | COMPUTE_FRAMES this to make load time quicker
						hasFields = cm.getFields();
						RefClassFieldMapper refMapper =  new RefClassFieldMapper(cw, className, hasFields);
						origReader.accept(refMapper, 0 );
						
						inputcode = cw.toByteArray();
					}else {
						return inputcode;
					}
				}
				
			} catch (ClassMirrorNotFoundException e) {
				return inputcode;
			}

		}
		
		{//its a ref class so we need to add the internal field copier
			ClassReader origReader = new ClassReader(inputcode);
			ConcClassWriter cw = new ConcClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS, loader.getDetector());//TODO: consider removing COMPUTE_MAXS | COMPUTE_FRAMES this to make load time quicker

			RefFieldCopierMethodAdder refMapper =  new RefFieldCopierMethodAdder(cw, hasFields, className);
			origReader.accept(refMapper, 0 );
			
			inputcode = cw.toByteArray();
		}			
		
		
		/*if(className.contains("CustRef")){
			System.out.println("effect of RefFieldMapper: " + className);
			try {
				BytecodePrettyPrinter.print(inputcode, true);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}*/
		
		return inputcode;
		
	}
	
	private static class RefFieldCopierMethodAdder extends ClassVisitor{

		private HashMap<String, FieldMirror> hasFields;
		private String className;

		public RefFieldCopierMethodAdder(ConcClassWriter cw, HashMap<String, FieldMirror> hasFields, String className) {
			super(ASM7, cw);
			this.hasFields = hasFields;
			this.className = className;
		}
		
		
		public void visitEnd() {
			MethodVisitor mv = super.visitMethod(ACC_PUBLIC, "initFields$", "(Lcom/concurnas/bootstrap/runtime/cps/Fiber;Lcom/concurnas/bootstrap/runtime/CopyTracker;)V", null, null);

			/*{
				AnnotationVisitor av0 = mv.visitAnnotation("Lcom/concurnas/runtime/ForceCPS;", true);
				av0.visitEnd();
			}*/

			mv.visitCode();
			if (hasFields != null) {//the refs seem to be the wrong way around...
				if(!hasFields.isEmpty()) {
					mv.visitMethodInsn(INVOKESTATIC, "com/concurnas/bootstrap/runtime/cps/Fiber", "getCurrentFiber", "()Lcom/concurnas/bootstrap/runtime/cps/Fiber;", false);
					mv.visitVarInsn(ASTORE, 3);
					int n=0;
					for(FieldMirror fm : hasFields.values()) {
						String name = fm.name;
						String desc = fm.desc;
						mv.visitVarInsn(ALOAD, 0);
						mv.visitFieldInsn(GETFIELD, className, name, "Ljava/util/WeakHashMap;");
						mv.visitInsn(DUP);
						
						mv.visitVarInsn(ALOAD, 3);
						mv.visitMethodInsn(INVOKEVIRTUAL, "java/util/WeakHashMap", "get", "(Ljava/lang/Object;)Ljava/lang/Object;", false);
						//copy..

						boolean fastcopy = false;
						if(desc.startsWith("[")){
						}else if(desc.length() != 1) {//non primativ, even the boxed version needs the slow copy
							String descClsName = desc.substring(1, desc.length()-1);
							try {
								Class.forName(descClsName.replace('/', '.'));
								//if loadable from root classloader (i.e. is a jdk class) then we cannot use the fast copier (since no impl)
							}catch(ClassNotFoundException e) {
								mv.visitTypeInsn(CHECKCAST, descClsName);
								
								mv.visitVarInsn(ALOAD, 2);
								/*mv.visitTypeInsn(NEW, "com/concurnas/bootstrap/runtime/CopyTracker");
								mv.visitInsn(DUP);
								mv.visitMethodInsn(INVOKESPECIAL, "com/concurnas/bootstrap/runtime/CopyTracker", "<init>", "()V");*/
								mv.visitInsn(ACONST_NULL);
								mv.visitMethodInsn(INVOKEVIRTUAL, descClsName, "copy", "(Lcom/concurnas/bootstrap/runtime/CopyTracker;Lcom/concurnas/bootstrap/runtime/CopyDefinition;)" + desc);

								fastcopy = true;
							}
									
						}
						
						if(!fastcopy) {
							mv.visitVarInsn(ASTORE, 4+n);
							
							mv.visitFieldInsn(GETSTATIC, "com/concurnas/runtime/bootstrapCloner/Cloner", "cloner", "Lcom/concurnas/runtime/bootstrapCloner/Cloner;");

							mv.visitVarInsn(ALOAD, 2);
							/*mv.visitTypeInsn(NEW, "com/concurnas/bootstrap/runtime/CopyTracker");
							mv.visitInsn(DUP);
							mv.visitMethodInsn(INVOKESPECIAL, "com/concurnas/bootstrap/runtime/CopyTracker", "<init>", "()V");*/

							mv.visitVarInsn(ALOAD, 4+n);
							mv.visitInsn(ACONST_NULL);
							mv.visitMethodInsn(INVOKEVIRTUAL, "com/concurnas/runtime/bootstrapCloner/Cloner", "clone", "(Lcom/concurnas/bootstrap/runtime/CopyTracker;Ljava/lang/Object;Lcom/concurnas/bootstrap/runtime/CopyDefinition;)Ljava/lang/Object;");
							
						}
						
						//copy done						
						
						mv.visitVarInsn(ALOAD, 1);
						mv.visitInsn(SWAP);
						mv.visitMethodInsn(INVOKEVIRTUAL, "java/util/WeakHashMap", "put", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", false);
						mv.visitInsn(POP);
						
						n++;
					}
				}
			}

			mv.visitLabel(new Label());
			mv.visitInsn(RETURN);
			mv.visitMaxs(4, 2);
			mv.visitEnd();

			super.visitEnd();
		}
	}
	
	
	/*
	 * convert ref field to WeakHadhMap
	 */
	private static class RefClassFieldMapper extends ClassVisitor{
		private String className;
		private HashMap<String, FieldMirror> fields;

		private RefClassFieldMapper( ClassWriter cv, String className, HashMap<String, FieldMirror> fields) {
			super(Opcodes.ASM7, cv);
			this.className=className;
			this.fields=fields;
		}
		
		public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
			return super.visitField(access, name, "Ljava/util/WeakHashMap;", "Ljava/util/WeakHashMap<Lcom/concurnas/bootstrap/runtime/cps/Fiber;L"+Concurnifier.boxReturnType(desc)+";>;", null);
		}
		
		//add initalizer
		public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
			MethodVisitor ret = cv.visitMethod(access, name, desc, signature, exceptions);
			if(name.equals("<init>")) {
				return new RefClassFieldInitMapper(ret);
			}
			
			return ret;
		}
		
		private class RefClassFieldInitMapper extends MethodVisitor{
			public RefClassFieldInitMapper(final MethodVisitor mv) {
				super(ASM7, mv);
			}
			
			boolean firstMethodVisit = true;
			public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
				super.visitMethodInsn(opcode, owner, name, desc, itf);
				
				if(firstMethodVisit) {
					firstMethodVisit=false;
					if(opcode == Opcodes.INVOKESPECIAL && name.equals("<init>")) {
						if(!owner.equals(className)) {//invocation of superclass, but not a this() reference
							for(String namex : fields.keySet()) {
								mv.visitVarInsn(ALOAD, 0);
								mv.visitTypeInsn(NEW, "java/util/WeakHashMap");
								mv.visitInsn(DUP);
								mv.visitMethodInsn(INVOKESPECIAL, "java/util/WeakHashMap", "<init>", "()V", false);
								mv.visitFieldInsn(PUTFIELD, className, namex, "Ljava/util/WeakHashMap;");
							}
						}
					}
				}
			}
			
		}
		
	}
	
	/*private static class StackAndLocals{
		public int stack;
		public int locals;
		
		public StackAndLocals(int stack, int locals) {
			this.stack = stack;
			this.locals = locals;
		}
	}*/
	
	private static class MethodLocalVarsSizeFinder extends ClassVisitor{
		//public HashMap<String, StackAndLocals> methNameAndDescToSAL = new HashMap<String, StackAndLocals>();
		public HashMap<String, Integer> methNameAndDescToSAL = new HashMap<String, Integer>();
		
		public MethodLocalVarsSizeFinder() {
			super(Opcodes.ASM7);
			
		}
		
		public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
			return new SALCreator(name + desc);
		}
		
		private class SALCreator extends MethodVisitor{
			private String name;
			public SALCreator(String name) {
				super(Opcodes.ASM7);
				this.name = name;
			}
			
			public void visitMaxs(int maxStack, int maxLocals) {
				methNameAndDescToSAL.put(name, maxLocals);
				super.visitMaxs(maxStack, maxLocals);
			}
		}
		
		
	}
	
/*	private static void unbox(MethodVisitor mv, String primative) {
		Integer h = 12;
		int a = h;
		switch(primative) {
			case "I": mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false); break;
		}
	}*/
	
	private static class NormalClassFieldMapper extends ClassVisitor{
		private ConcurnasClassLoader loader;
		private HashMap<String, Integer> methNameAndDescToSAL = new HashMap<String, Integer>();
		
		private NormalClassFieldMapper( ClassWriter cv, ConcurnasClassLoader loader, HashMap<String, Integer> methNameAndDescToSAL) {
			super(Opcodes.ASM7, cv);
			this.loader = loader;
			this.methNameAndDescToSAL = methNameAndDescToSAL;
		}
		
		public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
			Integer locals = methNameAndDescToSAL.get(name + desc);
			MethodVisitor ret = cv.visitMethod(access, name, desc, signature, exceptions);
			if(null == locals) {
				return ret;
			}
			return new NormalClassFieldMapperMethod(ret, locals);
		}
		
		private class NormalClassFieldMapperMethod extends MethodVisitor{
			private int tempVarForValueToPut;

			public NormalClassFieldMapperMethod(final MethodVisitor mv,  int locals) {
				super(ASM7, mv);
				this.tempVarForValueToPut = locals;
			}
			
			/*public void visitMaxs(int maxStack, int maxLocals) {
				mv.visitMaxs(maxStack, locals);
			}*/
			
			private boolean ownerHasRedirectedRefFields(String owner) {
				try {
					return classHasRefFields(loader, loader.getDetector().classForName(owner), owner);
				} catch (ClassMirrorNotFoundException e) {
					return false;
				}
			}
			
			public void visitFieldInsn(int opcode, String owner, String name, String desc) {
				if((opcode == GETFIELD || opcode == PUTFIELD) && ownerHasRedirectedRefFields(owner)) {
					if(opcode == GETFIELD) {
						mv.visitFieldInsn(GETFIELD, owner, name, "Ljava/util/WeakHashMap;");
						mv.visitMethodInsn(INVOKESTATIC, "com/concurnas/bootstrap/runtime/cps/Fiber", "getCurrentFiber", "()Lcom/concurnas/bootstrap/runtime/cps/Fiber;", false);
						mv.visitMethodInsn(INVOKEVIRTUAL, "java/util/WeakHashMap", "get", "(Ljava/lang/Object;)Ljava/lang/Object;", false);
						
						if(desc.length() == 1) {
							mv.visitTypeInsn(CHECKCAST, Concurnifier.boxReturnType(desc));
							Concurnifier.unboxToRequiredType(mv, desc);
						}else if(!desc.equals("Ljava/lang/Object;")) {
							mv.visitTypeInsn(CHECKCAST, desc.substring(1, desc.length()-1));
						}
					}else {//putfield
						//box
						Concurnifier.boxIfPrimative(mv, desc);
						mv.visitVarInsn(ASTORE, tempVarForValueToPut);

						mv.visitFieldInsn(GETFIELD, owner, name, "Ljava/util/WeakHashMap;");
						mv.visitMethodInsn(INVOKESTATIC, "com/concurnas/bootstrap/runtime/cps/Fiber", "getCurrentFiber", "()Lcom/concurnas/bootstrap/runtime/cps/Fiber;", false);
						
						mv.visitVarInsn(ALOAD, tempVarForValueToPut);
						mv.visitMethodInsn(INVOKEVIRTUAL, "java/util/WeakHashMap", "put", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", false);

						mv.visitInsn(POP);
						
						tempVarForValueToPut++;
					}
				}else {
					super.visitFieldInsn(opcode, owner, name, desc);
				}
			}
		}
		
	}
	
}
