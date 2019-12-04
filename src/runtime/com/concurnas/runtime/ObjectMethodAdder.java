package com.concurnas.runtime;

import java.lang.reflect.Modifier;
import java.util.HashMap;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import com.concurnas.runtime.cps.analysis.ConcClassWriter;
import com.concurnas.runtime.cps.mirrors.CachedClassMirrors;
import com.concurnas.runtime.cps.mirrors.ClassMirror;
import com.concurnas.runtime.cps.mirrors.ClassMirrorNotFoundException;
import com.concurnas.runtime.cps.mirrors.Detector;
import com.concurnas.runtime.cps.mirrors.MethodMirror;

public class ObjectMethodAdder implements Opcodes {
	private final byte[] code;
	private final  ConcClassUtil clloader;
	private final Detector detector;
	public ObjectMethodAdder(final byte[] code, final ConcClassUtil clloader) {
		this.code=code;
		this.clloader = clloader;
		this.detector=new Detector(new CachedClassMirrors(clloader));
	}
	
	private static HashMap<String, MissingStuff> classNameToMissingStuff = new HashMap<String, MissingStuff>();//TODO: hack remove the static thingy
	
	public final class MissingStuff{
		public boolean missingEquals = true;
		public boolean missingHashCode = true;
		public boolean missingToString = true;
	}
	
	private MissingStuff lazyCreateClsMapping(String clsName, boolean isInterface) throws ClassMirrorNotFoundException{
		String analysisName = clsName;
		
		MissingStuff myMissing = classNameToMissingStuff.get(analysisName);
		
		if(isInterface){
			if(null == myMissing){
				myMissing = new MissingStuff();
			}
			myMissing.missingEquals=false;
			myMissing.missingHashCode=false;
			myMissing.missingToString=false;

			classNameToMissingStuff.put(analysisName, myMissing);
		}
		else if(null == myMissing){
			myMissing = new MissingStuff();
			
			ClassMirror cm = detector.mirror.classForName(analysisName);
			if(cm==null){//oops happens with Globals
				myMissing.missingEquals=true;
				myMissing.missingHashCode=true;
				myMissing.missingToString=true;
			}
			else{
				for(MethodMirror m : cm.getDeclaredMethods()){
					String name = m.getName();
					String desc = m.getMethodDescriptor();
					
					if(name.equals("equals") && desc.equals("(Ljava/lang/Object;)Z") ){
						myMissing.missingEquals=false;
					}
					else if(name.equals("hashCode")  && desc.equals("()I")  ){
						myMissing.missingHashCode=false;
					}
					else if(name.equals("toString")  && desc.equals("()Ljava/lang/String;")  ){
						myMissing.missingToString=false;
					}
				}
				
				String supcls = cm.getSuperclass();
				
				if(supcls!=null && !supcls.equals("java/lang/Object")){//if null then really Object...
					//see if they have been defined in the superclass...
					MissingStuff supMissing = lazyCreateClsMapping(supcls, false);
					if(myMissing.missingEquals && !supMissing.missingEquals){
						myMissing.missingEquals = false;
					}
					
					if(myMissing.missingHashCode && !supMissing.missingHashCode){
						myMissing.missingHashCode = false;
					}
					
					if(myMissing.missingToString && !supMissing.missingToString){
						myMissing.missingToString = false;
					}
				}
				
			}

			classNameToMissingStuff.put(analysisName, myMissing);
		}
		
		return myMissing;
	}
	
	
	private final class OMAFinder extends ClassVisitor{
		
		public OMAFinder() {
			super(ASM7);
		}

		public boolean missingEquals = true;
		public boolean missingHashCode = true;
		public boolean missingToString = true;
		
		private MissingStuff myMissing = null;
		private String myClassName;
		
		private void findMissings(int access){
			try{
				myMissing = lazyCreateClsMapping(this.myClassName, Modifier.isInterface(access));
			}
			catch (ClassMirrorNotFoundException e) {
				myMissing=null;//backupmethod
				//TODO: test if this is triggered
			}
		}
		
		public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
			this.myClassName = name;
			findMissings(access);
			super.visit(version, access, name, signature, superName, interfaces);
		}
		
		public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
			if(myMissing == null){//backupmethod if analysis failed
				if(name.equals("equals") && desc.equals("(Ljava/lang/Object;)Z") ){
					missingEquals=false;
				}
				else if(name.equals("hashCode")  && desc.equals("()I")  ){
					missingHashCode=false;
				}
				else if(name.equals("toString")  && desc.equals("()Ljava/lang/String;")  ){
					missingToString=false;
				}
			}
			
			return super.visitMethod(access, name, desc, signature, exceptions);
		}
		
		public void visitEnd() {
			super.visitEnd();
			
			if(null == myMissing){//ensure set from backup method
				MissingStuff ms = new MissingStuff();
				ms.missingEquals = this.missingEquals;
				ms.missingHashCode = this.missingHashCode;
				ms.missingToString = this.missingToString;
				classNameToMissingStuff.put(this.myClassName, ms);
			}
		}
	}
	
	private class MissingMethodAdder extends ClassVisitor{
		public MissingMethodAdder(ClassVisitor cw) {
			super(ASM7, cw);
		}
		
		public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
			super.visit(version, access, name, signature, superName, interfaces);
			//add missing methods if need to:
			MissingStuff ms = classNameToMissingStuff.get(name);
			
			if(ms.missingEquals){
				MethodVisitor mv = super.visitMethod(ACC_PUBLIC + ACC_SYNTHETIC, "equals", "(Ljava/lang/Object;)Z", null, null);
				mv.visitCode();
				Label l0 = new Label();
				mv.visitLabel(l0);
				mv.visitVarInsn(ALOAD, 0);
				mv.visitVarInsn(ALOAD, 1);
				mv.visitMethodInsn(INVOKESPECIAL, superName, "equals", "(Ljava/lang/Object;)Z", false);
				mv.visitInsn(IRETURN);
				mv.visitMaxs(2, 2);
				mv.visitEnd();
			}
			
			if(ms.missingHashCode){
				MethodVisitor mv = super.visitMethod(ACC_PUBLIC + ACC_SYNTHETIC, "hashCode", "()I", null, null);
				mv.visitCode();
				Label l0 = new Label();
				mv.visitLabel(l0);
				mv.visitVarInsn(ALOAD, 0);
				mv.visitMethodInsn(INVOKESPECIAL, superName, "hashCode", "()I", false);
				mv.visitInsn(IRETURN);
				mv.visitMaxs(1, 1);
				mv.visitEnd();
			}

			if(ms.missingToString){
				MethodVisitor mv = super.visitMethod(ACC_PUBLIC + ACC_SYNTHETIC, "toString", "()Ljava/lang/String;", null, null);
				mv.visitCode();
				Label l0 = new Label();
				mv.visitLabel(l0);
				mv.visitVarInsn(ALOAD, 0);
				mv.visitMethodInsn(INVOKESPECIAL, superName, "toString", "()Ljava/lang/String;", false);
				mv.visitInsn(ARETURN);
				mv.visitMaxs(1, 1);
				mv.visitEnd();
			}
			
		}
	}
	
	
	
	public byte[] transform() {
		ClassReader cr = new ClassReader(code);
		
		OMAFinder finder = new OMAFinder();
		cr.accept(finder, 0);
		
		ClassWriter cw = new ConcClassWriter(ClassWriter.COMPUTE_MAXS|ClassWriter.COMPUTE_FRAMES, clloader.getDetector());// TODO:  turn off compute frames? - see what happens
		MissingMethodAdder mma = new MissingMethodAdder(cw);
		cr.accept(mma, 0);
		
		//TODO: DO NOT add method to interfaces
		
		return cw.toByteArray();
	}

}
