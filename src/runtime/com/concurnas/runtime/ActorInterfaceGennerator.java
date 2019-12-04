package com.concurnas.runtime;

import java.util.LinkedHashSet;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import com.concurnas.runtime.cps.mirrors.ClassMirror;
import com.concurnas.runtime.cps.mirrors.MethodMirror;

public class ActorInterfaceGennerator implements Opcodes {

	private static byte[] create(String name, LinkedHashSet<Pair<String, String>> methods) throws Exception {

		ClassWriter cw = new ClassWriter(0);
		MethodVisitor mv;

		cw.visit(V1_8, ACC_PUBLIC + ACC_ABSTRACT + ACC_INTERFACE, name, null, "java/lang/Object", null);
		//cw.visitSource("ActorInterface.java", null);
		for(Pair<String, String> mm: methods){
			String namea = mm.getA();
			String desc = mm.getB();
			//System.err.println(String.format("def %s %s", namea, desc));
			mv = cw.visitMethod(ACC_PUBLIC + ACC_ABSTRACT, namea, desc, null, null);
			mv.visitEnd();
		}
		/*
		{
			mv = cw.visitMethod(ACC_PUBLIC + ACC_ABSTRACT, "restart", "()V", null, null);
			mv.visitEnd();
		}
		*/
		cw.visitEnd();

		return cw.toByteArray();
	}

	public static byte[] actorInterfaceGennerator(String source, ConcurnasClassLoader loader, String ifaceName){
		LinkedHashSet<Pair<String, String>> methods = new LinkedHashSet<Pair<String, String>>();
		
		try {
			ClassMirror cm = loader.getDetector().classForName(source);
			if(null == cm){
				throw new Exception("Unable to find definition for: " + source);
			}
			
			String sup=null;
			while(null != cm){
				for(MethodMirror mm : cm.getDeclaredMethods()){
					String namea = mm.getName();
					if(!namea.equals("<init>") && mm.isPublicAndNonStatic() ){
						String desc=mm.getMethodDescriptor();
						methods.add(new Pair<String, String>(namea + "$ActorCall", desc));
					}
				}
				
				
				sup = cm.getSuperclassNoEx();
				if(null == sup){
					cm = null;
				}
				else{
					cm = loader.getDetector().classForName(sup);
				}
			}
			
			methods.add(new Pair<String, String>("toString$ActorSuperCall", "()Ljava/lang/String;"));
			methods.add(new Pair<String, String>("hashCode$ActorSuperCall", "()I"));
			methods.add(new Pair<String, String>("equals$ActorSuperCall", "(Ljava/lang/Object;)Z"));
			
			return create(ifaceName, methods);
		} catch (Exception e) {
			throw new RuntimeException("Failure during genneration of actor interface for: " + source, e);
		}
	}
}
