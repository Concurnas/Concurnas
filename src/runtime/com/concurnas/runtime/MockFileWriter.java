package com.concurnas.runtime;

import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedHashMap;

import com.concurnas.compiler.FileWriter;

public class MockFileWriter implements FileWriter {
	public LinkedHashMap <String, byte[]> nametoCode = new LinkedHashMap<String, byte[]> ();
	
	public byte[] getOutput()
	{
		return nametoCode.values().iterator().next();
	}
	
	@Override
	public Path writeClass(String path, String name, byte[] bytecode) {
		nametoCode.put(name,  bytecode);
		return null;
	}
	
	public LinkedHashMap<String, String> nameToJavaRep;
	
	@Override
	public void removeClassAndAllNestedInstances(String path) {
		
	}

	@Override
	public boolean hasWrittenClass(String name) {
		return nametoCode.containsKey(name);
	}

	@Override
	public byte[] getWrittenClass(String name) {
		return nametoCode.get(name);
	}

	@Override
	public Path getOutputPath(String path) throws IOException {
		return null;
	}

	@Override
	public Path getRoot() {
		return null;
	}
}
