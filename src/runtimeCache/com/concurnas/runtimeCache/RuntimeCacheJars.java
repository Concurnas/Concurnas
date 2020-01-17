package com.concurnas.runtimeCache;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.ExecutorService;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import com.concurnas.runtime.ConcurnasClassLoader;
import com.concurnas.runtime.OffHeapAugmentor;

public class RuntimeCacheJars extends RuntimeCache{

	private final File fromDirectory;
	private final ArrayList<File> foundJars;

	public static File getRTJarPath() throws URISyntaxException, IOException {
		Class<?> klass = Object.class;
		URL location = klass.getResource('/' + klass.getName().replace('.', '/') + ".class");
		File file = new File(((JarURLConnection) location.openConnection()).getJarFileURL().toURI());
		String fullpath = file.getAbsolutePath();
		return new File((fullpath.substring(0,fullpath.length() - 6)));		
	}
	
	public RuntimeCacheJars(String[] classpath, String toDirectory, boolean log) throws Exception {
		super(classpath, toDirectory, log);
		
		this.fromDirectory = getRTJarPath();
		this.foundJars = JarFinder.findJars(this.fromDirectory);
		super.modloader = new BootstrapJarLoader(this.foundJars, classpath);

		super.weaver = new Weaver(modloader);
		super.assignProgressTracker(foundJars.size());
	}

	public void processZip(ProgressTracker pt, ZipFile zf, File toa, int idx) throws IOException {
		Enumeration<? extends ZipEntry>  entries = zf.entries();
		ArrayList<ZipEntry> arEntries = new ArrayList<ZipEntry>(100);
		
		while (entries.hasMoreElements()) {
			arEntries.add((ZipEntry) entries.nextElement());
		}
		
		String shortname = zf.getName();
		shortname = shortname.substring(shortname.lastIndexOf(File.separator)+1);
		
		PctDoer tracker = new PctDoer(pt, arEntries.size(), 2, shortname + " - ", idx);
		
		ZipOutputStream out = null;
		if(null != toa){
			out = new ZipOutputStream(new FileOutputStream(toa));
		}
		
		ConcurnasClassLoader ccl = new ConcurnasClassLoader(new Path[]{});
		
		//BufferedReader input = new BufferedReader(new InputStreamReader(System.in));
		for(ZipEntry ze : arEntries){
			//log("Read " + ze.getName());
			
			if(!ze.isDirectory()){
				byte[] code = JarLoadUtil.getCodeFromJar(zf, ze);
			    
			    if(ze.getName().endsWith(".class")){
			    	//transform only classes 
			    	
			    	if(!ze.getName().equals("java/lang/Object.class")){
			    		code = OffHeapAugmentor.addOffHeapMethods(code, ccl);
			    	}
					
			    	HashMap<String, byte[]> nameToTrans = weaver.weave(code, log, true);
			    	
			    	for(String name: nameToTrans.keySet()){
			    		byte[] ccode = nameToTrans.get(name);
			    		
			    		if(null != out){
						    out.putNextEntry(new ZipEntry(name + ".class"));
						    out.write(ccode, 0, ccode.length);
						    out.closeEntry();
					    }
			    	}
			    }
			    else{
			    	if(null != out){
					    out.putNextEntry(ze);
					    out.write(code, 0, code.length);
					    out.closeEntry();
				    }
			    }
			}
			
			tracker.entryDone();
		}
		if(null != out){
			out.close();
		}
	}



	
	
	private void processInstance(ProgressTracker pt, File found, File fname, int idx) throws ZipException, IOException {
		try(ZipFile zf = new ZipFile(found)){
			processZip(pt, zf, fname, idx);
		}
		
		pt.onDone(idx);
	}


	protected int doAug(ExecutorService executorService, HashSet<String> findStaticLambdas) throws Exception{
		//ignore findStaticLambdas because there arn't any pre java 1.9
		int idx = -1;
		for(File found : foundJars){//thread to execute each
			idx++;
			File fname = new File(toDirectory + File.separator + found.getAbsolutePath().substring(fromDirectory.getAbsolutePath().length()+1));
			if(fname.exists()){
				super.pt.onDone(idx);
				continue;
			}
			
			String toCreate = fname.getAbsolutePath();
			
			if(toCreate.contains(File.separator)){//has nested dirs, ensure exists
				new File(toCreate.substring(0, toCreate.lastIndexOf(File.separatorChar))).mkdirs();
			}
			final int ifxpass = idx;
			executorService.submit(() -> {
				try {
					processInstance(super.pt, found, fname, ifxpass);
				} catch (Exception e) {
					e.printStackTrace();
					System.exit(2);
				} 
			});
			
			//System.err.println("" + fname);
			//processInstance(super.pt, found, fname, ifxpass);
			
		}
		return idx+1;
	}
	
}
