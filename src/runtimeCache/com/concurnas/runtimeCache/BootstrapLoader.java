package com.concurnas.runtimeCache;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.zip.ZipException;

import com.concurnas.lang.util.LRUCache;
import com.concurnas.runtime.ConcClassUtil;
import com.concurnas.runtime.cps.mirrors.CachedClassMirrors;
import com.concurnas.runtime.cps.mirrors.Detector;

public abstract class BootstrapLoader implements ConcClassUtil{

	private final String[] classpath;
	private final Detector detector;
	private HashSet<String> allStatLambdas;
	private RTClassloader rt = null;
	public HashMap<String, byte[]> cpsStateClasses = new HashMap<String, byte[]>();
	
	public BootstrapLoader(String[] classpath) throws ZipException, IOException{
		this.classpath = classpath;//no overlap in classpath so we dont need to check from last defined to first
		this.detector = new Detector(new CachedClassMirrors(this));
	}

	private final static Map<String, String> env = new HashMap<String, String>();
	static {
	    env.put("create", "true");
	}
	
	private final Map<String, byte[]> codeFromConcRTCache = Collections.synchronizedMap(new LRUCache<String, byte[]>(100));
	
	protected byte[] getCodeFromConcRuntime(String nameSlash) throws IOException{
		String fullname = "/" + nameSlash + ".class";
		
		byte[] ret = null;
		if(codeFromConcRTCache.containsKey(fullname)) {
			ret = codeFromConcRTCache.get(fullname);
		}
		else {
			for(String clsp : this.classpath){
	    		if(clsp.endsWith(".jar")) {
	    			
	    			synchronized(clsp) {
	    				String start = "jar:file:";
	        			if(!clsp.startsWith("/")) {
	        				start += "/";
	        			}
	        			
	        	        URI uri = URI.create((start + clsp).replace('\\', '/'));//TODO: some cache could be introduced here
	        			
	        	        try (FileSystem zipfs = FileSystems.newFileSystem(uri, env)) {
	        	            Path pathInZipfile = zipfs.getPath(fullname);
	        	            if(Files.exists(pathInZipfile)) {
	            				ret= Files.readAllBytes(pathInZipfile);
	            				break;
	        	            }
	        	        } 
	    			}
	    		}else {
	    			File f = new File(clsp + fullname);
	    			if(f.exists()){
	    				ret = Files.readAllBytes(f.toPath());
	    				break;
	    			}
	    		}
	    	}
			codeFromConcRTCache.put(fullname, ret);
		}
		return ret;
    }

	@Override
	public Detector getDetector() {
		return detector;
	}

	@Override
	public byte[] getBytecode(String name) {
		try{
			name = name.replace(".",  "/");
			byte[] code;
			try {
				code = getBytecodeSpecific(name);
			}catch(Exception e) {
				code = null;
			}
			
			if(code != null) {
				return code;
			}else{
				return getCodeFromConcRuntime(name);
			}
		}
		catch(Exception e){
			throw new RuntimeException("Unable to process bytecode for: " + name, e);
		}
	}
	
	public abstract byte[] getBytecodeSpecific(String name) throws IOException;
	
	@Override
	public HashMap<String, byte[]> getCpsStateClasses() {
		return cpsStateClasses;
	}
	
	private class RTClassloader extends ClassLoader{
		public Class<?> loadClass(String name) throws ClassNotFoundException {
			String nameSlash = name.replace(".", "/");
			byte[] code = cpsStateClasses.get(nameSlash);
			if(null != code){
				return super.defineClass(name, code, 0, code.length);
			}
			
	        return super.loadClass(name, false);
	    }
	}

	@Override
	public ClassLoader getParentCL() {
		if(null == rt){
			rt = new RTClassloader();
		}
		return rt; 
	}
	

	@Override
	public HashMap<String, HashSet<String>> getClassToGlobalDependancies() {
		return null;
	}

	@Override
	public Class<?> loadClassFromPrimordial(String className) throws ClassNotFoundException {
		return null;//Object.class;//all classes referenced are assumed to be primoridal unless static lambda as par below
	}

	public void setStaticLambdaClasses(HashSet<String> allStatLambdas) {
		this.allStatLambdas = allStatLambdas;
	}
	@Override
	public HashSet<String> getStaticLambdaClasses() {
		return allStatLambdas;
	}
	
	
}
