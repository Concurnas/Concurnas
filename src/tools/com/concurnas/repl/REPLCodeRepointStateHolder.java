package com.concurnas.repl;

import java.util.HashMap;
import java.util.Map;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.RemappingClassAdapter;
import org.objectweb.asm.commons.SimpleRemapper;

import com.concurnas.runtime.ConcurnasClassLoader;
import com.concurnas.runtime.Pair;
import com.concurnas.runtime.cps.analysis.ConcClassWriter;

public class REPLCodeRepointStateHolder extends ClassVisitor implements Opcodes {

	private class MethodToREPLStateRepointer extends MethodVisitor{

		public MethodToREPLStateRepointer(MethodVisitor mv) {
			super(ASM7, mv);
		}
		
		private void genericSswap(String desc) {
			if(desc.equals("J") || desc.equals("D")){
				mv.visitInsn(Opcodes.DUP_X2);
				mv.visitInsn(Opcodes.POP);
			}
			else{//1 slot each
				mv.visitInsn(Opcodes.SWAP);
			}
		}
		
		public void unboxToRequiredType(String desc){
			if( desc.startsWith("L") ){//object or array - do nothing
				mv.visitTypeInsn(CHECKCAST, desc.substring(1, desc.length()-1));
				return;
			}
			else if( desc.startsWith("[")){//object or array - do nothing
				mv.visitTypeInsn(CHECKCAST, desc);
				return;
			}
			else{
				switch(desc){
					case "Z": mv.visitTypeInsn(CHECKCAST, "java/lang/Boolean"); mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Boolean", "booleanValue", "()Z", false); break;
					case "B": mv.visitTypeInsn(CHECKCAST, "java/lang/Byte"); mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false); break;
					case "C": mv.visitTypeInsn(CHECKCAST, "java/lang/Character"); mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false); break;
					case "S": mv.visitTypeInsn(CHECKCAST, "java/lang/Short"); mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false); break;
					case "I": mv.visitTypeInsn(CHECKCAST, "java/lang/Integer"); mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false); break;
					case "J": mv.visitTypeInsn(CHECKCAST, "java/lang/Long"); mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Long", "longValue", "()J", false); break;
					case "F": mv.visitTypeInsn(CHECKCAST, "java/lang/Float"); mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false); break;
					case "D": mv.visitTypeInsn(CHECKCAST, "java/lang/Double"); mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false); break;
				}
			}
		}
		public void boxIfPrimative(String desc) {
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
		
		public void visitFieldInsn(int opcode, String owner, String name, String desc) {
			//owner=mapTopLevelClassName(owner);
			if(owner.equals(classNameToRedirect)) {
				if(opcode == PUTSTATIC) {
					mv.visitLdcInsn(name);
					genericSswap(desc);
					boxIfPrimative(desc);
					mv.visitMethodInsn(INVOKESTATIC, "com/concurnas/repl/REPLRuntimeState", "put", "(Ljava/lang/String;Ljava/lang/Object;)V", false);
					return;
				}else if(opcode == GETSTATIC) {
					mv.visitLdcInsn(name);
					mv.visitMethodInsn(INVOKESTATIC, "com/concurnas/repl/REPLRuntimeState", "get", "(Ljava/lang/String;)Ljava/lang/Object;", false);
					unboxToRequiredType(desc);
					return;
				}
			}
			
			mv.visitFieldInsn(opcode, owner, name, desc);
		}
		
		/*public void visitTypeInsn(final int opcode, String type) {
			type=mapTopLevelClassName(type);
			super.visitTypeInsn(opcode, type);
		  }*/
		
		public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
			if(owner.equals(classNameToRedirect)) {
				owner = newClassName;
			}/*else {
				owner=mapTopLevelClassName(owner);
			}*/
			mv.visitMethodInsn(opcode, owner, name, desc, itf);
		}
		
		  public void visitLdcInsn(Object value) {
			  if(value instanceof Type) {
				  String cn = ((Type)value).getClassName();
				  if(classNameToRedirect.equals(cn)) {
					  value = Type.getObjectType(newClassName);//Type.get.getType(newClassName);
				  }
			  }
		      mv.visitLdcInsn(value);
		  }
		
	}
	
	private String classNameToRedirect;
	private String newClassName;
	private boolean mapProvidedClassName;
	//private HashMap<String, String> classNameToMappedName;
	
	public REPLCodeRepointStateHolder(ClassVisitor cv, String classNameToRedirect, String newClassName, boolean mapProvidedClassName) {
		super(ASM7, cv);
		this.classNameToRedirect = classNameToRedirect;
		this.newClassName = newClassName;
		this.mapProvidedClassName = mapProvidedClassName;
		//this.classNameToMappedName = classNameToMappedName;
	}


	public void visitEnd() {
		MethodVisitor mv = this.visitMethod(ACC_PUBLIC + ACC_STATIC, "triggerClinit$", "()V", null, null);
		mv.visitCode();
		Label l0 = new Label();
		mv.visitLabel(l0);
		mv.visitInsn(RETURN);
		mv.visitMaxs(0, 0);
		mv.visitEnd();
		
		super.visitEnd();
	}
	
	public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
		return new MethodToREPLStateRepointer(cv.visitMethod(access, name, desc, signature, exceptions));
	}
	
	/*
	 * private String mapTopLevelClassName(String clsName) { return
	 * classNameToMappedName.containsKey(clsName)?classNameToMappedName.get(clsName)
	 * :clsName; }
	 */
	
	public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
		if(mapProvidedClassName) {
			name = newClassName;
		}/*else if(classNameToMappedName.containsKey(name)) {
			name = classNameToMappedName.get(name);
		}
		
		for(int n = 0; n < interfaces.length; n++) {
			interfaces[n] = mapTopLevelClassName(interfaces[n]);
		}
		
		superName=mapTopLevelClassName(superName);*/
		
		super.visit(version, access, name, signature, superName, interfaces);
	}

	public void visitSource(String source, String debug) {
		super.visitSource(newClassName, debug);
	}
	
	
	static byte[] repointToREPLStateHolder(byte[] code, String codeName, String newcodeName, boolean mapProvidedClassName, Map<String, String> classNameToMappedName) {
		ClassWriter cw = new ConcClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS, new ConcurnasClassLoader().getDetector());
		{
			ClassReader cr = new ClassReader(code);
			REPLCodeRepointStateHolder staticRedirector = new REPLCodeRepointStateHolder(cw, codeName, newcodeName, mapProvidedClassName);
			cr.accept(staticRedirector, 0);
		}
		
		byte[] ret = cw.toByteArray();
		
		cw = new ConcClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS, new ConcurnasClassLoader().getDetector());
		{
			ClassReader cr = new ClassReader(ret);
			RemappingClassAdapter remappingClassAdapter = new RemappingClassAdapter(cw, new SimpleRemapper(classNameToMappedName));
			cr.accept(remappingClassAdapter, ClassReader.EXPAND_FRAMES);
		}
		
		return cw.toByteArray();
	}
	
}
