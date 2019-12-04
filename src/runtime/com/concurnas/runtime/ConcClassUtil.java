package com.concurnas.runtime;

import java.util.HashMap;
import java.util.HashSet;

import com.concurnas.runtime.cps.mirrors.Detector;

public interface ConcClassUtil {
	public Detector getDetector();
	public HashMap<String, HashSet<String>> getClassToGlobalDependancies();
	public byte[] getBytecode(String name);
	public Class<?> loadClassFromPrimordial(String className) throws ClassNotFoundException;
	//public boolean isPrimodrialClass(String className);
	public HashMap<String, byte[]> getCpsStateClasses();
	public ClassLoader getParentCL();
	public HashSet<String> getStaticLambdaClasses();
	
	
}
