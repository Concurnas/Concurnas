package com.concurnas.runtime;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Stack;

import com.concurnas.bootstrap.runtime.InitUncreatable;
import com.concurnas.bootstrap.runtime.cps.Fiber;
import com.concurnas.compiler.ast.FuncType;
import com.concurnas.compiler.utils.Thruple;
import com.concurnas.lang.Shared;
import com.concurnas.lang.Uninterruptible;
import com.concurnas.runtime.ClassloaderUtils.ClassProvider;
import com.concurnas.runtime.ClassloaderUtils.FixedCP;
import com.concurnas.runtime.cps.mirrors.CachedClassMirrors;
import com.concurnas.runtime.cps.mirrors.Detector;

//TOOD: lock down this, i.e. all the protected stuff from ClassLoader that you dont use in here, prevent these from being called
@Shared
@Uninterruptible
public class ConcurnasClassLoader  extends ClassLoader implements ConcClassUtil {
	public static final String staticLambdaClassesCls = "com/concurnas/bootstrap/StaticLambdaClasses";
	
	public HashMap<String, Boolean> isImmtuableDirectory = new HashMap<String, Boolean>();
	public HashSet<String> classesDefinedHere = new HashSet<String>();
	private ClassLoader primordialClassLoader = Thread.currentThread().getContextClassLoader();
	private Stack<String> concstack = new Stack<String>();
	public HashMap<String, byte[]> cpsStateClasses = new HashMap<String, byte[]>();
	private final Path[] classpath;
	private final Path[] primordialClassPath;
	private HashMap<String, Class<?>> definedAlready = new HashMap<String, Class<?>>();
	public HashMap<String, HashSet<String>> classToGlobalDependancies = new HashMap<String, HashSet<String>>();//cache permits quick lookup for use in IsoTaskMethodGennerator - may not belong in this class JPT: refactor
	private HashMap<String, ClassProvider> clsToClasspath = new HashMap<String, ClassProvider>();
	private HashMap<String, ClassProvider> clsToPrimordialClasspath = new HashMap<String, ClassProvider>();
	   
	private Detector theDetector = new Detector(new CachedClassMirrors(this));
	private ConcurnasClassLoader parent = null;
	
	private static final HashSet<String> dontConc = new HashSet<String>();
	static {//this may need some tweaking at some point, i.e. when running lang with all runtime classses in a primordial package
		dontConc.add("com/concurnas/bootstrap/runtime/cps/State");
		dontConc.add("com/concurnas/bootstrap/runtime/cps/Fiber");
		dontConc.add("com/concurnas/bootstrap/runtime/transactions/LocalTransaction");
		dontConc.add("com/concurnas/bootstrap/runtime/ref/Ref");
		dontConc.add("com/concurnas/bootstrap/runtime/ref/DefaultRef");
		dontConc.add("com/concurnas/bootstrap/runtime/ref/DirectlyAssignable");
		//dontConc.add("org/jocl/CLLibLoader");
		//dontConc.add("org/jocl/LibUtils");
		dontConc.add("com/concurnas/bootstrap/runtime/ref/DirectlyGettable");
		dontConc.add("com/concurnas/bootstrap/runtime/ref/DirectlyArrayGettable");
		dontConc.add("com/concurnas/bootstrap/runtime/InitUncreatable");
		dontConc.add("com/concurnas/bootstrap/runtime/cps/Worker");// TODO: remove hack
		dontConc.add("com/concurnas/bootstrap/runtime/cps/Scheduler");// TODO: remove hack
		dontConc.add("com/concurnas/bootstrap/lang/ConcurnasSecurityManager");
	}
	
	protected final class BoolPair{
		public final Boolean immutable;
		public final Boolean canUseQuickMethod;//i.e. copy instead of invoking the runtime Copier
		public BoolPair(Boolean immutable, Boolean quick){
			this.immutable=immutable;
			this.canUseQuickMethod=quick;
		}
	}
	
	public ConcurnasClassLoader(Path[] classpath, Path[] primordialClassPath){
		this(classpath, primordialClassPath, null);
	}
	
	public ConcurnasClassLoader(Path[] classpath){
		this(classpath, null, null);
	}
	
	public ConcurnasClassLoader(){
		this(null, null, null);
	}
	
	public ConcurnasClassLoader(InitUncreatable x){
		this(null, null, null);
	}
	
	public ConcurnasClassLoader(Path[] classpath, Path[] primordialClassPath, ConcurnasClassLoader parent){
		this.classpath=classpath;//TODO: extend this with more java like functionality? + all your security etc?
		this.primordialClassPath=primordialClassPath;//TODO: this is a bit of a hack and can be removed from release? - just so that we can figure out what consitutes a primoridal at runtime
		loadClasses();
		this.parent  = parent;
	}
	

	
	private void loadClasses(){
		//later tnerites override earlier ones
		if(null != this.classpath){
			for(Path thing : this.classpath){
				//File thing = new File(element);
				ClassloaderUtils.populateClasses(thing, thing, clsToClasspath, true);
			}
		}

		if(this.primordialClassPath != null){
			for(Path thing : this.primordialClassPath){
				//File thing = new File(element);
				ClassloaderUtils.populateClasses(thing, thing, clsToPrimordialClasspath, false);
			}
		}
	}
	
	public byte[] getBytecode(String name, Fiber fib){
		return getBytecode(name);
	}
	
	
	
	public byte[] getBytecode(String name, boolean searchSystemClassloader){
		if(!searchSystemClassloader) {
			return null;
		}
		String nameSlash = name.replace('.', '/');
		
		ClassProvider found = clsToClasspath.get(nameSlash);
		try {
			if(null == found){
				found = obtainProviderIfRuntimeGennerated(nameSlash);
			}
			
			if(found != null){
				return found.provide(nameSlash);
			}

		} catch (IOException e) {
			//TODO: warning?
		}
		return null;
	}
	
	public byte[] getBytecode(String name){
		return getBytecode(name, true);
	}
	
	private ClassProvider obtainProviderIfRuntimeGennerated(String nameSlash) throws IOException{
		byte[] generatedActorIface = null;
		if(nameSlash.endsWith("$$ActorIterface") && nameSlash.startsWith("x")){
			//TODO: when the source actee gets updated, then the actor interface should as well
			String theActee = nameSlash.substring(1, nameSlash.length() - 15);//e.g. xjava.lang.String$$ActorIterface -> java.lang.String
			generatedActorIface = ActorInterfaceGennerator.actorInterfaceGennerator(theActee, this, nameSlash);
		}
		else if(nameSlash.endsWith(FuncType.classRefIfacePostfix) && nameSlash.startsWith(FuncType.classRefIfacePrefix)){
			String theActee = nameSlash.substring(1, nameSlash.length() - FuncType.classRefIfacePostfixLength);//e.g. xjava.lang.String$$ActorIterface -> java.lang.String
			generatedActorIface = ClassRefInterfaceGennerator.classRefInterfaceGennerator(theActee, this, nameSlash);
		}
		
		if(null != generatedActorIface){
			FixedCP ret = new FixedCP(generatedActorIface);
			clsToClasspath.put(nameSlash, ret);
			return ret;
		}
		
		return null;
	}
	
	
	public Class<?> getSuperClassRef(String desc) throws ClassNotFoundException{
		return primordialClassLoader.loadClass(desc.replace("/", "."));//JPT: dunno about this...
	}
	
	private Stack<String> isIMMStack = new Stack<String>();
	
	public BoolPair isImmutable(String desc){
		desc = desc.replace('/', '.');
		if(classesDefinedHere.contains(desc)){
			return new BoolPair(isImmtuableDirectory.get(desc), true);
		}
		else{
			try {//Ljava/util/ArrayList; => java.util.ArrayList
				String dd = desc.substring(1, desc.length()-1);
				if(!isIMMStack.contains(dd)) {
					isIMMStack.push(dd);
					try {
						this.findClass(dd);
					}finally {
						isIMMStack.pop();
					}
				}
				
				if(!classesDefinedHere.contains(desc)){
					//must have been defined higher up
					isImmtuableDirectory.put(desc, false);
					return new BoolPair(false, false);//not immutable and not quick
				}
				else{
					return new BoolPair(isImmtuableDirectory.get(desc), true);//if just having deps on self having loop
				}
			} catch (ClassNotFoundException e) {//oops
			}
			return new BoolPair(true, false);//conservative
		}
	}
	
	protected void registerTransformed(String name, byte[] trans){}
	
	public Class<?> defineClass(String name, byte[] b){
		//System.err.println("define: " + name);
		if(this.definedAlready.containsKey(name)) {
			return this.definedAlready.get(name);
		}
		
		try {
			//System.err.println("define: " + name);//com.concurnas.bootstrap.runtime.ReifiedType
			RequestedAndSupportingCls allitems = Concurnifier.initialConc(b, this, name);//TODO: refactor to give this class a better name and remove utility classes from definition
			if(null != allitems.supporting && !allitems.supporting.isEmpty()){
				for(Pair<String, Thruple< Boolean, byte[], HashSet<String>>> sup : allitems.supporting){
					defineClassSupportingEtc(sup.getA().replace('/', '.'), sup.getB());
				}
			}
			
			return defineClassSupportingEtc(name, allitems.requested);
		}catch(Throwable thr) {
			throw new RuntimeException("Failure during definition of class: " + name , thr);
		}
	}
	
	private synchronized Class<?> defineClassSupportingEtc(String name, Thruple<Boolean, byte[], HashSet<String>> res){
		String clsName = "L"+name+";";
		classesDefinedHere.add(clsName);
		
		concstack.add(clsName);
		try {
			Boolean isImmutable = res.getA();
			
			isImmtuableDirectory.put(clsName, isImmutable);
			byte[] requestedBytecode = res.getB();
			
			Class<?> cls = null;
			if(concstack.size() == 1){
				HashMap<String, byte[]> nameToTrans =null;
				try{//cps transofmration and globalization and thing with constructors etc
					nameToTrans = Cpsifier.doCPSTransform(this, name, requestedBytecode, false, false, false);//included global classes for module fields 
				}
				catch(Throwable t){
					//concstack.pop();
					throw t;
				}

				requestedBytecode = nameToTrans.get(name);
				registerTransformed(name, requestedBytecode);//for debug
				
				for(String namell : nameToTrans.keySet()){//extra globals...
					if(!namell.equals(name) && !definedAlready.containsKey(namell)){
						byte[] toTrans = nameToTrans.get(namell);
						registerTransformed(namell, toTrans);//for debug
						namell = namell.replaceAll("/", ".");
						//System.err.println("define: " + namell + " super: " + this);
						
						
						/*RuntimeMXBean bean = ManagementFactory.getRuntimeMXBean();
						List<String> jvmArgs = bean.getInputArguments();
						System.err.println("jvmargs: " + jvmArgs);*/
						
						/*
						 * if(namell.contains("TestCase$Globals$")) { try {
						 * BytecodePrettyPrinter.print(toTrans, true); } catch (Exception e) { // TODO
						 * Auto-generated catch block e.printStackTrace(); } }
						 */
						
						Class<?> clz = super.defineClass(namell, toTrans, 0, toTrans.length);
						definedAlready.put(namell, clz);
					}
				}
				//super.
				if(!definedAlready.containsKey(name)) {
					concstack.pop();
					cls = super.defineClass(name, requestedBytecode, 0, requestedBytecode.length);
					concstack.add(clsName);
					definedAlready.put(name, cls);
				}else {
					return definedAlready.get(name);
				}
			}
			return cls;
		}finally {
			if(!concstack.isEmpty()) {
				concstack.pop();
			}
		}
	}
	
	protected static final HashSet<String> getFromPrimoridal = new HashSet<String>();
	static{
		getFromPrimoridal.add("com/concurnas/bootstrap/runtime/InitUncreatable");
		getFromPrimoridal.add("com/concurnas/bootstrap/runtime/cps/Fiber");
		getFromPrimoridal.add("com/concurnas/bootstrap/runtime/ref/Ref");
		getFromPrimoridal.add("com/concurnas/bootstrap/runtime/ref/DirectlyAssignable");
		getFromPrimoridal.add("com/concurnas/bootstrap/runtime/ref/DirectlyGettable");
		getFromPrimoridal.add("com/concurnas/bootstrap/runtime/ref/DirectlyArrayGettable");
		getFromPrimoridal.add("com/concurnas/bootstrap/runtime/ref/DefaultRef");
		getFromPrimoridal.add("com/concurnas/bootstrap/runtime/cps/SyncTracker");
		getFromPrimoridal.add("com/concurnas/bootstrap/runtime/cps/State");
		
		getFromPrimoridal.add("com/concurnas/bootstrap/runtime/cps/Iso");//TODO: why was this excluded?
		
		getFromPrimoridal.add("com/concurnas/bootstrap/lang/offheap/Encoder");//TODO: why was this excluded?
		getFromPrimoridal.add("com/concurnas/bootstrap/lang/offheap/Decoder");//TODO: why was this excluded?
		
		getFromPrimoridal.add("com/concurnas/bootstrap/runtime/cps/IsoTask");
		getFromPrimoridal.add("com/concurnas/bootstrap/runtime/cps/AbstractIsoTask");
		getFromPrimoridal.add("com/concurnas/bootstrap/runtime/cps/IsoTaskNotifiable");
		getFromPrimoridal.add("com/concurnas/bootstrap/runtime/cps/IsoTaskAwait");
		getFromPrimoridal.add("com/concurnas/bootstrap/runtime/cps/IsoTaskEvery");
		getFromPrimoridal.add("com/concurnas/bootstrap/runtime/cps/IsoEvery");
		getFromPrimoridal.add("com/concurnas/bootstrap/runtime/cps/IsoAwait");
		getFromPrimoridal.add("com/concurnas/bootstrap/runtime/cps/IsoNotifiable");
		getFromPrimoridal.add("com/concurnas/bootstrap/runtime/cps/IsoCore");
		getFromPrimoridal.add("com/concurnas/bootstrap/runtime/transactions/TransactionHandler");
		getFromPrimoridal.add("com/concurnas/bootstrap/runtime/transactions/ChangesAndNotifies");
		getFromPrimoridal.add("com/concurnas/bootstrap/runtime/transactions/Transaction");
		getFromPrimoridal.add("com/concurnas/bootstrap/runtime/transactions/LocalTransaction");
		getFromPrimoridal.add("com/concurnas/bootstrap/runtime/ref/LocalArray");
		getFromPrimoridal.add("com/concurnas/bootstrap/runtime/ref/LocalArray$LAIterator");
		getFromPrimoridal.add("com/concurnas/bootstrap/runtime/cps/Woker");
		getFromPrimoridal.add("com/concurnas/bootstrap/runtime/cps/Scheduler");
		getFromPrimoridal.add("com/concurnas/bootstrap/runtime/CopyTracker");
		getFromPrimoridal.add("com/concurnas/bootstrap/runtime/CopyDefinition");
		getFromPrimoridal.add("com/concurnas/bootstrap/runtime/cps/DefaultIsoExceptionHandler");
		getFromPrimoridal.add("com/concurnas/bootstrap/lang/Lambda");
		getFromPrimoridal.add("com/concurnas/bootstrap/lang/Stringifier");
		getFromPrimoridal.add("com/concurnas/bootstrap/lang/Stringifier$NaturalOrderComparator");
		getFromPrimoridal.add("com/concurnas/bootstrap/lang/Lambda$Function0");
		getFromPrimoridal.add("com/concurnas/bootstrap/lang/Lambda$Function1");
		getFromPrimoridal.add("com/concurnas/bootstrap/runtime/cps/CObject");
		getFromPrimoridal.add("com/concurnas/lang/LangExt$SourceLocation");
		getFromPrimoridal.add("com/concurnas/lang/LangExt$Context");
		getFromPrimoridal.add("com/concurnas/lang/LangExt$Variable");
		getFromPrimoridal.add("com/concurnas/lang/LangExt$Result");
		getFromPrimoridal.add("com/concurnas/lang/LangExt$IterationResult");
		getFromPrimoridal.add("com/concurnas/lang/LangExt$ErrorOrWarning");
		getFromPrimoridal.add("com/concurnas/lang/LangExt$Method");
		getFromPrimoridal.add("com/concurnas/lang/LangExt$Function");
		getFromPrimoridal.add("com/concurnas/lang/LangExt$Class");
		getFromPrimoridal.add("com/concurnas/lang/LangExt$Field");
		getFromPrimoridal.add("com/concurnas/lang/LangExt$Location");
		getFromPrimoridal.add("com/concurnas/lang/LangExt$Constructor");
		getFromPrimoridal.add("com/concurnas/repl/REPLRuntimeState");
	}
	
	public Class<?> loadClass(String name, Fiber fib) throws ClassNotFoundException {
		return loadClass(name);
	}
	
	public Class<?> loadClass(String name) throws ClassNotFoundException {
		Class<?> ret = definedAlready.get(name);
		
		//System.err.println(" loadclass: " + name);
				
		if (null == ret) {
			// check parent first
			String slashname = name.replace(".", "/");
			if (getFromPrimoridal.contains(slashname)) {
				try {
					ret = primordialClassLoader.loadClass(name);// load up normal class,  object,  ArrayList Fiber etc
					definedAlready.put(name, ret);
					// System.err.println("loadClass: found in parent: " + name);

					return ret;
				} catch (ClassNotFoundException e) {
				}
				// oh the above is a bit nasty - we're not correctly concing the code cos of this
			}
			
			if (slashname.startsWith("org/jocl/") && parent != null) {// check parent first if there is one defined
				try {
					ret = parent.loadClass(name);
					if(ret != null) {
						definedAlready.put(name, ret);
					}
					
				} catch (ClassNotFoundException e) {
				}
			}

			if("com.concurnas.runtime.ConcurnasClassLoader".equals(name)){
				ret = ConcurnasClassLoader.class;
			}
			else if("com.concurnas.runtime.ConcClassUtil".equals(name)){
				ret = ConcClassUtil.class;
			}
			
			ret = ret != null ? ret : loadClass(name, (ClassProvider)null);
		}

		//System.err.println("loadclass: " + name + "->" + ret);
		
		return ret;
	}
	
	@Override
    public Class<?> findClass(String name) throws ClassNotFoundException {
		if(definedAlready.containsKey(name)) {
			return definedAlready.get(name);
		}
		
       return  super.findClass(name);
    }

    private Class<?> loadClass(String name, ClassProvider found) throws ClassNotFoundException {
    	//TODO: combine with above method
    	//TODO: exclude code from cdk?
    	String nameSlash = name.replace('.', '/');

    	Class<?> retola = definedAlready.get(name);
    	
    	if(null == retola){
    		byte[] code = cpsStateClasses.get(nameSlash);//TODO: does this need to be a cache at all? i think jvm caches after first return...
        	
        	if(null != code){
        		Class<?> ret = super.defineClass(name, code, 0, code.length);
        		definedAlready.put(name, ret);
        		return ret;
        		//return defineClass(name, code);
        	}
        	else{//can we obtain it from the filesystem classpath
        		try {
        			boolean isGloabl = false;
        			if(null == found){
        				if(nameSlash.endsWith("$Globals$")){
            	    		nameSlash = nameSlash.substring(0, nameSlash.length() - 9);
            	    		isGloabl=true;//load from main class then load from definedAlready as well
            	    	}
        				
        				found = clsToClasspath.get(nameSlash);
        				if(found == null){
        					found = obtainProviderIfRuntimeGennerated(nameSlash);
        				}
        			}
        			
        			if(found != null){
        				byte[] fsCode = found.provide(nameSlash);//cache?
        				
        				Class<?> ret;
        				if(dontConc.contains(nameSlash)){//should be loaded like a normal class
        					ret = super.defineClass(name, fsCode, 0, fsCode.length);
        					definedAlready.put(name, ret);
        				}
        				else if(isGloabl){
        					defineClass(name.substring(0, name.length() - 9), fsCode);
        					ret = definedAlready.get(name);
        				}
        				else{
        					ret = defineClass(name, fsCode);
        					definedAlready.put(name, ret);
        				}
        				
        				return ret;
        			}
    			} catch (IOException e) {
    				throw new ClassNotFoundException("Cannot load class");
    			}
        		
				/*if (null != this.parent) {
					try {
						return this.parent.loadClass(name);
					} catch (ClassNotFoundException e) {
					}
				}*/
        		
				try {
					String loadName = name.endsWith("$Globals$") ? name.substring(0, name.length() - 9) : name;
					Class<?> ret = super.loadClass(name);// load up normal class, object, ArrayList Fiber etc
					definedAlready.put(loadName, ret);
					ret = definedAlready.get(name);
					return ret;
				} catch (ClassNotFoundException e) {
				}
        		
        		return null;
        	}
    	}
    	else{
    		return retola;
    	}
    }

	public Class<?> loadClassFromPrimordial(String className) throws ClassNotFoundException {
    	ClassProvider found = clsToClasspath.get(className);
		//System.err.println("primordialClassLoader: " + className);
    	if(found != null){
    		return null;//not in the primoridal
    	}
    	else{
    		if(null!=primordialClassPath){//if we've restricted what consistues the primordials... [i.e. excluding the classes compiled under bin etc]
    			String nameSlash = className.replace('.', '/');
    			if(clsToPrimordialClasspath.containsKey(nameSlash)){
    				return primordialClassLoader.loadClass(className);
    			}
    			return null;//not primoridal
    		}
    		else{
    			return primordialClassLoader.loadClass(className);
    		}
    	}
	}

	@Override
	public Detector getDetector() {
		return this.theDetector;
	}

	@Override
	public HashMap<String, HashSet<String>> getClassToGlobalDependancies() {
		return classToGlobalDependancies;
	}

	@Override
	public HashMap<String, byte[]> getCpsStateClasses() {
		return cpsStateClasses;
	}

	@Override
	public ClassLoader getParentCL() {
		return this.getParent();
	}

	private static class StaticLambdaClassHolder{
		public HashSet<String> staticLambdaClasss;
	}
	
	private StaticLambdaClassHolder staticLambdaClassHolder;
	
	@Override
	public HashSet<String> getStaticLambdaClasses() {
		if(null == staticLambdaClassHolder) {
			staticLambdaClassHolder = new StaticLambdaClassHolder();
			
			try {
				Class<?> inst = primordialClassLoader.loadClass(staticLambdaClassesCls.replace('/', '.'));
				staticLambdaClassHolder.staticLambdaClasss = (HashSet<String>)inst.getMethod("get").invoke(null);
			}catch(Exception e) {
				e.printStackTrace();
			}
		}
		return staticLambdaClassHolder.staticLambdaClasss;
	}
}
