package com.concurnas.concc.utils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;

import com.concurnas.compiler.FileLoader;
import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;

public class ConccTestMockFileLoader implements FileLoader {

	public final FileSystem fs;
	
	public ConccTestMockFileLoader() {
		fs = Jimfs.newFileSystem(Configuration.unix());
	}
	
	
	@Override
	public boolean fileExists(String filename) {
		Path pp = getPath(filename);
		return pp != null && Files.exists(pp);
	}

	@Override
	public boolean directoryExists(String directory) {
		Path maybe = getPath(directory);
		return maybe != null && Files.isDirectory(maybe);
		//throw new RuntimeException("directoryExists not implemented");
	}

	@Override
	public String getCanonicalPath(String filename) throws IOException {
		Path pp = getPath(filename);
		return pp.toAbsolutePath().toString();
	}

	@Override
	public CharStream readFile(String filename) throws IOException {
		Path pp = getPath(filename);

		String ret = String.join("\n", Files.readAllLines(pp, StandardCharsets.UTF_8));
		
		return CharStreams.fromString(ret);
	}

	@Override
	public long lastModified(String filename) {
		throw new RuntimeException("lastModified not implemented");
	}

	@Override
	public Path getPath(String outputDirectory) {
		try {
			return fs.getPath(outputDirectory);
		}catch(InvalidPathException inv) {
			return null;
		}
	}

	@Override
	public String getSeparator() {
		return fs.getSeparator();
	}
}
