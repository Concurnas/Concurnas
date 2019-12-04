package com.concurnas.runtime;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import com.concurnas.runtime.cps.analysis.ConcClassWriter;

public class OffHeapAugmentor extends ClassVisitor implements Opcodes {

	public static byte[] addOffHeapMethods(byte[] inputcode, ConcurnasClassLoader loader){
		ClassReader origReader = new ClassReader(inputcode);
		ConcClassWriter cw = new ConcClassWriter( ClassWriter.COMPUTE_MAXS, loader.getDetector());//TODO: consider removing COMPUTE_MAXS | COMPUTE_FRAMES this to make load time quicker
		OffHeapAugmentor clonerTrans =  new OffHeapAugmentor(cw);
        origReader.accept(clonerTrans, 0 );
		
		return cw.toByteArray();
	}

	private boolean isInterface;
	private boolean isEnum;
	private String classname;
	private String superclassname;
    private HashMap<String, String> fieldNameToDesc = new HashMap<String, String>();
    //if we are leveraging an existing serializer, then we need to include the transient fields as the custom serializer may have included these
    private HashMap<String, String> fieldNameToDescIncTrans = new HashMap<String, String>();
	
    //private boolean isSerializable = false;
    private boolean isExternalizable = false;
    private boolean readObjSeri = false;
    private boolean writeObjSeri = false;
    
	private OffHeapAugmentor( ClassWriter cv) {
		super(Opcodes.ASM7, cv);
	}
	
    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        isInterface = Modifier.isInterface(access);
        isEnum = (access & Opcodes.ACC_ENUM) !=0;
     
        this.classname = name;
        this.superclassname = superName;
        
        for(String iface : interfaces){
        	/*if(iface.equals("java/io/Serializable")){
        		isSerializable = true;
        	}
        	else*/ if(iface.equals("java/io/Externalizable")){//iface requies the existance of the read and write object methods, no no further check is needed
        		isExternalizable = true;
        		break;
        	}
        }
        
        super.visit(version, access, name, signature, superName, interfaces);
    }
	
    @Override
    public FieldVisitor visitField(int access, String name, String desc, String signature, Object value){
    	if(!Modifier.isStatic(access)){
    		if(!Modifier.isTransient(access)){
    			fieldNameToDesc.put(name, desc);
    		}
    		fieldNameToDescIncTrans.put(name, desc);
    	}
		
		
    	return super.visitField(access, name, desc, signature, value);
    }
    
    private boolean toFromBinaryAlreadyDefined = false;
    
	public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
		if(access == Opcodes.ACC_PRIVATE){
			if(name.equals("readObject") && desc.equals("(Ljava/io/ObjectInputStream;)V")){
				readObjSeri=true;
			}else if(name.equals("writeObject") && desc.equals("(Ljava/io/ObjectOutputStream;)V")){
				writeObjSeri=true;
			}
		}else if(access == Opcodes.ACC_PUBLIC){
			if(name.equals("toBinary") && desc.equals("(Lcom/concurnas/bootstrap/lang/offheap/Encoder;)V")){
				toFromBinaryAlreadyDefined=true;
			}else if(name.equals("fromBinary") && desc.equals("(Lcom/concurnas/bootstrap/lang/offheap/Decoder;)V")){
				toFromBinaryAlreadyDefined=true;
			}
		}
	
		return super.visitMethod(access, name, desc, signature, exceptions);
	}
    
    @Override
    public void visitEnd() {
    	if(!isInterface){//returns a copy of itself
    		if(this.isEnum){
    			//addDMAMethodEnum();
    		}else{
    			if(isExternalizable){
    				addDMAMethodsViaExternaliableMethods();
    			}
    			else if(readObjSeri && writeObjSeri){
        			//serialiable things, redirect here....
    				addDMAMethodsViaSerialiableMethods();
    			}else if(!toFromBinaryAlreadyDefined){
    				
    				//System.err.println("add to class: " + classname);
    				try {
    					addDMAMethods();
    				}catch(Exception e) {
    					//TODO: why can we not add some DMA methods - shared symbol table error?
    					 super.visitEnd();
    					 return;
    				}
        			
    			}
    			
    		}
    		
    		createConstuctorWithDefaultInit();
    	}
		
        super.visitEnd();
    }
    
    private boolean isNonObjectParent(){
    	return !"java/lang/Object".equals(superclassname) && !"com/concurnas/bootstrap/runtime/cps/CObject".equals(superclassname);
    }
    
    private void addDMAMethodsViaExternaliableMethods(){
    	ArrayList<String> names = new ArrayList<String>(fieldNameToDescIncTrans.keySet());
    	Collections.sort(names);	
    	
    	{//toBinary
    		MethodVisitor mv = super.visitMethod(ACC_PUBLIC, "toBinary", "(Lcom/concurnas/bootstrap/lang/offheap/Encoder;)V", null, null);
    		mv.visitCode();
    		Label l0 = new Label();
    		Label l1 = new Label();
    		Label l2 = new Label();
    		mv.visitTryCatchBlock(l0, l1, l2, null);
    		Label l3 = new Label();
    		Label l4 = new Label();
    		mv.visitTryCatchBlock(l3, l4, l4, null);
    		Label l5 = new Label();
    		Label l6 = new Label();
    		mv.visitTryCatchBlock(l5, l6, l6, "java/io/IOException");
    		
    		if(isNonObjectParent()){
				Label l7 = new Label();
				mv.visitLabel(l7);
				mv.visitVarInsn(ALOAD, 0);
				mv.visitVarInsn(ALOAD, 1);
				mv.visitMethodInsn(INVOKESPECIAL, superclassname, "toBinary", "(Lcom/concurnas/bootstrap/lang/offheap/Encoder;)V", false);
			}
    		
    		Label l8 = new Label();
    		mv.visitLabel(l8);
    		mv.visitTypeInsn(NEW, "java/io/ByteArrayOutputStream");
    		mv.visitInsn(DUP);
    		mv.visitMethodInsn(INVOKESPECIAL, "java/io/ByteArrayOutputStream", "<init>", "()V", false);
    		mv.visitVarInsn(ASTORE, 2);
    		mv.visitLabel(l5);
    		mv.visitInsn(ACONST_NULL);
    		mv.visitVarInsn(ASTORE, 3);
    		mv.visitInsn(ACONST_NULL);
    		mv.visitVarInsn(ASTORE, 4);
    		mv.visitLabel(l3);
    		mv.visitTypeInsn(NEW, "java/io/ObjectOutputStream");
    		mv.visitInsn(DUP);
    		mv.visitVarInsn(ALOAD, 2);
    		mv.visitMethodInsn(INVOKESPECIAL, "java/io/ObjectOutputStream", "<init>", "(Ljava/io/OutputStream;)V", false);
    		mv.visitVarInsn(ASTORE, 5);
    		mv.visitLabel(l0);
    		mv.visitVarInsn(ALOAD, 0);
    		mv.visitVarInsn(ALOAD, 5);
    		mv.visitMethodInsn(INVOKEVIRTUAL, classname, "writeExternal", "(Ljava/io/ObjectOutput;)V", false);
    		mv.visitLabel(l1);
    		mv.visitVarInsn(ALOAD, 5);
    		Label l9 = new Label();
    		mv.visitJumpInsn(IFNULL, l9);
    		mv.visitVarInsn(ALOAD, 5);
    		mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/ObjectOutputStream", "close", "()V", false);
    		mv.visitJumpInsn(GOTO, l9);
    		mv.visitLabel(l2);
    		mv.visitVarInsn(ASTORE, 3);
    		mv.visitVarInsn(ALOAD, 5);
    		Label l10 = new Label();
    		mv.visitJumpInsn(IFNULL, l10);
    		mv.visitVarInsn(ALOAD, 5);
    		mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/ObjectOutputStream", "close", "()V", false);
    		mv.visitLabel(l10);
    		mv.visitVarInsn(ALOAD, 3);
    		mv.visitInsn(ATHROW);
    		mv.visitLabel(l4);
    		mv.visitVarInsn(ASTORE, 4);
    		mv.visitVarInsn(ALOAD, 3);
    		Label l11 = new Label();
    		mv.visitJumpInsn(IFNONNULL, l11);
    		mv.visitVarInsn(ALOAD, 4);
    		mv.visitVarInsn(ASTORE, 3);
    		Label l12 = new Label();
    		mv.visitJumpInsn(GOTO, l12);
    		mv.visitLabel(l11);
    		mv.visitVarInsn(ALOAD, 3);
    		mv.visitVarInsn(ALOAD, 4);
    		mv.visitJumpInsn(IF_ACMPEQ, l12);
    		mv.visitVarInsn(ALOAD, 3);
    		mv.visitVarInsn(ALOAD, 4);
    		mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Throwable", "addSuppressed", "(Ljava/lang/Throwable;)V", false);
    		mv.visitLabel(l12);
    		mv.visitVarInsn(ALOAD, 3);
    		mv.visitInsn(ATHROW);
    		mv.visitLabel(l6);
    		mv.visitVarInsn(ASTORE, 3);
    		Label l13 = new Label();
    		mv.visitLabel(l13);
    		mv.visitTypeInsn(NEW, "java/lang/RuntimeException");
    		mv.visitInsn(DUP);
    		mv.visitVarInsn(ALOAD, 3);
    		mv.visitMethodInsn(INVOKESPECIAL, "java/lang/RuntimeException", "<init>", "(Ljava/lang/Throwable;)V", false);
    		mv.visitInsn(ATHROW);
    		mv.visitLabel(l9);
    		mv.visitVarInsn(ALOAD, 1);
    		mv.visitVarInsn(ALOAD, 2);
    		mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/ByteArrayOutputStream", "toByteArray", "()[B", false);
    		mv.visitInsn(ICONST_1);
    		mv.visitMethodInsn(INVOKEINTERFACE, "com/concurnas/bootstrap/lang/offheap/Encoder", "putByteArray", "(Ljava/lang/Object;I)I", true);
    		mv.visitInsn(POP);
    		Label l14 = new Label();
    		mv.visitLabel(l14);
    		mv.visitInsn(RETURN);
    		mv.visitMaxs(3, 6);
    		mv.visitEnd();
    	}
    	
    	{//fromBinary
    		MethodVisitor mv = super.visitMethod(ACC_PUBLIC, "fromBinary", "(Lcom/concurnas/bootstrap/lang/offheap/Decoder;)V", null, null);
    		mv.visitCode();
    		Label l0 = new Label();
    		Label l1 = new Label();
    		Label l2 = new Label();
    		mv.visitTryCatchBlock(l0, l1, l2, null);
    		Label l3 = new Label();
    		Label l4 = new Label();
    		mv.visitTryCatchBlock(l3, l4, l4, null);
    		Label l5 = new Label();
    		Label l6 = new Label();
    		mv.visitTryCatchBlock(l5, l6, l6, "java/io/IOException");
    		mv.visitTryCatchBlock(l5, l6, l6, "java/lang/ClassNotFoundException");
    		
    		if(isNonObjectParent()){
				Label l7 = new Label();
				mv.visitLabel(l7);
				mv.visitVarInsn(ALOAD, 0);
				mv.visitVarInsn(ALOAD, 1);
				mv.visitMethodInsn(INVOKESPECIAL, superclassname, "fromBinary", "(Lcom/concurnas/bootstrap/lang/offheap/Decoder;)V", false);
			}
    		
    		Label l8 = new Label();
    		mv.visitLabel(l8);
    		mv.visitVarInsn(ALOAD, 1);
    		mv.visitInsn(ICONST_1);
    		mv.visitMethodInsn(INVOKEINTERFACE, "com/concurnas/bootstrap/lang/offheap/Decoder", "getByteArray", "(I)Ljava/lang/Object;", true);
    		mv.visitTypeInsn(CHECKCAST, "[B");
    		mv.visitVarInsn(ASTORE, 2);
    		Label l9 = new Label();
    		mv.visitLabel(l9);
    		mv.visitTypeInsn(NEW, "java/io/ByteArrayInputStream");
    		mv.visitInsn(DUP);
    		mv.visitVarInsn(ALOAD, 2);
    		mv.visitMethodInsn(INVOKESPECIAL, "java/io/ByteArrayInputStream", "<init>", "([B)V", false);
    		mv.visitVarInsn(ASTORE, 3);
    		mv.visitLabel(l5);
    		mv.visitInsn(ACONST_NULL);
    		mv.visitVarInsn(ASTORE, 4);
    		mv.visitInsn(ACONST_NULL);
    		mv.visitVarInsn(ASTORE, 5);
    		mv.visitLabel(l3);
    		mv.visitTypeInsn(NEW, "java/io/ObjectInputStream");
    		mv.visitInsn(DUP);
    		mv.visitVarInsn(ALOAD, 3);
    		mv.visitMethodInsn(INVOKESPECIAL, "java/io/ObjectInputStream", "<init>", "(Ljava/io/InputStream;)V", false);
    		mv.visitVarInsn(ASTORE, 6);
    		mv.visitLabel(l0);
    		mv.visitVarInsn(ALOAD, 0);
    		mv.visitVarInsn(ALOAD, 6);
    		mv.visitMethodInsn(INVOKEVIRTUAL, classname, "readExternal", "(Ljava/io/ObjectInput;)V", false);
    		mv.visitLabel(l1);
    		mv.visitVarInsn(ALOAD, 6);
    		Label l10 = new Label();
    		mv.visitJumpInsn(IFNULL, l10);
    		mv.visitVarInsn(ALOAD, 6);
    		mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/ObjectInputStream", "close", "()V", false);
    		mv.visitJumpInsn(GOTO, l10);
    		mv.visitLabel(l2);
    		mv.visitVarInsn(ASTORE, 4);
    		mv.visitVarInsn(ALOAD, 6);
    		Label l11 = new Label();
    		mv.visitJumpInsn(IFNULL, l11);
    		mv.visitVarInsn(ALOAD, 6);
    		mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/ObjectInputStream", "close", "()V", false);
    		mv.visitLabel(l11);
    		mv.visitVarInsn(ALOAD, 4);
    		mv.visitInsn(ATHROW);
    		mv.visitLabel(l4);
    		mv.visitVarInsn(ASTORE, 5);
    		mv.visitVarInsn(ALOAD, 4);
    		Label l12 = new Label();
    		mv.visitJumpInsn(IFNONNULL, l12);
    		mv.visitVarInsn(ALOAD, 5);
    		mv.visitVarInsn(ASTORE, 4);
    		Label l13 = new Label();
    		mv.visitJumpInsn(GOTO, l13);
    		mv.visitLabel(l12);
    		mv.visitVarInsn(ALOAD, 4);
    		mv.visitVarInsn(ALOAD, 5);
    		mv.visitJumpInsn(IF_ACMPEQ, l13);
    		mv.visitVarInsn(ALOAD, 4);
    		mv.visitVarInsn(ALOAD, 5);
    		mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Throwable", "addSuppressed", "(Ljava/lang/Throwable;)V", false);
    		mv.visitLabel(l13);
    		mv.visitVarInsn(ALOAD, 4);
    		mv.visitInsn(ATHROW);
    		mv.visitLabel(l6);
    		mv.visitVarInsn(ASTORE, 4);
    		Label l14 = new Label();
    		mv.visitLabel(l14);
    		mv.visitTypeInsn(NEW, "java/lang/RuntimeException");
    		mv.visitInsn(DUP);
    		mv.visitVarInsn(ALOAD, 4);
    		mv.visitMethodInsn(INVOKESPECIAL, "java/lang/RuntimeException", "<init>", "(Ljava/lang/Throwable;)V", false);
    		mv.visitInsn(ATHROW);
    		mv.visitLabel(l10);
    		mv.visitInsn(RETURN);
    		mv.visitMaxs(3, 7);
    		mv.visitEnd();
    	}
    }
    
    
    private void addDMAMethodsViaSerialiableMethods(){
    		ArrayList<String> names = new ArrayList<String>(fieldNameToDescIncTrans.keySet());
    		Collections.sort(names);	
    		
    	{//toBinary
			MethodVisitor mv = super.visitMethod(ACC_PUBLIC, "toBinary", "(Lcom/concurnas/bootstrap/lang/offheap/Encoder;)V", null, null);
			mv.visitCode();
			Label l0 = new Label();
			Label l1 = new Label();
			Label l2 = new Label();
			mv.visitTryCatchBlock(l0, l1, l2, null);
			Label l3 = new Label();
			Label l4 = new Label();
			mv.visitTryCatchBlock(l3, l4, l4, null);
			Label l5 = new Label();
			Label l6 = new Label();
			mv.visitTryCatchBlock(l5, l6, l6, "java/io/IOException");
			
			if(isNonObjectParent()){
				Label l7 = new Label();
				mv.visitLabel(l7);
				mv.visitVarInsn(ALOAD, 0);
				mv.visitVarInsn(ALOAD, 1);
				mv.visitMethodInsn(INVOKESPECIAL, superclassname, "toBinary", "(Lcom/concurnas/bootstrap/lang/offheap/Encoder;)V", false);
			}

			Label l8 = new Label();
			mv.visitLabel(l8);
			mv.visitTypeInsn(NEW, "java/io/ByteArrayOutputStream");
			mv.visitInsn(DUP);
			mv.visitMethodInsn(INVOKESPECIAL, "java/io/ByteArrayOutputStream", "<init>", "()V", false);
			mv.visitVarInsn(ASTORE, 2);
			mv.visitLabel(l5);
			mv.visitInsn(ACONST_NULL);
			mv.visitVarInsn(ASTORE, 3);
			mv.visitInsn(ACONST_NULL);
			mv.visitVarInsn(ASTORE, 4);
			mv.visitLabel(l3);
			mv.visitTypeInsn(NEW, "java/io/ObjectOutputStream");
			mv.visitInsn(DUP);
			mv.visitVarInsn(ALOAD, 2);
			mv.visitMethodInsn(INVOKESPECIAL, "java/io/ObjectOutputStream", "<init>", "(Ljava/io/OutputStream;)V", false);
			mv.visitVarInsn(ASTORE, 5);
			mv.visitLabel(l0);
			mv.visitVarInsn(ALOAD, 5);
			mv.visitVarInsn(ALOAD, 0);
			mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/ObjectOutputStream", "writeObject", "(Ljava/lang/Object;)V", false);
			mv.visitLabel(l1);
			mv.visitVarInsn(ALOAD, 5);
			Label l9 = new Label();
			mv.visitJumpInsn(IFNULL, l9);
			mv.visitVarInsn(ALOAD, 5);
			mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/ObjectOutputStream", "close", "()V", false);
			mv.visitJumpInsn(GOTO, l9);
			mv.visitLabel(l2);
			mv.visitVarInsn(ASTORE, 3);
			mv.visitVarInsn(ALOAD, 5);
			Label l10 = new Label();
			mv.visitJumpInsn(IFNULL, l10);
			mv.visitVarInsn(ALOAD, 5);
			mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/ObjectOutputStream", "close", "()V", false);
			mv.visitLabel(l10);
			mv.visitVarInsn(ALOAD, 3);
			mv.visitInsn(ATHROW);
			mv.visitLabel(l4);
			mv.visitVarInsn(ASTORE, 4);
			mv.visitVarInsn(ALOAD, 3);
			Label l11 = new Label();
			mv.visitJumpInsn(IFNONNULL, l11);
			mv.visitVarInsn(ALOAD, 4);
			mv.visitVarInsn(ASTORE, 3);
			Label l12 = new Label();
			mv.visitJumpInsn(GOTO, l12);
			mv.visitLabel(l11);
			mv.visitVarInsn(ALOAD, 3);
			mv.visitVarInsn(ALOAD, 4);
			mv.visitJumpInsn(IF_ACMPEQ, l12);
			mv.visitVarInsn(ALOAD, 3);
			mv.visitVarInsn(ALOAD, 4);
			mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Throwable", "addSuppressed", "(Ljava/lang/Throwable;)V", false);
			mv.visitLabel(l12);
			mv.visitVarInsn(ALOAD, 3);
			mv.visitInsn(ATHROW);
			mv.visitLabel(l6);
			mv.visitVarInsn(ASTORE, 3);
			Label l13 = new Label();
			mv.visitLabel(l13);
			mv.visitTypeInsn(NEW, "java/lang/RuntimeException");
			mv.visitInsn(DUP);
			mv.visitVarInsn(ALOAD, 3);
			mv.visitMethodInsn(INVOKESPECIAL, "java/lang/RuntimeException", "<init>", "(Ljava/lang/Throwable;)V", false);
			mv.visitInsn(ATHROW);
			mv.visitLabel(l9);
			mv.visitVarInsn(ALOAD, 1);
			mv.visitVarInsn(ALOAD, 2);
			mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/ByteArrayOutputStream", "toByteArray", "()[B", false);
			mv.visitInsn(ICONST_1);
			mv.visitMethodInsn(INVOKEINTERFACE, "com/concurnas/bootstrap/lang/offheap/Encoder", "putByteArray", "(Ljava/lang/Object;I)I", true);
			mv.visitInsn(POP);
			Label l14 = new Label();
			mv.visitLabel(l14);
			mv.visitInsn(RETURN);
			mv.visitMaxs(3, 6);
			mv.visitEnd();
    	}
    	
    	{//fromBinary
			MethodVisitor mv = super.visitMethod(ACC_PUBLIC, "fromBinary", "(Lcom/concurnas/bootstrap/lang/offheap/Decoder;)V", null, null);
    		mv.visitCode();
    		Label l0 = new Label();
    		Label l1 = new Label();
    		Label l2 = new Label();
    		mv.visitTryCatchBlock(l0, l1, l2, null);
    		Label l3 = new Label();
    		Label l4 = new Label();
    		mv.visitTryCatchBlock(l3, l4, l4, null);
    		Label l5 = new Label();
    		Label l6 = new Label();
    		mv.visitTryCatchBlock(l5, l6, l6, "java/io/IOException");
    		mv.visitTryCatchBlock(l5, l6, l6, "java/lang/ClassNotFoundException");
    		
    		if(isNonObjectParent()){
        		Label l7 = new Label();
        		mv.visitLabel(l7);
        		mv.visitVarInsn(ALOAD, 0);
        		mv.visitVarInsn(ALOAD, 1);
        		mv.visitMethodInsn(INVOKESPECIAL, superclassname, "fromBinary", "(Lcom/concurnas/bootstrap/lang/offheap/Decoder;)V", false);
        	}
    		
    		Label l8 = new Label();
    		mv.visitLabel(l8);
    		mv.visitVarInsn(ALOAD, 1);
    		mv.visitInsn(ICONST_1);
    		mv.visitMethodInsn(INVOKEINTERFACE, "com/concurnas/bootstrap/lang/offheap/Decoder", "getByteArray", "(I)Ljava/lang/Object;", true);
    		mv.visitTypeInsn(CHECKCAST, "[B");
    		mv.visitVarInsn(ASTORE, 2);
    		Label l9 = new Label();
    		mv.visitLabel(l9);
    		mv.visitTypeInsn(NEW, "java/io/ByteArrayInputStream");
    		mv.visitInsn(DUP);
    		mv.visitVarInsn(ALOAD, 2);
    		mv.visitMethodInsn(INVOKESPECIAL, "java/io/ByteArrayInputStream", "<init>", "([B)V", false);
    		mv.visitVarInsn(ASTORE, 3);
    		mv.visitLabel(l5);
    		mv.visitInsn(ACONST_NULL);
    		mv.visitVarInsn(ASTORE, 4);
    		mv.visitInsn(ACONST_NULL);
    		mv.visitVarInsn(ASTORE, 5);
    		mv.visitLabel(l3);
    		mv.visitTypeInsn(NEW, "java/io/ObjectInputStream");
    		mv.visitInsn(DUP);
    		mv.visitVarInsn(ALOAD, 3);
    		mv.visitMethodInsn(INVOKESPECIAL, "java/io/ObjectInputStream", "<init>", "(Ljava/io/InputStream;)V", false);
    		mv.visitVarInsn(ASTORE, 6);
    		mv.visitLabel(l0);
    		mv.visitVarInsn(ALOAD, 6);
    		mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/ObjectInputStream", "readObject", "()Ljava/lang/Object;", false);
    		mv.visitTypeInsn(CHECKCAST, classname);
    		mv.visitVarInsn(ASTORE, 7);
    		
    		for(String name : names){
        		Label l10 = new Label();
        		mv.visitLabel(l10);
        		String desc = fieldNameToDescIncTrans.get(name);

        		mv.visitVarInsn(ALOAD, 0);
        		mv.visitVarInsn(ALOAD, 7);
        		mv.visitFieldInsn(GETFIELD, classname, name, desc);
        		mv.visitFieldInsn(PUTFIELD, classname, name, desc);
    		}
    		
    		mv.visitLabel(l1);
    		mv.visitVarInsn(ALOAD, 6);
    		Label l11 = new Label();
    		mv.visitJumpInsn(IFNULL, l11);
    		mv.visitVarInsn(ALOAD, 6);
    		mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/ObjectInputStream", "close", "()V", false);
    		mv.visitJumpInsn(GOTO, l11);
    		mv.visitLabel(l2);
    		mv.visitVarInsn(ASTORE, 4);
    		mv.visitVarInsn(ALOAD, 6);
    		Label l12 = new Label();
    		mv.visitJumpInsn(IFNULL, l12);
    		mv.visitVarInsn(ALOAD, 6);
    		mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/ObjectInputStream", "close", "()V", false);
    		mv.visitLabel(l12);
    		mv.visitVarInsn(ALOAD, 4);
    		mv.visitInsn(ATHROW);
    		mv.visitLabel(l4);
    		mv.visitVarInsn(ASTORE, 5);
    		mv.visitVarInsn(ALOAD, 4);
    		Label l13 = new Label();
    		mv.visitJumpInsn(IFNONNULL, l13);
    		mv.visitVarInsn(ALOAD, 5);
    		mv.visitVarInsn(ASTORE, 4);
    		Label l14 = new Label();
    		mv.visitJumpInsn(GOTO, l14);
    		mv.visitLabel(l13);
    		mv.visitVarInsn(ALOAD, 4);
    		mv.visitVarInsn(ALOAD, 5);
    		mv.visitJumpInsn(IF_ACMPEQ, l14);
    		mv.visitVarInsn(ALOAD, 4);
    		mv.visitVarInsn(ALOAD, 5);
    		mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Throwable", "addSuppressed", "(Ljava/lang/Throwable;)V", false);
    		mv.visitLabel(l14);
    		mv.visitVarInsn(ALOAD, 4);
    		mv.visitInsn(ATHROW);
    		mv.visitLabel(l6);
    		mv.visitVarInsn(ASTORE, 4);
    		Label l15 = new Label();
    		mv.visitLabel(l15);
    		mv.visitTypeInsn(NEW, "java/lang/RuntimeException");
    		mv.visitInsn(DUP);
    		mv.visitVarInsn(ALOAD, 4);
    		mv.visitMethodInsn(INVOKESPECIAL, "java/lang/RuntimeException", "<init>", "(Ljava/lang/Throwable;)V", false);
    		mv.visitInsn(ATHROW);
    		mv.visitLabel(l11);
    		mv.visitInsn(RETURN);
    		mv.visitMaxs(3, 8);
    		mv.visitEnd();
    	}
    }
    
    
    private void createConstuctorWithDefaultInit(){
		//add initUncreatable and boolean[]
    	
    	{//toBinary
			MethodVisitor mv = super.visitMethod(ACC_PUBLIC, "<init>", "(Lcom/concurnas/bootstrap/runtime/InitUncreatable;[Z)V", null, null);
			mv.visitCode();
			mv.visitLabel(new Label());
			
			mv.visitVarInsn(ALOAD, 0);
			
			if(superclassname.equals("java/lang/Object")){
				mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V");
			}else if(superclassname.equals("com/concurnas/bootstrap/runtime/cps/CObject")){
				mv.visitMethodInsn(INVOKESPECIAL, "com/concurnas/bootstrap/runtime/cps/CObject", "<init>", "()V");
			}else{
				mv.visitVarInsn(ALOAD, 1);
				mv.visitVarInsn(ALOAD, 2);
				mv.visitMethodInsn(INVOKESPECIAL, superclassname, "<init>", "(Lcom/concurnas/bootstrap/runtime/InitUncreatable;[Z)V");
			}
			
			mv.visitVarInsn(ALOAD, 0);
			mv.visitVarInsn(ALOAD, 1);
			mv.visitVarInsn(ALOAD, 2);
			mv.visitMethodInsn(INVOKESPECIAL, classname, "defaultFieldInit$", "(Lcom/concurnas/bootstrap/runtime/InitUncreatable;[Z)V");
			mv.visitInsn(RETURN);
			mv.visitMaxs(2, 2);
			mv.visitEnd();
		}
    	
    }
	
    /*
    private void addDMAMethodEnum(){
    	//not used as we just store the string refering to the instance and map to that via Enum.valueOf(Thing.class, 'instance')
		
		{//toBinary
			MethodVisitor mv = super.visitMethod(ACC_PUBLIC, "toBinary", "(Lcom/concurnas/bootstrap/lang/offheap/Encoder;)V", null, null);
			mv.visitCode();
			Label l0 = new Label();
			mv.visitLabel(l0);
			mv.visitVarInsn(ALOAD, 1);
			mv.visitVarInsn(ALOAD, 0);
			mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Enum", "name", "()Ljava/lang/String;", false);
			mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "getBytes", "()[B", false);
			mv.visitInsn(ICONST_1);
			mv.visitMethodInsn(INVOKEINTERFACE, "com/concurnas/bootstrap/lang/offheap/Encoder", "putByteArray", "(Ljava/lang/Object;I)I", true);
			mv.visitInsn(POP);
			Label l1 = new Label();
			mv.visitLabel(l1);
			mv.visitInsn(RETURN);
			mv.visitMaxs(3, 2);
			mv.visitEnd();
		}
		
		
		
		{//fromBinary
			MethodVisitor mv = super.visitMethod(ACC_PUBLIC + ACC_STATIC, "fromBinary", "(Lcom/concurnas/bootstrap/lang/offheap/Decoder;)L"+classname+";", null, null);
			mv.visitCode();
			Label l0 = new Label();
			mv.visitLabel(l0);
			mv.visitTypeInsn(NEW, "java/lang/String");
			mv.visitInsn(DUP);
			mv.visitVarInsn(ALOAD, 0);
			mv.visitInsn(ICONST_1);
			mv.visitMethodInsn(INVOKEINTERFACE, "com/concurnas/bootstrap/lang/offheap/Decoder", "getByteArray", "(I)Ljava/lang/Object;", true);
			mv.visitTypeInsn(CHECKCAST, "[B");
			mv.visitMethodInsn(INVOKESPECIAL, "java/lang/String", "<init>", "([B)V", false);
			mv.visitMethodInsn(INVOKESTATIC, classname, "valueOf", "(Ljava/lang/String;)L"+classname+";", false);
			mv.visitInsn(ARETURN);
			mv.visitMaxs(4, 1);
			mv.visitEnd();
		}
		
    	
		{//toBinary
			MethodVisitor mv = super.visitMethod(ACC_PUBLIC, "toBinary", "(Lcom/concurnas/bootstrap/lang/offheap/Encoder;)V", null, null);
			mv.visitCode();
			Label l0 = new Label();
			mv.visitLabel(l0);
			mv.visitVarInsn(ALOAD, 1);
			mv.visitVarInsn(ALOAD, 0);
			mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Enum", "ordinal", "()I");
			mv.visitMethodInsn(INVOKEINTERFACE, "com/concurnas/bootstrap/lang/offheap/Encoder", "put", "(I)I");
			mv.visitInsn(POP);
			mv.visitInsn(RETURN);
			mv.visitMaxs(2, 2);
			mv.visitEnd();
		}
    	
		{//fromBinary
			MethodVisitor mv = super.visitMethod(ACC_PUBLIC + ACC_STATIC, "fromBinary", "(Lcom/concurnas/bootstrap/lang/offheap/Decoder;)L"+classname+";", null, null);
			mv.visitCode();
			Label l0 = new Label();
			mv.visitLabel(l0);
			
			
			mv.visitMethodInsn(INVOKESTATIC, classname, "values", "()[L"+classname+";");
			mv.visitVarInsn(ALOAD, 0);
			mv.visitMethodInsn(INVOKEINTERFACE, "com/concurnas/bootstrap/lang/offheap/Decoder", "getInt", "()I");
			mv.visitInsn(AALOAD);
			mv.visitInsn(ARETURN);
			
			mv.visitMaxs(2, 1);
			mv.visitEnd();
		}
    }*/
    
    private void addDMAMethods(){
    	ArrayList<String> names = new ArrayList<String>(fieldNameToDesc.keySet());
    	Collections.sort(names);	
    	
    	{//toBinary
    		MethodVisitor mv = super.visitMethod(ACC_PUBLIC, "toBinary", "(Lcom/concurnas/bootstrap/lang/offheap/Encoder;)V", null, null);
    		mv.visitAnnotation("Lcom/concurnas/lang/Uninterruptible;", true).visitEnd();
    		mv.visitCode();
    		
    		if(isNonObjectParent()){//skip super call if it resolves to object, as object has no state
        		Label l0 = new Label();
        		mv.visitLabel(l0);
        		mv.visitVarInsn(ALOAD, 0);
        		mv.visitVarInsn(ALOAD, 1);
        		mv.visitMethodInsn(INVOKESPECIAL, superclassname, "toBinary", "(Lcom/concurnas/bootstrap/lang/offheap/Encoder;)V", false);
    		}
    		
    		for(String name : names){
        		String desc = fieldNameToDesc.get(name);
        		
        		DMAType dmx;
    			
    			int arrayLevels = 0;
    			while(desc.charAt(arrayLevels) == '['){
    				arrayLevels++;	
    			}
    			boolean isObject = desc.endsWith(";");
    			if(isObject){
    				dmx = DMAType.descToType.get((arrayLevels > 0 ? "[":"") + "Ljava/lang/Object;");
    			}else{
    				dmx = DMAType.descToType.get(arrayLevels > 0? ("[" + desc.substring(arrayLevels, desc.length())):desc);
    			}
        		
    			Label l1 = new Label();
        		mv.visitLabel(l1);
        		mv.visitVarInsn(ALOAD, 1);
        		mv.visitVarInsn(ALOAD, 0);
        		mv.visitFieldInsn(GETFIELD, classname, name, desc);
        		if(dmx.isArray){
        			CodeGenUtils.intOpcode(mv, arrayLevels);
        		}
        		mv.visitMethodInsn(INVOKEINTERFACE, "com/concurnas/bootstrap/lang/offheap/Encoder", dmx.putName, dmx.isArray ?"(Ljava/lang/Object;I)I":isObject?"(Ljava/lang/Object;)I":"("+desc+")I", true);
        		mv.visitInsn(POP);
    		}
    		mv.visitInsn(RETURN);
    		mv.visitMaxs(3, 2);
    		mv.visitEnd();
    	}
    	
    	
    	{//fromBinary
    		MethodVisitor mv = super.visitMethod(ACC_PUBLIC, "fromBinary", "(Lcom/concurnas/bootstrap/lang/offheap/Decoder;)V", null, null);
    		mv.visitAnnotation("Lcom/concurnas/lang/Uninterruptible;", true).visitEnd();
    		mv.visitCode();
    		
    		if(isNonObjectParent()){//skip super call if it resolves to object, as object has no state
        		Label l0 = new Label();
        		mv.visitLabel(l0);
        		mv.visitVarInsn(ALOAD, 0);
        		mv.visitVarInsn(ALOAD, 1);
        		mv.visitMethodInsn(INVOKESPECIAL, superclassname, "fromBinary", "(Lcom/concurnas/bootstrap/lang/offheap/Decoder;)V", false);
    		}
    		
    		
    		int namezin = names.size();
    		Label[] tryCatchfinLabels = new Label[namezin*3];
    		
    		for(int n=0; n < namezin; n++){
    			Label blcokstart = new Label();
    			Label blockend = new Label();
    			Label catchstart = new Label();
    			tryCatchfinLabels[n*3] = blcokstart;
    			tryCatchfinLabels[n*3+1] = blockend;
    			tryCatchfinLabels[n*3+2] = catchstart;
    			mv.visitTryCatchBlock(blcokstart, blockend, catchstart, "com/concurnas/bootstrap/lang/offheap/MissingField");
    		}
    		
    		
    		mv.visitVarInsn(ALOAD, 1);
    		mv.visitMethodInsn(INVOKEINTERFACE, "com/concurnas/bootstrap/lang/offheap/Decoder", "canThrowMissingFieldException", "()Z", true);
    		Label onfalse = new Label();
    		Label onAfterIf = new Label();
    		mv.visitJumpInsn(IFEQ, onfalse);

    		
    		for(boolean withException : new boolean[]{true, false}){
    			if(!withException){
    				mv.visitLabel(onfalse);
    			}
    			/*else{
    				mv.visitFieldInsn(GETSTATIC, "java/lang/System", "err", "Ljava/io/PrintStream;");
    				mv.visitLdcInsn("hi metaBinary");
    				mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V", false);
    			}*/
    			int n=0;
    			for(String name : names){
            		String desc = fieldNameToDesc.get(name);
            		            		
        			mv.visitLabel(withException?tryCatchfinLabels[n*3] : new Label());
        			
        		/*	mv.visitFieldInsn(GETSTATIC, "java/lang/System", "err", "Ljava/io/PrintStream;");
    				mv.visitLdcInsn("get: " + name + "[" + withException);
    				mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V", false);
            		*/
        			
        			
        			mv.visitVarInsn(ALOAD, 0);
        			mv.visitVarInsn(ALOAD, 1);
        				
        			DMAType dmx;
        			
        			int arrayLevels = 0;
        			while(desc.charAt(arrayLevels) == '['){
        				arrayLevels++;	
        			}
        			
        			boolean isObject = desc.endsWith(";");
        			if(isObject){
        				dmx = DMAType.descToType.get((arrayLevels > 0 ? "[":"") + "Ljava/lang/Object;");
        			}else{
        				dmx = DMAType.descToType.get(arrayLevels > 0? ("[" + desc.substring(arrayLevels, desc.length())):desc);
        			}
        			
        			if(dmx.isArray && !isObject){
        				CodeGenUtils.intOpcode(mv, arrayLevels);
        			}
        			
        			mv.visitMethodInsn(INVOKEINTERFACE, "com/concurnas/bootstrap/lang/offheap/Decoder", dmx.getName, dmx.isArray && !isObject ?"(I)Ljava/lang/Object;" : isObject?"()Ljava/lang/Object;":"()" + desc);
        			
        			if(dmx.isArray || (isObject && !desc.equals("Ljava/lang/Object;"))){//array or object, but not Object.class
        				mv.visitTypeInsn(CHECKCAST, arrayLevels>0?desc:desc.substring(1, desc.length()-1));
        			}
        			
        			mv.visitFieldInsn(PUTFIELD, classname, name, desc);
        			
/*
        			mv.visitFieldInsn(GETSTATIC, "java/lang/System", "err", "Ljava/io/PrintStream;");
    				mv.visitLdcInsn("i put field: " + name + "[" + withException);
    				mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V", false);
            		*/
        			
        			if(withException){
        				Label afterCatch = n < (namezin-1)?tryCatchfinLabels[(n+1)*3]:onAfterIf;
        				mv.visitLabel(tryCatchfinLabels[n*3+1]);
            			mv.visitJumpInsn(GOTO, afterCatch);
            			mv.visitLabel(tryCatchfinLabels[n*3+2]);
            			mv.visitVarInsn(ASTORE, 2);//pop?
            			/*if(n == namezin && withException){
            				mv.visitLabel(afterCatch);
            			}*/
        			}
        			
        			n++;
        		}
    			if(withException){
					mv.visitJumpInsn(GOTO, onAfterIf);
					mv.visitLabel(onfalse);
    			}else{
    				mv.visitLabel(onAfterIf);
    			}
    			
    		}
    		
    		mv.visitInsn(RETURN);
    		mv.visitMaxs(3, 2);
    		mv.visitEnd();
    	}	
    	
    	{//metaBinary
    		MethodVisitor mv = super.visitMethod(ACC_PUBLIC + ACC_STATIC, "metaBinary", "()[Ljava/lang/String;", null, null);
    		mv.visitAnnotation("Lcom/concurnas/lang/Uninterruptible;", true).visitEnd();
			mv.visitCode();
			/*
			String[] aa = new String[]{"one", "two"};
			int arlen = aa.length;
			String[] aa2 = new String[arlen++];
			System.arraycopy(aa, 0, aa2, 0, arlen);
			*/

			int toRetVar;
			int varoffsetVar = 1;
			if(isNonObjectParent()){//add parent fields - prepend list
				toRetVar  =2;
        		mv.visitLabel(new Label());
        		mv.visitMethodInsn(INVOKESTATIC, superclassname, "metaBinary", "()[Ljava/lang/String;");
        		mv.visitVarInsn(ASTORE, 0);
        		mv.visitVarInsn(ALOAD, 0);
        		mv.visitInsn(ARRAYLENGTH);
        		mv.visitVarInsn(ISTORE, varoffsetVar);

				mv.visitVarInsn(ILOAD, varoffsetVar);
        		CodeGenUtils.intOpcode(mv, names.size()*2);
				mv.visitInsn(IADD);
				mv.visitTypeInsn(ANEWARRAY, "java/lang/String");
        		mv.visitVarInsn(ASTORE, toRetVar);
				
				mv.visitVarInsn(ALOAD, 0);
				mv.visitInsn(ICONST_0);
				mv.visitVarInsn(ALOAD, toRetVar);
				mv.visitInsn(ICONST_0);
				mv.visitVarInsn(ILOAD, varoffsetVar);
				mv.visitMethodInsn(INVOKESTATIC, "java/lang/System", "arraycopy", "(Ljava/lang/Object;ILjava/lang/Object;II)V", false);
    		}
			else{//no super call
				toRetVar=0;
				CodeGenUtils.intOpcode(mv, names.size()*2);
				mv.visitTypeInsn(ANEWARRAY, "java/lang/String");
				mv.visitVarInsn(ASTORE, toRetVar);
				CodeGenUtils.intOpcode(mv, 0);
        		mv.visitVarInsn(ISTORE, varoffsetVar);
			}
			
			
			int n=0;
			for(String name : names){
				mv.visitLabel(new Label());
				mv.visitVarInsn(ALOAD, toRetVar);
				mv.visitVarInsn(ILOAD, varoffsetVar);
				mv.visitIincInsn(varoffsetVar, 1);
				mv.visitLdcInsn(name);
				mv.visitInsn(AASTORE);
				
				mv.visitLabel(new Label());
				mv.visitVarInsn(ALOAD, toRetVar);
				mv.visitVarInsn(ILOAD, varoffsetVar);
				mv.visitIincInsn(varoffsetVar, 1);
				mv.visitLdcInsn(fieldNameToDesc.get(name));
				mv.visitInsn(AASTORE);
			}
			
			mv.visitLabel(new Label());
			mv.visitVarInsn(ALOAD, toRetVar);
			mv.visitInsn(ARETURN);
    	}
    	
    }
    
    
    private static enum DMAType{
    	Int("I", "put", "getInt", false),
    	Long("J", "put", "getLong", false),
    	Double("D", "put", "getDouble", false),
    	Float("F", "put", "getFloat", false),
    	Byte("B", "put", "getByte", false),
    	Char("C", "put", "getChar", false),
    	Short("S", "put", "getShort", false),
    	Boolean("Z", "put", "getBoolean", false),
    	
    	IntAr("[I", "putIntArray", "getIntArray", true),
    	LongAr("[J", "putLongArray", "getLongArray", true),
    	DoubleAr("[D", "putDoubleArray", "getDoubleArray", true),
    	FloatAr("[F", "putFloatArray", "getFloatArray", true),
    	ByteAr("[B", "putByteArray", "getByteArray", true),
    	CharAr("[C", "putCharArray", "getCharArray", true),
    	ShortAr("[S", "putShortArray", "getShortArray", true),
    	BooleanAr("[Z", "putBooleanArray", "getBooleanArray", true),
    	
    	Object("Ljava/lang/Object;", "put", "getObject", false),
    	ObjectAr("[Ljava/lang/Object;", "putObjectArray", "getObject", true);
    	
    	//others, int[] plus object
    	
    	public String desc;
    	public String putName;
    	public String getName;
    	public boolean isArray;
    	
    	DMAType(String desc, String putName, String getName, boolean isArray){
    		this.desc=desc;
    		this.putName=putName;
    		this.getName=getName;
    		this.isArray=isArray;
    	}
    	
    	private static HashMap<String, DMAType> descToType = new HashMap<String, DMAType>();
    	static{
    		for(DMAType tt : DMAType.values()){
    			descToType.put(tt.desc, tt);
    		}
    	}
    }
}
