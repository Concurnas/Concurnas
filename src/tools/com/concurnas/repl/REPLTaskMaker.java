package com.concurnas.repl;

import java.util.Iterator;
import java.util.Set;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import com.concurnas.conc.TaskMaker;

public class REPLTaskMaker extends TaskMaker implements Opcodes{


	public REPLTaskMaker(String invokerclassName, String classBeingTested, Set<String> newvars){
		super(invokerclassName, classBeingTested, newvars, false);
	}
		
	protected void makeApply(ClassWriter cw) {
		MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "apply", "()Ljava/lang/Void;", null, null);
		mv.visitCode();
		
		Label start = new Label();
		Label end = new Label();
		Label handle = new Label();
		mv.visitTryCatchBlock(start, end, handle, "java/lang/Throwable");
		
		mv.visitLabel(start);
		
		mv.visitMethodInsn(INVOKESTATIC, classBeingTested, "triggerClinit$", "()V", false);
		
		if(newvars != null) {
			mv.visitLabel(new Label());
			mv.visitVarInsn(ALOAD, 0);
			
			mv.visitTypeInsn(NEW, "java/lang/StringBuilder");
			mv.visitInsn(DUP);
			mv.visitMethodInsn(INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "()V", false);
			
			//int n=0;
			//int sz = newvars.size();
			Iterator<String> itr = newvars.stream().sorted().iterator();
			while(itr.hasNext()) {
				String var = itr.next();

				mv.visitLabel(new Label());
				mv.visitFieldInsn(GETSTATIC, "com/concurnas/repl/REPLRuntimeState", "vars", "Ljava/util/Map;");
				mv.visitLdcInsn(var);
				mv.visitMethodInsn(INVOKEINTERFACE, "java/util/Map", "containsKey", "(Ljava/lang/Object;)Z", true);
				Label onmissing = new Label();
				mv.visitJumpInsn(IFEQ, onmissing);
				mv.visitTypeInsn(NEW, "java/lang/StringBuilder");
				mv.visitInsn(DUP);
				mv.visitLdcInsn(var + " ==> ");
				mv.visitMethodInsn(INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "(Ljava/lang/String;)V", false);
				mv.visitFieldInsn(GETSTATIC, "com/concurnas/repl/REPLRuntimeState", "vars", "Ljava/util/Map;");
				mv.visitLdcInsn(var);
				mv.visitMethodInsn(INVOKEINTERFACE, "java/util/Map", "get", "(Ljava/lang/Object;)Ljava/lang/Object;", true);
				mv.visitMethodInsn(INVOKESTATIC, "com/concurnas/bootstrap/lang/Stringifier", "stringify", "(Ljava/lang/Object;)Ljava/lang/String;", false);
				mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
				//if(n++ < sz) {
					mv.visitLdcInsn("\n");
					mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
				//}
				mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false);
				Label after = new Label();
				mv.visitJumpInsn(GOTO, after);
				mv.visitLabel(onmissing);
				
				String errMsg = "|  ERROR variable "+var+" does not exist";
				//if(n++ < sz) {
					errMsg += "\n";
				//}
				mv.visitLdcInsn(errMsg);
				mv.visitLabel(after);
				
				mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
			}
			
			mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false);
			
			mv.visitMethodInsn(INVOKESPECIAL, invokerclassName, "setResult", "(Ljava/lang/String;)V", false);
		}else {
			mv.visitLabel(new Label());
			mv.visitVarInsn(ALOAD, 0);
			mv.visitLdcInsn("");
			mv.visitMethodInsn(INVOKESPECIAL, invokerclassName, "setResult", "(Ljava/lang/String;)V", false);
		}
		mv.visitLabel(end);
		Label toRet = new Label();
		mv.visitJumpInsn(GOTO, toRet);

		//
		mv.visitLabel(handle);
		
		mv.visitFrame(Opcodes.F_SAME1, 0, null, 1, new Object[] {"java/lang/Throwable"});
		mv.visitVarInsn(ASTORE, 1);
		
		//mv.visitVarInsn(ALOAD, 1);
		//mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Throwable", "printStackTrace", "()V", false);
		
		mv.visitVarInsn(ALOAD, 0);
		mv.visitLdcInsn(classBeingTested);
		mv.visitLdcInsn(invokerclassName);
		mv.visitVarInsn(ALOAD, 1);
		
		
		mv.visitMethodInsn(INVOKESTATIC, "com/concurnas/repl/REPLExceptionFormatter", "formatException", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/Throwable;)Ljava/lang/String;", false);
		mv.visitMethodInsn(INVOKESPECIAL, invokerclassName, "setResult", "(Ljava/lang/String;)V", false);
		//
		
		
		mv.visitLabel(toRet);
		mv.visitInsn(ACONST_NULL);
		mv.visitInsn(ARETURN);
		mv.visitEnd();
		
	}
	
	private void sdfsdf() {
		String xxx = REPLRuntimeState.vars.containsKey("myVar")?"myVar ==> " + REPLRuntimeState.vars.get("myVar"):"|  ERROR variable myVar does not exist";
	}
}
