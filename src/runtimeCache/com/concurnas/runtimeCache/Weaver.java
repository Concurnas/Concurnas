package com.concurnas.runtimeCache;

import java.util.HashMap;
import org.objectweb.asm.Opcodes;
import com.concurnas.runtime.Cpsifier;

public class Weaver implements Opcodes {

	public BootstrapLoader rTJarEtcLoader;

	public Weaver(BootstrapLoader rTJarEtcLoader){
		this.rTJarEtcLoader = rTJarEtcLoader;
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
			if(name.equals("java/util/AbstractCollection") ||  name.equals("java/util/AbstractMap")){
				code = ToBoolean.addToBoolean(name, code, rTJarEtcLoader, false);
			}
			if(name.equals("java/lang/String")  ){
				code = ToBoolean.addToBoolean(name, code, rTJarEtcLoader, true);
			}
			
			ret = Cpsifier.doCPSTransform(rTJarEtcLoader, name, code, assumeNoPrimordials, log, true);//included global classes for module fields
		}
		
		return ret;
	}
	
	
}
