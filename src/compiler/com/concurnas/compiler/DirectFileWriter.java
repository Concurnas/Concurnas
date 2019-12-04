package com.concurnas.compiler;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.stream.Stream;

public class DirectFileWriter implements FileWriter {

	private HashMap<String, String> nameToPath = new HashMap<String, String>();
	
	@Override
	public Path writeClass(String path, String name, byte[] bytecode) throws IOException {
		nameToPath.put(name,  path);
		Path pp = Paths.get(path);
		
		Files.createDirectories(pp.getParent());
		Files.write(pp, bytecode);
		return pp;
	}

	@Override
	public void removeClassAndAllNestedInstances(String path) {
		
		try {
			File f = new File(path);
			String abs = f.getCanonicalPath();
			final String starpath = abs.substring(0, abs.length()-6);
			
			Path root = Paths.get(abs.substring(0, abs.lastIndexOf(File.separator)+1));
			Stream<Path> stream =
	                Files.find(root, 1,
	                        (xx, basicFileAttributes) -> {
	                            File file = xx.toFile();
	                            try {
									if(!file.isDirectory() && file.getCanonicalPath().startsWith(starpath) && file.toString().endsWith(".class")) {
										return true;
									}
								} catch (IOException e) {
								}
	                            
	                            return false;
	                        });
	        stream.forEach(a -> a.toFile().delete());
	        stream.close();
		}catch(IOException e) {
			
		}
	}

	@Override
	public boolean hasWrittenClass(String name) {
		return nameToPath.containsKey(name);
	}

	@Override
	public byte[] getWrittenClass(String name) {
		try {
			return Files.readAllBytes(Paths.get(nameToPath.get(name)));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public Path getOutputPath(String path) throws IOException {
		return Paths.get(path);
	}

	@Override
	public Path getRoot() {
		return Paths.get(".");
	}
}
