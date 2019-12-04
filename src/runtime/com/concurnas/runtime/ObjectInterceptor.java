package com.concurnas.runtime;

import java.lang.reflect.Modifier;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import com.concurnas.runtime.cps.analysis.ConcClassWriter;


/**
 * Replace all java/lang/Object instaces with com/concurnas/bootstrap/runtime/cps/CObject 
 */
public class ObjectInterceptor implements Opcodes {
	private final byte[] code;
	private final  ConcClassUtil clloader;
	private boolean inRuntimeCacheMode;
	//private final Detector detector;
	public ObjectInterceptor(final byte[] code, final ConcClassUtil clloader, boolean inRuntimeCacheMode) {
		this.code=code;
		this.clloader = clloader;
		this.inRuntimeCacheMode = inRuntimeCacheMode;
	}
	
	
	private class RefcMethodVisitor extends MethodVisitor{

		public RefcMethodVisitor(MethodVisitor mv) {
			super(ASM7, mv);
		}
		
		
		public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
			
			if(owner.equals("java/lang/Object") && name.equals("<init>") && desc.equals("()V") && opcode == INVOKESPECIAL && itf==false){
				owner = "com/concurnas/bootstrap/runtime/cps/CObject";
			}
			//owner = convertOwner(owner);
			super.visitMethodInsn(opcode, owner, name, desc, itf);
		}
		
		public void visitTypeInsn(int opcode, String type) {
			//new Object -> new CObject
			if(!inRuntimeCacheMode && opcode == Opcodes.NEW && type.equals("java/lang/Object")){
				type = "com/concurnas/bootstrap/runtime/cps/CObject";
			}
			super.visitTypeInsn(opcode, type);
		}
	}
	
	private class ReflecClassVisitor extends ClassVisitor{

		public ReflecClassVisitor(ClassVisitor cw){
			super(ASM7, cw);
		}
		private boolean isInterface;
		public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
			isInterface = Modifier.isInterface(access);
			
			if(!isInterface && superName.equals("java/lang/Object")){
				superName = "com/concurnas/bootstrap/runtime/cps/CObject";
			}
			super.visit(version, access, name, signature, superName, interfaces);
		}
		
		public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
			MethodVisitor ret = super.visitMethod(access, name, desc, signature, exceptions);
			if(!isInterface){
				ret = new RefcMethodVisitor(ret);
			}
			return ret;
		}
	}
	
	
	public byte[] transform() {
		ClassReader cr = new ClassReader(code);
		
		ClassWriter cw = new ConcClassWriter(ClassWriter.COMPUTE_MAXS|ClassWriter.COMPUTE_FRAMES, clloader.getDetector());//NOT doing frames etc here, ok i guess as nothing changes
		ReflecClassVisitor mma = new ReflecClassVisitor(cw);
		cr.accept(mma, 0);
		
		return cw.toByteArray();
	}

}
