package com.concurnas.concc.utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.concurnas.compiler.FileWriter;

public class ConccTestMockFileWriter implements FileWriter{
	
	public final ConccTestMockFileLoader loader;
	
	public ConccTestMockFileWriter(ConccTestMockFileLoader loader) {
		this.loader = loader;
	}

	@Override
	public Path writeClass(String path, String name, byte[] bytecode) throws IOException {
		Path pp = this.loader.getPath(path);
		Files.createDirectories(pp.getParent());
		Files.write(pp, bytecode);
		return pp;
	}
	
	@Override
	public Path getOutputPath(String path) throws IOException {
		return this.loader.getPath(path);
	}

	@Override
	public void removeClassAndAllNestedInstances(String path) {
		
	}

	@Override
	public boolean hasWrittenClass(String name) {
		return false;
	}

	@Override
	public byte[] getWrittenClass(String name) {
		return null;
	}

	@Override
	public Path getRoot() {
		return this.loader.fs.getPath(".");
	}
}
