package com.concurnas.lang.precompiled;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.commons.Remapper;
import org.objectweb.asm.commons.RemappingClassAdapter;

public class ClassNameRemapper {
	
	public static byte[] remapClass(byte[] resource, String nameFrom, String nameTo) {
		ClassReader reader = new ClassReader(resource);

		ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
		ClassVisitor visitor = new RemappingClassAdapter(writer, new Remapper() {
			@Override
			public String map(String from) {
				if (from.equals(nameFrom)) {
					return nameTo;
				}
				return from;
			}
		});

		reader.accept(visitor, ClassReader.EXPAND_FRAMES);
		return writer.toByteArray();
	}

}
