package com.concurnas.runtimeCache;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class JarFinder {

	public static final ArrayList<File> findJars(File location) throws IOException{
		ArrayList<File> foundJars = new ArrayList<File>();
		findJars(location, foundJars);
		return foundJars;
	}
	
	private final static void findJars(final File f, final ArrayList<File> foundJars) throws IOException {
		if (f.isDirectory()) {
			final File[] childs = f.listFiles();
			for (File child : childs) {
				findJars(child, foundJars);
			}
			return;
		}
		if (f.getName().endsWith("jar")) {
			foundJars.add(f);
		}
	}
	
}
