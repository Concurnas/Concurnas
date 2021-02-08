package com.concurnas.runtimeCache;

import java.util.HashMap;
import java.util.HashSet;

import org.objectweb.asm.Opcodes;
import com.concurnas.runtime.Cpsifier;

public class RuntimeCacheWeaver implements Opcodes {

	public BootstrapLoader rTJarEtcLoader;
	public Cpsifier cpsifier;

	public RuntimeCacheWeaver(BootstrapLoader rTJarEtcLoader, HashSet<String> findStaticLambdas){
		this.rTJarEtcLoader = rTJarEtcLoader;
		this.cpsifier = new Cpsifier(rTJarEtcLoader, findStaticLambdas);
	}
	
	private static HashSet<String> addToBooleanMethod = new HashSet<String>();
	static {
		addToBooleanMethod.add("java/util/AbstractCollection");
		addToBooleanMethod.add("java/util/AbstractMap");
		addToBooleanMethod.add("java/lang/String");
		addToBooleanMethod.add("java/lang/Boolean");
	}
	
	public HashMap<String, byte[]> weave(byte[] code, boolean log, boolean assumeNoPrimordials){
		String name = ClassNameFinder.getClassName(code);
		HashMap<String, byte[]> ret;
		if(name.startsWith("javafx/embed/swt/") 
				|| name.equals("java/lang/Object") 
				|| name.equals("java/lang/annotation/Annotation")
				|| name.equals("com/sun/imageio/plugins/tiff/TIFFT6Compressor")//err in transformation
				){
			ret = new HashMap<String, byte[]>();
			ret.put(name, code);
		}
		else{
			if(addToBooleanMethod.contains(name)) {
				code = ToBoolean.addToBoolean(name, code, rTJarEtcLoader);
			}
			
			ret = this.cpsifier.doCPSTransform(name, code, assumeNoPrimordials, log, true);//included global classes for module fields
		}
		
		return ret;
	}
	
	
}
