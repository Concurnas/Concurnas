grammar Conc;

//-cp c:\jars\include.jar;c:\code\myClass.class thing.jar
//-cp c:\jars\include.jar;c:\code\myClass.class thing.class
//-cp c:\jars\include.jar;c:\code\myClass.class thing


@header{
package com.concurnas.conc;
}
 
conc : options* source=fileOrDirNode? arg* EOF;

options:
	('-cp'|'-classpath') classpath #OptionClasspath
	| '--help' #OptionHelpMe
	| '-s' #ServerMode
	| '-werror' #OptionWError
	| '-bc' #OptionByteCode
	;
	
classpath: fileOrDirNode ((';'|':') fileOrDirNode)* ;

fileOrDirNode : '.' | FILEORDIR;

stringArg: STR_ARG;

arg: ( FILEORDIR | SINGLED | stringArg);


WS: ( '\t' | ' ' | '\r' | '\n'| '\u000C' )+ -> skip ;


FILEORDIR : ~('-' | ';' | ']' | '['| '\t' | ' ' | '\r' | '\n'| '\u000C' ) ~(';' | ']' | '['| '\t' | ' ' | '\r' | '\n'| '\u000C' )+ 
		  | '"' ~( '\t' | ' ' | '\r' | '\n'| '\u000C' )+ '"' 
		  | '\'' ~( '\t' | ' ' | '\r' | '\n'| '\u000C' )+ '\'' ;

SINGLED : HexDigit;

fragment HexDigit
    : [0-9a-fA-F]
    ;

fragment EscapeSequence
    : '\\' [btnfr{"'\\]
    | '\\' ([0-3]? [0-7])? [0-7]
    | '\\' 'u'+ HexDigit HexDigit HexDigit HexDigit
    ;

STR_ARG
  : '"' (   ~( '\\' | '"'  ) | EscapeSequence )*'"' 
  | '\'' (   ~( '\\' | '\''  ) | EscapeSequence )*'\'' 
  ;