package com.concurnas.runtimeCache;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class RuntimeCacheCreator {

	
	public static void makeCache(String[] cp, Path outputToDir, boolean modules) throws Exception {
		RuntimeCache rc;
		
		if(!modules) {
			rc = new RuntimeCacheJars(cp, outputToDir.toAbsolutePath().toString(), false);
		}else {
			rc = new RuntimeCacheModules(cp, outputToDir.toAbsolutePath().toString(), false);
		}

		rc.doAgumentation(modules);
	}
	
	public static void main(String[] args) throws Exception {
		String vendor = System.getProperty("java.vendor");
		String version = System.getProperty("java.specification.version");
				
		if(null == vendor || !vendor.equals("Oracle Corporation")) {
			String vmname = System.getProperty("java.vm.name");
			if(null == vmname || !vmname.contains("OpenJDK")) {
				System.err.println(String.format("WARN: Only Oracle JVM is supported, results with %s are unknown", vendor));
			}
		}
		
		boolean modules = true;
		try {
			double versiond = Double.parseDouble(version);
			if(versiond < 1.9) {
				modules = false;
			}
		}catch(Exception nfe) {
			System.err.println(String.format("WARN: Unable to determine JVM specification from version String: %s - assuming Java 9+", version));
		}
		
		String binDir = "build/classes/java/main";
		if(Files.exists(Paths.get("bin/main"))) {
			binDir = "bin/main";
		}
		
		makeCache(new String[]{binDir}, Paths.get("./installed"), modules);
	}
}
