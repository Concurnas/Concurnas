package com.concurnas.conc;

import java.io.File;
import java.util.ArrayList;

public class ConcInstance {
	
	public boolean helpMe;
	public ArrayList<String> classpath;
	public boolean serverMode;
	public String sourceFile = null;
	public String[] cmdLineArgs;
	public boolean werror;
	public boolean bytecode;
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		
		if(helpMe) {
			sb.append("--help ");
		}

		if(classpath != null) {
			sb.append("-cp ");
			sb.append(String.join(File.pathSeparator, classpath));
			sb.append(" ");
		}
		
		if(serverMode) {
			sb.append("-s ");
		}
		
		if(werror) {
			sb.append("-werror ");
		}
		
		if(bytecode) {
			sb.append("-bc ");
		}
		
		if(null != sourceFile) {
			sb.append(sourceFile);
			sb.append(" ");
		}
		
		if(null != cmdLineArgs) {
			for(int n = 0; n < cmdLineArgs.length; n++) {
				sb.append(cmdLineArgs[n]);
				if(n <= cmdLineArgs.length-1) {
					sb.append(" ");
				}
			}
		}
		
		return sb.toString().trim();
	}
}