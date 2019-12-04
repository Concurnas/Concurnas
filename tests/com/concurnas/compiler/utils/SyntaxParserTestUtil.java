package com.concurnas.compiler.utils;

import java.nio.file.Paths;
import java.util.ArrayList;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.atn.PredictionMode;

import com.concurnas.compiler.ConcurnasLexer;
import com.concurnas.compiler.ConcurnasParser;
import com.concurnas.compiler.ErrorHolder;
import com.concurnas.compiler.LexParseErrorCapturer;

public class SyntaxParserTestUtil {
	private final LexParseErrorCapturer lexerErrors = new LexParseErrorCapturer("");
	private final LexParseErrorCapturer parserErrors = new LexParseErrorCapturer("");
	private final ConcurnasLexer lexer;
	private final ConcurnasParser parser;
	
	public SyntaxParserTestUtil(){
		lexer = new ConcurnasLexer(null);
		CommonTokenStream tokens = new CommonTokenStream(lexer);
		parser = new ConcurnasParser(tokens);
		parser.setInputStream(new CommonTokenStream(lexer));

		parser.removeErrorListeners(); // remove ConsoleErrorListener
		parser.addErrorListener(lexerErrors); // add ours
		
		parser.getInterpreter().setPredictionMode(PredictionMode.SLL); 
		
		lexer.removeErrorListeners(); // remove ConsoleErrorListener
		lexer.addErrorListener(parserErrors); // add ours
	}
	
	public void parse(String filename, String data) throws Exception{
		parse(filename, CharStreams.fromString(data));
	}
	public void parse(String filename) throws Exception{
		parse(filename, CharStreams.fromPath(Paths.get(filename)));
	}
	
	public void parse(String filename, CharStream input) throws Exception{
		
		lexer.setInputStream(input);
		
		lexerErrors.errors = new  ArrayList<ErrorHolder>();
		parserErrors.errors = new  ArrayList<ErrorHolder>();
		
		
		lexerErrors.setFilename(filename);
		parserErrors.setFilename(filename);
		
		parser.code();
		
		lexer.reset();
		parser.reset();
		
		printOrThrowLastErrors(false, true);
	}
	
	public void printOrThrowLastErrors(boolean print, boolean throwit) {
		ArrayList<ErrorHolder> lexerErrorsAR = lexerErrors.errors;
		ArrayList<ErrorHolder> parserErrorsAR = parserErrors.errors;

		int lexerErrorsSZ = lexerErrorsAR.size();
		int parserErrorsSZ = parserErrorsAR.size();
		int totErrs = lexerErrorsSZ + parserErrorsSZ;
		if (totErrs > 0) {
			if (print) {
				if (lexerErrorsSZ > 0) {
					System.out.println("Lexer Errors: " + lexerErrorsSZ);
					for (ErrorHolder s : lexerErrorsAR) {
						System.out.println(s);
					}
				}

				if (parserErrorsSZ > 0) {
					System.out.println("Parser Errors: " + parserErrorsSZ);
					for (ErrorHolder s : parserErrorsAR) {
						System.out.println(s);
					}
				}
			}

			if (throwit) {
				throw new RuntimeException("Total Errors: " + totErrs);
			}
		}
	}
	
	public void printLastErrors(){
		printOrThrowLastErrors(true, false);
	}
}
