package com.concurnas.compiler;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CommonToken;
import org.antlr.v4.runtime.CommonTokenFactory;
import org.antlr.v4.runtime.TokenSource;
import org.antlr.v4.runtime.misc.Pair;

public class LocationRepointCommonTokenFactory extends CommonTokenFactory {
	private int lineoffset;
	private int coloffset;

	public LocationRepointCommonTokenFactory(int lineoffset, int coloffset){
		this.lineoffset = lineoffset;
		this.coloffset = coloffset;
	}
	
	@Override
	public CommonToken create(Pair<TokenSource, CharStream> source, int type, String text,
							  int channel, int start, int stop,
							  int line, int charPositionInLine)
	{
		return super.create( source,  type,  text, channel,  start,  stop, /*line +*/ lineoffset,  charPositionInLine + coloffset);
	}
	
}
