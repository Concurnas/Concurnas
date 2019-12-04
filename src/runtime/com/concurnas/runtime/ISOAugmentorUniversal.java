package com.concurnas.runtime;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import com.concurnas.runtime.cps.analysis.ConcClassWriter;
import com.concurnas.runtime.cps.analysis.Fiberizer;

/**
 * //acts on any _runExecute method of an ISO
 * Maps from:
 *     private boolean _runExecute(){
		this.fiber.down();
		
    	this.func.apply();//add in fiber at runtime
    	
		return this.fiber.end();
    }
    
    to:
    	
        private boolean _runExecute(){
		this.fiber.down();
		
    	this.func.apply(this.fiber);//add in fiber at runtime
    	
		return this.fiber.end();
    }
    
    at runtime.
 */
public class ISOAugmentorUniversal {
	
	private static class TheAugmentor extends ClassVisitor implements Opcodes {
		
		public TheAugmentor(ClassVisitor cv) {
			super(ASM7, cv);
		}
		
		private static class MethodProcessor extends MethodVisitor{

			public MethodProcessor(MethodVisitor mv) {
				super(ASM7, mv);
			}
			
			public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
				if(!owner.equals("com/concurnas/bootstrap/runtime/transactions/LocalTransaction") && !owner.equals("com/concurnas/bootstrap/runtime/cps/Fiber") && !owner.equals("java/util/ArrayList") && !owner.equals("java/util/LinkedHashSet") ){//JPT: bug here is that if you call something which is not fiberized, this will not work
					//TODO: fix if clause above, looks like a bit of a hack
        			mv.visitVarInsn(ALOAD, 0);
        			mv.visitFieldInsn(GETFIELD, "com/concurnas/bootstrap/runtime/cps/Iso", "fiber", "Lcom/concurnas/bootstrap/runtime/cps/Fiber;");
        			desc =	desc.replace(")", Fiberizer.FIBER_SUFFIX);
        			//
        			if((owner.equals("com/concurnas/bootstrap/runtime/cps/IsoTask") || owner.equals("com/concurnas/bootstrap/runtime/cps/AbstractIsoTask")) && name.equals("apply")){
        				//MHA: I dont know why but if we try to go direct via the abstract method, it complains - so we're doing something wrong with abstract methods?
        				owner = "com/concurnas/bootstrap/lang/Lambda$Function0";
        				desc = "(Lcom/concurnas/bootstrap/runtime/cps/Fiber;)Ljava/lang/Object;";
        			}
				}
				super.visitMethodInsn(opcode, owner, name, desc, itf);
			}
			
		}
		
		 public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
			 MethodVisitor vm = super.visitMethod(access, name, desc, signature, exceptions);
			 return name.equals("_runExecute")?new MethodProcessor(vm):vm; 
		 }
	}
	
	
	public static byte[] augment(byte[] fsCode, ConcClassUtil clloader) {
		
		ClassReader cr = new ClassReader(fsCode);
		ClassWriter cw = new ConcClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS, clloader.getDetector());//TODO: remove the computes
		
		TheAugmentor aug = new TheAugmentor(cw);
		
		cr.accept(aug, 0);
		
		return cw.toByteArray();
	}

}
