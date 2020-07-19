package com.concurnas.compiler.constants;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Stack;

import com.concurnas.bootstrap.runtime.InitUncreatable;
import com.concurnas.bootstrap.runtime.cps.Fiber;
import com.concurnas.compiler.ast.AccessModifier;
import com.concurnas.compiler.ast.ClassDefJava;
import com.concurnas.compiler.ast.FuncType;
import com.concurnas.compiler.ast.PrimativeType;
import com.concurnas.compiler.ast.PrimativeTypeEnum;
import com.concurnas.compiler.ast.Type;
import com.concurnas.compiler.utils.CompiledClassUtils;
import com.concurnas.compiler.utils.TypeDefTypeProvider;
import com.concurnas.compiler.utils.TypeDefTypeProvider.TypeDef;
import com.concurnas.lang.Actor;
import com.concurnas.lang.ParamName;
import com.concurnas.lang.TypedActor;
import com.concurnas.runtime.ConcImmutable;
import com.concurnas.runtime.DefaultParamUncreatable;
import com.concurnas.runtime.cps.IsoRegistrationSet;

public class UncallableMethods {

	public final static HashMap<String, HashSet<FuncType>> GLOBAL_UNCALLABLE_METHODS = new HashMap<String, HashSet<FuncType>>();
	static
	{
		//these methods are not permitted as being callable from Object (and they are all final as well so cannot be extended)
		/*
		o.notify();
		o.notifyAll();
		o.wait();
		o.wait(6l);
		o.wait(6l, 3);
		*/
		
		//dunno if this works very well in terms of objects not primatives as arguments... errrm, direct type match only?
		
		ArrayList<Type> emptyArgs = new ArrayList<Type>();
		PrimativeType voida = new PrimativeType(PrimativeTypeEnum.VOID);
		FuncType boringMethod = new FuncType(emptyArgs, voida);
		
		HashSet<FuncType> oneBoring = new HashSet<FuncType>();
		oneBoring.add(boringMethod);
		
		GLOBAL_UNCALLABLE_METHODS.put("notify", oneBoring);
		GLOBAL_UNCALLABLE_METHODS.put("notifyAll", oneBoring);
		
		ArrayList<Type> argsOneLog = new ArrayList<Type>();
		argsOneLog.add(new PrimativeType(PrimativeTypeEnum.LONG));

		ArrayList<Type> argsOneLogOneInt = new ArrayList<Type>();
		argsOneLogOneInt.add(new PrimativeType(PrimativeTypeEnum.LONG));
		argsOneLogOneInt.add(new PrimativeType(PrimativeTypeEnum.INT));
		
		HashSet<FuncType> tasteOfWait = new HashSet<FuncType>();
		tasteOfWait.add(boringMethod);
		tasteOfWait.add(new FuncType(argsOneLog, voida));
		tasteOfWait.add(new FuncType(argsOneLogOneInt, voida));
		
		GLOBAL_UNCALLABLE_METHODS.put("wait", tasteOfWait);
		
		//GLOBAL_UNCALLABLE_METHODS.put("finalize", oneBoring);//TODO: i think this is callable uncallable?
	}
	
	public final static HashSet<ClassDefJava> UNAVAILABLE_CLASSES = new HashSet<ClassDefJava>();
	static
	{
		//TODO: add more conc internals here?
		//UNAVAILABLE_CLASSES.add(new ClassDefJava(Thread.class));
		UNAVAILABLE_CLASSES.add(new ClassDefJava(IsoRegistrationSet.class));//conc internal
		UNAVAILABLE_CLASSES.add(new ClassDefJava(ConcImmutable.class));
		//UNAVAILABLE_CLASSES.add(new ClassDefJava(CopyTracker.class));
		UNAVAILABLE_CLASSES.add(new ClassDefJava(Fiber.class));
		UNAVAILABLE_CLASSES.add(new ClassDefJava(ParamName.class));
		UNAVAILABLE_CLASSES.add(new ClassDefJava(java.util.concurrent.ForkJoinWorkerThread.class));//TODO: this is not a great way to do this better to have thing which searches parent for the ofending unavaiable classes
		UNAVAILABLE_CLASSES.add(new ClassDefJava(InitUncreatable.class));//TODO: this is not a great way to do this better to have thing which searches parent for the ofending unavaiable classes
		UNAVAILABLE_CLASSES.add(new ClassDefJava(DefaultParamUncreatable.class));//TODO: this is not a great way to do this better to have thing which searches parent for the ofending unavaiable classes
	}
	
	public final static HashSet<ClassDefJava> UNEXTENDABLE_ACTOR_CLASSES = new HashSet<ClassDefJava>();
	static
	{
		//TODO: add more conc internals here?
		UNEXTENDABLE_ACTOR_CLASSES.add(new ClassDefJava(TypedActor.class));
		UNEXTENDABLE_ACTOR_CLASSES.add(new ClassDefJava(Actor.class));
	}
	
	private static Map<String/*ref name*/, Integer> LAMNDA_FUNC_TYPES = new HashMap<String/*ref name*/, Integer>();
	
	private static Map<String/*ref name*/, String> AUTO_IMPORTS = new HashMap<String/*ref name*/, String>();
	static
	{
		for(int n =0; n<=23; n++)
		{
			String name = "com.concurnas.bootstrap.lang.Lambda.Function" +n;
			LAMNDA_FUNC_TYPES.put(name, n);
			LAMNDA_FUNC_TYPES.put(name+"v", n);
			AUTO_IMPORTS.put("Function" +n, name);
			AUTO_IMPORTS.put("Function" +n+"v", name);
		}
		
		AUTO_IMPORTS.put("Boolean", "java.lang.Boolean");
		AUTO_IMPORTS.put("Byte", "java.lang.Byte");
		AUTO_IMPORTS.put("Character", "java.lang.Character");
		AUTO_IMPORTS.put("Class", "java.lang.Class");
		AUTO_IMPORTS.put("ClassLoader", "java.lang.ClassLoader");
		AUTO_IMPORTS.put("ClassValue", "java.lang.ClassValue");
		AUTO_IMPORTS.put("Compiler", "java.lang.Compiler");
		AUTO_IMPORTS.put("Double", "java.lang.Double");
		AUTO_IMPORTS.put("Enum", "java.lang.Enum");
		AUTO_IMPORTS.put("Float", "java.lang.Float");
		AUTO_IMPORTS.put("InheritableThreadLocal", "java.lang.InheritableThreadLocal");
		AUTO_IMPORTS.put("Integer", "java.lang.Integer");
		AUTO_IMPORTS.put("Long", "java.lang.Long");
		AUTO_IMPORTS.put("Math", "java.lang.Math");
		AUTO_IMPORTS.put("Number", "java.lang.Number");
		AUTO_IMPORTS.put("Object", "java.lang.Object");
		AUTO_IMPORTS.put("Package", "java.lang.Package");
		AUTO_IMPORTS.put("Process", "java.lang.Process");
		AUTO_IMPORTS.put("ProcessBuilder", "java.lang.ProcessBuilder");
		AUTO_IMPORTS.put("Runtime", "java.lang.Runtime");
		AUTO_IMPORTS.put("RuntimePermission", "java.lang.RuntimePermission");
		AUTO_IMPORTS.put("SecurityManager", "java.lang.SecurityManager");
		AUTO_IMPORTS.put("Short", "java.lang.Short");
		AUTO_IMPORTS.put("StackTraceElement", "java.lang.StackTraceElement");
		AUTO_IMPORTS.put("StrictMath", "java.lang.StrictMath");
		AUTO_IMPORTS.put("String", "java.lang.String");
		AUTO_IMPORTS.put("StringBuffer", "java.lang.StringBuffer");
		AUTO_IMPORTS.put("StringBuilder", "java.lang.StringBuilder");
		AUTO_IMPORTS.put("System", "java.lang.System");
		AUTO_IMPORTS.put("Thread", "java.lang.Thread");
		AUTO_IMPORTS.put("ThreadGroup", "java.lang.ThreadGroup");
		AUTO_IMPORTS.put("ThreadLocal", "java.lang.ThreadLocal");
		AUTO_IMPORTS.put("Error", "java.lang.Error");
		AUTO_IMPORTS.put("Exception", "java.lang.Exception");
		AUTO_IMPORTS.put("Throwable", "java.lang.Throwable");
		AUTO_IMPORTS.put("Void", "java.lang.Void");
		//interfaces
		AUTO_IMPORTS.put("Appendable", "java.lang.Appendable");
		AUTO_IMPORTS.put("AutoCloseable", "java.lang.AutoCloseable");
		AUTO_IMPORTS.put("CharSequence", "java.lang.CharSequence");
		AUTO_IMPORTS.put("Cloneable", "java.lang.Cloneable");
		AUTO_IMPORTS.put("Comparable", "java.lang.Comparable");
		AUTO_IMPORTS.put("Iterable", "java.lang.Iterable");
		AUTO_IMPORTS.put("Readable", "java.lang.Readable");
		AUTO_IMPORTS.put("Runnable", "java.lang.Runnable");
		//exceptions
		AUTO_IMPORTS.put("ArithmeticException", "java.lang.ArithmeticException");
		AUTO_IMPORTS.put("ArrayIndexOutOfBoundsException", "java.lang.ArrayIndexOutOfBoundsException");
		AUTO_IMPORTS.put("ArrayStoreException", "java.lang.ArrayStoreException");
		AUTO_IMPORTS.put("ClassCastException", "java.lang.ClassCastException");
		AUTO_IMPORTS.put("ClassNotFoundException", "java.lang.ClassNotFoundException");
		AUTO_IMPORTS.put("CloneNotSupportedException", "java.lang.CloneNotSupportedException");
		AUTO_IMPORTS.put("EnumConstantNotPresentException", "java.lang.EnumConstantNotPresentException");
		AUTO_IMPORTS.put("Exception", "java.lang.Exception");
		AUTO_IMPORTS.put("IllegalAccessException", "java.lang.IllegalAccessException");
		AUTO_IMPORTS.put("IllegalArgumentException", "java.lang.IllegalArgumentException");
		AUTO_IMPORTS.put("IllegalMonitorStateException", "java.lang.IllegalMonitorStateException");
		AUTO_IMPORTS.put("IllegalStateException", "java.lang.IllegalStateException");
		AUTO_IMPORTS.put("IllegalThreadStateException", "java.lang.IllegalThreadStateException");
		AUTO_IMPORTS.put("IndexOutOfBoundsException", "java.lang.IndexOutOfBoundsException");
		AUTO_IMPORTS.put("InstantiationException", "java.lang.InstantiationException");
		AUTO_IMPORTS.put("InterruptedException", "java.lang.InterruptedException");
		AUTO_IMPORTS.put("NegativeArraySizeException", "java.lang.NegativeArraySizeException");
		AUTO_IMPORTS.put("NoSuchFieldException", "java.lang.NoSuchFieldException");
		AUTO_IMPORTS.put("NoSuchMethodException", "java.lang.NoSuchMethodException");
		AUTO_IMPORTS.put("NullPointerException", "java.lang.NullPointerException");
		AUTO_IMPORTS.put("NumberFormatException", "java.lang.NumberFormatException");
		AUTO_IMPORTS.put("ReflectiveOperationException", "java.lang.ReflectiveOperationException");
		AUTO_IMPORTS.put("RuntimeException", "java.lang.RuntimeException");
		AUTO_IMPORTS.put("SecurityException", "java.lang.SecurityException");
		AUTO_IMPORTS.put("StringIndexOutOfBoundsException", "java.lang.StringIndexOutOfBoundsException");
		AUTO_IMPORTS.put("TypeNotPresentException", "java.lang.TypeNotPresentException");
		AUTO_IMPORTS.put("UnsupportedOperationException", "java.lang.UnsupportedOperationException");
		//errors
		AUTO_IMPORTS.put("AbstractMethodError", "java.lang.AbstractMethodError");
		AUTO_IMPORTS.put("AssertionError", "java.lang.AssertionError");
		AUTO_IMPORTS.put("BootstrapMethodError", "java.lang.BootstrapMethodError");
		AUTO_IMPORTS.put("ClassCircularityError", "java.lang.ClassCircularityError");
		AUTO_IMPORTS.put("ClassFormatError", "java.lang.ClassFormatError");
		AUTO_IMPORTS.put("Error", "java.lang.Error");
		AUTO_IMPORTS.put("ExceptionInInitializerError", "java.lang.ExceptionInInitializerError");
		AUTO_IMPORTS.put("IllegalAccessError", "java.lang.IllegalAccessError");
		AUTO_IMPORTS.put("IncompatibleClassChangeError", "java.lang.IncompatibleClassChangeError");
		AUTO_IMPORTS.put("InstantiationError", "java.lang.InstantiationError");
		AUTO_IMPORTS.put("InternalError", "java.lang.InternalError");
		AUTO_IMPORTS.put("LinkageError", "java.lang.LinkageError");
		AUTO_IMPORTS.put("NoClassDefFoundError", "java.lang.NoClassDefFoundError");
		AUTO_IMPORTS.put("NoSuchFieldError", "java.lang.NoSuchFieldError");
		AUTO_IMPORTS.put("NoSuchMethodError", "java.lang.NoSuchMethodError");
		AUTO_IMPORTS.put("OutOfMemoryError", "java.lang.OutOfMemoryError");
		AUTO_IMPORTS.put("StackOverflowError", "java.lang.StackOverflowError");
		AUTO_IMPORTS.put("ThreadDeath", "java.lang.ThreadDeath");
		AUTO_IMPORTS.put("UnknownError", "java.lang.UnknownError");
		AUTO_IMPORTS.put("UnsatisfiedLinkError", "java.lang.UnsatisfiedLinkError");
		AUTO_IMPORTS.put("UnsupportedClassVersionError", "java.lang.UnsupportedClassVersionError");
		AUTO_IMPORTS.put("VerifyError", "java.lang.VerifyError");
		AUTO_IMPORTS.put("VirtualMachineError", "java.lang.VirtualMachineError");
		//concurnas lang lambda
		AUTO_IMPORTS.put("Lambda", "com.concurnas.bootstrap.lang.Lambda");
		AUTO_IMPORTS.put("gpus", "com.concurnas.lang.gpus"); 
		AUTO_IMPORTS.put("concurrent", "com.concurnas.lang.concurrent"); 
		AUTO_IMPORTS.put("GPUStubFunction", "com.concurnas.lang.GPUStubFunction"); 
		//others
		AUTO_IMPORTS.put("Concurnas", "com.concurnas.runtime.utils.Concurnas"); //TODO: move to com.concurnas.lang
		AUTO_IMPORTS.put("Nullable", "com.concurnas.lang.nullable.Nullable"); //TODO: move to com.concurnas.lang
		//annotations
		AUTO_IMPORTS.put("Deprecated", "java.lang.Deprecated");
		AUTO_IMPORTS.put("Override", "java.lang.Override");
		AUTO_IMPORTS.put("SafeVarargs", "java.lang.SafeVarargs");
		AUTO_IMPORTS.put("SuppressWarnings", "java.lang.SuppressWarnings");
		AUTO_IMPORTS.put("Named", "com.concurnas.lang.Named");
		//remote
		AUTO_IMPORTS.put("Remote", "com.concurnas.lang.dist.Remote"); 
		addModules();
	}
	
	private static void addModules() {
		addAllPublicInModule("com.concurnas.lang.ranges");
		addAllPublicInModule("com.concurnas.lang.tuples");
		addAllPublicInModule("com.concurnas.lang.datautils");
	}
	
	private static void addAllPublicInModule(String... module) {
		for(String mod : module) {
			try {
				Class<?> cls = Class.forName(mod);
				
				for(Method ma : cls.getDeclaredMethods()) {
					if(Modifier.isStatic(ma.getModifiers())) {
						String name = ma.getName();
						AUTO_IMPORTS.put(name, mod + "." + name);
					}
				}
				
				for(Class<?> mcalss : cls.getClasses()) {
					String name = mcalss.getName();
					String shortname = name.substring(name.indexOf('$')+1);
					AUTO_IMPORTS.put(shortname, name.replace('$', '.') );
				}
				
				
				//typedefs
				TypeDefTypeProvider tp = CompiledClassUtils.getTypeDef(cls, null);
				if(null != tp) {
					for(TypeDef argsToTypeAndGen : tp.alltypeAndGens) {
						AccessModifier am = argsToTypeAndGen.am;
						if(am == AccessModifier.PUBLIC || am == AccessModifier.PACKAGE) {
							String name = argsToTypeAndGen.name;
							AUTO_IMPORTS.put(name, mod + "." + name);
						}
					}
				}
				
				
			} catch (ClassNotFoundException e) {
				//skip
			}
		}
		
	}
	
	public static boolean isLambda(String fullname)
	{
		return LAMNDA_FUNC_TYPES.containsKey(fullname);
	}
	
	public static int getLambdaDegree(String fullname)
	{
		return LAMNDA_FUNC_TYPES.get(fullname);
	}
	
	/*
	 * Clone a copy
	 */
	//public static Map<String/*ref name*/, String> cloneAutoImports()
	public static Stack<Map<String/*ref name*/, String>> cloneAutoImports(){
		Stack<Map<String/*ref name*/, String>> reet = new Stack<Map<String/*ref name*/, String>>();
		Map<String/*ref name*/, String> ret = new HashMap<String/*ref name*/, String>();
		for(String var :  AUTO_IMPORTS.keySet()){
			ret.put(var, AUTO_IMPORTS.get(var));
		}
		reet.add(ret);
		return reet;
	}
	
}
