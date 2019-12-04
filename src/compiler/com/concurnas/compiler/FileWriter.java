package com.concurnas.compiler;

import java.io.IOException;
import java.nio.file.Path;

public interface FileWriter {
	public Path writeClass(String path, String name, byte[] bytecode) throws IOException;

	public boolean hasWrittenClass(String name);
	public byte[] getWrittenClass(String name);
	
	/**
	 * Remove /a/b/c/myClass.class as well as /a/b/c/myClass$*.class etc
	 * @param path
	 */
	public void removeClassAndAllNestedInstances(String path);

	public Path getOutputPath(String path) throws IOException;

	public Path getRoot();
}
