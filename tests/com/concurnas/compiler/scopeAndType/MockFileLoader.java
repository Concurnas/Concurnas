package com.concurnas.compiler.scopeAndType;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;

import com.concurnas.compiler.FileLoader;

public class MockFileLoader implements FileLoader{

	private final Map<String, String> fnToContents = new HashMap<String, String>();
	
	private final Set<String> directories = new HashSet<String>();

	private String srcDir;
	
	public MockFileLoader(String srcDir){
		this.srcDir = srcDir;
	}
	
	public ArrayList<String> getAllFiles(){
		ArrayList<String> files = new ArrayList<String>();
		for(String file : fnToContents.keySet()){
			files.add(file.substring(this.srcDir.length()+1));
		}
		Collections.sort(files);
		return files;
	}
	
	public void addFile(String filename, String contents)
	{
		fnToContents.put(filename, contents);
		
		String curDir = "";
		String[] elements = filename.split("\\" + FileLoader.pathSeparatorString);
		for(int n=0; n < elements.length-1; n++)
		{
			if(n !=0)
			{
				curDir += FileLoader.pathSeparatorChar;
			}
			curDir += elements[n];
			directories.add(curDir);
		}
	}
	//getcontents
	
	@Override
	public boolean fileExists(String filename) {
		return fnToContents.containsKey(filename);
	}

	@Override
	public boolean directoryExists(String directory) {
		return directories.contains(directory);
	}

	@Override
	public String getCanonicalPath(String filename) throws IOException {
		return filename;
	}

	@Override
	public CharStream readFile(String filename) throws IOException {
		CharStream ret = CharStreams.fromString(fnToContents.get(filename));
		//ret.name = filename;
		return ret;
	}

	@Override
	public long lastModified(String filename) {
		return 0;
	}
	
	@Override
	public Path getPath(String outputDirectory) {
		return null;
	}
	
	@Override
	public String getSeparator() {
		return File.separator;
	}
	
}
