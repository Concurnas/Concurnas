package com.concurnas.compiler.utils;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;

public class CompiledCodeClassLoader {
	private static ClassLoader cl;
	static {
		try
		{
			cl = new URLClassLoader(new URL[]{(new File(".")).toURI().toURL()});
		}
		catch(Exception e)
		{
			
		}
	}
	
	public final Class<?> moduleLevelCls;
	
	public CompiledCodeClassLoader(File compiledFileName) throws Exception 
	{	
		//moduleLevelCls = cl.loadClass(compiledFileName.toString().replaceAll("/", "."));
		moduleLevelCls=null;
		//TODO: CompiledCodeClassLoader
	}
	
	public Object getResource(ITEM_TYPE type, String remainingDottedName, boolean isChildClassAsking)
	{
		if(moduleLevelCls == null)
		{
			return null;
		}
		else if(null == remainingDottedName || remainingDottedName.equals(("")))
		{
			return moduleLevelCls;
		}
		else
		{
			return CompiledClassUtils.getResourceFromClass(moduleLevelCls, type, remainingDottedName, isChildClassAsking, true);
		}
		
	}

}
