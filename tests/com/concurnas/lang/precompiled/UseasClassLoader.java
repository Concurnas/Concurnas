package com.concurnas.lang.precompiled;

import java.util.HashMap;

import com.concurnas.bootstrap.lang.Lambda.ClassRef;
import com.concurnas.runtime.ConcurnasClassLoader;
import com.concurnas.runtime.ClassloaderUtils.ClassProvider;

public class UseasClassLoader  extends ConcurnasClassLoader {
	
	ConcurnasClassLoader normalClassLoader = (ConcurnasClassLoader)UseasClassLoader.class.getClassLoader(); 
	
	HashMap<String, String> useAsMapping  = new HashMap<String, String>();
	
	public void useas( String fromx,  String useas){
		useAsMapping.put(useas, fromx);
	}
	
	private HashMap<String, Class<?>> definedalready = new HashMap<String, Class<?>>();
	
	public Class<?> loadClass( String name ) throws ClassNotFoundException  {
		Class<?> ret;
		
		if(definedalready.containsKey(name)){
			ret= definedalready.get(name);
		}else{
			if(useAsMapping.containsKey(name) ){
				String fromNamex = useAsMapping.get(name);
				byte[] bytez = normalClassLoader.getBytecode(fromNamex);
				bytez = ClassNameRemapper.remapClass(bytez, fromNamex, name);
				ret= super.defineClass(name, bytez);
			}
			else{
				ret= normalClassLoader.loadClass(name);
			}
			definedalready.put(name, ret);
		}
		
		return ret;
	}
	
	@Override
	public byte[] getBytecode(String className) {
		return normalClassLoader.getBytecode(className);
	}
	
}
