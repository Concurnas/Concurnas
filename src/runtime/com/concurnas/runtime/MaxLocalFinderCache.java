package com.concurnas.runtime;

import java.lang.reflect.Modifier;
import java.util.HashMap;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.AnalyzerAdapter;


public class MaxLocalFinderCache {

	private static class MaxLocalFinder extends ClassVisitor implements Opcodes {

		public HashMap<String, Integer> maxlocalMap = new HashMap<String, Integer>();
		private final String owner;
		
		public MaxLocalFinder(String owner) {
			super(ASM7);
			this.owner=owner;
		}
		
		private class MaxiMethod extends AnalyzerAdapter{
			private final String name;
			private final String desc;
			public MaxiMethod(String owner, int access, String name, String desc, MethodVisitor mv) {
				super(ASM7, owner, access, name, desc, mv);
				this.name=name;
				this.desc=desc;
				if(Modifier.isAbstract(access) || Modifier.isNative(access) || Modifier.isSynchronized(access) ){
					maxlocalMap.put(name+desc, 0);//visitMaxs not called below for abstracts, so they get zero vars
				}
			}
			
			public void visitMaxs(final int maxStack, final int maxLocals) {
				maxlocalMap.put(name+desc, maxLocals);
				super.visitMaxs(maxStack, maxLocals);//note this does not get called on abstract methods
			}
			
		}
		
	    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
	        return new MaxiMethod(owner, access, name, desc, super.visitMethod(access, name, desc, signature, exceptions));
	    }
	}
	
	private final HashMap<String, HashMap<String, Integer>> localMaps = new HashMap<String, HashMap<String, Integer>>();
	
	public HashMap<String, Integer> getMaxlocalMap(String name, ClassReader cr){
		HashMap<String, Integer> ret = localMaps.get(name);
		if(null == ret){
			MaxLocalFinder mlf = new MaxLocalFinder(name);
			cr.accept(mlf, ClassReader.EXPAND_FRAMES);
			ret = mlf.maxlocalMap;;
			localMaps.put(name, ret);
		}
		return ret;
	}
	
}
