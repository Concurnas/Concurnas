package com.concurnas.runtime;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import com.concurnas.runtime.cps.analysis.ConcClassWriter;

/**
 * converts all calls to the following reflection methods as follows: 
 * mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Class", "getDeclaredMethods", "()[Ljava/lang/reflect/Method;", false);
 * 	=>
 * mv.visitMethodInsn(INVOKESTATIC, "com/concurnas/bootstrap/runtime/cps/ReflectionHelper", "getDeclaredMethods", "(Ljava/lang/Class;)[Ljava/lang/reflect/Method;", false);
 *
 */
public class ReflectionHelperRedirector implements Opcodes {
	private final byte[] code;
	private final  ConcClassUtil clloader;
	//private final Detector detector;
	public ReflectionHelperRedirector(final byte[] code, final ConcClassUtil clloader) {
		this.code=code;
		this.clloader = clloader;
		//this.detector=new Detector(new CachedClassMirrors(clloader));
	}

	private final static String reflectionHelperClass = "com/concurnas/bootstrap/runtime/cps/ReflectionHelper";
	
	private class RefcMethodVisitor extends MethodVisitor{

		public RefcMethodVisitor(MethodVisitor mv) {
			super(ASM7, mv);
		}
		
		public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
			if(opcode == INVOKEVIRTUAL && itf==false){
				if(owner.equals("java/lang/Class")){
					if(name.equals("getDeclaredMethods") && desc.equals("()[Ljava/lang/reflect/Method;")){
						opcode = INVOKESTATIC;
						owner = reflectionHelperClass;
						desc = "(Ljava/lang/Class;)[Ljava/lang/reflect/Method;";
					}
					else if(name.equals("getConstructors") && desc.equals("()[Ljava/lang/reflect/Constructor;")){
						opcode = INVOKESTATIC;
						owner = reflectionHelperClass;
						desc = "(Ljava/lang/Class;)[Ljava/lang/reflect/Constructor;";
					}
					
					/*else if(name.equals("getConstructor") && desc.equals("([Ljava/lang/Class;)Ljava/lang/reflect/Constructor;")){
						opcode = INVOKESTATIC;
						owner = reflectionHelperClass;
						desc = "(Ljava/lang/Class;[Ljava/lang/Class;)Ljava/lang/reflect/Constructor;";
					}*/
					
					else if(name.equals("getMethods") && desc.equals("()[Ljava/lang/reflect/Method;")){
						opcode = INVOKESTATIC;
						owner = reflectionHelperClass;
						desc = "(Ljava/lang/Class;)[Ljava/lang/reflect/Method;";
					}
					else if(name.equals("getMethod") && desc.equals("(Ljava/lang/String;[Ljava/lang/Class;)Ljava/lang/reflect/Method;")){
						opcode = INVOKESTATIC;
						owner = reflectionHelperClass;
						desc = "(Ljava/lang/Class;Ljava/lang/String;[Ljava/lang/Class;)Ljava/lang/reflect/Method;";
					}
					else if(name.equals("getDeclaredMethod") && desc.equals("(Ljava/lang/String;[Ljava/lang/Class;)Ljava/lang/reflect/Method;")){
						opcode = INVOKESTATIC;
						owner = reflectionHelperClass;
						desc = "(Ljava/lang/Class;Ljava/lang/String;[Ljava/lang/Class;)Ljava/lang/reflect/Method;";
					}
				}else if(owner.equals("java/lang/reflect/Method")){
					//mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/reflect/Method", "getParameterTypes", "()[Ljava/lang/Class;", false);
					if(name.equals("getParameterTypes") && desc.equals("()[Ljava/lang/Class;")){
						opcode = INVOKESTATIC;
						owner = reflectionHelperClass;
						desc = "(Ljava/lang/reflect/Method;)[Ljava/lang/Class;";
					}
					else if(name.equals("getParameterCount") && desc.equals("()I")){
						opcode = INVOKESTATIC;
						owner = reflectionHelperClass;
						desc = "(Ljava/lang/reflect/Method;)I";
					}
					else if(name.equals("getParameters") && desc.equals("()[Ljava/lang/reflect/Parameter;")){
						opcode = INVOKESTATIC;
						owner = reflectionHelperClass;
						desc = "(Ljava/lang/reflect/Method;)[Ljava/lang/reflect/Parameter;";
					}
					else if(name.equals("invoke") && desc.equals("(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;")){
						opcode = INVOKESTATIC;
						owner = reflectionHelperClass;
						desc = "(Ljava/lang/reflect/Method;Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;";
					}
				}
				
				
			}
			
			
			
			/*else if(owner.equals("java/lang/reflect/Method") && name.equals("getParameterTypes") && desc.equals("()[Ljava/lang/Class;") && opcode == INVOKEVIRTUAL && itf==false){
				opcode = INVOKESTATIC;
				owner = reflectionHelperClass;
				desc = "(Ljava/lang/reflect/Method;)[Ljava/lang/Class;";
			}*/
			//TODO: find a better way to do this and extend this to the rest of the reflection calls
			super.visitMethodInsn(opcode, owner, name, desc, itf);
		}
		
	}
	
	private class ReflecClassVisitor extends ClassVisitor{

		public ReflecClassVisitor(ClassVisitor cw){
			super(ASM7, cw);
		}
		
		private boolean skipApplication;
		
		public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
			skipApplication = name.equals(reflectionHelperClass);
			super.visit(version, access, name, signature, superName, interfaces);
		}
		
		public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
			MethodVisitor ret = super.visitMethod(access, name, desc, signature, exceptions);
			if(!skipApplication){
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
