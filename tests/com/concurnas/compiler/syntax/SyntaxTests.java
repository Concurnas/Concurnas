package com.concurnas.compiler.syntax;

import java.nio.file.Paths;
import java.util.ArrayList;

import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.atn.PredictionMode;
import org.antlr.v4.runtime.tree.ParseTree;
import org.junit.Test;

import com.concurnas.compiler.ASTCreator;
import com.concurnas.compiler.ConcurnasLexer;
import com.concurnas.compiler.ConcurnasParser;
import com.concurnas.compiler.ErrorHolder;
import com.concurnas.compiler.LexParseErrorCapturer;
import com.concurnas.compiler.ast.Block;

public class SyntaxTests {
	
	public Block runTest( String filename) throws Exception{
		CharStream input = CharStreams.fromPath(Paths.get(filename));
		return runTest(input, filename);
	}
	
	public Block runTest( CharStream input, String filename) throws Exception{
		LexParseErrorCapturer lexerErrors = new LexParseErrorCapturer(filename);
		LexParseErrorCapturer parserErrors = new LexParseErrorCapturer(filename);
		
		ConcurnasLexer lexer = new ConcurnasLexer(input);
		CommonTokenStream tokens = new CommonTokenStream(lexer);
		ConcurnasParser parser = new ConcurnasParser(tokens);
		parser.setInputStream(new CommonTokenStream(lexer));

		parser.getInterpreter().setPredictionMode(PredictionMode.SLL); 
		
		parser.removeErrorListeners(); // remove ConsoleErrorListener
		parser.addErrorListener(lexerErrors); // add ours
		
		lexer.removeErrorListeners(); // remove ConsoleErrorListener
		lexer.addErrorListener(parserErrors); // add ours
		
		ParseTree tree = parser.code();
		
		ArrayList<ErrorHolder> lexerErrorsAR = lexerErrors.errors;
		ArrayList<ErrorHolder> parserErrorsAR = parserErrors.errors;
		
		checkForErrors(lexerErrorsAR, parserErrorsAR);
		
		//no errors, create AST
		Block ret = (Block)new ASTCreator(filename, parserErrors).visit(tree);

		checkForErrors(lexerErrorsAR, parserErrorsAR);
		
		return ret;
	}
	
	private void checkForErrors(ArrayList<ErrorHolder> lexerErrorsAR, ArrayList<ErrorHolder> parserErrorsAR) throws Exception{
		int lexerErrorsSZ = lexerErrorsAR.size();
		int parserErrorsSZ = parserErrorsAR.size();
		int totErrs = lexerErrorsSZ + parserErrorsSZ;
		if(totErrs > 0)
		{
			if(lexerErrorsSZ > 0)
			{
				System.out.println("Lexer Errors: " + lexerErrorsSZ);
				for(ErrorHolder s : lexerErrorsAR)
				{
					System.out.println(s);
				}
			}
			
			if(parserErrorsSZ > 0)
			{
				System.out.println("Parser Errors: " + parserErrorsSZ);
				for(ErrorHolder s : parserErrorsAR)
				{
					System.out.println(s);
				}
			}
			
			throw new Exception("Total Errors: " + totErrs);
		}
	}
	
	@Test
	public void newlinestuff() throws Exception {
		runTest("./tests/com/concurnas/compiler/syntax/newlinestuff.conc");
	}
	
	
	@Test
	public void syntaxToImplement() throws Exception {
		runTest("./tests/com/concurnas/compiler/syntax/syntaxToImplement.conc");
	}
	
	@Test
	public void imports() throws Exception {
		runTest("./tests/com/concurnas/compiler/syntax/imports.conc");
	}

	@Test
	public void lambda() throws Exception {//bit slow
		runTest("./tests/com/concurnas/compiler/syntax/lambda.conc");
	}
	
	@Test
	public void play() throws Exception {
		runTest("./tests/com/concurnas/compiler/syntax/play.conc");
	}
	
	@Test
	public void compoundRet() throws Exception {
		runTest("./tests/com/concurnas/compiler/syntax/compoundret.conc");
	}
	
	@Test
	public void genneral() throws Exception {
		runTest("./tests/com/concurnas/compiler/syntax/genneral.conc");
	}
	
	@Test
	public void scratchpad() throws Exception {
		runTest("./tests/com/concurnas/compiler/syntax/scratchpad.conc");
	}
	

	@Test
	public void restart() throws Exception {
		runTest("./tests/com/concurnas/compiler/syntax/restart.conc");
	}
	
}
