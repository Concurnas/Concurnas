package com.concurnas.build;

import java.io.File;

public class LibCompilation {
	
    public static int exec(String cmd) {
    	try {
    		  String javaHome = System.getProperty("java.home");
  	        String javaBin = javaHome + File.separator + "bin" + File.separator + "java";
  	        String classpath = System.getProperty("java.class.path");
  	        String className = com.concurnas.concc.Concc.class.getName();

  	        ProcessBuilder builder = new ProcessBuilder( javaBin, "-cp", classpath, className, cmd);

  	        Process process = builder.inheritIO().start();
  	        process.waitFor();
	        return process.exitValue();
    	}catch(Exception e) {
    		e.printStackTrace();
    	}
    	
    	return 0;
    }
	
	private int cnt = 1;
	private final int titleLen;
	
	public LibCompilation(String title) {
		titleLen = title.length();
		System.out.println(title);
	}
	
	private int doStep(String name, String cmd) {
		String msg = String.format("Step %s - %s", cnt++, name);
		int msgLen = msg.length();
		if(msgLen % 2 == 1) {
			msg += " ";
		}
		
		int remainer = (titleLen - (msgLen + 2))/2;
		
		String block = new String(new char[remainer]).replace("\0", "-");
		System.out.println(String.format("%s %s %s", block, msg, block));
		
		int ret = exec(cmd);
		System.out.println();
		return ret;
	}
	
	public static void main(String args[]) {
		String title = "=============== Starting Library Build ===============";
		String end   = "=============== Finished Library Build===============";
		LibCompilation lc = new LibCompilation(title);
		
		String outputDir = "./bin/main";
		if(args.length == 1) {
			//output path passed in
			outputDir = args[0];
		}
		
		int retval = 0;
		retval += lc.doStep("types", String.format("-d %s ./src/runtime[com/concurnas/lang/types.conc]", outputDir));
		retval += lc.doStep("tuples", String.format("-d %s ./src/runtime[com/concurnas/lang/tuples.conc]", outputDir));
		retval += lc.doStep("ranges", String.format("-d %s ./src/runtime[com/concurnas/lang/ranges.conc]", outputDir));
		retval += lc.doStep("datautils", String.format("-d %s ./src/runtime[com/concurnas/lang/datautils.conc]", outputDir));
		retval += lc.doStep("copy", String.format("-d %s ./src/runtime[com/concurnas/runtime/copy.conc]", outputDir));
		
		retval += lc.doStep("concurrent", String.format("-d %s ./src/runtime[com/concurnas/lang/concurrent.conc]", outputDir));
		retval += lc.doStep("gpus", String.format("-d %s ./src/runtime[com/concurnas/lang/gpus.conc]", outputDir));
		retval += lc.doStep("gpubuiltin", String.format("-d %s ./src/runtime[com/concurnas/lang/gpubuiltin.conc]", outputDir));
		retval += lc.doStep("nullable", String.format("-d %s ./src/runtime[com/concurnas/lang/nullable.conc]", outputDir));
		retval += lc.doStep("pulsar", String.format("-d %s ./src/runtime[com/concurnas/lang/pulsar.conc]", outputDir));
		retval += lc.doStep("dist", String.format("-d %s ./src/runtime[com/concurnas/lang/dist.conc]", outputDir));

		System.out.println(end);
		System.exit(retval);
	}
}
