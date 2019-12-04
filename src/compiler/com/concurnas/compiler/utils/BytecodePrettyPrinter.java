package com.concurnas.compiler.utils; //TODO: move me to dev utils.

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.util.Textifier;
import org.objectweb.asm.util.TraceClassVisitor;

public class BytecodePrettyPrinter {

	
	private static class MethodNamePrinter extends ClassVisitor {
	    public MethodNamePrinter() {
	        super(Opcodes.ASM7);
	    }

	    public ArrayList<String> methodNames = new ArrayList<String>();
	    
	    @Override
	    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
	    	methodNames.add(name + ":" + desc);
	        return super.visitMethod(access, name, desc, signature, exceptions);
	    }
	}
	

	public static void printNAmes(byte[] bytecode) throws Exception {
		
		ClassReader cr = new ClassReader(bytecode);
		MethodNamePrinter mnp = new MethodNamePrinter();
		cr.accept(mnp, 0);
		
		for(String entry : mnp.methodNames){
			System.out.println(entry);
		}
	}
	
	private static class OffSetPrinter extends ClassReader {

		public HashMap<Integer, Label> offsetLabel = new HashMap<Integer, Label>();

		public OffSetPrinter(InputStream is) throws IOException {
			super(is);
		}

		protected Label readLabel(int offset, Label[] labels) {
			try{
				Label ret = super.readLabel(offset, labels);
				offsetLabel.put(offset, ret);
				return ret;
			}
			catch(Exception e){
				String clsNAme = super.getClassName();
				int j=9;
				return null;
			}
		}
	}

	private static class TextifierLabelTrack extends Textifier {
		private HashMap<String, Map<Label, String>> alllabelMaps = new HashMap<String, Map<Label, String>>();

		public TextifierLabelTrack() {
			super(Opcodes.ASM7);
			super.labelNames = new HashMap<Label, String>();
		}

		private TextifierLabelTrack nextTracker = null;
		
		@Override
		protected Textifier createTextifier() {
			if(null ==  nextTracker){
				return new Textifier();
			}
			return nextTracker;
		}
		
		@Override
	    public Textifier visitMethod(final int access, final String name, final String desc, final String signature, final String[] exceptions) {
			nextTracker = new TextifierLabelTrack();
			alllabelMaps.put(name + desc,  nextTracker.getLabelNames());
			
	    	return super.visitMethod(access, name,   desc,   signature,  exceptions);
	    }
		
		private Map<Label, String> getLabelNames() {
			return super.labelNames;
		}
		
		/*public HashMap<String, Map<Label, String>> getAllLabelNames() {
			return alllabelMaps;
		}*/
		
		public Map<Label, String> getAllLabelNamesFlat() {
			Map<Label, String> ret = new HashMap<Label, String>();
			for(String method : alllabelMaps.keySet()){
				Map<Label, String> v = alllabelMaps.get(method);
				for(Label l : v.keySet()){
					ret.put(l, v.get(l) + " " + method);
				}
			}
			
			return ret;
		}
		
	}
	
	private final static boolean printOffsets = false;//TODO: refactor all this debug checks stuff elsewhere or comment out?
	
	public static String print(byte[] bytecode, boolean print) throws Exception {
		return print(bytecode, print, System.out, null);
	}
	public static String print(byte[] bytecode, boolean print, PrintStream outputps, String prefix) throws Exception {
		InputStream is = new ByteArrayInputStream(bytecode);
		OffSetPrinter cr = new OffSetPrinter(is);

		OutputStream output;
		if(prefix != null) {
			output = new OutputStream() {
				private StringBuilder string = new StringBuilder();
				
				boolean postNewLine=false;
				@Override
				public void write(int b) throws IOException {
					if(postNewLine) {
						this.string.append(prefix);
						postNewLine=false;
					}
					
					this.string.append((char) b);
					if(b == '\n') {
						postNewLine = true;
					}
				}

				public String toString() {
					return this.string.toString();
				}
			};
			output.write('\n');
		}else {
			output = new OutputStream() {
				private StringBuilder string = new StringBuilder();

				@Override
				public void write(int b) throws IOException {
					this.string.append((char) b);
				}

				public String toString() {
					return this.string.toString();
				}
			};
		}
		
		
		String pp;
		if (print) {
			TextifierLabelTrack labelTrack = new TextifierLabelTrack();
	
			cr.accept(new TraceClassVisitor(null, labelTrack, new PrintWriter(output)), 0);
			pp = output.toString();
			
			if(outputps != null) {
				outputps.println(pp);
				if(printOffsets){
					outputps.println("Offsets:");

					ArrayList<Integer> offsets = new ArrayList<Integer>(cr.offsetLabel.keySet());
					Collections.sort(offsets);
					Map<Label, String> ltoStr = labelTrack.getAllLabelNamesFlat();

					for (int offset : offsets) {
						Label got = cr.offsetLabel.get(offset);
						outputps.println(String.format("%s -> %s", offset, ltoStr.get(got)));
					}
				}
			}
		}
		else{
			cr.accept(new TraceClassVisitor(new PrintWriter(output)), 0);
			pp = output.toString();
		}

		return pp;
	}

}
