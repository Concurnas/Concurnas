package com.concurnas.build;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import com.concurnas.runtime.ConcurnasClassLoader;
import com.concurnas.runtime.cps.analysis.ConcClassWriter;


/**
 * Splice in version and date from version.tex file and splice this into
 * com.concurnas.runtimeCache.ReleaseInfo.class.
 * 
 * Example version.tex:
 * v.1.13.1 (30/10/2019)
 */
public class VersionSplicer implements Opcodes{
	
	private static class InsertIntoRInfo extends ClassVisitor{

		private String date;
		private String version;

		public InsertIntoRInfo(ClassWriter cw, String version, String date) {
			super(ASM7, cw);
			this.version = version;
			this.date = date;
		}


		public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
			if(name.equals("getVersion")) {
				{
					MethodVisitor mv = cv.visitMethod(ACC_PUBLIC + ACC_STATIC, "getVersion", "()Ljava/lang/String;", null, null);
					mv.visitCode();
					mv.visitLabel(new Label());
					mv.visitLdcInsn(version);
					mv.visitInsn(ARETURN);
					mv.visitMaxs(1, 0);
					mv.visitEnd();
				}
				return null;
			}else if(name.equals("getVersionDate")) {
				{
					MethodVisitor mv = cv.visitMethod(ACC_PUBLIC + ACC_STATIC, "getVersionDate", "()Ljava/lang/String;", null, null);
					mv.visitCode();
					mv.visitLabel(new Label());
					mv.visitLdcInsn(date);
					mv.visitInsn(ARETURN);
					mv.visitMaxs(1, 0);
					mv.visitEnd();
				}
				
				return null;
			}
			

			return cv.visitMethod(access, name, desc, signature, exceptions);
		}
	}
	
	public static void main(String[] args) throws IOException {
		Path vinfo = Paths.get("./book/version.tex");
		List<String> lines = Files.readAllLines(vinfo);
		String[] versionAndDate = lines.get(0).trim().split(" ");
		
		String version = versionAndDate[0];
		String dt = versionAndDate[1];
		dt = dt.substring(1, dt.length()-1);
		
		Path releaseInfoClsPath = Paths.get("./build/classes/java/main/com/concurnas/runtimeCache/ReleaseInfo.class");

		ClassReader cr = new ClassReader(Files.readAllBytes(releaseInfoClsPath));
		
		ClassWriter cw = new ConcClassWriter(ClassWriter.COMPUTE_MAXS|ClassWriter.COMPUTE_FRAMES, new ConcurnasClassLoader().getDetector());
		InsertIntoRInfo mma = new InsertIntoRInfo(cw, version, dt);
		cr.accept(mma, 0);
		
		Files.write(releaseInfoClsPath, cw.toByteArray());
		System.out.println(String.format("Spliced in version: %s and date: %s", version, dt));
	}
}
