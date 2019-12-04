package com.concurnas.compiler.visitors.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import com.concurnas.compiler.SchedulerRunner;
import com.concurnas.compiler.visitors.ScopeAndTypeChecker;
import com.concurnas.compiler.visitors.Utils;
import com.concurnas.lang.LangExt.Context;
import com.concurnas.lang.LangExt.ErrorOrWarning;
import com.concurnas.lang.LangExt.IterationResult;
import com.concurnas.lang.LangExt.Result;
import com.concurnas.lang.LangExt.SourceLocation;
import com.concurnas.runtime.ConcurnasClassLoader;

public class LanguageExtensionRunner implements Opcodes {

	private final int line;
	private final int col;
	private final String langExtName;
	private final SourceLocation srcLoc;
	private final long ide;
	private final Class<?> langExtCls;
	private final ConcurnasClassLoader localisedClasslaoder;
	private final SchedulerRunner scheduler;
	
	public LanguageExtensionRunner(int line, int col, String langExtName, SourceLocation srcLoc, ConcurnasClassLoader mainClassLoader, long ide, Class<?> langExtCls, SchedulerRunner scheduler) {// throws ClassNotFoundException, InstantiationException, IllegalAccessException {
		this.line = line;
		this.col = col;
		this.langExtName = langExtName;
		this.srcLoc = srcLoc;
		this.localisedClasslaoder = mainClassLoader;//new LangExtLoaderCL(mainClassLoader);//mainClassLoader
		this.ide = ide;
		this.langExtCls = langExtCls;
		this.scheduler = scheduler;
	}
	
	
	private Object langExtIstance;
	private Constructor<?> initTaskConstructor;
	private Method initTaskGetResultMethod;
	
	private Constructor<?> iteratorTaskConstructor;
	private Method iteratorTaskClassResultMethod;
	
	public boolean init(ScopeAndTypeChecker satc) {
		String fullclassname = langExtCls.getName();
		String clsName = fullclassname.replaceAll("\\.", "/");
		
		{
			try {
				String invokerCls = "ExtLangLoader$$Init" + ide;
				byte[] initializerCode = makeInitOrIterExecutorTaskIso(invokerCls, clsName, true);
				Class<?> initTaskClass = localisedClasslaoder.defineClass(invokerCls, initializerCode);
				initTaskConstructor = getConstructor(initTaskClass, "public "+invokerCls+"("+fullclassname+",int,int,com.concurnas.lang.LangExt$SourceLocation,java.lang.String)", 5);
				
				initTaskGetResultMethod = Utils.getMethod(initTaskClass, "public synchronized com.concurnas.lang.LangExt$Result "+invokerCls+".getResult() throws java.lang.InterruptedException", 0);
			}catch(Throwable e) {
				satc.raiseError(line, col, String.format("Unable to create initalizer for: %s as: %s", langExtName, Utils.stackTrackToString(e)));
				return false;
			}
			
		}
		
		{
			try {
				String iterateCls = "ExtLangLoader$$Iter" + ide;
				byte[] iterateCode = makeInitOrIterExecutorTaskIso(iterateCls, clsName, false);
				Class<?> iteratorTaskClass = localisedClasslaoder.defineClass(iterateCls, iterateCode);

				iteratorTaskConstructor = getConstructor(iteratorTaskClass, "public "+iterateCls+"("+fullclassname+",com.concurnas.lang.LangExt$Context)", 2);
				
				iteratorTaskClassResultMethod = Utils.getMethod(iteratorTaskClass, "public synchronized com.concurnas.lang.LangExt$IterationResult "+iterateCls+".getResult() throws java.lang.InterruptedException", 0);
			}catch(Throwable e) {
				satc.raiseError(line, col, String.format("Unable to create iterator for: %s as: %s", langExtName, Utils.stackTrackToString(e)));
				return false;
			}
		}
	
		
		return true;
		
	}
	
	public boolean initalizeLangExt(ScopeAndTypeChecker satc, String source) {
		try {
			langExtIstance = langExtCls.newInstance();
		}catch( Throwable e) {
			satc.raiseError(line, col, String.format("Unable to create instance of: %s as: %s", langExtName, e));
			return false;
		}
		
		try {
			//TODO: find constructor above
			Object langExtInitClsTask = initTaskConstructor.newInstance(langExtIstance, this.line, this.col, srcLoc, source);
			scheduler.invokeScheudlerTask(langExtInitClsTask);//pass in other args, int int sl, source
			
			boolean ret = true;
			try {
				Result result = (Result)initTaskGetResultMethod.invoke(langExtInitClsTask);
				ret = !reportErrorsWarnings(satc, result);
				
			}catch(InvocationTargetException getEx) {
				//getEx.printStackTrace();
				satc.raiseError(line, col, String.format("Error when executing initializer for: %s as: %s", langExtName, Utils.stackTrackToString(getEx.getCause()) ));
				ret= false;
			}
			
			return ret;
		}catch(Throwable e) {
			Throwable cause = e.getCause();
			if(null != cause) {
				e = cause;
			}
			satc.raiseError(line, col, String.format("Unable to run initializer for: %s as: %s", langExtName, Utils.stackTrackToString(e)));
			return false;
		}
	}
	
	private boolean reportErrorsWarnings(ScopeAndTypeChecker satc, Result result) {
		boolean reterrors = false;
		List<ErrorOrWarning> errors = result.getErrors();
		if(!errors.isEmpty()) {
			for(ErrorOrWarning eow : errors) {
				satc.raiseError(eow.getLine(), eow.getCol(), langExtName + ": " + eow.getText());
				reterrors=true;
			}
		}
		
		List<ErrorOrWarning> warnings = result.getWarnings();
		if(!warnings.isEmpty()) {
			for(ErrorOrWarning eow : warnings) {
				satc.raiseWarning(eow.getLine(), eow.getCol(), langExtName + ": " + eow.getText(), null);
			}
		}
		
		return reterrors;
	}
	
	public String iterateLangExt(ScopeAndTypeChecker satc, Context ctx) {
		
		try {
			Object iteratorTask = iteratorTaskConstructor.newInstance(langExtIstance, ctx);
			scheduler.invokeScheudlerTask(iteratorTask);//pass in context
			
			try {
				IterationResult result = (IterationResult)(iteratorTaskClassResultMethod.invoke(iteratorTask));
				reportErrorsWarnings(satc, result);

				String code = result.getOutputCode();
				
				if(null == code || code.trim().isEmpty()) {
					satc.raiseError(line, col, String.format("%s has produced invalid Concurnas code: empty String", langExtName));
					code = null;
				}
				
				
				return code;
			}catch(InvocationTargetException getEx) {
				satc.raiseError(line, col, String.format("Error when executing iterator for: %s as: %s", langExtName, Utils.stackTrackToString(getEx.getCause()) ));
			}
			
		}catch(Throwable e) {
			Throwable cause = e.getCause();
			if(null != cause) {
				e = cause;
			}
			satc.raiseError(line, col, String.format("Unable to run iterator for: %s as: %s", langExtName, Utils.stackTrackToString(e)));
		}
		return null;
	}
	
	

	
	
	
	private static Constructor<?> getConstructor(Class<?> cls, String name, int args){
		Constructor<?>[] cons = cls.getConstructors();
		for(Constructor<?> m : cons){
			if(m.getParameterTypes().length == args && m.toString().equals(name)) {
				return m;
			}
		}
		throw new RuntimeException("Cannot find constructor");
	}
	
	
	private byte[] makeInitOrIterExecutorTaskIso(String invokerclassName, String classBeingTested, boolean isInit) {
		// invokerclassName = "com/concurnas/compiler/bytecode/IsoTester"
		// classBeingTested = "com/concurnas/compiler/bytecode/MyTest"
		
		String classBeingTestedLComma = "L" + classBeingTested + ";";
		
		ClassWriter cw = new ClassWriter(0);
		FieldVisitor fv;
		MethodVisitor mv;

		cw.visit(52, ACC_PUBLIC + ACC_SUPER, invokerclassName, "Lcom/concurnas/bootstrap/runtime/cps/IsoTask;", "com/concurnas/bootstrap/runtime/cps/IsoTask", null);

		cw.visitSource(invokerclassName + ".java", null);
		cw.visitInnerClass("com/concurnas/bootstrap/lang/Lambda$Function0", "com/concurnas/bootstrap/lang/Lambda", "Function0", ACC_PUBLIC + ACC_STATIC + ACC_ABSTRACT);
		
		String resultCls = isInit?"Lcom/concurnas/lang/LangExt$Result;":"Lcom/concurnas/lang/LangExt$IterationResult;";
		
		{
			fv = cw.visitField(ACC_PRIVATE, "result", resultCls, null, null);
			fv.visitEnd();
			
			fv = cw.visitField(ACC_PRIVATE, "exception", "Ljava/lang/Throwable;", null, null);
			fv.visitEnd();
			
			fv = cw.visitField(ACC_PRIVATE, "$isInitCompleteFlag", "Lcom/concurnas/bootstrap/runtime/ref/DefaultRef;", "Lcom/concurnas/bootstrap/runtime/ref/DefaultRef<Ljava/lang/Boolean;>;", null);
			fv.visitEnd();
			
			fv = cw.visitField(ACC_PRIVATE, "opon", classBeingTestedLComma, null, null);
			fv.visitEnd();
			
			if(isInit) {
				fv = cw.visitField(ACC_PRIVATE, "line", "I", null, null);
				fv.visitEnd();
				
				fv = cw.visitField(ACC_PRIVATE, "col", "I", null, null);
				fv.visitEnd();
				
				fv = cw.visitField(ACC_PRIVATE, "srcloc", "Lcom/concurnas/lang/LangExt$SourceLocation;", null, null);
				fv.visitEnd();
				
				fv = cw.visitField(ACC_PRIVATE, "src", "Ljava/lang/String;", null, null);
				fv.visitEnd();
			}else {//iterate
				fv = cw.visitField(ACC_PRIVATE, "context", "Lcom/concurnas/lang/LangExt$Context;", null, null);
				fv.visitEnd();
			}
			
		}
		{
			if(isInit) {
				mv = cw.visitMethod(ACC_PUBLIC, "<init>", "("+classBeingTestedLComma+"IILcom/concurnas/lang/LangExt$SourceLocation;Ljava/lang/String;)V", null, null);
			}else {//iterate
				mv = cw.visitMethod(ACC_PUBLIC, "<init>", "("+classBeingTestedLComma+"Lcom/concurnas/lang/LangExt$Context;)V", null, null);
			}
			mv.visitCode();
			Label l0 = new Label();
			mv.visitLabel(l0);
			mv.visitVarInsn(ALOAD, 0);

			mv.visitInsn(ACONST_NULL);
			
			mv.visitMethodInsn(INVOKESPECIAL, "com/concurnas/bootstrap/runtime/cps/IsoTask", "<init>", "(Ljava/lang/Class;)V", false);
			
			mv.visitVarInsn(ALOAD, 0);
			mv.visitTypeInsn(NEW, "com/concurnas/runtime/ref/Local");
			mv.visitInsn(DUP);
			mv.visitInsn(ICONST_1);
			mv.visitTypeInsn(ANEWARRAY, "java/lang/Class");
			mv.visitInsn(DUP);
			mv.visitInsn(ICONST_0);
			mv.visitLdcInsn(org.objectweb.asm.Type.getType("Ljava/lang/Boolean;"));
			mv.visitInsn(AASTORE);
			mv.visitMethodInsn(INVOKESPECIAL, "com/concurnas/runtime/ref/Local", "<init>", "([Ljava/lang/Class;)V", false);
			mv.visitFieldInsn(PUTFIELD, invokerclassName, "$isInitCompleteFlag", "Lcom/concurnas/bootstrap/runtime/ref/DefaultRef;");
		

			mv.visitVarInsn(ALOAD, 0);
			mv.visitVarInsn(ALOAD, 1);
			mv.visitFieldInsn(PUTFIELD, invokerclassName, "opon", classBeingTestedLComma);
			
			if(isInit) {
				mv.visitVarInsn(ALOAD, 0);
				mv.visitVarInsn(ILOAD, 2);
				mv.visitFieldInsn(PUTFIELD, invokerclassName, "line", "I");

				mv.visitVarInsn(ALOAD, 0);
				mv.visitVarInsn(ILOAD, 3);
				mv.visitFieldInsn(PUTFIELD, invokerclassName, "col", "I");
				
				mv.visitVarInsn(ALOAD, 0);
				mv.visitVarInsn(ALOAD, 4);
				mv.visitFieldInsn(PUTFIELD, invokerclassName, "srcloc", "Lcom/concurnas/lang/LangExt$SourceLocation;");

				mv.visitVarInsn(ALOAD, 0);
				mv.visitVarInsn(ALOAD, 5);
				mv.visitFieldInsn(PUTFIELD, invokerclassName, "src", "Ljava/lang/String;");
			}else {//iterate
				mv.visitVarInsn(ALOAD, 0);
				mv.visitVarInsn(ALOAD, 2);
				mv.visitFieldInsn(PUTFIELD, invokerclassName, "context", "Lcom/concurnas/lang/LangExt$Context;");
			}
			
			mv.visitInsn(RETURN);
			mv.visitEnd();
		}
		{
			mv = cw.visitMethod(ACC_PUBLIC, "apply", "()Ljava/lang/Void;", null, null);
			mv.visitCode();
			Label l0 = new Label();
			Label l1 = new Label();
			Label l2 = new Label();
			mv.visitTryCatchBlock(l0, l1, l2, "java/lang/Throwable");
			mv.visitLabel(l0);
			

			mv.visitVarInsn(ALOAD, 0);
			mv.visitFieldInsn(GETFIELD, invokerclassName, "opon", classBeingTestedLComma);
			
			if(isInit) {
				mv.visitVarInsn(ALOAD, 0);
				mv.visitFieldInsn(GETFIELD, invokerclassName, "line", "I");
				mv.visitVarInsn(ALOAD, 0);
				mv.visitFieldInsn(GETFIELD, invokerclassName, "col", "I");
				mv.visitVarInsn(ALOAD, 0);
				mv.visitFieldInsn(GETFIELD, invokerclassName, "srcloc", "Lcom/concurnas/lang/LangExt$SourceLocation;");
				mv.visitVarInsn(ALOAD, 0);
				mv.visitFieldInsn(GETFIELD, invokerclassName, "src", "Ljava/lang/String;");
				mv.visitMethodInsn(INVOKEVIRTUAL, classBeingTested, "initialize", "(IILcom/concurnas/lang/LangExt$SourceLocation;Ljava/lang/String;)"+resultCls, false);
			}else {//iterate
				mv.visitVarInsn(ALOAD, 0);
				mv.visitFieldInsn(GETFIELD, invokerclassName, "context", "Lcom/concurnas/lang/LangExt$Context;");
				mv.visitMethodInsn(INVOKEVIRTUAL, classBeingTested, "iterate", "(Lcom/concurnas/lang/LangExt$Context;)"+resultCls, false);
			}
						
			mv.visitVarInsn(ASTORE, 1);
			Label l3 = new Label();
			mv.visitLabel(l3);
			mv.visitVarInsn(ALOAD, 0);
			mv.visitVarInsn(ALOAD, 1);
			mv.visitMethodInsn(INVOKESPECIAL, invokerclassName, "setResult", "("+resultCls+")V", false);
			mv.visitLabel(l1);
			Label l4 = new Label();
			//java.lang.System.ide
			mv.visitJumpInsn(GOTO, l4);
			mv.visitLabel(l2);
			mv.visitFrame(Opcodes.F_SAME1, 0, null, 1, new Object[] {"java/lang/Throwable"});
			mv.visitVarInsn(ASTORE, 1);
			Label l5 = new Label();
			mv.visitLabel(l5);
			Label l6 = new Label();
			mv.visitLabel(l6);
			
			
			mv.visitVarInsn(ALOAD, 0);
			
			//mv.visitVarInsn(ALOAD, 1);
			//mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Throwable", "getMessage", "()Ljava/lang/String;", false);
			//mv.visitMethodInsn(INVOKESPECIAL, invokerclassName, "setResult", "(Ljava/lang/String;)V", false);//TODO: exception thrown in init or iteration
			
			
			mv.visitVarInsn(ALOAD, 1);
			mv.visitMethodInsn(INVOKESPECIAL, invokerclassName, "setException", "(Ljava/lang/Throwable;)V", false);//TODO: exception thrown in init or iteration
			
			
			mv.visitLabel(l4);
			mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
			mv.visitInsn(ACONST_NULL);
			mv.visitInsn(ARETURN);
			mv.visitMaxs(2, 2);
			mv.visitEnd();
		}
		

		{
			mv = cw.visitMethod(ACC_PUBLIC + ACC_BRIDGE + ACC_SYNTHETIC, "apply", "()Ljava/lang/Object;", null, null);
			mv.visitCode();
			Label l0 = new Label();
			mv.visitLabel(l0);
			mv.visitVarInsn(ALOAD, 0);
			mv.visitMethodInsn(INVOKEVIRTUAL, invokerclassName, "apply", "()Ljava/lang/Void;", false);
			mv.visitInsn(ARETURN);
			mv.visitMaxs(1, 1);
			mv.visitEnd();
		}
		
		{
			mv = cw.visitMethod(ACC_PRIVATE + ACC_SYNCHRONIZED, "setResult", "("+resultCls+")V", null, null);
			mv.visitCode();
			Label l0 = new Label();
			mv.visitLabel(l0);
			mv.visitVarInsn(ALOAD, 0);
			mv.visitVarInsn(ALOAD, 1);
			mv.visitFieldInsn(PUTFIELD, invokerclassName, "result", resultCls);
			mv.visitLabel(new Label());
			mv.visitVarInsn(ALOAD, 0);
			mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Object", "notifyAll", "()V", false);
			mv.visitLabel(new Label());
			mv.visitInsn(RETURN);
			mv.visitMaxs(2, 2);
			mv.visitEnd();
		}
		
		{
			mv = cw.visitMethod(ACC_PRIVATE + ACC_SYNCHRONIZED, "setException", "(Ljava/lang/Throwable;)V", null, null);
			mv.visitCode();
			

			Label l0 = new Label();
			mv.visitLabel(l0);
			
			
			
			mv.visitVarInsn(ALOAD, 0);
			mv.visitVarInsn(ALOAD, 1);
			mv.visitFieldInsn(PUTFIELD, invokerclassName, "exception", "Ljava/lang/Throwable;");
			
			mv.visitLabel(new Label());
			mv.visitVarInsn(ALOAD, 0);
			mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Object", "notifyAll", "()V", false);

			
			mv.visitLabel(new Label());
			mv.visitInsn(RETURN);
			mv.visitMaxs(2, 2);
			mv.visitEnd();
		}
		
		{
			mv = cw.visitMethod(ACC_PUBLIC + ACC_SYNCHRONIZED, "getResult", "()"+resultCls, null, new String[] { "java/lang/InterruptedException" });
			mv.visitCode();
			Label l0 = new Label();
			mv.visitLabel(l0);
			
			Label l1 = new Label();
			mv.visitJumpInsn(GOTO, l1);
			Label l2 = new Label();
			mv.visitLabel(l2);
			mv.visitVarInsn(ALOAD, 0);
			mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Object", "wait", "()V", false);
			mv.visitLabel(l1);
			mv.visitVarInsn(ALOAD, 0);
			mv.visitFieldInsn(GETFIELD, invokerclassName, "exception", "Ljava/lang/Throwable;");
			Label l3 = new Label();
			mv.visitJumpInsn(IFNONNULL, l3);
			mv.visitVarInsn(ALOAD, 0);
			mv.visitFieldInsn(GETFIELD, invokerclassName, "result", resultCls);
			mv.visitJumpInsn(IFNULL, l2);
			mv.visitLabel(l3);
			
			Label l4 = new Label();
			mv.visitLabel(l4);
			mv.visitVarInsn(ALOAD, 0);
			mv.visitFieldInsn(GETFIELD, invokerclassName, "exception", "Ljava/lang/Throwable;");
			Label l5 = new Label();
			mv.visitJumpInsn(IFNULL, l5);
			Label l6 = new Label();
			mv.visitLabel(l6);
			mv.visitVarInsn(ALOAD, 0);
			mv.visitFieldInsn(GETFIELD, invokerclassName, "exception", "Ljava/lang/Throwable;");
			mv.visitInsn(ATHROW);
			mv.visitLabel(l5);

			mv.visitVarInsn(ALOAD, 0);
			mv.visitFieldInsn(GETFIELD, invokerclassName, "result", resultCls);
			mv.visitJumpInsn(IFNULL, l2);
			mv.visitLabel(new Label());
			mv.visitVarInsn(ALOAD, 0);
			mv.visitFieldInsn(GETFIELD, invokerclassName, "result", resultCls);
			mv.visitInsn(ARETURN);
			mv.visitMaxs(1, 1);
			mv.visitEnd();
		}
		{
			mv = cw.visitMethod(ACC_PUBLIC, "signature", "()[Ljava/lang/Object;", null, null);
			mv.visitCode();
			Label l0 = new Label();
			mv.visitLabel(l0);
			mv.visitInsn(ACONST_NULL);
			mv.visitInsn(ARETURN);
			mv.visitMaxs(1, 1);
			mv.visitEnd();
		}
		
		{//getIsInitCompleteFlag
			mv = cw.visitMethod(ACC_PUBLIC, "getIsInitCompleteFlag", "()Lcom/concurnas/bootstrap/runtime/ref/DefaultRef;", "()Lcom/concurnas/bootstrap/runtime/ref/DefaultRef<Ljava/lang/Boolean;>;", null);
			mv.visitCode();
			Label l0 = new Label();
			mv.visitLabel(l0);
			mv.visitVarInsn(ALOAD, 0);
			mv.visitFieldInsn(GETFIELD, invokerclassName, "$isInitCompleteFlag", "Lcom/concurnas/bootstrap/runtime/ref/DefaultRef;");
			mv.visitInsn(ARETURN);
			mv.visitMaxs(1, 1);
			mv.visitEnd();
		}
		
		cw.visitEnd();

		return cw.toByteArray();
	}
}