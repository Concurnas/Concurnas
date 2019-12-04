package com.concurnas.runtime;

import java.lang.reflect.Modifier;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import com.concurnas.runtime.cps.analysis.ANFTransform;
import com.concurnas.runtime.cps.analysis.ConcClassWriter;
import com.concurnas.runtime.cps.analysis.Fiberizer;

/**
 * If a native method invokes an interface type on a java object which has not been implemented (as the fiberized version has in its place), we need the 'normal'
 * version to map to the fiberized version with a fake fiber. We do this here by converting the interface method into a defualt method
 * @author jason
 *
 */
public class AddDefaultMethodsToInterfaceForFiber extends ClassVisitor implements Opcodes{

	public AddDefaultMethodsToInterfaceForFiber(ClassVisitor classVisitor) {
		super(ASM7, classVisitor);
	}
	
	private String name;
	public void visit(final int version, final int access, final String name, final String signature, final String superName, final String[] interfaces) {
		this.name = name;
		cv.visit(version<V1_8?V1_8:version, access, name, signature, superName, interfaces);//upgrade to min java spec supporting default methods 
	}

	public MethodVisitor visitMethod(final int access, final String name, final String descriptor, final String signature, final String[] exceptions) {

		if (!Modifier.isStatic(access) &&  ((access & ACC_ABSTRACT ) != 0 && !descriptor.contains(Fiberizer.FIBER_SUFFIX)) ) {
			// replace with default method pointing to fiberized version
			{
				MethodVisitor methodVisitor = cv.visitMethod(access & ~ACC_ABSTRACT, name, descriptor, signature, null);
				//annotations indicating that this method is normally obligatory to implement! (i.e. normally not default)
				//@DefaultMethodRequiresImplementation
				
				{
					AnnotationVisitor annotationVisitor0 = methodVisitor.visitAnnotation("Lcom/concurnas/bootstrap/runtime/DefaultMethodRequiresImplementation;", true);
					annotationVisitor0.visitEnd();
				}
				methodVisitor.visitCode();
				
				methodVisitor.visitLabel(new Label());
				methodVisitor.visitVarInsn(ALOAD, 0);
				
				//load others
				int locVar=1;
				for(char c : ANFTransform.getPrimOrObj(descriptor)){//reload the stack from vars
					methodVisitor.visitVarInsn(ANFTransform.getLoadOp(c), locVar++);
					if(c == 'D' || c=='J'){
						locVar++;
					}
				}
				
				methodVisitor.visitMethodInsn(INVOKESTATIC, "com/concurnas/bootstrap/runtime/cps/Fiber", "getCurrentFiberWithCreate", "()Lcom/concurnas/bootstrap/runtime/cps/Fiber;", false);
				methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "com/concurnas/bootstrap/runtime/cps/Fiber", "begin", "()Lcom/concurnas/bootstrap/runtime/cps/Fiber;", false);
				
				methodVisitor.visitMethodInsn(INVOKEINTERFACE, this.name, name, descriptor.replace(")", Fiberizer.FIBER_SUFFIX), true);

				String dd = Type.getMethodType(descriptor).getReturnType().getDescriptor();
				methodVisitor.visitInsn(dd.startsWith("[")?ARETURN:ANFTransform.getReturnOp(descriptor.charAt(descriptor.length()-1)));//return
				
				methodVisitor.visitMaxs(2, 1);
				methodVisitor.visitEnd();
				return null;
			}
		}

		return cv.visitMethod(access, name, descriptor, signature, exceptions);
	}
	
	private static class IsInterfaceChecker extends ClassVisitor{
		public boolean isInterface;
		public IsInterfaceChecker() {
			super(ASM7);
		}
		public void visit(final int version, final int access, final String name, final String signature, final String superName, final String[] interfaces) {
			isInterface = Modifier.isInterface(access) && Modifier.isAbstract(access) && ((access & ACC_ANNOTATION)==0);
		}
	}

	public static byte[] transform(byte[] code, ConcClassUtil loader) {
		
		IsInterfaceChecker checkler = new IsInterfaceChecker();
		new ClassReader(code).accept(checkler, 0);
		if(checkler.isInterface) {
			ClassWriter cw = new ConcClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS, loader.getDetector());
			new ClassReader(code).accept(new AddDefaultMethodsToInterfaceForFiber(cw), 0);

			return cw.toByteArray();
		}else {
			return code;
		}
	}

}
