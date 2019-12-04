package com.concurnas.runtimeCache;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

public class BootstrapJarLoader extends BootstrapLoader{
	private final List<File> jdkjars;
	
	public BootstrapJarLoader(final ArrayList<File> foundJars, String[] classpath) throws ZipException, IOException{
		super(classpath);
		this.jdkjars = foundJars;
		loadJarsToLocations();
	}
	
	private class FileAndZipEntry{
		public final ZipFile file;
		public final ZipEntry entry;
		
		public FileAndZipEntry(final ZipFile file, final ZipEntry entry){
			this.file = file;
			this.entry = entry;
		}
	}
	
	private final HashMap<String, FileAndZipEntry> nameToLocation = new HashMap<String, FileAndZipEntry>();
	
	private void loadJarsToLocations() throws ZipException, IOException{
		for(File zfa : this.jdkjars){
			ZipFile zf = new ZipFile(zfa);
			Enumeration<?> entries = zf.entries();
			while(entries.hasMoreElements()){
				ZipEntry ze = (ZipEntry)entries.nextElement();
				if(!ze.isDirectory()){
					String name = ze.getName();
					if(name.endsWith(".class")){
						name = name.substring(0, name.length()-6);
						
						nameToLocation.put(name, new FileAndZipEntry(zf, ze));
					}
				}
			}
		}
	}
	
	@Override
	public byte[] getBytecodeSpecific(String name) throws IOException{
		FileAndZipEntry location = nameToLocation.get(name);
		if(null != location){
			byte[] code = JarLoadUtil.getCodeFromJar(location.file, location.entry);
			return code;
		}
		return null;
	}
}