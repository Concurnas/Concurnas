package com.concurnas.runtime;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Stack;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import com.concurnas.bootstrap.runtime.ref.Ref;
import com.concurnas.compiler.utils.Fourple;
import com.concurnas.compiler.utils.Thruple;
import com.concurnas.lang.Shared;
import com.concurnas.runtime.ConcurnasClassLoader.BoolPair;
import com.concurnas.runtime.cps.analysis.ConcClassWriter;
import com.concurnas.runtime.cps.mirrors.ClassMirror;
import com.concurnas.runtime.cps.mirrors.ClassMirrorNotFoundException;
import com.concurnas.runtime.cps.mirrors.Detector;

public class Concurnifier implements Opcodes   {
	
	private static final class ClonerTransformer extends ClassVisitor {
		private final static HashSet<String> immutablePrims = new HashSet<String>();
		static{
			immutablePrims.add("I");
			immutablePrims.add("Z");
			immutablePrims.add("J");
			immutablePrims.add("F");
			immutablePrims.add("D");
			immutablePrims.add("S");
			immutablePrims.add("C");
			immutablePrims.add("B");
			immutablePrims.add("S");
			immutablePrims.add("Ljava/lang/String;");
			
			immutablePrims.add("Ljava/lang/Integer;");
			immutablePrims.add("Ljava/lang/Long;");
			immutablePrims.add("Ljava/lang/Boolean;");
			immutablePrims.add("Ljava/lang/Class;");
			immutablePrims.add("Ljava/lang/Float;");
			immutablePrims.add("Ljava/lang/Double;");
			immutablePrims.add("Ljava/lang/Character;");
			immutablePrims.add("Ljava/lang/Byte;");
			immutablePrims.add("Ljava/lang/Short;");
			immutablePrims.add("Ljava/lang/Void;");
			
			immutablePrims.add("Ljava/math/BigInteger;");
			immutablePrims.add("Ljava/math/BigDecimal;");
			
			immutablePrims.add("Ljava/net/URI;");
			immutablePrims.add("Ljava/net/URL;");
			immutablePrims.add("Ljava/util/UUID;");
			immutablePrims.add("Ljava/util/regex/Pattern;");
			
			immutablePrims.add("boolean");
			immutablePrims.add("byte");
			immutablePrims.add("char");
			immutablePrims.add("double");
			immutablePrims.add("float");
			immutablePrims.add("int");
			immutablePrims.add("long");
			immutablePrims.add("short");
		}
		
		private final ConcurnasClassLoader immClassHolder;
		private final Detector extraClassInfo;
		
		public ClonerTransformer( ClassWriter cv, ConcurnasClassLoader immClassHolder) {
			super(Opcodes.ASM7, cv);
			this.immClassHolder=immClassHolder;
			this.extraClassInfo = this.immClassHolder.getDetector();
		}
		
		private boolean isInner=false;
		private boolean isInterface=false;
		private boolean isImmutable=true;//innocent until proven guitly
		private boolean isShared=false;
		private boolean isActor=false;
		private boolean isTransient=false;
		private boolean isClosedClass=false;
		private boolean isEnum=false;
		private Stack<String> currentClass = new Stack<String>();
		private Stack<String> currentClassPure = new Stack<String>();
		private Stack<String> superClass = new Stack<String>();
		private Stack<String[]> interfaces = new Stack<String[]>();
		private String theCurrentclass;

        //private boolean hasCopyConstructorAlready = false;
		private boolean hasDefaultFieldInit = false;
		private static final String obj = "java/lang/Object";
		private static final String enu = "java/lang/Enum";
		
		
		@Override
		public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
			// hasCopyConstructorAlready = false;
			isInner = true;// name.contains("$");//TODO: we only augment inner classes... for now -what about static stuff?
			isInterface = Modifier.isInterface(access);
			isClosedClass = (access & Modifier.FINAL) != 0;// non sealed
			isEnum = (access & Opcodes.ACC_ENUM) != 0;

			isImmutable = isEnum || isClosedClass;
			theCurrentclass = "L" + name + ";";
			currentClass.add(theCurrentclass);

			currentClassPure.add(name);

			superClass.push(superName);
			this.interfaces.push(interfaces);
			
			try {
				ClassMirror cm = this.extraClassInfo.classForName(name);
				if (null != cm) {
					isShared = cm.getAnnotations().contains("com/concurnas/lang/Shared");
				}
			} catch (ClassMirrorNotFoundException e1) {
			}

			if(immutablePrims.contains("L" + superName + ";")) {
				isImmutable=true;
			}
			else if (!obj.equals(superName)) {
				BoolPair imQuicke = immClassHolder.isImmutable("L" + superName + ";");
				ArrayList<String> sup = null;
				try {
					sup = this.extraClassInfo.getSuperClasses(superName);
					} catch (ClassMirrorNotFoundException e) {
				}

				if (!isShared) {
					isShared = sup.stream().anyMatch(a -> {
						try {
							ClassMirror cm = this.extraClassInfo.classForName(a);
							return cm == null ? false : cm.getAnnotations().contains("com/concurnas/lang/Shared");
						} catch (ClassMirrorNotFoundException e) {
							return false;
						}
					});
				}

				isActor = sup.contains("com/concurnas/lang/Actor");

				isImmutable &= (null == imQuicke.immutable ? false : imQuicke.immutable);
			}
			
			try {
				if (!isShared) {
					HashSet<String> ifaces = this.extraClassInfo.getTraitsAndTraitSuperClasses(name);
					isShared = ifaces.stream().anyMatch(a -> {
						try {
							ClassMirror cm = this.extraClassInfo.classForName(a);
							return cm == null ? false : cm.getAnnotations().contains("com/concurnas/lang/Shared");
						} catch (ClassMirrorNotFoundException e) {
							return false;
						}
					});
				}
				
			} catch (ClassMirrorNotFoundException e1) {
			}
			

			if (hasTransientAnnotation(name)) {
				isTransient = true;
			}

			super.visit(version, access, name, signature, superName, interfaces);
		}
        
        private static final String transientAnnotation = "com/concurnas/lang/Transient";
        
        private boolean hasTransientAnnotation(String name){
        	ClassMirror cm;
			try {
				while(null != name && !name.equals("java/lang/Object")){
					cm = this.extraClassInfo.classForName(name);
					if(null != cm){
						HashSet<String> anots = cm.getAnnotations();
						
						if(null != anots){
							if(anots.contains(transientAnnotation)) {
								return true;
							}
							/*for(String ann : anots){
								if(ann.equals(transientAnnotation)){
									return true;
								}
							}*/
						}
						
						name = cm.getSuperclass();
					}else{
						break;
					}
				}
			} catch (ClassMirrorNotFoundException e) {
				return true;//just return null on copy...
			}
        	
        	return false;
        }
        
        public HashSet<String> immutableDependsOn = new HashSet<String>();
        
        private ArrayList<Fourple<String, String, Boolean, Boolean>> fieldsForCopyConstructor = new ArrayList<Fourple<String, String, Boolean, Boolean>>();
        
        private ArrayList<Fourple<Integer, String, String, Boolean>> fields = new ArrayList<Fourple<Integer, String, String, Boolean>>();
        
        private class SharedFieldFinder extends FieldVisitor{
			private String name;

			public SharedFieldFinder(FieldVisitor fv, String name) {
				super(ASM7, fv);
				this.name = name;
			}
        	
		    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
		    	if(visible && desc.equals("Lcom/concurnas/lang/Shared;")) {
		    		sharedfields.add(name);
		    	}
		    	return super.visitAnnotation(desc, visible);
		    }
			
        }
        
        private HashSet<String> sharedfields = new HashSet<String>();
        
        @Override
        public FieldVisitor visitField(int access, String name, String desc, String signature, Object value){
	    	if(isInner){
	    		Fourple<Integer, String, String, Boolean> trup = new Fourple<Integer, String, String, Boolean>(access, name, desc, null != signature && !desc.equals(signature));
	    			    		
	    		if(currentClass.contains(desc)){
	    			fields.add(0, trup);
	    		}
	    		else{
	    			fields.add(trup);
	    		}
	    	}
    	
	    	return new SharedFieldFinder(super.visitField(access, name, desc, signature, value), name);
        }
		
        private void postProcessFields(){
        	//Collections.reverse(fields);
        	int n = fields.size()-1;
        	
        	while(n >= 0){
        		Fourple<Integer, String, String, Boolean> field = fields.get(n);
        		
        		int access = field.getA();
        		String name = field.getB();
        		String desc = field.getC();
        		boolean isGeneric = field.getD();
        		
            	boolean finalVar =  (access & Modifier.FINAL) != 0;
            	boolean isStatic =  (access & Modifier.STATIC) != 0;
            	boolean isTransient =  (access & Modifier.TRANSIENT) != 0;
        		boolean privateVar =  (access & Modifier.PRIVATE) != 0 || (access & Modifier.PROTECTED) != 0;
        		
        		if(!isTransient){
        			if(!(finalVar && privateVar)){
            			if(this.isEnum && this.currentClass.peek().equals(desc)){
            				//enum element, even though these are not final or private
            			}
            			else{
                			isImmutable=false;
            			}
            		}
            		if(!isStatic){
                		fieldsForCopyConstructor.add(determineImm(name, desc, isGeneric));
            		}
        		}else{
        			isImmutable=false;//must be copied if transient elements
        		}
        		
        		n--;
        	}
        	
        }
        
        private Fourple<String, String, Boolean, Boolean> determineImm(String name, String desc, boolean isGeneric){
    		Boolean isTypeImmu = true;
    		Boolean canUseQuickMethod = true;
    		if(isGeneric){
    			isTypeImmu=false;
    			canUseQuickMethod=false;
				isImmutable=false;
    		}
    		else if(desc.startsWith("[")){//its an array
    			if(this.isEnum && name.equals("ENUM$VALUES")){
    				//enum values field ignore
    			}
    			else{
    				isTypeImmu=false;
        			canUseQuickMethod=false;
    				isImmutable=false;
    			}
    		}
    		else if(currentClass.contains(desc)){//last thing
    			isTypeImmu = isImmutable;
    		}
    		else if(!immutablePrims.contains(desc))//&& ! (currentClass.contains(desc) && isImmutable)
    		{//not basic immutable...
    			//TODO: check for actor, ref, shared (annotation?), also final array of things that are immutable is ok
    			BoolPair got = immClassHolder.isImmutable(desc);
    			isTypeImmu = got.immutable;
    			canUseQuickMethod= got.canUseQuickMethod;
    			if(isTypeImmu==null){
    				//TODO: can this code ever be reached?
    				immutableDependsOn.add(desc);
    				isTypeImmu=false;
    				isImmutable=false;
    			}
    			else{
    				if(!isTypeImmu){
    					isImmutable=false;
    				}
    			}
    		}
    		
    		boolean iface = false;
    		boolean isShared=false;
    		if(desc.startsWith("L")) {
				try {
					String clsName = desc.substring(1, desc.length()-1);
					ClassMirror cm = this.extraClassInfo.classForName(clsName);
					if(null != cm) {
						iface = cm.isInterface();
						
						isShared = cm.getAnnotations().contains("Lcom/concurnas/lang/Shared;");
						
						if (!isShared) {
							String superClass = cm.getSuperclassNoEx();
							while (superClass != null) {
								cm = this.extraClassInfo.classForName(superClass);
							//	System.out.println(" " + superClass);
								isShared = cm.getAnnotations().contains("Lcom/concurnas/lang/Shared;");
								if (isShared) {
									break;
								}
								superClass = cm.getSuperclassNoEx();
							}

							if (!isShared) {
								try {
									HashSet<String> ifaces = this.extraClassInfo.getTraitsAndTraitSuperClasses(clsName);
									isShared = ifaces.stream().anyMatch(a -> {
										try {
											ClassMirror cmx = this.extraClassInfo.classForName(a);
											return cmx == null ? false : cmx.getAnnotations().contains("com/concurnas/lang/Shared");
										} catch (ClassMirrorNotFoundException e) {
											return false;
										}
									});
								} catch (ClassMirrorNotFoundException e1) {
								}
							}
						}
					}
					
				} catch (ClassMirrorNotFoundException e) {
					canUseQuickMethod=false;
				}
			}
    		
    		//this is shared, or parent is shared...
    		
    		return new Fourple<String, String, Boolean, Boolean>(name, desc, isShared || sharedfields.contains(name) || isTypeImmu, canUseQuickMethod && !iface);
        }
        
		public void visitInnerClass(String name, String outerName, String innerName, int access) {
			boolean ifaceBefore = isInterface;
			isInterface = Modifier.isInterface(access);
			
			if (isInner && !isInterface) {
				if (!currentClass.contains("L" + name + ";")) {
					this.isImmutable = false;//not immutale because has a child class (who knows what that kid is getting up to) :(
				}
			}
			super.visitInnerClass(name, outerName, innerName, access);
			isInterface=ifaceBefore;
		}
        
		
		
        @Override
        public void visitEnd() {
        	
        	if(isTransient){
        		addNullCopier();
        	}
        	
        	else if(isInner && !isInterface){//returns a copy of itself
        		ArrayList<Fourple<Integer, String, String, Boolean>> oldFields = fields;
        		postProcessFields();
        		fields=null;
        		boolean addcop = addCopier();
        		
        		{
        			String rawSupCls = "L" + this.superClass.peek() + ";";
                	String rawclsName = currentClass.peek();
                	String clsName = rawclsName.substring(1, rawclsName.length()-1);
                	addCopierBridge(rawSupCls, rawclsName, clsName);
                	
                	/*String[] items = this.interfaces.peek();
            		if(null != items && items.length > 0) {
            			for(String item : items) {
            				//addCopierBridge("L" +item+ ";", rawclsName, clsName);
            			}
            		}*/
        		}
        		
        		//if(callsConstructor){ super.visitAnnotation("Lcom/concurnas/runtime/ConcFastCloner;", true).visitEnd(); }
        		
        		if(/*!hasCopyConstructorAlready &&*/ !isImmutable && addcop){//it's changable, and calls the constructor...
        			try{
        				addConstructor(oldFields);
        			}
        			catch(ClassNotFoundException e){
        				throw new RuntimeException(e.getMessage(), e);
        			}
        		}
        		
        		if(isImmutable || isEnum){
            		super.visitAnnotation("Lcom/concurnas/runtime/ConcImmutable;", true).visitEnd();
            	}
        	}/*else if(isInterface) {
        		String rawclsName = currentClass.peek();
        		MethodVisitor mv = super.visitMethod(ACC_PUBLIC + ACC_ABSTRACT, "copy", "(Lcom/concurnas/bootstrap/runtime/CopyTracker;Lcom/concurnas/bootstrap/runtime/CopyDefinition;)" + rawclsName, null, null);
        		mv.visitEnd();
        	}*/
    		
            super.visitEnd();
        }
        
        @Override
    	public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        	/*if(access == ACC_PUBLIC && name.equals("<init>") && desc.equals("(Lcom/concurnas/bootstrap/runtime/CopyTracker;" + theCurrentclass +")V") && signature == null && exceptions == null){
        		hasCopyConstructorAlready=true;
        	}*/
        	
        	if((access & ACC_PRIVATE) == ACC_PRIVATE && (access & Opcodes.ACC_SYNTHETIC) == Opcodes.ACC_SYNTHETIC && name.equals("defaultFieldInit$") && desc.equals("(Lcom/concurnas/bootstrap/runtime/InitUncreatable;[Z)V") && signature == null && exceptions == null){
        		hasDefaultFieldInit=true;
        	}
        	
    		return super.visitMethod(access, name, desc, signature, exceptions);
    	}

        private void visitConstructorFieldsSet(MethodVisitor mv, String clsName, int copyDescSlot) {
        	
        	if(copyDescSlot > -1) {
        		//first we set the default values
        		CodeGenUtils.intOpcode(mv, fieldsForCopyConstructor.size());
        		mv.visitIntInsn(NEWARRAY, T_BOOLEAN);
        		mv.visitVarInsn(ASTORE, 4);
        	}
        	int n=0;
        	for(Fourple<String, String, Boolean, Boolean> field: fieldsForCopyConstructor){
				String desc = field.getB();
				String fname = field.getA();
				//call copier if type nonim
				Label continueOnNonInclude = null;
				if(copyDescSlot > -1) {//check copier to ensure we can include this field in the first place
					
					
        			mv.visitVarInsn(ALOAD, 4);
        			CodeGenUtils.intOpcode(mv, n++);
					
					mv.visitVarInsn(ALOAD, copyDescSlot);
					mv.visitLdcInsn(fname);
					mv.visitMethodInsn(INVOKEINTERFACE, "com/concurnas/bootstrap/runtime/CopyDefinition", "shouldCopyField", "(Ljava/lang/String;)Z", true);
					mv.visitInsn(DUP);
					mv.visitVarInsn(ISTORE, 5);
					//should copy => should not set default
					
					Label ifFalse = new Label();
					mv.visitJumpInsn(IFEQ, ifFalse);
					mv.visitInsn(ICONST_0);
					Label carryOn = new Label();
					mv.visitJumpInsn(GOTO, carryOn);
					mv.visitLabel(ifFalse);
					mv.visitInsn(ICONST_1);
					mv.visitLabel(carryOn);
					
            		mv.visitInsn(BASTORE);

					mv.visitVarInsn(ILOAD, 5);
					continueOnNonInclude = new Label();
					mv.visitJumpInsn(IFEQ, continueOnNonInclude);
   	        	}
				
				if(!field.getC()){//is type changable? else jsut copy it
	   	        	boolean useQuickMethod = field.getD();
	   	        	String justClassDesc;
	   	        	if(desc.startsWith("[")){
	   	        		justClassDesc = desc;// desc.substring(0, desc.length()-1);
	   	        	}else{
	   	        		justClassDesc = desc.substring(1, desc.length()-1);
	   	        	}
	   	        	
	   	        	mv.visitVarInsn(ALOAD, 0);

	   	        	if(copyDescSlot > -1) {//check copier desc
						mv.visitVarInsn(ALOAD, copyDescSlot);
						mv.visitLdcInsn(fname);
	   	        	}
	   	        	
	   	        	mv.visitVarInsn(ALOAD, 2);
					mv.visitFieldInsn(GETFIELD, clsName , fname, desc);
					
					if(copyDescSlot > -1) {//check copier desc
						mv.visitMethodInsn(INVOKEINTERFACE, "com/concurnas/bootstrap/runtime/CopyDefinition", "getOverride", "(Ljava/lang/String;Ljava/lang/Object;)Ljava/lang/Object;", true);
					    mv.visitTypeInsn(CHECKCAST, justClassDesc);
					}

					Label noNuill = new Label();
					mv.visitJumpInsn(IFNONNULL, noNuill);
					mv.visitInsn(ACONST_NULL);
					//null, ret null
					Label after = new Label();
					mv.visitJumpInsn(GOTO, after);
					
					mv.visitLabel(noNuill);
					
					
	   	        	if(useQuickMethod){//normal:
						mv.visitVarInsn(ALOAD, 2);
						
						mv.visitFieldInsn(GETFIELD, clsName , fname, desc);
						mv.visitVarInsn(ALOAD, 1);//TODO: what about arrays?
						
						if(copyDescSlot > -1) {//check copier desc
							mv.visitVarInsn(ALOAD, copyDescSlot);
							mv.visitLdcInsn(fname);
							mv.visitMethodInsn(INVOKEINTERFACE, "com/concurnas/bootstrap/runtime/CopyDefinition", "getFieldCopier", "(Ljava/lang/String;)Lcom/concurnas/bootstrap/runtime/CopyDefinition;", true);
		   	        	}else {
		   	        		mv.visitInsn(ACONST_NULL);
		   	        	}
						
						mv.visitMethodInsn(INVOKEVIRTUAL, justClassDesc, "copy", "(Lcom/concurnas/bootstrap/runtime/CopyTracker;Lcom/concurnas/bootstrap/runtime/CopyDefinition;)" + desc, false);
	   	        	}else{//if type from bootstrap, invoke slow copier
	   	        		mv.visitFieldInsn(GETSTATIC, "com/concurnas/runtime/bootstrapCloner/Cloner", "cloner", "Lcom/concurnas/runtime/bootstrapCloner/Cloner;");
	   	        		mv.visitVarInsn(ALOAD, 1);
	   	        		mv.visitVarInsn(ALOAD, 2);
	   	        		mv.visitFieldInsn(GETFIELD, clsName , fname, desc);

	   	        		if(copyDescSlot > -1) {//check copier desc
							mv.visitVarInsn(ALOAD, copyDescSlot);
							mv.visitLdcInsn(fname);
							mv.visitMethodInsn(INVOKEINTERFACE, "com/concurnas/bootstrap/runtime/CopyDefinition", "getFieldCopier", "(Ljava/lang/String;)Lcom/concurnas/bootstrap/runtime/CopyDefinition;", true);
		   	        	}else {
		   	        		mv.visitInsn(ACONST_NULL);
		   	        	}
	   	        		
	   	        		mv.visitMethodInsn(INVOKEVIRTUAL, "com/concurnas/runtime/bootstrapCloner/Cloner", "clone", "(Lcom/concurnas/bootstrap/runtime/CopyTracker;Ljava/lang/Object;Lcom/concurnas/bootstrap/runtime/CopyDefinition;)Ljava/lang/Object;", false);
	   	        		mv.visitTypeInsn(CHECKCAST, justClassDesc);
	   	        	}

	   	        	mv.visitLabel(after);
				}else{
	   	        	mv.visitVarInsn(ALOAD, 0);
					if(copyDescSlot > -1) {//check copier desc
						mv.visitVarInsn(ALOAD, copyDescSlot);
						mv.visitLdcInsn(fname);
	   	        	}
					
					mv.visitVarInsn(ALOAD, 2);
					mv.visitFieldInsn(GETFIELD, clsName , fname, desc);
					if(copyDescSlot > -1) {
						boxIfPrimative(mv, desc);
						mv.visitMethodInsn(INVOKEINTERFACE, "com/concurnas/bootstrap/runtime/CopyDefinition", "getOverride", "(Ljava/lang/String;Ljava/lang/Object;)Ljava/lang/Object;", true);
					    mv.visitTypeInsn(CHECKCAST, boxReturnType(desc));
						unboxToRequiredType(mv, desc);
					}
					
				}
				mv.visitFieldInsn(PUTFIELD, clsName , fname, desc);

				if(null != continueOnNonInclude) {
					mv.visitLabel(continueOnNonInclude);
				}
			}
        	
        	if(copyDescSlot > -1) {//load up missing default values
        		
        		mv.visitVarInsn(ALOAD, copyDescSlot);
				mv.visitMethodInsn(INVOKEINTERFACE, "com/concurnas/bootstrap/runtime/CopyDefinition", "incDefaults", "()Z", true);
				

				Label continueNoIncludeDefaults = new Label();
				mv.visitJumpInsn(IFEQ, continueNoIncludeDefaults);
        		
        		mv.visitVarInsn(ALOAD, 0);
        		mv.visitInsn(ACONST_NULL);
        		mv.visitVarInsn(ALOAD, 4);
				mv.visitMethodInsn(INVOKESPECIAL, clsName, "defaultFieldInit$", "(Lcom/concurnas/bootstrap/runtime/InitUncreatable;[Z)V", false);

				mv.visitLabel(continueNoIncludeDefaults);
        	}
        	
        }
        
		private void addConstructor(ArrayList<Fourple<Integer, String, String, Boolean>> allItems) throws ClassNotFoundException {
			//TODO: if runtime option set: output copy information?
			String rawclsName = currentClass.peek();
			String clsName = rawclsName.substring(1, rawclsName.length()-1);
			
			MethodVisitor mv = super.visitMethod(ACC_PUBLIC, "<init>", "(Lcom/concurnas/bootstrap/runtime/CopyTracker;"+ rawclsName  +"Lcom/concurnas/bootstrap/runtime/CopyDefinition;)V", null, null);
			mv.visitCode();
			
			//pass ref to super

			String superName = superClass.peek();
			
			mv.visitVarInsn(ALOAD, 0);
			if(obj.equals(superName)){
				mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
			}else if(enu.equals(superName)){

				mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Enum", "<init>", "()V", false);
			}
			else{
				mv.visitVarInsn(ALOAD, 1);
				mv.visitVarInsn(ALOAD, 2);

				{
					mv.visitVarInsn(ALOAD, 3);
					
					Label copySpecNotNull = new Label();
					mv.visitJumpInsn(IFNONNULL, copySpecNotNull);
					
					mv.visitInsn(ACONST_NULL);
					
					Label after = new Label();
					mv.visitJumpInsn(GOTO, after);
					mv.visitLabel(copySpecNotNull);
					mv.visitVarInsn(ALOAD, 3);
					mv.visitMethodInsn(INVOKEINTERFACE, "com/concurnas/bootstrap/runtime/CopyDefinition", "getSuperCopier", "()Lcom/concurnas/bootstrap/runtime/CopyDefinition;", true);
					
					
					mv.visitLabel(after);
				}
				mv.visitMethodInsn(INVOKESPECIAL, superName, "<init>", "(Lcom/concurnas/bootstrap/runtime/CopyTracker;L"+superName+";Lcom/concurnas/bootstrap/runtime/CopyDefinition;)V", false);
			}
			
			if(fieldsForCopyConstructor.size() > 0){
				//if haz fields, then add copy of self to da local index
				//do CopyDefinition != null path first then null path
				
				mv.visitVarInsn(ALOAD, 3);
				
				Label copySpecNotNull = new Label();
				mv.visitJumpInsn(IFNONNULL, copySpecNotNull);
				
				visitConstructorFieldsSet(mv, clsName, -1);
				
				Label after = new Label();
				mv.visitJumpInsn(GOTO, after);
				mv.visitLabel(copySpecNotNull);
				
				visitConstructorFieldsSet(mv, clsName, 3);
				mv.visitLabel(after);
			}
			
			if(hasDefaultFieldInit){//add a call to the defaultFieldInit$ method with refernces to the transient fields if there are any
				int ofsize = allItems.size();
				int usedFieldsSize = fieldsForCopyConstructor.size();
				
				if(ofsize > 0 && ofsize > usedFieldsSize){//interesting? contains transients
					mv.visitVarInsn(ALOAD, 0);
					mv.visitInsn(ACONST_NULL);
					mv.visitInsn(ACONST_NULL);

					mv.visitMethodInsn(INVOKESPECIAL, clsName, "defaultFieldInit$", "(Lcom/concurnas/bootstrap/runtime/InitUncreatable;[Z)V");
				}
			}
			
			//if has a defaultFieldInit$(Lcom/concurnas/bootstrap/runtime/InitUncreatable;[Z)V
			
			
			mv.visitInsn(RETURN);
			mv.visitMaxs(2, 3);//doesnt have to be right
			mv.visitEnd();
		}

		private void addNullCopier(){
        	String rawclsName = currentClass.peek();
			MethodVisitor mv = super.visitMethod(Opcodes.ACC_PUBLIC, "copy", "(Lcom/concurnas/bootstrap/runtime/CopyTracker;Lcom/concurnas/bootstrap/runtime/CopyDefinition;)" + rawclsName, null, null);
        	mv.visitCode();
        	
        	mv.visitInsn(ACONST_NULL);
			mv.visitInsn(ARETURN);
        	
			
			mv.visitMaxs(0, 0); 
			mv.visitEnd();
		}
		
		public void addCopierBridge(String rawSupCls, String rawclsName, String clsName) {
			//java/lang/Object
			MethodVisitor mv = super.visitMethod(ACC_PUBLIC + ACC_BRIDGE + ACC_SYNTHETIC, "copy", "(Lcom/concurnas/bootstrap/runtime/CopyTracker;Lcom/concurnas/bootstrap/runtime/CopyDefinition;)" + rawSupCls, null, null);
			mv.visitCode();
			mv.visitVarInsn(ALOAD, 0);
			mv.visitVarInsn(ALOAD, 1);
			mv.visitVarInsn(ALOAD, 2);
			mv.visitMethodInsn(INVOKEVIRTUAL, clsName, "copy", "(Lcom/concurnas/bootstrap/runtime/CopyTracker;Lcom/concurnas/bootstrap/runtime/CopyDefinition;)" + rawclsName, false);
			mv.visitInsn(ARETURN);
			mv.visitMaxs(2, 2);
			mv.visitEnd();
		
		}
		
        private boolean addCopier(){
        	boolean callsConstructor=true;
        	String rawclsName = currentClass.peek();
        	String clsName = rawclsName.substring(1, rawclsName.length()-1);
        	MethodVisitor mv = super.visitMethod(Opcodes.ACC_PUBLIC, "copy", "(Lcom/concurnas/bootstrap/runtime/CopyTracker;Lcom/concurnas/bootstrap/runtime/CopyDefinition;)" + rawclsName, null, null);
        	mv.visitCode();
        	
        	
        	/*Label l0 = new Label();
        	mv.visitLabel(l0);
        	mv.visitLineNumber(6, l0);
        	mv.visitFieldInsn(GETSTATIC, "java/lang/System", "err", "Ljava/io/PrintStream;");
        	mv.visitLdcInsn("called copy on: " + clsName);
        	mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V", false);
        	*/

    		//System.err.println("gen for: " + clsName);
        	if(isActor || isShared) {
        		mv.visitVarInsn(ALOAD, 0);
    			mv.visitInsn(ARETURN);
        	}
        	else if(isImmutable || isEnum){//enums are this
        		
        		mv.visitVarInsn(ALOAD, 1);
				mv.visitFieldInsn(GETFIELD, "com/concurnas/bootstrap/runtime/CopyTracker", "clonedAlready", "Ljava/util/Map;");
				mv.visitVarInsn(ALOAD, 0);
				mv.visitVarInsn(ALOAD, 0);
				mv.visitMethodInsn(INVOKEINTERFACE, "java/util/Map", "put", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;");
				mv.visitInsn(POP);//dummy value

        		mv.visitVarInsn(ALOAD, 0);
    			mv.visitInsn(ARETURN);
        	}
        	else{
        		//need to copy self, first check to see if it's been copied already...
        		
        		/*
				public CopierInner copy(CopyTracker a){
					Object already = a.clonedAlready.get(a);
					if(null != already){
						return (CopierInner)already;
					}
					return new CopierInner(a, this);
				} 
        		 */
        		
        		mv.visitVarInsn(ALOAD, 1);
        		mv.visitFieldInsn(GETFIELD, "com/concurnas/bootstrap/runtime/CopyTracker", "clonedAlready", "Ljava/util/Map;");
        		mv.visitVarInsn(ALOAD, 0);
        		mv.visitMethodInsn(INVOKEINTERFACE, "java/util/Map", "get", "(Ljava/lang/Object;)Ljava/lang/Object;");
        		mv.visitVarInsn(ASTORE, 3);
        		
        		mv.visitVarInsn(ALOAD, 3);
        		
        		Label onNull = new Label();
        		mv.visitJumpInsn(IFNULL, onNull);
        		
        		///Label onNotNull = new Label();
        		//mv.visitLabel(onNotNull);
        		
        		mv.visitVarInsn(ALOAD, 3);
        		mv.visitTypeInsn(CHECKCAST, clsName);
        		mv.visitInsn(ARETURN);
        		
        		
        		mv.visitLabel(onNull);
        		
				//walk up super chain, if meet
				String superName = superClass.peek();
				while(callsConstructor && !superName.equals("java/lang/Object")){
					try{
						Class<?> primoidial = immClassHolder.getSuperClassRef(superName);
						callsConstructor = !Ref.class.isAssignableFrom(primoidial) && !Throwable.class.isAssignableFrom(primoidial)  ;//dont use copier if subtype of ref or throwable
						break;
					}
					catch(Exception e){
					}
					
					try{
						Class<?> supClassRef = immClassHolder.loadClass(superName);
						superName = supClassRef.getSuperclass().getName();
					}
					catch(Exception e){//panic
						callsConstructor=false;
						break;
					}
				}
				
				if(callsConstructor){
					mv.visitVarInsn(ALOAD, 1);
					mv.visitFieldInsn(GETFIELD, "com/concurnas/bootstrap/runtime/CopyTracker", "clonedAlready", "Ljava/util/Map;");
					mv.visitVarInsn(ALOAD, 0);
					mv.visitVarInsn(ALOAD, 0);
					mv.visitMethodInsn(INVOKEINTERFACE, "java/util/Map", "put", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;");
					mv.visitInsn(POP);//dummy value
					
					mv.visitTypeInsn(NEW, clsName);
					mv.visitInsn(DUP);
	    			mv.visitInsn(DUP);
	    			mv.visitVarInsn(ALOAD, 1);
	    			mv.visitVarInsn(ALOAD, 0);
	    			mv.visitVarInsn(ALOAD, 2);
	    			mv.visitMethodInsn(INVOKESPECIAL, clsName, "<init>", "(Lcom/concurnas/bootstrap/runtime/CopyTracker;"+rawclsName+"Lcom/concurnas/bootstrap/runtime/CopyDefinition;)V");

	    			mv.visitVarInsn(ALOAD, 1);
					mv.visitFieldInsn(GETFIELD, "com/concurnas/bootstrap/runtime/CopyTracker", "clonedAlready", "Ljava/util/Map;");
					mv.visitInsn(SWAP);
					mv.visitVarInsn(ALOAD, 0);
					mv.visitInsn(SWAP);
					mv.visitMethodInsn(INVOKEINTERFACE, "java/util/Map", "put", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;");
					mv.visitInsn(POP);//correct value, a copy
				}
				else{
					//mv.visitVarInsn(ALOAD, 0);
					mv.visitFieldInsn(GETSTATIC, "com/concurnas/runtime/bootstrapCloner/Cloner", "cloner", "Lcom/concurnas/runtime/bootstrapCloner/Cloner;");
   	        		mv.visitVarInsn(ALOAD, 1);
   	        		mv.visitVarInsn(ALOAD, 0);
   	        		mv.visitVarInsn(ALOAD, 2);
   	        		mv.visitMethodInsn(INVOKEVIRTUAL, "com/concurnas/runtime/bootstrapCloner/Cloner", "clone", "(Lcom/concurnas/bootstrap/runtime/CopyTracker;Ljava/lang/Object;Lcom/concurnas/bootstrap/runtime/CopyDefinition;)Ljava/lang/Object;");
   	        		mv.visitTypeInsn(CHECKCAST, clsName);
				}
    			
    			mv.visitInsn(ARETURN);
        	}
			
			mv.visitMaxs(0, 0); 
			mv.visitEnd();
			
			return callsConstructor;
        }
	}
	
	private static Thruple<Boolean, byte[], HashSet<String>> addCopyConstructorAndOffHeapMethods(byte[] inputClass, ConcurnasClassLoader loader){
		//should only modify conc files, java files being called can be ingored
		ClassReader origReader = new ClassReader(inputClass);
		ConcClassWriter cw = new ConcClassWriter(ClassWriter.COMPUTE_MAXS|ClassWriter.COMPUTE_FRAMES, loader.getDetector());//TODO: consider removing COMPUTE_MAXS | COMPUTE_FRAMES this to make load time quicker
        ClonerTransformer clonerTrans =  new ClonerTransformer(cw, loader);
        origReader.accept(clonerTrans, 0 );
        byte[] outputCode = cw.toByteArray();

        outputCode = OffHeapAugmentor.addOffHeapMethods(outputCode, loader);
        
        String clsName = clonerTrans.theCurrentclass;
        clsName = clsName.substring(1, clsName.length()-1);
        outputCode = RefFieldMapper.doMapping(outputCode, clsName, loader);
        
		/*try {
			BytecodePrettyPrinter.print(outputCode, true);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}  	*/
        
        
        //TODO: add immutable annotation for use in Cloner so as to avoid unecisary cloneing
        Boolean isImmutable = !clonerTrans.immutableDependsOn.isEmpty()?null:(clonerTrans.isActor || clonerTrans.isImmutable || clonerTrans.isEnum);
        
	    return new Thruple<Boolean, byte[], HashSet<String>>(isImmutable, outputCode, clonerTrans.immutableDependsOn);
	}
	
	/*private static boolean isActorOrActorSubtype(final String name, ConcurnasClassLoader loader){
		boolean ret = false;
		try {
			String findName = name.replace('.', '/');
			while(findName!=null && !findName.equals("java/lang/Object")){
				if(findName.equals("com/concurnas/lang/Actor")){
					ret=true;
					break;
	    		}
				ClassMirror cm = loader.theDetector.classForName(findName);
				findName = null != cm?cm.getSuperclassNoEx():null;
			}
		} catch (ClassMirrorNotFoundException e) {
			
		}
		return ret;		
	}*/
	
	public static class ActorAugmentor extends ClassVisitor implements Opcodes {
		private HashMap<String, HashSet<Integer>> methodToSharedParams;
		private ConcurnasClassLoader loader;

		public ActorAugmentor( ClassWriter cv, ConcurnasClassLoader loader, HashMap<String, HashSet<Integer>> methodToSharedParams) {
			super(Opcodes.ASM7, cv);
			this.loader = loader;
			this.methodToSharedParams = methodToSharedParams;
		}
		
		
		private static HashSet<String>  noAugmentationOf = new HashSet<String>();
		{
			noAugmentationOf.add("hashCode()I");
			noAugmentationOf.add("equals(Ljava/lang/Object;)Z");
			noAugmentationOf.add("toString()Ljava/lang/String;");
		}
		
		private boolean isFromObject(String name, String desc){
			return noAugmentationOf.contains(name + desc);
		}
		
		private boolean excluded(String name, String desc){
			if("com/concurnas/lang/Actor".equals(this.currentClassName) || "com/concurnas/lang/TypedActor".equals(this.currentClassName)){
				return false;
			}
			return isFromObject(name , desc);
		}
		//bytecodeSandbox$MyActor$$LambdaFor$setD$ActorSuperCall
		
		private HashMap<String, Integer> basicLambdaNameToArg = new HashMap<String, Integer>();
		
		public static ArrayList<String> typeDescForRef(ConcurnasClassLoader loader,String genericSignature, String desc){
			//TODO: replace this with MethodSignatureVisitor from DependencyTracker
			ArrayList<String> ret = new ArrayList<String>(1);
			
			ClassMirror ifGenericClass = loader.getDetector().classForNameNoExcep(Type.getType(desc).getReturnType().getClassName());
			typeDescForRef(loader, genericSignature.substring(genericSignature.lastIndexOf(')')+1), ret, ifGenericClass);
			
			//System.err.println(String.format("%s %s -> %s", genericSignature, desc, ret));
			
			return ret;
		}
				
		private static int getNextSemi(String input) {
			int needCloseArrow = 0;
			boolean inObj = false;
			for(int n=0; n < input.length(); n++) {
				switch(input.charAt(n)) {
					case '<' : needCloseArrow++;break;
					case ';' : if(needCloseArrow == 0) { return n; }break;
					case '>' : needCloseArrow--;break;
					case 'L' : inObj = true;
					case '[' : break;
					case 'Z': 
					case 'B': 
					case 'S': 
					case 'I': 
					case 'J': 
					case 'F': 
					case 'D': 
					case 'C': if(!inObj && needCloseArrow == 0) { return n; }break;
				}
			}
			return -1;
		}
		
		
		public static void typeDescForRef(ConcurnasClassLoader loader, String genericSignature, ArrayList<String> ret, ClassMirror ifGenericClass){
			if(genericSignature.charAt(0) == 'C') {
				genericSignature = "Lcom/concurnas/lang/Void;";
			}else {
				genericSignature = boxReturnType(genericSignature);
			}
			
			int firstLT = genericSignature.indexOf('<');
			if(firstLT > -1) {
				typeDescForRef(loader, genericSignature.substring(0, firstLT), ret, ifGenericClass);//self
				if(null == ifGenericClass) {//load compiled type from last one above
					ifGenericClass = loader.getDetector().classForNameNoExcep(ret.get(ret.size()-1));
				}
				
				genericSignature = genericSignature.substring(firstLT+1, genericSignature.length()-1);
				int ai=0;
				String[] genArgs = ifGenericClass == null? null: ifGenericClass.getGenericArguments();
				
				int n=0;
				
				//Ljava/util/ArrayList<Ljava/lang/Comparable<Ljava/lang/Object;>;>;
				
				while((ai = getNextSemi(genericSignature)) > -1) {
					String inst = genArgs[n++];
					String tt;
					try {
						tt = Type.getType(inst).getReturnType().getClassName();
					}catch(IllegalArgumentException | StringIndexOutOfBoundsException x) {
						tt = inst;
					}
					
					ClassMirror genClsMirror  = loader.getDetector().classForNameNoExcep(tt);
					typeDescForRef(loader, genericSignature.substring(0, ai+1), ret, genClsMirror);
					genericSignature = genericSignature.substring(ai+1);
				}
				
				if(n == 0 && genericSignature.startsWith("*")) {
					ret.add("java/lang/Object");
				}
				
			}else {
				if(genericSignature.startsWith("[")) {
					int n=1;
					while(genericSignature.length()-1 > n && genericSignature.charAt(n) == '[') {
						n++;
					}
					
					StringBuilder sb = new StringBuilder();
					for(int m=0; m < n; m++) {
						sb.append("[");
					}
					
					if(genericSignature.charAt(n) == 'L') {
						genericSignature = genericSignature.substring(n+1, genericSignature.length());
						if(genericSignature.endsWith(";")) {
							genericSignature = genericSignature.substring(0, genericSignature.length()-1);
						}
						if(null == loader.getDetector().classForNameNoExcep(genericSignature)) {//must be generic
							
							genericSignature=sb.append(ifGenericClass.getName().replace('.', '/') + ";").toString();
						}else {
							//WE ARE NOT DONE HERE ARE WE?
							//genericSignature += ";";
							
							
							genericSignature = sb.append("L" + genericSignature + ";").toString();
						}
					}else {//primative
						genericSignature=sb.append(genericSignature.charAt(n)).toString();
					}
					
					
				}else {
					if( null == loader.getDetector().classForNameNoExcep(genericSignature)) {//must be generic
						genericSignature=ifGenericClass.getName().replace('.', '/');
					}
				}
				
				ret.add(genericSignature);
			}
		}
		
		private void makeTypeArray(MethodVisitor mv, ArrayList<String> types) {
			
			int typeSize = types.size();
			BytecodeGenUtils.intOpcode(mv, typeSize);
			mv.visitTypeInsn(ANEWARRAY, "java/lang/Class");
			
			for(int n=0; n < typeSize; n++){//construct array as normal
				String ttt = types.get(n);
				mv.visitInsn(DUP);
				BytecodeGenUtils.intOpcode(mv, n);
				mv.visitLdcInsn(Type.getObjectType(ttt));//only add when not at end...
				
				mv.visitInsn(AASTORE);
			}
			
		}
		
		private void addActorSuperCall(int access, String name, String desc, String signature, String[] exceptions, boolean directToCopy) {
			//String newdesc = desc.substring(0, desc.lastIndexOf(")")) + ")Lcom/concurnas/runtime/ref/Local;";
			String newname = name+"$ActorSuperCall";
			
			Integer cnt = basicLambdaNameToArg.get(newname);//due to method overloading, same name different args, so we need more than one class to support this
			if(null == cnt){
				basicLambdaNameToArg.put(newname, 1);
				cnt = 0;
			}
			else{
				basicLambdaNameToArg.put(newname, ++cnt);
			}
			
			MethodVisitor mv = super.visitMethod(access, newname, desc, signature, exceptions);
			mv.visitCode();
			
			Type[] args = Type.getArgumentTypes(desc);
			int argsLen = args.length;
			ArrayList<Pair<Type, Boolean>> argsShared = new ArrayList<Pair<Type, Boolean>>(argsLen);
			if(null != methodToSharedParams && methodToSharedParams.containsKey(name + desc)) {
				HashSet<Integer> sharedParams = methodToSharedParams.get(name + desc);
				 for(int n=0; n < argsLen; n++) {
					 argsShared.add(new Pair<>(args[n], sharedParams.contains(n)));
				 }
			}
			else {
				 for(int n=0; n < argsLen; n++) {
					 argsShared.add(new Pair<>(args[n], false));
				 }
			}
			
			Type ret = Type.getReturnType(desc);
			Type origRet = ret;
			//Ljava/lang/Object;
			if(ret.getDescriptor().equals("V")){
				ret = Type.getType("Lcom/concurnas/lang/Void;");
			}
			
			int startFromSlot = Modifier.isStatic(access) ? 0 : 1;
			
			int emptyvarSlot = startFromSlot;
			for(Type tt : args){
				String dec = tt.getDescriptor();
				emptyvarSlot++;
				if(dec.equals("J") || dec.equals("D")){
					emptyvarSlot++;
				}
			}
			
			String lambdaForActorSup = currentClassName + "$$LambdaFor$" + newname + cnt;
			mv.visitTypeInsn(NEW, "com/concurnas/runtime/ref/Local");
			mv.visitInsn(DUP);
			
		    makeTypeArray(mv, typeDescForRef(loader, signature==null?desc:signature, desc));
		    
		    mv.visitMethodInsn(INVOKESPECIAL, "com/concurnas/runtime/ref/Local", "<init>", "([Ljava/lang/Class;)V", false);
		    mv.visitVarInsn(ASTORE, emptyvarSlot);
		    mv.visitVarInsn(ALOAD, 0);
		    mv.visitFieldInsn(GETFIELD, "com/concurnas/lang/Actor", "processQueue", "Lcom/concurnas/runtime/channels/PriorityQueue;");
			mv.visitInsn(ICONST_1);//actor operations have first place in queue
			mv.visitTypeInsn(NEW, "com/concurnas/runtime/Pair");
		    mv.visitInsn(DUP);
		    mv.visitVarInsn(ALOAD, emptyvarSlot);
		    mv.visitTypeInsn(NEW, lambdaForActorSup); 
		    mv.visitInsn(DUP);
		    int n=startFromSlot;
		    StringBuilder sb = new StringBuilder("(");
		    for(Type tt : args){
				String dec = tt.getDescriptor();
				applyLoad(mv, dec, n++);
				if(dec.equals("J") || dec.equals("D")){
					n++;
				}
				sb.append(dec);
			}
		    sb.append(")V");
		    String constuctorDesc = sb.toString();
		    
		    mv.visitMethodInsn(INVOKESPECIAL, lambdaForActorSup, "<init>", constuctorDesc, false);//dependant on arguments passed in
		    mv.visitMethodInsn(INVOKESPECIAL, "com/concurnas/runtime/Pair", "<init>", "(Ljava/lang/Object;Ljava/lang/Object;)V", false);
		    mv.visitMethodInsn(INVOKEVIRTUAL, "com/concurnas/runtime/channels/PriorityQueue", "add", "(ILjava/lang/Object;)V", false);
		    mv.visitVarInsn(ALOAD, emptyvarSlot);
		    //get
		    //cast to return type
		    
		    mv.visitMethodInsn(INVOKEVIRTUAL, "com/concurnas/runtime/ref/Local", "get", "()Ljava/lang/Object;", false);

			String retDesc = ret.getDescriptor();
			if(retDesc.equals("Lcom/concurnas/lang/Void;")) {
				mv.visitInsn(POP);//not needed
			}else {
				mv.visitTypeInsn(CHECKCAST, boxReturnType(retDesc));
			}
		    String origdesc = origRet.getDescriptor();
		    unboxToRequiredType(mv, origdesc);//getIntValue etc
		    mv.visitInsn(getReturnOpcode(origdesc));//IRETURN etc
			//nice job
			
			LambdaClassStuff lcs = new LambdaClassStuff();

			lcs.className = lambdaForActorSup;//bytecodeSandbox$MyClass$$HelperLambdaThingy
			lcs.operatesOnClass = "L" + currentClassName + ";";//LbytecodeSandbox$MyClass;
			lcs.operatesOnClassSimple = currentClassName; //bytecodeSandbox$MyClass
			lcs.operatesOnMethodOriginSimple = isFromObject(name, desc)?"java/lang/Object":currentClassName; //java/lang/Object or bytecodeSandbox$MyClass
			lcs.args = argsShared;
			lcs.constuctorDesc = constuctorDesc;//(II)V
			lcs.normalApplyMethodReturn = retDesc;//I
			lcs.normalApplyMethodDec = "()" + Type.getObjectType(boxReturnType(retDesc));// + ";";//()Ljava/lang/Integer; <- boxed return type
			lcs.normalApplyMethodApplyToDec = desc;//desc;
			lcs.normalApplyMethodName = directToCopy?name+"$ActorSuperCallObjM":name;//plus unless directToCopy->$ActorSuperCallObjM, e.g. toString -> toString$ActorSuperCallObjM
			this.lambdaclassStuffs.add(lcs);
			
		}
		
		private ArrayList<LambdaClassStuff> lambdaclassStuffs = new ArrayList<LambdaClassStuff>();
		
		public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
			//public methods get repointed to protected
			if(needToAddgetClass){
				addGetClasstoActorClass();
			}
			
			boolean isFromObject = isFromObject(name , desc);
			int origAccess = access;
			if(!name.endsWith("$ActorSuperCall") && !name.endsWith("$ActorCall") && !name.endsWith("$ActorSuperCallObjM") && !name.equals("<init>") && !excluded(name, desc)){//not already agumented
				//if((access & ACC_PUBLIC) == ACC_PUBLIC){//TODO: what about hashcode and equals?
				
				if((access & ACC_PUBLIC) == ACC_PUBLIC) {
					addActorSuperCall(origAccess, name, desc, signature, exceptions, isFromObject);
				}else if(!name.endsWith("init") && !name.endsWith("defaultFieldInit$") && !this.currentClassName.equals("com/concurnas/lang/Actor")){
					
					if((access & ACC_PRIVATE) == ACC_PRIVATE){
						access = (origAccess & ~ACC_PRIVATE) + ACC_PUBLIC;
					}
					else if((access & ACC_PROTECTED) == ACC_PROTECTED){
						access = (origAccess & ~ACC_PROTECTED) + ACC_PUBLIC;
					}else {
						access += ACC_PUBLIC;
					}
					addActorSuperCall(access, name, desc, signature, exceptions, isFromObject);
				
				}
				
			}
			
			if(isFromObject && ("com/concurnas/lang/Actor".equals(this.currentClassName) || "com/concurnas/lang/TypedActor".equals(this.currentClassName))){//implementation gets copied here...
				return super.visitMethod(origAccess, name + "$ActorSuperCallObjM", desc, signature, exceptions);
			}else{
				return super.visitMethod(access, name, desc, signature, exceptions);
			}
		}
		
		private void addGetClasstoActorClass(){
			MethodVisitor mv = super.visitMethod(ACC_PUBLIC, "getClass$ActorSuperCall", "()Ljava/lang/Class;", "()Ljava/lang/Class<*>;", null);
			mv.visitCode();
			
			mv.visitVarInsn(ALOAD, 0);
			mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "getClass", "()Ljava/lang/Class;", false);
			mv.visitInsn(ARETURN);
			mv.visitEnd();
			
			needToAddgetClass=false;
		}
		
		private boolean needToAddgetClass = false;
		
		private String currentClassName;
		public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
			currentClassName = name;
			
			if(name.equals("com/concurnas/lang/Actor")){
				needToAddgetClass = true;
			}
			
			super.visit(version, access, name, signature, superName, interfaces);
		}
	}
	
	private static int getReturnOpcode(String desc){
		switch(desc){
			case "V": return RETURN;
			case "Z":
			case "B":
			case "C":
			case "S":
			case "I": return IRETURN;
			case "J": return LRETURN;
			case "F": return FRETURN;
			case "D": return DRETURN;
			
			default: return ARETURN;//PrimativeTypeEnum.LAMBDA
		}
	}
	
	public static void unboxToRequiredType(MethodVisitor mv, String desc){
		if( desc.startsWith("L") || desc.startsWith("[")){//object or array - do nothing
			return;
		}
		else{
			switch(desc){
				case "Z": mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Boolean", "booleanValue", "()Z", false); break;
				case "B": mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false); break;
				case "C": mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false); break;
				case "S": mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false); break;
				case "I": mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false); break;
				case "J": mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Long", "longValue", "()J", false); break;
				case "F": mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false); break;
				case "D": mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false); break;
			}
		}
	}
	
	private static String makeIntoClassForm(String desc){
		switch(desc){
			case "Z": return "Z";
			case "B": return "B";
			case "C": return "C";
			case "S": return "S";
			case "I": return "I";
			case "J": return "J";
			case "F": return "F";
			case "D": return "D";
			case "V": return "V";
		}
		
		if(!desc.startsWith("[")){
			return "L" + desc + ";";
		}
		
		return desc;
	}
	
	public static String boxReturnType(String desc){
		if( desc.startsWith("L")){//object or array - do nothing
			return desc.substring(1, desc.length()-1);
		}
		else if(desc.startsWith("[")){
			return desc;
		}
		else{
			switch(desc){
				case "Z": return "java/lang/Boolean";
				case "B": return "java/lang/Byte";
				case "C": return "java/lang/Character";
				case "S": return "java/lang/Short";
				case "I": return "java/lang/Integer";
				case "J": return "java/lang/Long";
				case "F": return "java/lang/Float";
				case "D": return "java/lang/Double";
				case "V": return "java/lang/Void";
			}
		}
		
		return desc;
	}
	
	public static void applyLoad(MethodVisitor mv, String desc, int space) {
		if( desc.startsWith("L") || desc.startsWith("[")){//object or array
			mv.visitVarInsn(Opcodes.ALOAD, space);
		}
		else{
			switch(desc){
				case "J": mv.visitVarInsn(Opcodes.LLOAD, space); break;
				case "F":mv.visitVarInsn(Opcodes.FLOAD, space); break;
				case "D": mv.visitVarInsn(Opcodes.DLOAD, space); break;
				default: mv.visitVarInsn(Opcodes.ILOAD, space); break;//int etc
			}
		}
	}
	
	public static void boxIfPrimative(MethodVisitor mv, String desc) {
		if( desc.startsWith("L") || desc.startsWith("[")){//object or array - do nothing
			return;
		}
		else{
			switch(desc){
				case "Z": mv.visitFieldInsn(INVOKESTATIC, "java/lang/Boolean", "valueOf", "(Z)Ljava/lang/Boolean;"); break;
				case "B": mv.visitFieldInsn(INVOKESTATIC, "java/lang/Byte", "valueOf", "(B)Ljava/lang/Byte;"); break;
				case "C": mv.visitFieldInsn(INVOKESTATIC, "java/lang/Character", "valueOf", "(C)Ljava/lang/Character;"); break;
				case "S": mv.visitFieldInsn(INVOKESTATIC, "java/lang/Integer", "valueOf", "(S)Ljava/lang/Integer;"); break;//TODO: short is wrong who cares
				case "I": mv.visitFieldInsn(INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;"); break;
				case "J": mv.visitFieldInsn(INVOKESTATIC, "java/lang/Long", "valueOf", "(J)Ljava/lang/Long;"); break;
				case "F": mv.visitFieldInsn(INVOKESTATIC, "java/lang/Float", "valueOf", "(F)Ljava/lang/Float;"); break;
				case "D": mv.visitFieldInsn(INVOKESTATIC, "java/lang/Double", "valueOf", "(D)Ljava/lang/Double;"); break;
			}
		}		
	}
		
	
	private static class LambdaClassStuff{//ugh
		public String className;//bytecodeSandbox$MyClass$$HelperLambdaThingy
		public String operatesOnClass;//LbytecodeSandbox$MyClass;
		public String operatesOnClassSimple; //bytecodeSandbox$MyClass
		public String operatesOnMethodOriginSimple; //java/lang/Object
		public ArrayList<Pair<Type, Boolean>> args;
		public String constuctorDesc;//(II)V
		public String normalApplyMethodReturn;//I
		public String normalApplyMethodDec;//()Ljava/lang/Integer;
		public String normalApplyMethodApplyToDec;//(I)Ljava/lang/Integer;
		@Shared
		public String normalApplyMethodName;//plus
	}
	
	private static byte[] gennerateLambdaClassForActorOps(LambdaClassStuff lcs, Detector det){
		String className = lcs.className;
		String operatesOnClass = lcs.operatesOnClass;
		String operatesOnClassSimple = lcs.operatesOnClassSimple;
		ArrayList<Pair<Type, Boolean>> args = lcs.args;
		String constuctorDesc = lcs.constuctorDesc;
		String normalApplyMethodReturn = lcs.normalApplyMethodReturn;
		String normalApplyMethodDec = lcs.normalApplyMethodDec;
		String normalApplyMethodApplyToDec = lcs.normalApplyMethodApplyToDec;
		String normalApplyMethodName = lcs.normalApplyMethodName;
		
		///////
		
		ClassWriter cw = new ConcClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS, det);
		cw.visit(V1_8, ACC_PUBLIC + ACC_SUPER + ACC_SYNTHETIC, className, null, "com/concurnas/bootstrap/lang/Lambda$Function0", null);
		cw.visitInnerClass("com/concurnas/bootstrap/lang/Lambda$Function0", "com/concurnas/bootstrap/lang/Lambda", "Function0", ACC_PUBLIC + ACC_STATIC + ACC_ABSTRACT);//dunno why this is needed

		{//fields
			//operates on the actor...
			FieldVisitor fv = cw.visitField(ACC_PRIVATE + ACC_FINAL, "opon", operatesOnClass, null, null);
			fv.visitEnd();
			
			//fields
			int f = 0;
			for(Pair<Type, Boolean> argx : args){
				Type arg = argx.getA();
				fv = cw.visitField(ACC_PRIVATE + ACC_FINAL, "f" + f++, arg.getDescriptor(), null, null);
				if(argx.getB()){
					fv.visitAnnotation("Lcom/concurnas/lang/Shared;", true).visitEnd();
				}
				fv.visitEnd();
			}
		}
		
		{//init
			MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "<init>", constuctorDesc, null, null);
			mv.visitCode();
			mv.visitVarInsn(ALOAD, 0);
			mv.visitLdcInsn(Type.getType(operatesOnClass));
			//mv.visitInsn(ACONST_NULL);
		    mv.visitFieldInsn(INVOKESPECIAL, "com/concurnas/bootstrap/lang/Lambda$Function0", "<init>", "(Ljava/lang/Class;)V");
		    //set fields
		    int f = 0;
		    int slots=1;
			for(Pair<Type, Boolean> argx : args){
				Type arg = argx.getA();
				mv.visitVarInsn(ALOAD, 0);
			    String dec = arg.getDescriptor();			    
			    applyLoad(mv, dec, slots++);
				if(dec.equals("J") || dec.equals("D")){
					slots++;
				}
			    mv.visitFieldInsn(PUTFIELD, className, "f" + f++, dec);
			}
		    
			mv.visitInsn(RETURN);
		}
		
		{//apply
			MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "apply", normalApplyMethodDec, null, null);
			mv.visitCode();
			
			/*mv.visitFieldInsn(GETSTATIC, "java/lang/System", "err", "Ljava/io/PrintStream;");
			mv.visitLdcInsn("i got here");
			mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V");
*/
			Label nonull = new Label();
			mv.visitVarInsn(ALOAD, 0);
		    mv.visitFieldInsn(GETFIELD, className, "opon", operatesOnClass);
		    mv.visitInsn(DUP);
		    mv.visitJumpInsn(IFNONNULL, nonull);
		    mv.visitTypeInsn(NEW, "com/concurnas/lang/LambdaException");
		    mv.visitInsn(DUP);
		    mv.visitLdcInsn("Method reference has not been bound to instance object, call bind() before invocation");
		    mv.visitMethodInsn(INVOKESPECIAL, "com/concurnas/lang/LambdaException", "<init>", "(Ljava/lang/String;)V", false);
		    mv.visitInsn(ATHROW);
		    mv.visitLabel(nonull);
		    
		    int f=0;
		    for(Pair<Type, Boolean> argx : args){
				Type arg = argx.getA();
				mv.visitVarInsn(ALOAD, 0);
			    String dec = arg.getDescriptor();			    
			    //applyLoad(mv, dec, f);
			    mv.visitFieldInsn(GETFIELD, className, "f" + f++, dec);
			}
		    mv.visitMethodInsn(INVOKEVIRTUAL, operatesOnClassSimple, normalApplyMethodName, normalApplyMethodApplyToDec, false);
		    if(normalApplyMethodReturn.equals("Lcom/concurnas/lang/Void;")){
		    	mv.visitInsn(ACONST_NULL);//dummy up the return
		    }
		    else{
			    boxIfPrimative(mv, normalApplyMethodReturn);
		    }
		    
		    mv.visitInsn(ARETURN); 
		}
		
		if(!normalApplyMethodDec.equals("()Ljava/lang/Object;"))//only gennerate bridge method if we are doing any bridging
		{//apply bridge
			MethodVisitor mv = cw.visitMethod(ACC_PUBLIC + ACC_BRIDGE, "apply", "()Ljava/lang/Object;", null, null);
			mv.visitCode();
			mv.visitVarInsn(ALOAD, 0);
		    mv.visitMethodInsn(INVOKEVIRTUAL, className, "apply", normalApplyMethodDec, false);
			mv.visitInsn(ARETURN); 
		}
		
		{//bind
			MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "bind", "(Ljava/lang/Object;)V", null, null);
			mv.visitCode();
			
			mv.visitVarInsn(ALOAD, 0);
			mv.visitVarInsn(ALOAD, 1);
			mv.visitTypeInsn(CHECKCAST, operatesOnClassSimple);
		    mv.visitFieldInsn(PUTFIELD, className, "opon", operatesOnClass);
			mv.visitInsn(RETURN); 
		}

		{//signature, dont bother
			MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "signature", "()[Ljava/lang/Object;", null, null);
			mv.visitCode();
			/*mv.visitInsn(ICONST_1);
			mv.visitTypeInsn(ANEWARRAY, "java/lang/Object");
			mv.visitInsn(DUP);
			mv.visitInsn(ICONST_0);
			mv.visitInsn(ACONST_NULL);
			mv.visitInsn(AASTORE);*/
			mv.visitInsn(ACONST_NULL);
			mv.visitInsn(ARETURN); 
		}
		
		{//hashCode, dont bother
			MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "hashCode", "()I", null, null);
			mv.visitCode();
			mv.visitInsn(ICONST_1);
			mv.visitInsn(IRETURN); 
		}
		
		{//equals, dont bother
			MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "equals", "(Ljava/lang/Object;)Z", null, null);
			mv.visitCode();
			mv.visitInsn(ICONST_0);
			mv.visitInsn(IRETURN); 
		}
	
	/*
	public synthetic class bytecodeSandbox$$Lambda7 extends com/concurnas/bootstrap/lang/Lambda$Function0  {

		  // compiled from: bytecodeSandbox.conc
		  // access flags 0x409
		  public static abstract INNERCLASS com/concurnas/bootstrap/lang/Lambda$Function0 com/concurnas/bootstrap/lang/Lambda Function0

		  // access flags 0x2
		  // signature LbytecodeSandbox$MyClass;
		  // declaration: bytecodeSandbox$MyClass
		  private LbytecodeSandbox$MyClass; opon

		  // access flags 0x2
		  // signature I
		  // declaration: int
		  private I f0

		  // access flags 0x1
		  public <init>(I)V
		    ALOAD 0
		    LDC LbytecodeSandbox$MyClass;.class
		    INVOKESPECIAL com/concurnas/bootstrap/lang/Lambda$Function0.<init> (Ljava/lang/Class;)V
		    ALOAD 0
		    ILOAD 1
		    PUTFIELD bytecodeSandbox$$Lambda7.f0 : I
		    RETURN
		    MAXSTACK = 2
		    MAXLOCALS = 2

		  // access flags 0x1
		  public apply()Ljava/lang/Integer;
		    ALOAD 0
		    GETFIELD bytecodeSandbox$$Lambda7.opon : LbytecodeSandbox$MyClass;
		    DUP
		    IFNONNULL L0
		    NEW com/concurnas/lang/LambdaException
		    DUP
		    LDC "Method reference has not been bound to instance object, call bind() before invocation"
		    INVOKESPECIAL com/concurnas/lang/LambdaException.<init> (Ljava/lang/String;)V
		    ATHROW
		   L0
		   FRAME SAME1 bytecodeSandbox$MyClass
		    ALOAD 0
		    GETFIELD bytecodeSandbox$$Lambda7.f0 : I
		    INVOKEVIRTUAL bytecodeSandbox$MyClass.plus (I)I
		    INVOKESTATIC java/lang/Integer.valueOf (I)Ljava/lang/Integer;
		    ARETURN
		    MAXSTACK = 4
		    MAXLOCALS = 1

		  // access flags 0x1041
		  public synthetic bridge apply()Ljava/lang/Object;
		    ALOAD 0
		    INVOKEVIRTUAL bytecodeSandbox$$Lambda7.apply ()Ljava/lang/Integer;
		    ARETURN
		    MAXSTACK = 1
		    MAXLOCALS = 1

		  // access flags 0x1
		  public signature()[Ljava/lang/Object;
		    ICONST_5
		    ANEWARRAY java/lang/Object
		    ASTORE 1
		    ALOAD 1
		    ICONST_0
		    NEW java/lang/Integer
		    DUP
		    ALOAD 0
		    GETFIELD bytecodeSandbox$$Lambda7.opon : LbytecodeSandbox$MyClass;
		    INVOKESTATIC java/lang/System.identityHashCode (Ljava/lang/Object;)I
		    INVOKESPECIAL java/lang/Integer.<init> (I)V
		    AASTORE
		    ALOAD 1
		    ICONST_1
		    LDC "bytecodeSandbox$MyClass"
		    AASTORE
		    ALOAD 1
		    ICONST_2
		    LDC "plus(I)I"
		    AASTORE
		    ALOAD 1
		    ICONST_3
		    ICONST_1
		    ANEWARRAY java/lang/Object
		    DUP
		    ICONST_0
		    ALOAD 0
		    GETFIELD bytecodeSandbox$$Lambda7.f0 : I
		    INVOKESTATIC java/lang/Integer.valueOf (I)Ljava/lang/Integer;
		    AASTORE
		    AASTORE
		    ALOAD 1
		    ICONST_4
		    LDC "()Ljava/lang/Integer;"
		    AASTORE
		    ALOAD 1
		    ARETURN
		    MAXSTACK = 6
		    MAXLOCALS = 2

		  // access flags 0x1
		  public equals(Ljava/lang/Object;)Z
		    ALOAD 1
		    IFNULL L0
		    ALOAD 1
		    INSTANCEOF com/concurnas/bootstrap/lang/Lambda
		    IFEQ L0
		    ALOAD 1
		    CHECKCAST com/concurnas/bootstrap/lang/Lambda
		    INVOKEVIRTUAL com/concurnas/bootstrap/lang/Lambda.signature ()[Ljava/lang/Object;
		    ALOAD 0
		    INVOKEVIRTUAL bytecodeSandbox$$Lambda7.signature ()[Ljava/lang/Object;
		    INVOKESTATIC com/concurnas/lang/Equalifier.equals ([Ljava/lang/Object;[Ljava/lang/Object;)Z
		    IRETURN
		   L0
		   FRAME SAME
		    ICONST_0
		    IRETURN
		    MAXSTACK = 2
		    MAXLOCALS = 2

		  // access flags 0x1
		  public hashCode()I
		    ALOAD 0
		    INVOKEVIRTUAL bytecodeSandbox$$Lambda7.signature ()[Ljava/lang/Object;
		    INVOKESTATIC com/concurnas/lang/Hasher.hashCode ([Ljava/lang/Object;)I
		    IRETURN
		    MAXSTACK = 1
		    MAXLOCALS = 1

		  // access flags 0x1
		  public bind(Ljava/lang/Object;)V
		    ALOAD 0
		    ALOAD 1
		    CHECKCAST bytecodeSandbox$MyClass
		    PUTFIELD bytecodeSandbox$$Lambda7.opon : LbytecodeSandbox$MyClass;
		    RETURN
		    MAXSTACK = 2
		    MAXLOCALS = 2
		}
	 	*/
		cw.visitEnd();
		
		return cw.toByteArray();
	}
	
	private static class SharedMethodParamFinder extends ClassVisitor{
		
		public SharedMethodParamFinder( ClassWriter cv) {
			super(Opcodes.ASM7, cv);
		}
		
		public HashMap<String, HashSet<Integer>> methodToSharedParams = null;//new HashMap<String, HashSet<Integer>>();
		
		public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
			return new SharedParamFinder(super.visitMethod(access, name, desc, signature, exceptions), name + desc);
		}
		
		private class SharedParamFinder extends MethodVisitor{

			private String nameAndDesc;
			
			public SharedParamFinder(final MethodVisitor mv, String nameAndDesc)  {
				super(ASM7, mv);
				this.nameAndDesc = nameAndDesc;
			}
			
			
			public AnnotationVisitor visitParameterAnnotation(int parameter, String desc, boolean visible) {
				if(desc.equals("Lcom/concurnas/lang/Shared;")) {
					if(methodToSharedParams == null) {
						methodToSharedParams = new HashMap<String, HashSet<Integer>>();
					}
					
					HashSet<Integer> addTo = methodToSharedParams.get(this.nameAndDesc);
					if(null == addTo) {
						addTo = new HashSet<Integer>();
						methodToSharedParams.put(this.nameAndDesc, addTo);
					}
					addTo.add(parameter);
				}
				return null;
			}
		}
		
	}
	
	private static ArrayList<Pair<String, byte[]>> augmentActorClass(byte[] inputClass, ConcurnasClassLoader loader){
		
		ArrayList<Pair<String, byte[]>> ret = new ArrayList<Pair<String, byte[]>>();
		
		ClassReader origReader = new ClassReader(inputClass);
		ConcClassWriter cw = new ConcClassWriter(ClassWriter.COMPUTE_MAXS|ClassWriter.COMPUTE_FRAMES, loader.getDetector());//TODO: consider removing COMPUTE_MAXS | COMPUTE_FRAMES this to make load time quicker
		
		SharedMethodParamFinder smpf = new SharedMethodParamFinder(null);
		origReader.accept(smpf, 0 );
		
		ActorAugmentor tranformer =  new ActorAugmentor(cw, loader, smpf.methodToSharedParams);
        origReader.accept(tranformer, 0 );
        byte[] outputCode = cw.toByteArray();
		
        ret.add(new Pair<String, byte[]>(null, outputCode));
        if(!tranformer.lambdaclassStuffs.isEmpty()){
        	Detector det = loader.getDetector();
        	for(LambdaClassStuff lcs : tranformer.lambdaclassStuffs){
            	ret.add(new Pair<String, byte[]>(lcs.className, gennerateLambdaClassForActorOps(lcs, det)));
            }
        }
        
        return ret;
	}
	
	/**
	 * 1. Augments actor class if needs be, and outputs supporting classes
	 * 2. Adds copier (and constructor if needs be)
	 * 3. adds toBinary, fromBinary
	 * 4. converts non private ref fields to WeakHadhMap<Fiber, Object>... (+callers and initilizer)
	 * returns isimmutable, output code, immutable dependants for initial
	 * plus additional classes that need to be defined so as to support the thing if its an actor
	 */
    public static RequestedAndSupportingCls initialConc(byte[] inputClass, ConcurnasClassLoader loader, String name)  {
    	RequestedAndSupportingCls reqAndSupport = new RequestedAndSupportingCls();
    	
    	if(loader.getDetector().isActor(name)/*isActorOrActorSubtype(name, loader)*/){
    		ArrayList<Pair<String, byte[]>> incActorAugAndSupportingClasses = augmentActorClass(inputClass, loader);
    		int sz = incActorAugAndSupportingClasses.size();
    		ArrayList<Pair<String, Thruple< Boolean, byte[], HashSet<String>>>> suporting = new ArrayList<Pair<String, Thruple< Boolean, byte[], HashSet<String>>>>(sz-1);
    		
    		reqAndSupport.requested = addCopyConstructorAndOffHeapMethods(incActorAugAndSupportingClasses.get(0).getB(), loader);
    		//nasty hack from above
    		loader.isImmtuableDirectory.put("L"+name+";", reqAndSupport.requested.getA());
    		//JPT: this is a nasty hack to prevent an inifnite loop when the supporting classes themselves, in their copy constructors, reference the main class
        	loader.classesDefinedHere.add("L"+name+";");
    		
    		for(int n=1; n < sz; n++){
    			Pair<String, byte[]> su = incActorAugAndSupportingClasses.get(n);
    			Thruple<Boolean, byte[], HashSet<String>> aug = addCopyConstructorAndOffHeapMethods(su.getB(), loader);
    			suporting.add(new Pair<String, Thruple<Boolean, byte[], HashSet<String>>>(su.getA(), aug) );
    		}
    		
    		reqAndSupport.supporting = suporting;
    	}else{
    		//System.err.println("items: " + name);
    		reqAndSupport.requested = addCopyConstructorAndOffHeapMethods(inputClass, loader);
    		
    	}
    	return reqAndSupport;
    }
}
