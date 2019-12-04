package com.concurnas.concc;

import java.io.File;
import java.util.ArrayList;

public class ConccInstance {
	
	public boolean helpMe;
	public String globalRoot;
	public ArrayList<String> classpath;
	public String outputDirectory;
	public ArrayList<SourceToCompile> sources = new ArrayList<SourceToCompile>();
	public boolean warnAsError=false;
	public boolean verbose=false;
	public boolean copyall;
	public String outputJar;
	public boolean doCleanup;
	public String mfestEntryPoint;
	
	public static class SourceToCompile{
		public String root;
		public String dirOrFile;

		public SourceToCompile(String root, String dirOrFile) {
			this.root = root;
			this.dirOrFile = dirOrFile;
		}
		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder();
			
			if(root != null) {
				sb.append(root);
				sb.append("[");
			}
			
			sb.append(dirOrFile);
			
			if(root != null) {
				sb.append("]");
			}
			
			return sb.toString();
		}
	}
	
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		
		if(helpMe) {
			sb.append("--help ");
		}

		if(warnAsError) {
			sb.append("-werror ");
		}
		
		if(verbose) {
			sb.append("-verbose ");
		}
		
		if(copyall) {
			sb.append("-a ");
		}
		
		if(doCleanup) {
			sb.append("-clean ");
		}
		
		if(null != outputJar) {
			sb.append("-jar " + outputJar);
			if(null != mfestEntryPoint) {
				sb.append("(" + mfestEntryPoint + ")");
			}
			sb.append(" ");
		}
		
		if(classpath != null) {
			sb.append("-cp ");
			sb.append(String.join(File.pathSeparator, classpath));
			sb.append(" ");
		}
		
		if(outputDirectory != null) {
			sb.append("-d ");
			sb.append(outputDirectory);
			sb.append(" ");
		}
		
		if(globalRoot != null) {
			sb.append("-root ");
			sb.append(globalRoot);
			sb.append(" ");
		}
		
		ArrayList<String> srcStrings = new ArrayList<String>(sources.size());
		
		for(SourceToCompile src : sources) {
			srcStrings.add(src.toString());
		}
		
		sb.append(String.join(" ", srcStrings));
		
		return sb.toString();
	}
	
}
