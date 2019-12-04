package com.concurnas.compiler;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import org.antlr.v4.runtime.CharStream;

public interface FileLoader {
	public boolean fileExists(String filename); //exists and is file
	public boolean directoryExists(String directory);
	public String getCanonicalPath(String filename) throws IOException;
	
	public CharStream readFile(String filename) throws IOException;
	
	public long lastModified(String filename);
	
	public static final char pathSeparatorChar = File.separatorChar;
	public static final String pathSeparatorString = ""+pathSeparatorChar;
	/**
	 * 
	 * @param outputDirectory
	 * @return null if invalid syntax for path etc
	 */
	public Path getPath(String filename);
	public String getSeparator();
	
}
