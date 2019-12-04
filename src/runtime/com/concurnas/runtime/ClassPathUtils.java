package com.concurnas.runtime;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ClassPathUtils {

	public static String[] getSystemClassPathAsStrings() {
		String classpath = System.getProperty("java.class.path");
		return classpath.split(File.pathSeparator);
	}
	
	public static Path[] getSystemClassPathAsPaths() {
		String[] elms = getSystemClassPathAsStrings();
		Path[] entries = new Path[elms.length];
		for(int n=0; n < elms.length; n++) {
			entries[n] = Paths.get(elms[n]);
		}
		return entries;
	}
	
	public static Path[] getInstallationPath() {
		String rtCache = System.getProperty("com.concurnas.rtCache");
		if(null == rtCache) {
			rtCache = "installed";
		}
		return new Path[] {Paths.get(rtCache)};
	}
}
