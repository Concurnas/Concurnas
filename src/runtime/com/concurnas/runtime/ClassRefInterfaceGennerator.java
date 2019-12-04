package com.concurnas.runtime;

import java.util.LinkedHashSet;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import com.concurnas.compiler.ast.FuncType;
import com.concurnas.runtime.cps.mirrors.ClassMirror;
import com.concurnas.runtime.cps.mirrors.ConstructorMirror;
import com.concurnas.runtime.cps.mirrors.MethodMirror;

public class ClassRefInterfaceGennerator implements Opcodes {

	private static byte[] create(String name, LinkedHashSet<String> constructors, String superclass) throws Exception {

		ClassWriter cw = new ClassWriter(0);
		MethodVisitor mv;

		cw.visit(V1_8, ACC_PUBLIC + ACC_ABSTRACT + ACC_INTERFACE, name, null, "java/lang/Object", superclass==null?null:new String[]{FuncType.classRefIfacePrefix  +superclass +  FuncType.classRefIfacePostfix });
		for(String desc: constructors){
			
			desc = desc.substring(0, desc.lastIndexOf("V")) + "Ljava/lang/Object;";
			
			mv = cw.visitMethod(ACC_PUBLIC + ACC_ABSTRACT, "apply", desc, null, null);
			mv.visitEnd();
		}
		cw.visitEnd();

		return cw.toByteArray();
	}

	public static byte[] classRefInterfaceGennerator(String source, ConcurnasClassLoader loader, String ifaceName){
		LinkedHashSet<String> publicConstructors = new LinkedHashSet<String>();
		
		try {
			if(source.endsWith("$$ActorIterface")){
				source = source.substring(0, source.length() - 15);
			}
			ClassMirror cm = loader.getDetector().classForName(source);
			if(null == cm){
				throw new Exception("Unable to find definition for: " + source);
			}
			String superclass = cm.getSuperclass();
			if(null != superclass){
				superclass=superclass.replace(".", "/");
			}
			
			boolean isNestedOrActpr = 0 < cm.isNestedClass() || loader.getDetector().isActor(source);
			
			for(ConstructorMirror mm : cm.getDeclaredConstructorsWithoutHiddenArgs()){
				
				if(mm.isPublic() ){
					String md = mm.getMethodDescriptor();
					if(isNestedOrActpr){//(LbytecodeSandbox$Outer;Ljava/lang/String;)V
						//cut off first argument (outer class)
						md = "("+md.substring(md.indexOf(";")+1);
					}
					
					publicConstructors.add(md);
				}
			}
				
			
			//byte[] codex =  create(ifaceName, methods);
			//BytecodePrettyPrinter.print(codex, true);
			
			return create(ifaceName, publicConstructors, superclass);
		} catch (Exception e) {
			throw new RuntimeException("Failure during genneration of actor interface for: " + source, e);
		}
	}
}
