package com.concurnas.runtimeCache;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.concurnas.runtime.Pair;

/**
 * We need to globalize all classes having a clinit method which either directly or indirectly refernces an invokedynamic instruction
 * 4 steps:
 * 1). Direct:   Identify all classes having a clinit which have a invokedynamic within them
 * 2). Indirect: find all classes having a clinit which references a static variable declared in a prevoiusly marked class
 * 3). Indirect: find all classes having a clinit calling a method (static or other) which has a invoke dynamic call
 * 4). Indirect: Any class inheriting a static field from a class with affected clinit - note: these intermediate classes need to be globalized too
 * 
 * Indirect calls require us to take a graph based approach, we build a dependency graph then work backwards from all the nodes
 * 
 */
public class StaticLambdaClassesFinder {
	private HashSet<String> classesWithAffectedClinit;
	private List<Path> modulePaths;
	private ProgressTracker pt;
	private String toDirectory;
	private BootstrapLoader rTJarEtcLoader;

	public StaticLambdaClassesFinder(ProgressTracker pt, HashSet<String> findStaticLambdas, List<Path> modulePaths, String toDirectory, BootstrapLoader rTJarEtcLoader) {
		this.pt = pt;
		this.classesWithAffectedClinit = findStaticLambdas;
		this.modulePaths = modulePaths;
		this.toDirectory = toDirectory;
		this.rTJarEtcLoader = rTJarEtcLoader; 
	}
	
	private void buildGraphAndGetDirectStaticLambdas(Path mod, String fname, int idx, StaticLambdaAndGraphBuilder graphBuilder) throws IOException {
		List<Path> entires = Files.walk(mod).filter(a -> !Files.isDirectory(a) ).collect(Collectors.toList());
		
		String shortname = mod.getFileName().toString();
		PctDoer tracker = new PctDoer(pt, entires.size(), 2, shortname + " - ", idx);
		
		Map<String, String> env = new HashMap<>(); 
        env.put("create", "true");
        
        String jarInit = "jar:file:";
        if(!this.toDirectory.startsWith("/")) {
        	jarInit += "/";
        }
        
        URI uri = URI.create((jarInit + this.toDirectory + File.separator + fname).replace('\\',  '/'));
		
		try (FileSystem zipfs = FileSystems.newFileSystem(uri, env)) {
			for(Path thing : entires) {
				String tfname = thing.getFileName().toString();
				if(tfname.endsWith(".class")) {
					byte[] code = Files.readAllBytes(thing);
					if(graphBuilder.hasStaticLambda(code, rTJarEtcLoader)) {
						classesWithAffectedClinit.add(ClassNameFinder.getClassName(code));
					}
				}
				
				tracker.entryDone();
			}
        }
		

		
	}
	
	static class ClassNode{
		public HashSet<Pair<ClassNode, Boolean>> nodesDependingOnMyStaticFields = new HashSet<Pair<ClassNode, Boolean>>();
		public HashMap<String, HashSet< Pair<ClassNode, String> >> callersOfMethod = new HashMap<String, HashSet< Pair<ClassNode, String> >>();
		
		public String myName;
		public HashSet<String> methodsHavingInvokeDynamic = new HashSet<String>();
		public ClassNode(String myName) {
			this.myName = myName;
		}
		
		@Override
		public String toString() {
			return String.format("%s:[%s]", myName, nodesDependingOnMyStaticFields.size());
		}
	}
	
	private HashMap<String, ClassNode> classNodes = new HashMap<String, ClassNode>();
	
	public final static HashSet<String> staticLamExclusions = new HashSet<String>();
	static {
		staticLamExclusions.add("java/io/ObjectInputFilter$Config");
		staticLamExclusions.add("java/util/concurrent/ConcurrentHashMap");
		staticLamExclusions.add("java/io/ObjectInputStream");
	}
	
	/**
	 * On finish findStaticLambdas will be augmneted with classes having an affected clinit, which need globalization
	 * @throws IOException
	 */
	public void go() throws IOException {
		StaticLambdaAndGraphBuilder graphBuilder = new StaticLambdaAndGraphBuilder(classNodes);
		
		for(Path mod : modulePaths) {
			String fname = mod.getFileName().toString() + ".jar";
			buildGraphAndGetDirectStaticLambdas(mod, fname, 0, graphBuilder);
		}
		

		classesWithAffectedClinit.removeAll(staticLamExclusions);
		//now we interogate classNameToDependors
		LinkedList<String> toProcess = new LinkedList<String>(classNodes.keySet());
		while(!toProcess.isEmpty()) {
			String inst = toProcess.pop();
			if(classesWithAffectedClinit.contains(inst)) {//'infect' all items that call clinit with lambda
				follow(inst, new HashSet<String>());
			}
			
			//'infect' all clinit which eventually call methods with lambdas
			ClassNode checkme = classNodes.get(inst);
			if(!checkme.methodsHavingInvokeDynamic.isEmpty()) {
				//check to see if there are any root callers from clinit methods
				for(String mNameAndSig : checkme.methodsHavingInvokeDynamic) {
					findClinitRoots(checkme, mNameAndSig, new HashSet<String>(), toProcess);
				}
			}
		}
		
		pt.onDone(0);
	}
	

	private void findClinitRoots(ClassNode checkme, String mNameAndSig, HashSet<String> curVisit, LinkedList<String> toProcess) {
		if(curVisit.contains(checkme.myName)) {
			return;//skip to avoid inf cycles
		}
		curVisit.add(checkme.myName);
		
		HashSet<Pair<ClassNode, String>> callers = checkme.callersOfMethod.get(mNameAndSig);
		if(null != callers) {
			for(Pair<ClassNode, String> caller : callers) {
				if(caller.getB().equals("<clinit>()V")) {
					String cname = caller.getA().myName;
					if(!classesWithAffectedClinit.contains(cname)) {
						classesWithAffectedClinit.add(cname);
						toProcess.add(cname);
					}
				}else {
					//normal method, follow this set...
					ClassNode dep = caller.getA();
					HashSet<Pair<ClassNode, String>> callersOfThisMethod = dep.callersOfMethod.get(caller.getB());
					if(null != callersOfThisMethod) {
						for(Pair<ClassNode, String> acaller : callersOfThisMethod) {
							findClinitRoots(acaller.getA(), acaller.getB(), curVisit, toProcess);
						}
					}
				}
			}
		}
	}
	
	private void follow(String inst, HashSet<String> curVisit) {
		if(curVisit.contains(inst)) {
			return;//skip to avoid inf cycles
		}
		curVisit.add(inst);
		
		ClassNode cnDeps = classNodes.get(inst);
		for(Pair<ClassNode, Boolean> dependorAndIsClinit : cnDeps.nodesDependingOnMyStaticFields) {
			String depName = dependorAndIsClinit.getA().myName;
			if(staticLamExclusions.contains(depName)) {
				break;
			}
			
			if(dependorAndIsClinit.getB()) {//its called by a <clinit>
				classesWithAffectedClinit.add(depName);
			}
			follow(depName, curVisit);
		}
		//curVisit.remove(inst);
	}
	
}
