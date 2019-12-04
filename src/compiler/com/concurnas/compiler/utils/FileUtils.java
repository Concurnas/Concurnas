package com.concurnas.compiler.utils;

import java.io.BufferedReader;
import java.io.FileReader;

public class FileUtils {
	public static String readFile(String filename) throws Exception
	{
		BufferedReader in = null;
		StringBuilder ret = new StringBuilder();
		try
		{
			in = new BufferedReader(new FileReader(filename));
			String ll = in.readLine();
			while(null != ll)
			{
				ret.append(ll);
				ll = in.readLine();
				if(null != ll )
				{
					ret.append("\n");
				}
			}
		}
		catch(Exception e)
		{
			try
			{
				in.close();
			}
			catch(Exception ee)
			{
			}
			throw e;
		}
		in.close();
		ret.append("\n");
		return ret.toString();
	}
}
