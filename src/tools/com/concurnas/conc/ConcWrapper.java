package com.concurnas.conc;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.concurnas.runtimeCache.ReleaseInfo;
import com.concurnas.runtimeCache.RuntimeCacheCreator;

/**
 * Ensure that Concurnas has cached enviroment before progressing with with
 * concc, conc or repl...
 * 
 * TODO: requires better unit tests
 */
public class ConcWrapper {

	private static String createJVMArgs(Path dir, boolean modules) throws IOException {
		if (modules) {
			List<Path> files = Files.walk(dir).filter(a -> !Files.isDirectory(a) && a.toString().endsWith(".jar")).sorted().collect(Collectors.toList());
			List<String> jars = files.stream().map(a -> a.toAbsolutePath().toString()).collect(Collectors.toList());
			List<String> justnames = files.stream().map(a -> a.getFileName().toString()).map(a -> a.substring(0, a.length() - 4)).collect(Collectors.toList());

			StringBuilder sb = new StringBuilder();

			int n = 0;
			for (String jar : jars) {
				String justjar = justnames.get(n++);
				//--patch-module java.base=./installed/java.base.jar 
				//--patch-module java.compiler=./installed/java.compiler.jar 
				//...etc...
				sb.append(String.format("--patch-module=%s=%s\n", justjar, jar));
			}

			sb.append(String.format("--add-exports=java.base/com.concurnas.bootstrap.runtime.cps=ALL-UNNAMED,%s\n", String.join(",", justnames)));

			sb.append("--add-exports=java.base/com.concurnas.bootstrap.lang.offheap=java.instrument\n");
			sb.append("--add-exports=java.base/com.concurnas.bootstrap=ALL-UNNAMED\n");
			sb.append("--add-exports=java.base/com.concurnas.bootstrap.lang=ALL-UNNAMED\n");
			sb.append("--add-exports=java.base/com.concurnas.bootstrap.lang.offheap=ALL-UNNAMED\n");
			sb.append("--add-exports=java.base/com.concurnas.bootstrap.lang.util=ALL-UNNAMED\n");
			sb.append("--add-exports=java.base/com.concurnas.bootstrap.runtime=ALL-UNNAMED\n");
			sb.append("--add-exports=java.base/com.concurnas.bootstrap.runtime.ref=ALL-UNNAMED\n");
			sb.append("--add-exports=java.base/com.concurnas.bootstrap.runtime.transactions=ALL-UNNAMED\n");
			sb.append("--add-exports=java.base/jdk.internal.ref=ALL-UNNAMED\n");

			sb.append("--add-opens=java.base/java.nio=ALL-UNNAMED\n");
			sb.append("--add-opens=java.base/com.concurnas.bootstrap.lang=ALL-UNNAMED");

			return sb.toString().trim();
		} else {
			List<String> jars = Files.walk(dir).filter(a -> !Files.isDirectory(a) && a.toString().endsWith(".jar")).map(a -> a.toAbsolutePath().toString()).collect(Collectors.toList());
			return "-Xbootclasspath:" + String.join(File.pathSeparator, jars);
		}
	}

	private static class DirAndFile {
		public Path file;
		public Path dir;

		public DirAndFile(Path dir, Path file) {
			this.dir = dir;
			this.file = file;
		}
	}

	private static DirAndFile generateUniqueFileName(Path prefixdir) {
		int leftLimit = 97; // letter 'a'
		int rightLimit = 122; // letter 'z'
		int targetStringLength = 8;
		int attempts = 100;// avoid inf loop
		int attempt = 0;

		while (attempt < attempts) {

			Random random = new Random();
			StringBuilder buffer = new StringBuilder(targetStringLength);
			for (int i = 0; i < targetStringLength; i++) {
				int randomLimitedInt = leftLimit + (int) (random.nextFloat() * (rightLimit - leftLimit + 1));
				buffer.append((char) randomLimitedInt);
			}
			String generatedString = buffer.toString();

			Path made = prefixdir.resolve(generatedString);
			Path madeFile = prefixdir.resolve(generatedString + CACHE_FILE_TYPE);
			if (!Files.exists(made) && !Files.exists(madeFile)) {
				return new DirAndFile(made, madeFile);
			}
			attempt++;
		}

		throw new RuntimeException("Unable to create location to store rtcache within: " + prefixdir);
	}

	public static class PathandVMArgs {
		private Path rtCacheDir;
		private String args;

		public PathandVMArgs(Path rtCacheDir, String args) {
			this.rtCacheDir = rtCacheDir;
			this.args = args;
		}
		
	}
	
	private static PathandVMArgs performCache(Path prefixDir, String rtVersion, boolean modules) throws Exception {
		System.out.println("Caching runtime enviroment for " + rtVersion + ". This needs to be done only once. This may take a few minutes...");

		DirAndFile dirAndfile = generateUniqueFileName(prefixDir);
		Path storeDir = dirAndfile.dir;
		Path fname = dirAndfile.file;

		String classpath = System.getProperty("java.class.path");
		RuntimeCacheCreator.makeCache(classpath.split(File.pathSeparator), storeDir, modules);

		List<String> li = new ArrayList<String>();
		li.add(rtVersion);

		Files.write(fname, li);// last written so if above is interupted we shouldn't arrive with a partial
								// commit
		return new PathandVMArgs(storeDir, createJVMArgs(storeDir, modules));
	}

	private static final String CACHE_FILE_TYPE = ".cac";

	private static class CacheEstablisher {
		public String bootstrapcp;
		public Path rootRtCacheDir;
		public Path rtCacheDir;

		private void establishCache() throws Exception {
			String concHome = System.getProperty("com.concurnas.home");

			if (concHome == null) {
				throw new RuntimeException("system propertly: com.concurnas.home must be set. e.g. -Dcom.concurnas.home=???");
			}

			{
				String vendor = System.getProperty("java.vendor");
				if (null == vendor || !vendor.equals("Oracle Corporation")) {
					String vmname = System.getProperty("java.vm.name");
					if(null == vmname || !vmname.contains("OpenJDK")) {
						System.err.println(String.format("WARN: Only Oracle JDK and OpenJDK is supported, results with %s are unknown", vendor));
					}
				}
			}

			String version = System.getProperty("java.specification.version");
			boolean modules = true;
			try {
				double versiond = Double.parseDouble(version);
				
				if (versiond < 1.9) {
					modules = false;
				}
				
				version = System.getProperty("java.version");//this is more detailed if specified
				if(null == version) {
					version = System.getProperty("java.specification.version");
				}
			} catch (Exception nfe) {
				System.err.println(String.format("WARN: Unable to determine JVM specification from version String: %s - assuming Java 9+", version));
				version = "1.9";
			}

			String rtVersion = String.format("Concurnas version: %s and JVM Spec: %s", ReleaseInfo.getVersion(), version);

			IOException firstFail = null;
			int n = 0;
			Path[] toTry = new Path[] { Paths.get(concHome + File.separator + "rtCache"), Paths.get("." + File.separator + "rtCache") };
			for (Path inst : toTry) {
				rootRtCacheDir = inst;
				if (Files.exists(rootRtCacheDir)) {
					// see if we can find rtVersion
					try (Stream<Path> cachedInstances = Files.walk(rootRtCacheDir, 1)) {
						List<Path> cacheInstances = cachedInstances.filter(a -> !Files.isDirectory(a) && a.toString().endsWith(CACHE_FILE_TYPE)).collect(Collectors.toList());
						for (Path instance : cacheInstances) {
							String foundRTVersion = Files.readAllLines(instance).get(0).trim();
							if (rtVersion.equals(foundRTVersion)) {
								String name = instance.getFileName().toString();
								Path cachedDir = instance.resolveSibling(Paths.get(name.substring(0, name.length() - 4)));
								if (Files.isDirectory(cachedDir)) {
									rtCacheDir = cachedDir;
									bootstrapcp = createJVMArgs(cachedDir, modules);
									return;
								}
							}
						}
					}
					if (Files.isWritable(rootRtCacheDir)) {
						break;
					}
				} else {
					if (firstFail != null) {
						System.out.println("Cannot create rtCache directory: " + firstFail.getMessage() + " will attempt to write to working directory...");
					}

					try {
						Files.createDirectories(rootRtCacheDir);
						break;
					} catch (IOException e) {
						if (firstFail != null || n == 1) {
							throw new RuntimeException("Cannot create rtCache directory: " + e.getMessage(), e);
						}
						firstFail = e;
					}
				}
				n++;
			}
			
			try {//just show warning on initial cache
				double versiond = Double.parseDouble(version);
				if (versiond < 1.8 || versiond > 13 || 11 == (int)Math.floor(versiond)) {
					System.err.println("WARN: Concurnas has been verified as compatible with Oracle JDK and OpenJDK Java versions 1.8, 9, 10, 12 and 13, version: " + version + " detected. Behaviour is unknown.");
				}
			}catch(Exception ohwell) {}

			PathandVMArgs pathAndVMArgs = performCache(rootRtCacheDir, rtVersion, modules);
			bootstrapcp = pathAndVMArgs.args;
			rtCacheDir = pathAndVMArgs.rtCacheDir;
			return;
		}
	}

	private static int exec(String mainClass, String bootStrapPath, Path rtCacheDir, List<String> extraJVMArgs, List<String> args, boolean onlyRuntime) throws IOException, InterruptedException {
		String javaHome = System.getProperty("java.home");
		String javaBin = javaHome + File.separator + "bin" + File.separator + "java";

		String classpath = System.getProperty("java.class.path");
		// String className = com.concurnas.concc.Concc.class.getName();

		RuntimeMXBean bean = ManagementFactory.getRuntimeMXBean();
		List<String> jvmArgs = bean.getInputArguments();
		// javaBin, xAJVMArgs, "-cp", classpath, className, cmd
		List<String> procCmd = new ArrayList<String>(5 + jvmArgs.size());
		procCmd.add(javaBin);
		for (String itm : bootStrapPath.split("\n")) {
			procCmd.add(itm.trim());
		}

		procCmd.addAll(jvmArgs);
		procCmd.add("-Dcom.concurnas.rtCache=" + rtCacheDir.toAbsolutePath().toString());
		if (onlyRuntime) {
			String[] items = classpath.split(File.pathSeparator);
			for (int n = 0; n < items.length; n++) {
				if (items[n].startsWith("Concurnas-rt-")) {
					classpath = items[n];
					break;
				}
			}
		}
		procCmd.addAll(extraJVMArgs);
		
		procCmd.add("-cp");
		procCmd.add(classpath);
		procCmd.add(mainClass);
		procCmd.addAll(args);

		ProcessBuilder builder = new ProcessBuilder(procCmd);
		builder.inheritIO();
		Process process = builder.start();
		process.waitFor();
		return process.exitValue();
	}

	public static void main(String args[]) {
		if (args.length >= 1) {
			try {
				String cmd = args[0];
				List<String> tmpcmdArgs = Arrays.asList(args);
				List<String> extraJVMarg = new ArrayList<String>(tmpcmdArgs.size()-1);
				List<String> cmdArgs = new ArrayList<String>(tmpcmdArgs.size()-1);
				
				if (tmpcmdArgs.size() > 1) {
					tmpcmdArgs = tmpcmdArgs.subList(1, tmpcmdArgs.size());
					
					for(String earg : tmpcmdArgs) {
						if(earg.startsWith("-J")) {
							extraJVMarg.add(earg.substring(2));
						}else if(earg.startsWith("-D")){
							extraJVMarg.add(earg);
						}else {
							cmdArgs.add(earg);
						}
					}
					
				}
				CacheEstablisher ce = new CacheEstablisher();
				ce.establishCache();
				String mainClass;
				boolean onlyRuntime;
				if (cmd.equals("conc")) {
					mainClass = "com.concurnas.conc.Conc";
					onlyRuntime = cmdArgs.isEmpty() ? false : true;
				} else if (cmd.equals("concc")) {
					mainClass = "com.concurnas.concc.Concc";
					onlyRuntime = false;
				} else {
					throw new RuntimeException("unknown command: " + cmd);
				}

				System.exit(exec(mainClass, ce.bootstrapcp, ce.rtCacheDir, extraJVMarg, cmdArgs, onlyRuntime));

			} catch (Throwable trhe) {
				System.err.println(String.format("\nCannot start Concurnas %s due to: %s", args[0], trhe.getMessage()));
				trhe.printStackTrace();
			}
		} else {
			System.err.println("Command name must be specified");
		}
		System.exit(1);
	}
}
