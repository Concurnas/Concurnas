package com.concurnas.runtime;

import java.util.HashMap;
import java.util.HashSet;

import com.concurnas.compiler.utils.BytecodePrettyPrinter;
import com.concurnas.runtime.cps.analysis.Fiberizer;
import com.concurnas.runtime.cps.mirrors.ClassMirror;
import com.concurnas.runtime.cps.mirrors.ClassMirrorNotFoundException;

public class Cpsifier {

	private static final HashSet<String> skip = new HashSet<String>();
	static {
		skip.add("com.concurnas.runtime.InitUncreatable");
		// skip.add("java/lang/Object");
	}

	/*
	 * private static final HashSet<String> nonPasuableMethods = new
	 * HashSet<String>(); static {
	 * nonPasuableMethods.add("java/lang/StringBuilder");
	 * nonPasuableMethods.add("java/lang/AbstractStringBuilder"); }
	 */

	// java.lang.StringBuilder , java.lang.AbstractStringBuilder

	public static HashSet<String> onlyMethods = null;// Hack for testing, remove

	private static boolean isTrait(ConcClassUtil clloader, String className) {
		try {
			ClassMirror cm = clloader.getDetector().classForName(className);
			if (null != cm) {
				HashSet<String> annots = cm.getAnnotations();
				if (annots.contains("com/concurnas/lang/Trait")) {
					return true;
				}

				/*
				 * if(null != annots) { for (int n = 0; n < annots.length; n++) { if
				 * (annots[n].equals("com/concurnas/lang/Trait")) { return true; } } }
				 */
			}

		} catch (ClassMirrorNotFoundException uhoh) {
		}

		return false;
	}


	private ConcClassUtil clloader;
	private HashSet<String> staticLambdaClasses;

	public void setConcurnasClassLoader(ConcClassUtil clloader) {
		this.clloader = clloader;
	}
	
	public Cpsifier(ConcClassUtil clloader, HashSet<String> findStaticLambdas) {
		this.clloader = clloader;
		this.staticLambdaClasses = findStaticLambdas;
	}
	
	public Cpsifier(ConcClassUtil clloader) {
		this(clloader, clloader.getStaticLambdaClasses());
	}
	
	public Cpsifier(HashSet<String> findStaticLambdas) {
		this.staticLambdaClasses = findStaticLambdas;
	}

	public Cpsifier() {
		
	}
	
	public HashMap<String, byte[]> doCPSTransform(String className, byte[] inputClassBytes, boolean assumeNoPrimordials, boolean verbose, boolean opOnBootstrap) {
		// TODO: remove assumeNoPrimordials once augmentation of rt.jar is
		// complete
		try {
			HashMap<String, byte[]> ret = new HashMap<String, byte[]>();

			if(opOnBootstrap && className.equals("sun/misc/Unsafe")) {
				//if Unsafe, redirect calls to Class.isHidden to POP; FALSE 
				inputClassBytes = UsafeIsHiddenRemapper.transform(inputClassBytes, clloader);
			}
			//System.err.println("proc: " + className);
			
			if (!skip.contains(className)) {

				//if (className.contains("NumberFormatProviderImpl") ) {
				//	System.out.println("prior weaving names: " + className);
				//	BytecodePrettyPrinter.print(inputClassBytes, true);
				//}
				
				HashMap<String, byte[]> globalizerClasses = null;
				if (assumeNoPrimordials || Globalizer.NoGlobalizationException(className) || isTrait(clloader, className)) {
					globalizerClasses = new HashMap<String, byte[]>();
					// we 'upgrade' the code to the latest spec... (which
					// includes stackframes etc)
					// some jdk code for whatever reason is in version 1.5
					if (assumeNoPrimordials && opOnBootstrap) {
						if (staticLambdaClasses != null) {
							// globalizerClasses.putAll(StaticLambdaHandler.redirectAndConvert(staticLambdaClasses,
							// code));

							MaxLocalFinderCache mlf = new MaxLocalFinderCache();
							Globalizer globalizer = new Globalizer(inputClassBytes, mlf, staticLambdaClasses, opOnBootstrap);
							globalizerClasses = globalizer.transform(className, clloader);

						} else {
							globalizerClasses.put(className, BCUpgrader.upgradeCode(className, inputClassBytes, clloader));
						}
					} else {
						globalizerClasses.put(className, BCUpgrader.upgradeCode(className, inputClassBytes, clloader));
					}
				} else {
					MaxLocalFinderCache mlf = new MaxLocalFinderCache();
					Globalizer globalizer = new Globalizer(inputClassBytes, mlf, null, opOnBootstrap);
					globalizerClasses = globalizer.transform(className, clloader);
				}

				CpsifierBCProvider bcp = new CpsifierBCProvider(clloader);

				for (String namea : globalizerClasses.keySet()) {
					bcp.overrideName(namea, globalizerClasses.get(namea));
				}

				for (String namea : globalizerClasses.keySet()) {
					byte[] code = globalizerClasses.get(namea);
					
					if (namea.contains("JustAnEnum")) {
						System.out.println("post global: " + namea);
						BytecodePrettyPrinter.print(code, true);
					}
					
					
					boolean isCObj = namea.equals("com/concurnas/bootstrap/runtime/cps/CObject");

					if (!isCObj) {
						ObjectInterceptor oic = new ObjectInterceptor(code, clloader, opOnBootstrap);
						code = oic.transform();
					}

					ReflectionHelperRedirector rhr = new ReflectionHelperRedirector(code, clloader);
					code = rhr.transform();// redirect reflection to version
											// that filters out augmented Fiber
											// methods

					// ObjectMethodAdder oma = new ObjectMethodAdder(code, clloader);
					// code = oma.transform();//TODO: should this be added to globalizers?
					
					InitConverter2 ic = new InitConverter2(code, bcp, namea, assumeNoPrimordials, opOnBootstrap);// isGlob =>globals already have init
					code = ic.transform();

					// if code is a child of IsoTask, gennerate methods: setup,
					// teardown [if missing]
					// static code analysis to prepopulate
					IsoTaskMethodAdder itgma = new IsoTaskMethodAdder(code, namea, clloader);
					if (!assumeNoPrimordials) {// disable from rt.jar augmentation (because there isnt any!)
						code = itgma.transform();
					}
					
					// add getGlobalDependancies$
					code = GetDependanciesMethodAdder.transform(code, clloader);
					
					
					/*
					 * if (namea.contains("java/lang/ProcessHandleImpl") ) {
					 * System.out.println("prior weaving names: " + namea);
					 * BytecodePrettyPrinter.print(code, true); }
					 */
					
					
					
					
					Fiberizer cw = new Fiberizer(code, clloader, assumeNoPrimordials, verbose);// , nonPasuableMethods.contains(className));
					cw.onlyMethods = onlyMethods;// TODO: remove testing hack
					
					try {
						code = cw.weave();
					} catch (Exception e) {
						//String err = BytecodePrettyPrinter.print(code, false);
						//System.err.println(String.format("Failure during concurnas runtime transformation in cps-ification of: %s as: %s\nCode:\n%s", namea, e.getMessage(), err));
						throw e;
					}
										
					//post fiber...
					code = AddDefaultMethodsToInterfaceForFiber.transform(code, clloader);

					/*
					  if (namea.contains("bytecodeSandbox")) {
					  System.out.println("post weaving names: " + namea);
					  BytecodePrettyPrinter.print(code, true); }
					 */

					/*
					 * if (namea.contains("bytecodeSandbox$XXX")) {
					 * System.out.println("post weaving names: " + namea);
					 * BytecodePrettyPrinter.print(code, true); }
					 */

					/*if (namea.contains("JustAnEnum")) {
						System.out.println("post weaving names: " + namea);
						BytecodePrettyPrinter.print(code, true);
					}*/

					ret.put(namea, code);
				}
			} else {
				ret.put(className, inputClassBytes);
			}

			return ret;
		} catch (Throwable thr) {
			//thr.printStackTrace();
			throw new RuntimeException(String.format("Error during concurrnas runtime bytecode transformation of '%s' - %s", className, thr.getMessage()), thr);
		}
	}


	public static int errcnt = 0;

}
