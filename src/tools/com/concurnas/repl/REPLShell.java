package com.concurnas.repl;

import java.util.HashMap;
import java.util.List;
import java.util.regex.Pattern;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.Token;
import org.jline.builtins.Widgets.AutopairWidgets;
import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.LineReader.Option;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.UserInterruptException;
import org.jline.reader.impl.DefaultHighlighter;
import org.jline.reader.impl.DefaultParser;
import org.jline.reader.impl.DefaultParser.Bracket;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.AttributedString;

import com.concurnas.compiler.ConcurnasLexer;
import com.concurnas.compiler.LexParseErrorCapturer;

public class REPLShell {

	private static class TheHighlighter extends DefaultHighlighter {
		private static final int Color_Keyword = 175;
		private static final int Color_Operator = 51;
		private static final int Color_Number = 214;
		//private static final int Color_Comment = 7;
		private static final int Color_String = 73;
		private static final int Color_Default =15;
		
		private static final HashMap<String, Integer> tokenToColor = new HashMap<String, Integer>();
		static {
			for(String str : new String[]{"LANG_EXT", "REGEX_STRING_ITM", "STRING_ITMquot", "STRING_ITMcit"}) {
				tokenToColor.put(str, Color_String);
			}
			
			for(String keyWord : new String[]{"VAL", "VAR", "PRIVATE", "PUBLIC", "INJECT", "PROTECTED", "PACKAGE", "DOT", "DOTDOT", "DDD",
											 "'val'", "'var'", "'private'", "'public'", "'inject'", "'protected'", "'package'", "'.'", "'..'", "'...'",
											 "'assert'", "'del'", "'transient'", "'shared'", "'lazy'", "'override'", "'break'", "'continue'", 
											 "'throw'", "'return'", "'import'", "'using'", "'from'", "'as'", "'typedef'", "'await'", "'def'", "'gpudef'", 
											 "'gpukernel'", "'global'", "'local'", "'constant'", "'in'", "'out'", "'this'", "'provider'", "'single'", 
											 "'provide'", "'abstract'", "'open'", "'closed'", "'class'", "'trait'", "'actor'", "'of'", "'extends'", 
											 "'with'", "'new'", "'annotation'", "'enum'", "'else'", "'for'", "'parfor'", "'parforsync'", "'match'", "'case'", 
											 "'also'", "'if'", "'elif'", "'async'", "'pre'", "'post'", "'while'", "'loop'", "'try'", "'finally'", "'catch'",
											 "'trans'", "'init'", "'sync'", "'onchange'", "'every'", "'void'", "'boolean'", "'bool'", "'size_t'", "'int'", 
											 "'long'", "'float'", "'double'", "'byte'", "'short'", "'char'", "'lambda'", "'is'", "'isnot'", "'sizeof'",
											 "'super'", "'changed'", "'null'", "'true'", "'false'", "'default'"}){
				tokenToColor.put(keyWord, Color_Keyword);
			}
			
			for(String operator : new String[]{"'-'", "'+'", "'*'", "'<'", "'>'", "'/'", "'**'", "'++'", "'--'", "'mod'", "'or'", "'and'", "'not'", "'comp'",
											   "'band'", "'bor'", "'bxor'", "'-='", "'*='", "'/='", "'mod='", "'**='", "'+='", "'or='", "'and='", "'<<='", 
											   "'>>='", "'>>>='", "'band='", "'bor='", "'bxor='", "'\\='","'=='", "'<>'", "'&=='", "'&<>'", "'>=='", "'<=='",
											   "'!'", "'@'", "'?'", "'??'", "'?:'", "'^'", "'&'", "'\\.'", "'?.'", "'~'"}){
				tokenToColor.put(operator, Color_Operator);
			}
			
			for(String number : new String[]{"LONGINT", "SHORTINT", "INT", "FLOAT", "DOUBLE"}){
				tokenToColor.put(number, Color_Number);
			}
			
			//for(String comment : new String[]{"MULTILINE_COMMENT", "LINE_COMMENT"}){
			//	tokenToColor.put(comment, Color_Comment);
			//}
			//ignore: "NAME", IGNORE_NEWLINE, NEWLINE, "LPARA", "RPARA", "LBRACK", "ALBRACK", "RBRACK", "LBRACE", "RBRACE", "WS", "WS2",
			//ignore: "';'", "'=>'", "':'", "','", "'<-'", "'<='", "'='", "'|'"
		}
		
		@Override
		public AttributedString highlight(LineReader reader, String buffer) {
			
			LexParseErrorCapturer errs = new LexParseErrorCapturer("");
			
			CharStream input = CharStreams.fromString(buffer, "");
			ConcurnasLexer lexer = new ConcurnasLexer(input);
			lexer.removeErrorListeners();
			lexer.addErrorListener(errs);
			
			List<? extends Token> toks = lexer.getAllTokens();
			if(toks.isEmpty() || !errs.errors.isEmpty()) {
				return AttributedString.fromAnsi(buffer);//do nothing
			}
			
			int cursor = 0;
			
			StringBuilder sb = new StringBuilder();
			for(Token tok : toks) {
				int startIdx = tok.getStartIndex();
				while(cursor++ < startIdx-1) {
					sb.append(" ");//add whitespace padding
				}
				
				String tokenName = ConcurnasLexer.VOCABULARY.getDisplayName(tok.getType());
				
				int color = Color_Default;
				if(null != tokenName && tokenToColor.containsKey(tokenName)) {
					color = tokenToColor.get(tokenName);
				}

				sb.append(String.format("\u001b[38;5;%sm%s", color, tok.getText()));
				cursor = tok.getStopIndex();
			}
			
			int len = buffer.length();
			while(cursor++ < len-1) {
				sb.append(" ");//trailing spaces
			}
			return AttributedString.fromAnsi(sb.toString());
		}

		@Override
		public void setErrorPattern(Pattern errorPattern) {

		}

		@Override
		public void setErrorIndex(int errorIndex) {

		}
	}
    
	public static int replLoop(String concVersion, String vmname, String version, boolean bc, boolean verbose) {
		LineReader reader;
		REPL repl;
		try {
			repl = new REPL(false, bc, false, verbose);
			
			DefaultParser parser = new DefaultParser();
			parser.setEofOnUnclosedBracket(Bracket.CURLY, Bracket.ROUND, Bracket.SQUARE);
			
			DefaultHighlighter highlighter = new TheHighlighter();
			
	        reader = LineReaderBuilder.builder()
	                //.completer(finalCompleter)
	                .parser(parser)
	                .highlighter(highlighter)
	                .variable(LineReader.SECONDARY_PROMPT_PATTERN, "%P   > ")
	                .variable(LineReader.INDENTATION, 2)
	                .option(Option.DISABLE_EVENT_EXPANSION, true)
	                //.option(Option.INSERT_BRACKET, true)
	                .option(Option.EMPTY_WORD_OPTIONS, false)
	                .build();
	        AutopairWidgets autopairWidgets = new AutopairWidgets(reader);// Create autopair widgets
	        autopairWidgets.enable();// Enable autopair 
		}catch(Exception e) {
        	System.err.println("Unknown error in REPL - cannot start: " + e.getMessage());
        	e.printStackTrace();
        	return 1;
		}
		
		System.out.println(String.format("Welcome to Concurnas %s (%s, Java %s).", concVersion, vmname, version));
		System.out.println("Currently running in REPL mode. For help type: /help\n");
		
		while (true) {
            String line = null;
            try {
                line = reader.readLine("conc> ").trim();
                String result;
                if(line.startsWith("/")) {
                	result = repl.cmdHandler(line.substring(1));
                }else {
                	result = repl.processInput(line);
                }
                result = result.trim();
                
                if(result.isEmpty()) {
                	System.out.println();
                }else {
                	System.out.println(result.trim()+"\n");
                }
            } catch (UserInterruptException e) {//CTRL + C
            	System.out.println("|  Bye!");
            	System.exit(0);
                return 0;
            }  catch (EndOfFileException e) {// Handle CTRL + D
            	System.out.println("|  Bye!");
                return 0;
            }catch(Throwable e) {
            	System.err.println("Unknown error in REPL: " + e.getMessage());
            	e.printStackTrace();
            	return 1;
            }
		}
	}
	/*
	 * public static void main(String[] args) { REPLShell.replLoop("","","",false,
	 * false); }
	 */
}
