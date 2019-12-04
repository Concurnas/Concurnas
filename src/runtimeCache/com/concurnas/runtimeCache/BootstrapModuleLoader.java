package com.concurnas.runtimeCache;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.zip.ZipException;

public class BootstrapModuleLoader  extends BootstrapLoader{

	public BootstrapModuleLoader(String[] classpath, List<Path> modules) throws ZipException, IOException {
		super(classpath);
		
		loadAllModules(modules);
	}

	private final HashMap<String, Path> nameToLocation = new HashMap<String, Path>();
	
	private void loadAllModules(List<Path> modules) throws IOException {
		for(Path mod : modules) {
			int mlen = mod.toString().length()+1;
			Files.walk(mod).filter(a -> !Files.isDirectory(a) && a.toString().endsWith(".class") ).forEach(a -> {
				String nn = a.toString();
				String name = nn.substring(mlen,  nn.length()-6);
				nameToLocation.put(name, a);
			});
		}
	}



	@Override
	public byte[] getBytecodeSpecific(String name)  throws IOException{
		Path got = nameToLocation.get(name);
		if(got == null) {
			throw new RuntimeException("missing: " + name);
		}
		
		return Files.readAllBytes(got);
	}

}
