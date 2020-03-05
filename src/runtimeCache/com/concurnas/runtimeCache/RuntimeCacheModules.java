package com.concurnas.runtimeCache;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

import com.concurnas.runtime.ConcurnasClassLoader;
import com.concurnas.runtime.OffHeapAugmentor;


/**
 * Perform caching in instance of modules
 * invoked only when caching process is run on jdk version 9+ (<=8 uses RuntimeCacheJars)
 */
public class RuntimeCacheModules extends RuntimeCache{
	
	public RuntimeCacheModules(String[] classpath, String toDirectory, boolean log) throws IOException {
		super(classpath, toDirectory, log);
	}

	private boolean shouldExclude(String fname, String fullPath) {
		if(fname.equals("module-info.class") ) {
			return true;
		}
		
		return false;
	}
	
	private void processInstance(ProgressTracker pt, Path mod, String fname, int idx) throws IOException {
		List<Path> entires = Files.walk(mod).filter(a -> !Files.isDirectory(a)).collect(Collectors.toList());
		
		String shortname = mod.getFileName().toString();
		PctDoer tracker = new PctDoer(pt, entires.size(), 2, shortname + " - ", idx);
		
		//new zip jar file system output
		ConcurnasClassLoader ccl = new ConcurnasClassLoader(new Path[]{});
		
		Map<String, String> env = new HashMap<>(); 
        env.put("create", "true");
        
        String jarInit = "jar:file:";
        if(!this.toDirectory.startsWith("/")) {
        	jarInit += "/";
        }
        
        URI uri = URI.create((jarInit + super.toDirectory + File.separator + fname).replace('\\',  '/'));
		
		try (FileSystem zipfs = FileSystems.newFileSystem(uri, env)) {
			for(Path thing : entires) {
		
				Path root=zipfs.getPath("/");
				
				String tfname = thing.getFileName().toString();
				
				if( shouldExclude(tfname,  thing.toString()) ) {
					//skip
				}
				else if(tfname.endsWith(".class")) {
					//augment
					byte[] code = Files.readAllBytes(thing);
					
					if(!thing.toString().equals("/modules/java.base/java/lang/Object.class")  ){
			    		code = OffHeapAugmentor.addOffHeapMethods(code, ccl);
			    	}
					HashMap<String, byte[]> nameToTrans;
					try {
						nameToTrans = weaver.weave(code, log, true);
					}catch(Throwable e) {
						System.err.println("Error in Processing: " + thing + ": " + e.getMessage());
						e.printStackTrace();
						continue;
					}
			    	for(String name: nameToTrans.keySet()){
			    		byte[] ccode = nameToTrans.get(name);
			    		Path inst = root.resolve(name + ".class");
			    		Files.createDirectories(inst.getParent());
			    		Files.write(inst, ccode);
			    	}
				}else {
					Path pathInJar=root;
					int cnt = 0;
					for(Path comp : thing) {
						if(cnt < 2) {
							cnt++;
							continue;
						}
						if(pathInJar == null) {
							pathInJar = comp;
						}else {
							pathInJar = pathInJar.resolve(comp.toString());
						}
						if(!Files.exists(pathInJar)) {
							Files.createDirectory(pathInJar);
						}
					}
					
					Files.copy( thing, pathInJar, StandardCopyOption.REPLACE_EXISTING ); 
				}
				
				tracker.entryDone();
				
			}
        }
		pt.onDone(idx);
	}
	
	private static Path modStrToURI(Module mod) {
		String name = mod.getName();
		Path ret = Paths.get(URI.create("jrt:/" + name));
		
		if(!ret.startsWith("/modules/")){
			ret = Paths.get(URI.create("jrt:/modules/" + name));
		}
		
		return ret;
	}
	
	@Override
	public int doAug(ExecutorService executorService, HashSet<String> findStaticLambdas) throws IOException {
		
		List<Path> modulePaths = ModuleLayer.boot().modules().stream().map(a-> modStrToURI(a) ).sorted().collect(Collectors.toList());
		super.assignProgressTracker(modulePaths.size()+1);
		
		super.modloader = new BootstrapModuleLoader(classpath, modulePaths);
		super.weaver = new Weaver(modloader);
		
		findStaticLambdas.add("java/util/concurrent/atomic/AtomicBoolean");
		
		StaticLambdaClassesFinder slcfinder = new StaticLambdaClassesFinder(super.pt, findStaticLambdas, modulePaths, super.toDirectory, super.weaver.rTJarEtcLoader);
		slcfinder.go();
		
		if(!findStaticLambdas.isEmpty()) {
			super.modloader.setStaticLambdaClasses(findStaticLambdas);
		}
		
		//findStaticLambdas.stream().sorted().forEach(System.out::println);
		
		int idx = 0;
		
		for(Path mod : modulePaths) {
			idx++;
			String fname = mod.getFileName().toString() + ".jar";
			final int ifxpass = idx;
			executorService.submit(() -> {
				try {
					processInstance(super.pt, mod, fname, ifxpass);
				} catch (Exception e) {
					e.printStackTrace();
				}
			});
			
			//processInstance(super.pt, mod, fname, ifxpass);
			
		}
		return modulePaths.size();
	}

	
}
