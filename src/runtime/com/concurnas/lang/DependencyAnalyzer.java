package com.concurnas.lang;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.TypePath;
import org.objectweb.asm.signature.SignatureReader;
import org.objectweb.asm.signature.SignatureVisitor;

import com.concurnas.runtime.ConcurnasClassLoader;
import com.concurnas.runtime.cps.analysis.ConcClassWriter;

/**
 * Extract the likely, but possibly non complete list of dependencies of a class (including transitive dependencies).
 * 
 * This works within Concurnas only for classes which are user defined (or from libraries etc). Classes which are not
 * user defined (e.g. java/lang/Object) will not be included in the dependency map.
 * 
 * @author Jason
 *
 */
public class DependencyAnalyzer implements Opcodes{
	
	/**
	 * @param aClass the class to analyse
	 * @return a map from class name to bytecode
	 */
	public static Map<String, byte[]> getDependenciesOf(Class<?> aClass){
		ClassLoader cl = aClass.getClassLoader();
		if(!(cl instanceof ConcurnasClassLoader)) {
			throw new RuntimeException("Classloader for requested class: " + aClass + " must be of type: ConcurnasClassLoader, but it's of type: " + cl);
		}
		
		ConcurnasClassLoader clloader = (ConcurnasClassLoader)cl;
		
		LinkedHashMap<String, byte[]> alreadyProcessed = new LinkedHashMap<String, byte[]>();
		LinkedList<String> toProcess = new LinkedList<String>();
		toProcess.add(aClass.getName());
		
		while(!toProcess.isEmpty()) {
			String toProc = toProcess.pop();
			if(toProc == null || alreadyProcessed.containsKey(toProc)) {
				continue;
			}

			//System.err.println(" what: " + toProc);
			
			byte[] code = clloader.getBytecode(toProc, false);
			alreadyProcessed.put(toProc, code);
			if(code != null) {
				//System.err.println("~what: " + toProc);
				ClassReader cr = new ClassReader(code);
				//ClassWriter cw = new ConcClassWriter(ClassWriter.COMPUTE_MAXS|ClassWriter.COMPUTE_FRAMES, clloader.getDetector());//NOT doing frames etc here, ok i guess as nothing changes
				DependencyFinderClass depf = new DependencyFinderClass(/* cw */);
				cr.accept(depf, 0);
				toProcess.addAll(depf.dependencies);
			}
		}
		
		{
			List<String> toremove = alreadyProcessed.keySet().stream().filter(a -> alreadyProcessed.get(a) == null).collect(Collectors.toList());
			for(String rem : toremove) {
				alreadyProcessed.remove(rem);
			}
		}
		
		return alreadyProcessed;
	}
	
	private static class DependencyTracker{
		private final LinkedHashSet<String> dependencies;
		
		public DependencyTracker(LinkedHashSet<String> dependencies) {
			this.dependencies = dependencies;
		}
		
		private void addToDeps(Object value) {
			if(value != null) {
				dependencies.add(value.getClass().getName());
			}
		}
		private void addToDeps(String signature, String desc) {
			if(signature == null) {
				signature = desc;
			}
			final SignatureReader signatureReader = new SignatureReader(signature);
			SignatureVisitor visitor = new MethodSignatureVisitor(this.dependencies);
			signatureReader.accept(visitor);
		}
		
		private void addToDeps(String desc) {
			addToDeps(desc, desc);
		}
		private void add(String type) {
			dependencies.add(type);
		}
		
	}
	
	private static class MethodSignatureVisitor extends SignatureVisitor {
		private LinkedHashSet<String> dependencies;

		public MethodSignatureVisitor(LinkedHashSet<String> dependencies) {
			super(ASM7);
			this.dependencies = dependencies;
		}
		
	    public void visitFormalTypeParameter(String name) {
	    	dependencies.add(name);
	    }

	    public void visitTypeVariable(String name) {
	    	dependencies.add(name);
	    }
	    
	    public void visitClassType(String name) {
	    	dependencies.add(name);
	    }

	    public void visitInnerClassType(String name) {
	    	dependencies.add(name);
	    }
	}
	
	
	private static class DependencyFinderAnnotation extends AnnotationVisitor{
		private final DependencyTracker dependencyTracker;

		public DependencyFinderAnnotation(AnnotationVisitor av, DependencyTracker dependencyTracker) {
			super(ASM7, av);
			this.dependencyTracker=dependencyTracker;
		}
		
		public void visit(String name, Object value) {
			dependencyTracker.addToDeps(value);
			super.visit(name, value);
	    }

	    public void visitEnum(String name, String desc, String value) {
	    	dependencyTracker.addToDeps(desc);
	        if (av != null) {
	            av.visitEnum(name, desc, value);
	        }
	    }

	    public AnnotationVisitor visitAnnotation(String name, String desc) {
	    	dependencyTracker.addToDeps(desc);
	    	return new DependencyFinderAnnotation(av.visitAnnotation(name, desc), dependencyTracker);
	    }
	}
	
	private static class DependencyFinderfield extends FieldVisitor{
		private final DependencyTracker dependencyTracker;

		public DependencyFinderfield(FieldVisitor fv, DependencyTracker dependencyTracker) {
			super(ASM7, fv);
			this.dependencyTracker=dependencyTracker;
		}
		
		public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
			dependencyTracker.addToDeps(desc);
	    	return new DependencyFinderAnnotation(super.visitAnnotation(desc, visible), dependencyTracker);
	    }

		public AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath, String desc, boolean visible) {
			dependencyTracker.addToDeps(desc);
			return new DependencyFinderAnnotation(super.visitTypeAnnotation(typeRef, typePath, desc, visible), dependencyTracker);
		}
	}
	
	private static class DependencyFinderMethod extends MethodVisitor{
		private final DependencyTracker dependencyTracker;
		
		public DependencyFinderMethod(MethodVisitor mv, DependencyTracker dependencyTracker) {
			super(ASM7, mv);
			this.dependencyTracker=dependencyTracker;
		}

		public AnnotationVisitor visitAnnotationDefault() {
			return new DependencyFinderAnnotation(super.visitAnnotationDefault(), dependencyTracker);
		}

		public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
			dependencyTracker.addToDeps(desc);
			return new DependencyFinderAnnotation(super.visitAnnotation(desc, visible), dependencyTracker);
		}

		public AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath, String desc, boolean visible) {
			dependencyTracker.addToDeps(desc);
			return new DependencyFinderAnnotation(super.visitTypeAnnotation(typeRef, typePath, desc, visible), dependencyTracker);
		}

		public AnnotationVisitor visitParameterAnnotation(int parameter, String desc, boolean visible) {
			dependencyTracker.addToDeps(desc);
			return new DependencyFinderAnnotation(super.visitParameterAnnotation(parameter, desc, visible), dependencyTracker);
		}

		public void visitTypeInsn(int opcode, String type) {
			dependencyTracker.add(type);
		}

		public void visitFieldInsn(int opcode, String owner, String name, String desc) {
			dependencyTracker.add(owner);
			dependencyTracker.addToDeps(desc);
		}

		public void visitMethodInsn(int opcode, String owner, String name, String desc) {
			dependencyTracker.add(owner);
			dependencyTracker.addToDeps(desc);
		}

		public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
			dependencyTracker.add(owner);
			dependencyTracker.addToDeps(desc);
		}

		public void visitInvokeDynamicInsn(String name, String desc, Handle bsm, Object... bsmArgs) {
			if(bsm != null) {
				dependencyTracker.add(bsm.getOwner());
				dependencyTracker.addToDeps(bsm.getDesc());
			}
			dependencyTracker.addToDeps(desc);
			
			for(Object bsmArg : bsmArgs) {
				dependencyTracker.addToDeps(bsmArg);
			}
		}

		public void visitMultiANewArrayInsn(String desc, int dims) {
			dependencyTracker.addToDeps(desc);
		}

		public AnnotationVisitor visitInsnAnnotation(int typeRef, TypePath typePath, String desc, boolean visible) {
			dependencyTracker.addToDeps(desc);
			return new DependencyFinderAnnotation(super.visitInsnAnnotation(typeRef, typePath, desc, visible), dependencyTracker);
		}

		public void visitTryCatchBlock(Label start, Label end, Label handler, String type) {
			dependencyTracker.add(type);
		}

		public AnnotationVisitor visitTryCatchAnnotation(int typeRef, TypePath typePath, String desc, boolean visible) {
			dependencyTracker.addToDeps(desc);
			return new DependencyFinderAnnotation(super.visitTryCatchAnnotation(typeRef, typePath, desc, visible), dependencyTracker);
		}
		
		public void visitLocalVariable(String name, String desc, String signature, Label start, Label end, int index) {
			dependencyTracker.addToDeps(signature, desc);
		}

		public AnnotationVisitor visitLocalVariableAnnotation(int typeRef, TypePath typePath, Label[] start, Label[] end, int[] index, String desc, boolean visible) {
			dependencyTracker.addToDeps(desc);
			return new DependencyFinderAnnotation(super.visitLocalVariableAnnotation(typeRef, typePath, start, end, index, desc, visible), dependencyTracker);
		}
	}
	
	
	private static class DependencyFinderClass extends ClassVisitor{
		
		public final LinkedHashSet<String> dependencies;
		private final DependencyTracker dependencyTracker;
		
		public DependencyFinderClass(/* ClassVisitor cw */){
			super(ASM7/* , cw */);
			dependencies = new LinkedHashSet<String>();
			dependencyTracker = new DependencyTracker(dependencies);//(new LinkedHashSet<String>();
		}
		
		
		public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
			dependencies.add(superName);
			for(String iface : interfaces) {
				dependencies.add(iface);
			}
			super.visit(version, access, name, signature, superName, interfaces);
		}

		public void visitOuterClass(String owner, String name, String desc) {
			dependencies.add(owner);
			dependencyTracker.addToDeps(desc);
			super.visitOuterClass(owner, name, desc);
		}

		public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
			dependencyTracker.addToDeps(desc);
			return new DependencyFinderAnnotation(super.visitAnnotation(desc, visible), dependencyTracker);
		}

		public AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath, String desc, boolean visible) {
			dependencyTracker.addToDeps(desc);
			return new DependencyFinderAnnotation(super.visitTypeAnnotation(typeRef, typePath, desc, visible), dependencyTracker);
		}

		/*public void visitInnerClass(String name, String outerName, String innerName, int access) {
			dependencies.add(name);
			dependencies.add(outerName);
			dependencies.add(innerName);
			super.visitInnerClass(name, outerName, innerName, access);
		}*/

		public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
			dependencyTracker.addToDeps(desc);
			dependencyTracker.addToDeps(value);
			
			return new DependencyFinderfield(super.visitField(access, name, desc, signature, value), dependencyTracker);
		}

		public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
			dependencyTracker.addToDeps(signature, desc);
			
			if(null != exceptions) {
				for(String excep : exceptions) {
					dependencies.add(excep);
				}
			}
			
			return new DependencyFinderMethod(super.visitMethod(access, name, desc, signature, exceptions), dependencyTracker);
		}
	}
}
