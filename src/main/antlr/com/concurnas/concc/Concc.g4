grammar Concc;

//-cp c:\jars\include.jar;c:\code\myClass.class -d c:\compiled -root c:\work thing.conc thing2.conc c:\work\stuff [ thing.conc thing2.conc ] "anoth[]er1" 'anoth[]er2'


@header{
package com.concurnas.concc;
}

concc : options* sources* EOF;

sources:
	ford+=fileOrDirNode+
	| root=fileOrDirNode '[' (ford+=fileOrDirNode)+ ']'
	;
	
options:
	'-d' fileOrDirNode #OptionOutputDir
	| ('-a' | '-all') #AllCopy
	| '-jar' jarFile=fileOrDirNode ( '[' mfest=fileOrDirNode  ']' )? #CreateJar
	| '-root' fileOrDirNode #OptionRoot
	| ('-cp'|'-classpath') classpath #OptionClasspath
	| '-werror' #OptionWError
	| '-verbose' #OptionVerbose
	| '--help' #OptionHelpMe
	| ('-c' | '-clean') #OptionClean
	;
	
classpath: fileOrDirNode ( (';'|':') fileOrDirNode)* ;

fileOrDirNode : '.' | FILEORDIR;

WS: ( '\t' | ' ' | '\r' | '\n'| '\u000C' )+ -> skip ;

FILEORDIR : ~('-' | ';' | ')' | '('| ']' | '['| '\t' | ' ' | '\r' | '\n'| '\u000C' ) ~(';' | ')' | '(' | ']' | '['| '\t' | ' ' | '\r' | '\n'| '\u000C' )+ | '"' ~( '\t' | ' ' | '\r' | '\n'| '\u000C' )+ '"' | '\'' ~( '\t' | ' ' | '\r' | '\n'| '\u000C' )+ '\'' ;