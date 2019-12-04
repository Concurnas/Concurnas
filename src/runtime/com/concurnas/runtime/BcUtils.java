package com.concurnas.runtime;

import java.lang.reflect.Modifier;

import org.objectweb.asm.Type;

public class BcUtils {

	public static int incArgPointsFromDesc(String desc, int access) {
		Type[] args = Type.getArgumentTypes(desc);
		int incon = 0;
		for (int n = 0; n < args.length; n++) {
			incon += args[n].getSize();
		}

		if (!Modifier.isStatic(access)) {// not static thus first var is self reference
			incon++;
		}
		return incon;
	}

}
