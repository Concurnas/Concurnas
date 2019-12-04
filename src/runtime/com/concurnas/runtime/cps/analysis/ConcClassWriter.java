package com.concurnas.runtime.cps.analysis;

import com.concurnas.runtime.cps.mirrors.Detector;

public class ConcClassWriter extends org.objectweb.asm.ClassWriter {
	private final Detector detector;
	
/*	public ClassWriter(final int flags, final ConcurnasClassLoader classLoader) {
		super(flags);
		this.detector = new Detector(new CachedClassMirrors(classLoader));
	}*/

	public ConcClassWriter(final int flags, final Detector detector) {
		super(flags);
		this.detector = detector;
	}

	protected String getCommonSuperClass(final String type1, final String type2) {
		try {
			//System.err.println("detect on: " + type1 + " -> " + type2);
			
			String x = detector.getCommonSuperClass(type1, type2);
			return x;
		} catch (com.concurnas.runtime.cps.mirrors.ClassMirrorNotFoundException e) {
			return "java/lang/Object";
		}
	}
	
}
