package com.concurnas.compiler;

import java.util.ArrayList;
import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;

public class LexParseErrorCapturer extends BaseErrorListener {
	public ArrayList<ErrorHolder> errors = new ArrayList<ErrorHolder>();
	private String filename;
	
	public LexParseErrorCapturer(final String filename){
		setFilename(filename);
	}
	
	public void setFilename(final String filename){
		this.filename = filename;
	}
	
	@Override
	public void syntaxError(Recognizer<?,?> recognizer, Object offendingSymbol, int line, int charPositionInLine, String message, RecognitionException e) {
		errors.add(new ErrorHolder(filename, line, charPositionInLine, message));
	}
}