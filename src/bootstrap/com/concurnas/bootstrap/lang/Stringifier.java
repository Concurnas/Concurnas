package com.concurnas.bootstrap.lang;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.AbstractCollection;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Stringifier {
	
	private static class NaturalOrderComparator implements Comparator<String>
	{
	    int compareRight(String a, String b)
	    {
	        int bias = 0;
	        int ia = 0;
	        int ib = 0;

	        // The longest run of digits wins. That aside, the greatest
	        // value wins, but we can't know that it will until we've scanned
	        // both numbers to know that they have the same magnitude, so we
	        // remember it in BIAS.
	        for (;; ia++, ib++)
	        {
	            char ca = charAt(a, ia);
	            char cb = charAt(b, ib);

	            if (!Character.isDigit(ca) && !Character.isDigit(cb))
	            {
	                return bias;
	            }
	            else if (!Character.isDigit(ca))
	            {
	                return -1;
	            }
	            else if (!Character.isDigit(cb))
	            {
	                return +1;
	            }
	            else if (ca < cb)
	            {
	                if (bias == 0)
	                {
	                    bias = -1;
	                }
	            }
	            else if (ca > cb)
	            {
	                if (bias == 0)
	                    bias = +1;
	            }
	            else if (ca == 0 && cb == 0)
	            {
	                return bias;
	            }
	        }
	    }

	    public int compare(String a, String b)
	    {
	        int ia = 0, ib = 0;
	        int nza = 0, nzb = 0;
	        char ca, cb;
	        int result;

	        while (true){
	            // only count the number of zeroes leading the last number compared
	            nza = nzb = 0;

	            ca = charAt(a, ia);
	            cb = charAt(b, ib);

	            // skip over leading spaces or zeros
	            while (Character.isSpaceChar(ca) || ca == '0')
	            {
	                if (ca == '0')
	                {
	                    nza++;
	                }
	                else
	                {
	                    // only count consecutive zeroes
	                    nza = 0;
	                }

	                ca = charAt(a, ++ia);
	            }

	            while (Character.isSpaceChar(cb) || cb == '0')
	            {
	                if (cb == '0')
	                {
	                    nzb++;
	                }
	                else
	                {
	                    // only count consecutive zeroes
	                    nzb = 0;
	                }

	                cb = charAt(b, ++ib);
	            }

	            // process run of digits
	            if (Character.isDigit(ca) && Character.isDigit(cb)){
	                if ((result = compareRight(a.substring(ia), b.substring(ib))) != 0)
	                {
	                    return result;
	                }
	            }

	            if (ca == 0 && cb == 0){
	                // The strings compare the same. Perhaps the caller
	                // will want to call strcmp to break the tie.
	                return nza - nzb;
	            }

	            if (ca < cb){
	                return -1;
	            }
	            else if (ca > cb){
	                return +1;
	            }

	            ++ia;
	            ++ib;
	        }
	    }

	    static char charAt(String s, int i)
	    {
	        if (i >= s.length()){
	            return 0;
	        }
	        else{
	            return s.charAt(i);
	        }
	    }
	}
	
	private static NaturalOrderComparator comp = new NaturalOrderComparator();
	
	public static String stringify(Object toStr){
		return stringify(toStr, false);
	}
	
	public static String stringify(Object toStr, boolean inArray){
		if(null == toStr){
			return "null";
		}
		else if(toStr instanceof List<?>){
			try {
				Method m = toStr.getClass().getMethod("toString");
				if(!m.getDeclaringClass().equals(AbstractCollection.class)){//defined class has a toString method which isnt the one from AbstractMap
					return toStr.toString(); 
				}
			} catch (Exception e) {
				//oh well
			}
			
			List<?> var = (List<?>)toStr;
			StringBuilder sb = new StringBuilder();
			//sb.append("ME_LIST:[");
			sb.append('[');
			int sz = var.size();
			int zsm1 = sz-1;
			for(int n=0; n < sz; n++){
				sb.append(stringify(var.get(n)));
				
				if(n != zsm1){
					sb.append(", ");
				}
			}
			sb.append(']');
			
			return sb.toString();
		}
		else if(toStr instanceof Set<?>){
			try {
				Method m = toStr.getClass().getMethod("toString");
				if(!m.getDeclaringClass().equals(AbstractCollection.class)){//defined class has a toString method which isnt the one from AbstractMap
					return toStr.toString(); 
				}
			} catch (Exception e) {
				//oh well
			}
			
			Set<?> var = (Set<?>)toStr;
			StringBuilder sb = new StringBuilder();
			sb.append('[');
			int sz = var.size();
			int zsm1 = sz-1;
			Object[] ar = var.toArray();
			for(int n=0; n < sz; n++){
				sb.append(stringify(ar[n]));
				
				if(n != zsm1){
					sb.append(", ");
				}
			}
			sb.append(']');
			
			return sb.toString();
		}
		else if(toStr instanceof Map<?,?>){
			try {
				Method m = toStr.getClass().getMethod("toString");
				if(!m.getDeclaringClass().equals(AbstractMap.class)){//defined class has a toString method which isnt the one from AbstractMap
					return toStr.toString(); 
				}
			} catch (Exception e) {
				//oh well
			}
			
			Map<?, ?> asMap = (Map<?, ?>)toStr;
			
			Set<?> keys = asMap.keySet();
			ArrayList<String> keysStr = new ArrayList<String>(keys.size());
			Map<String, Object> strToOrig = new HashMap<String, Object>();
			for(Object k : keys){
				String asStr = stringify(k);
				strToOrig.put(asStr, k);
				keysStr.add(asStr);
			}
			
			Collections.sort(keysStr, comp);
			int keyLen = keys.size();
			int keyLenM1 = keyLen-1;
			StringBuilder sb = new StringBuilder();
			sb.append('{');
			int n=0;
			for(Object key : keysStr ){
				Object oKey = strToOrig.get(key);
				sb.append(key);
				sb.append("->");//TODO: change to ->
				sb.append(stringify(asMap.get(oKey)));
				if(n++ != keyLenM1){
					sb.append(", ");
				}
			}
			sb.append('}');
			return sb.toString();
		}
		else if(toStr instanceof Annotation){
			Annotation anot = (Annotation)toStr;
			Class<? extends Annotation> annotType = anot.annotationType();
			StringBuilder sb = new StringBuilder("@"+annotType.getSimpleName());
			
			ArrayList<Method> methods = new ArrayList<Method>();
			for(Method mx : annotType.getDeclaredMethods()){
				methods.add(mx);
			}
			Collections.sort(methods, new Comparator<Object>(){//ensure consistant ordering of arguments
			       public int compare(Object o1, Object o2) {
			           return ((Method)o1).getName().compareTo(((Method)o2).getName());
				}});
			
			if(methods.size() > 0){
				sb.append("(");
				for(int mx=0; mx < methods.size(); mx++){
					Method kk = methods.get(mx);
					sb.append(kk.getName() + " = ");
					try {
						sb.append(Stringifier.stringify( kk.invoke(anot) ));//stringify does annotations already actually
					} catch (Throwable e) {
						sb.append("[]");
					}
					
					if(mx < methods.size()-1){
						sb.append(", ");
					}
				}
				sb.append(")");
			}
			return sb.toString();
		}
		else{
			Class<?> cls = toStr.getClass();
			if( cls.isArray()){
				
				boolean moreDimentions = cls.getComponentType().isArray();
				
				int arDim = Array.getLength(toStr);
				int arDimM1 = arDim-1;
				
				StringBuilder sb = new StringBuilder();
				if(!inArray) {
					sb.append('[');
				}
				
				for (int n = 0; n < arDim; n++) {
					sb.append(stringify(Array.get(toStr, n), true));
					if(n != arDimM1){
						sb.append(moreDimentions?" ; ":" ");
					}
				}
				if(!inArray) {
					sb.append(']');
				}
				
				return sb.toString();
			}
			else{
				return toStr.toString();
			}
		}
	}
	
	public static String stringify(Object[] toStr){
		if(null == toStr){
			return "null";
		}
		else{
			StringBuilder sb = new StringBuilder();
			sb.append('[');
			int sz = toStr.length;
			int zsm1 = sz-1;
			for(int n=0; n < sz; n++){
				sb.append(stringify(toStr[n]));
				
				if(n != zsm1){
					sb.append(" ");
				}
			}
			sb.append(']');
			
			return sb.toString();
		}
	}
	
	
	public static String stringify(boolean b) {
		return "" + b;
	}
	
	public static String stringify(char b) {
		return "" + b;
	}
	
	public static String stringify(double b) {
		return "" + b;
	}
	
	public static String stringify(float b) {
		return "" + b;
	}
	
	public static String stringify(int b) {
		return "" + b;
	}
	
	public static String stringify(long b) {
		return "" + b;
	}
	
	public static String stringify(String b) {
		return b;
	}
	
	//TODO: add shortcuts for int[], int[][][], double[] etc...
}
