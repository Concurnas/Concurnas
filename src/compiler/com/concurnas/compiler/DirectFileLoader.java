package com.concurnas.compiler;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;


public class DirectFileLoader implements FileLoader{

	@Override
	public boolean fileExists(String filename) {
		File fn = new File(filename);
		return fn.exists() && fn.isFile();
	}

	@Override
	public boolean directoryExists(String directory) {
		File fn = new File(directory);
		return fn.exists() && fn.isDirectory();
	}

	@Override
	public String getCanonicalPath(String filename) throws IOException {
		return (new File(filename)).getCanonicalPath();
	}

	@Override
	public CharStream readFile(String filename) throws IOException {
		return CharStreams.fromFileName(this.getCanonicalPath(filename));
	}

	@Override
	public long lastModified(String filename) {
		return (new File(filename)).lastModified();
	}
	
	@Override
	public Path getPath(String outputDirectory) {
		return Paths.get(outputDirectory);
	}
	
	@Override
	public String getSeparator() {
		return File.separator;
	}

}
