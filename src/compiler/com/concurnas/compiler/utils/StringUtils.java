package com.concurnas.compiler.utils;

import java.util.Collection;
import java.util.Iterator;

public class StringUtils {
	public static String join(Collection<?> s, String delimiter) 
	{
	     StringBuilder builder = new StringBuilder();
	     Iterator<?> iter = s.iterator();
	     while (iter.hasNext()) 
	     {
	         builder.append(iter.next());
	         if (!iter.hasNext())
	         {
	           break;                  
	         }
	         builder.append(delimiter);
	     }
	     return builder.toString();
	 }
	
	public static String join(String[] items, int startIdx)
	{
		StringBuilder sb =  new StringBuilder();
		for(int n=startIdx; n < items.length; n++)
		{
			sb.append(items[n]);
			if(n != items.length-1)
			{
				sb.append('.');
			}
		}
		
		return sb.toString();
	}
}
