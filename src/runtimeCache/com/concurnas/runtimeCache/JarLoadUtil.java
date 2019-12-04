package com.concurnas.runtimeCache;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class JarLoadUtil {
	
	//TODO: use java nio approach to solving this problem
	public static byte[]  getCodeFromJar(ZipFile zf, ZipEntry ze) throws IOException{
		BufferedInputStream  br = new BufferedInputStream(zf.getInputStream(ze));
		
		byte[] buff = new byte[1024]; //figure out avg size needed
		ByteArrayOutputStream bao = new ByteArrayOutputStream();
	    int n = 0;
	    while ((n = br.read(buff)) >= 0) {
	        bao.write(buff, 0, n);
	    }
	    br.close();
	    bao.close();
	    return bao.toByteArray();
	}
	
}
