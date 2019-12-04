package com.concurnas.runtime;

import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.zip.ZipException;

import com.concurnas.conc.ConcUtis;

public class ClassloaderUtils {
	public static void populateClasses(Path root, Path loc, HashMap<String, ClassProvider> clses, boolean firstClass){
		try{
			if(Files.exists(loc)) {
				if(Files.isDirectory(loc)){
					Files.list(loc).forEach(a -> populateClasses(root, a, clses, false));
				}
				else{
					String name = loc.toString();
					if(name.endsWith(".class")){
						String pkgFullName;
						if(firstClass) {
							byte[] codex = Files.readAllBytes(loc);
							pkgFullName = ConcUtis.extractClassName(codex);
						}else {
							pkgFullName = loc.toString();
							pkgFullName = pkgFullName.substring(root.toString().length()+1, pkgFullName.length() - 6);
							pkgFullName = pkgFullName.replace("\\", "/");
						}
						
						
						clses.put(pkgFullName, new FileCP(loc));
					}
					else if (name.endsWith(".jar")){
						JarCP jarcp = new JarCP(loc);
						jarcp.augmentclassPath(clses);
					}
				}
			}
		}
		catch(Exception e){
			e.printStackTrace();
			System.out.println(String.format("WARN: Partial failure to process classpath element %s under %s due to: %s", root, loc, e.getMessage()));
		}
	}
	
	
	public abstract static class ClassProvider{
		public abstract byte[] provide(String x) throws IOException;
	}
	
	public static class FixedCP extends ClassProvider{
		private byte[] fixed;

		public FixedCP(byte[] fixed){
			this.fixed = fixed;
		}
		public byte[] provide(String f) throws IOException{
			return fixed;
		}
	}
	
	public static class FileCP extends ClassProvider{
		private Path fl;

		public FileCP(Path x){
			this.fl = x;
		}
		
		public byte[] provide(String f) throws IOException{
			//TODO: some should be cached?
			return Files.readAllBytes(this.fl);
		}
	}
	
	public static class JarCP extends ClassProvider{
		private URI jarUri;

		private static final Map<String, String> env = new HashMap<String, String>();
		static {
			 env.put("create", "true");
		}
		
		public JarCP(Path pp) throws ZipException, IOException{
			this.jarUri = URI.create("jar:" + pp.toUri());
		}

		//used to ensure we don't hit FileSystemAlreadyExistsException on seperate conclassloader instances
		public static final Map<URI, URI> strToURI  = Collections.synchronizedMap(new WeakHashMap<URI, URI>());
		
		@Override
		public byte[] provide(String x) throws IOException {
	        
			synchronized(strToURI.computeIfAbsent(jarUri, (URI a) -> a)) {
				try (FileSystem zipfs = FileSystems.newFileSystem(jarUri, env)) {
					return Files.readAllBytes(zipfs.getPath("/" + x + ".class"));
		        } catch(Exception e) {
		        	e.printStackTrace();
		        	throw e;
		        }
			}
			
		}
		
		private void addToClsP(String classname, HashMap<String, ClassProvider> clsToClasspath) {
			clsToClasspath.put(classname.substring(1, classname.length()-6), this);
		}
		
		public void augmentclassPath(HashMap<String, ClassProvider> clsToClasspath){

			synchronized(strToURI.computeIfAbsent(jarUri, (URI a) -> a)) {
				try (FileSystem zipfs = FileSystems.newFileSystem(jarUri, env)) {
					Path root = zipfs.getPath("/");
					Files.walk(root).filter(a -> !Files.isDirectory(a) && a.toString().endsWith(".class")).forEach(a -> addToClsP(a.toString(), clsToClasspath));
		        } catch (IOException e) {
					e.printStackTrace();
				} 
			}
		}
	}
	
}
