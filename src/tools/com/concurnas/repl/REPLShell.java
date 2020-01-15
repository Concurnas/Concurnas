package com.concurnas.repl;

import org.jline.builtins.Widgets.AutopairWidgets;
import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.LineReader.Option;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.UserInterruptException;
import org.jline.reader.impl.DefaultParser;
import org.jline.reader.impl.DefaultParser.Bracket;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

public class REPLShell {

	/*
	 * private static class TheHighlighter implements Highlighter{
	 * 
	 * @Override public AttributedString highlight(LineReader reader, String buffer)
	 * { return sh.highlight(reader.readLine()); }
	 * 
	 * @Override public void setErrorPattern(Pattern errorPattern) {
	 * 
	 * }
	 * 
	 * @Override public void setErrorIndex(int errorIndex) {
	 * 
	 * } }
	 */
    
	public static int replLoop(boolean bc, boolean verbose) {
		LineReader reader;
		REPL repl;
		try {
			repl = new REPL(false, bc, false, verbose);
			
			/*
			 * URI nanoDefURI =
			 * REPLShell.class.getResource("/com/concurnas/repl/java.nanorc").toURI();
			 * Highlighter highlighter; if(nanoDefURI.toString().startsWith("jar")) {
			 * Map<String, String> env = new HashMap<>(); String[] array =
			 * nanoDefURI.toString().split("!"); FileSystem fs =
			 * FileSystems.newFileSystem(URI.create(array[0]), env); Path syntax =
			 * fs.getPath(array[1]); highlighter = new
			 * TheHighlighter(SyntaxHighlighter.build(syntax, "Concurnas")); fs.close();
			 * }else { Path syntax = Path.of(nanoDefURI); highlighter = new
			 * TheHighlighter(SyntaxHighlighter.build(syntax, "Concurnas")); }
			 */
			
			
			DefaultParser parser = new DefaultParser();
			parser.setEofOnUnclosedBracket(Bracket.CURLY, Bracket.ROUND, Bracket.SQUARE);
			
	        TerminalBuilder builder = TerminalBuilder.builder();
	        Terminal terminal = builder.build();
	        reader = LineReaderBuilder.builder()
	                .terminal(terminal)
	                //.completer(finalCompleter)
	                .parser(parser)
	                //.highlighter(highlighter)
	                .variable(LineReader.SECONDARY_PROMPT_PATTERN, "%P   > ")
	                .variable(LineReader.INDENTATION, 2)
	                //.option(Option.INSERT_BRACKET, true)
	                .option(Option.EMPTY_WORD_OPTIONS, false)
	                .build();
	        
	        
            
	        
	        
	        // Create autopair widgets
	        AutopairWidgets autopairWidgets = new AutopairWidgets(reader);
	        // Enable autopair 
	        autopairWidgets.enable();
		}catch(Exception e) {
        	System.err.println("Unknown error in REPL - cannot start: " + e.getMessage());
        	e.printStackTrace();
        	return 1;
		}
		
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
            } catch (UserInterruptException e) {
                // Ignore
            } catch (EndOfFileException e) {
                return 0;
            }catch(Exception e) {
            	System.err.println("Unknown error in REPL: " + e.getMessage());
            	e.printStackTrace();
            	return 1;
            }
		}
	}
	
	public static void main(String[] args) {
		REPLShell.replLoop(false, false);
	}
	
}
