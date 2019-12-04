grammar Concurnas;

@header{
	package com.concurnas.compiler;
	import java.util.Map;
	import java.util.HashMap;
	import java.util.List;
	import java.util.ArrayList;
	import java.util.LinkedHashSet;
	import java.util.Stack;
}

@lexer::members {
	private int defaultTransHandlerId = 0;

	boolean skipNewLine=false;
	Stack<Boolean> prevskip = new Stack<Boolean>();

	public boolean permitDollarPrefixRefName = false;
}



code
	: line* EOF
 ;         


line
	: 
	 stmts
	| nls=NEWLINE+
	| nop=';' ';' //nop
 ;

stmts: csOrss (     (';'|NEWLINE+)  csOrss)*   (';'|NEWLINE+)? ;

csOrss: comppound_str_concat|compound_stmt|simple_stmt;//actor MyClass(12) expression reachable version takes priority over compound statement

comppound_str_concat: compound_stmt additiveOp_*;//permits us to do this: a = {} + "str concat"

	
single_line_block 
  :  NEWLINE* '=>' NEWLINE* single_line_element (';' single_line_element)* ';'? //NEWLINE
  ;

single_line_element: comppound_str_concat|compound_stmt|simple_stmt| (nop=';'); 

///////////// simple_stmt /////////////

simple_stmt :
	exprListShortcut
  | assignment
  | annotations
  | assert_stmt
  | delete_stmt  
  | return_stmt
  | throw_stmt
  | flow_stmt        
  | import_stmt
  | typedef_stmt 
  | await_stmt   
  | lonleyExpression
  //| u=using_stmt    {$ret = $u.ret;} //dsls
  ;

lonleyExpression: expr_stmt_tuple;

exprListShortcut :
 	a1=refName atype1=mustBeArrayType ( aassStyle=assignStyle arhsExpr = expr_stmt_tuple)? //to deal with cases: myvar Integer[][] = null - which are otherwise picked up by next line...
	| e1=refName e2=refName (rest=expr_stmt_)+;//to deal with cases such as: thing call arg. But watch out for: thing String[]

mustBeArrayType: (primitiveType | namedType | tupleType) trefOrArrayRef+ | funcType trefOrArrayRef*;

transientAndShared: //can be defined either way around
	trans='transient'
	| shared='shared'
	| lazy='lazy'
	| trans='transient' shared='shared'
	| trans='transient' lazy='lazy'
	| lazy='lazy' shared='shared'
	| shared='shared' trans='transient'
	| lazy='lazy' trans='transient'
	| shared='shared' lazy='lazy'
	| lazy='lazy' shared='shared' trans='transient'
	| lazy='lazy' trans='transient' shared='shared'
	| shared='shared' lazy='lazy' trans='transient'
	| shared='shared' trans='transient' lazy='lazy'
	| trans='transient' lazy='lazy' shared='shared'
	| trans='transient' shared='shared' lazy='lazy'
;

assignment:
	 (annotations NEWLINE? )? ppp? (override='override')? transAndShared=transientAndShared? gpuVarQualifier? valvar=(VAL|VAR)? prefix=('-'|'+'|'~')?    (refname = refName typeNoNTTuple) ( assStyle=assignStyle ( rhsAnnotShurtcut = annotation | rhsAssignment = assignmentForcedRHS | rhsExpr = expr_stmt_tuple ) | onchangeEveryShorthand )?
	| (annotations NEWLINE? )? ppp? (override='override')? transAndShared=transientAndShared? gpuVarQualifier?  valvar=(VAL|VAR)? prefix=('-'|'+'|'~')?  refname = refName (refCnt+=':')* ( assStyle=assignStyle (  rhsAnnotShurtcut = annotation | rhsAssignment = assignmentForcedRHS | rhsExpr = expr_stmt_tuple  )   | onchangeEveryShorthand)
	| LPARA assignmentTupleDereflhsOrNothing (',' assignmentTupleDereflhsOrNothing)+ RPARA ( assStyle=assignStyle (  rhsAssignment = assignmentForcedRHS | rhsExpr = expr_stmt_tuple  )   | onchangeEveryShorthand)
	| ppp? (override='override')? transAndShared=transientAndShared? gpuVarQualifier? valvar=(VAL|VAR)? prefix=('-'|'+'|'~')?  assignee=expr_stmt ( assStyle=assignStyle  (rhsAnnotShurtcut = annotation | rhsAssignment = assignmentForcedRHS | rhsExpr = expr_stmt_tuple )  | onchangeEveryShorthand)
	| lonleyannotation = annotation
	;
	
assignmentForcedRHS:
	 (annotations NEWLINE? )? ppp? (override='override')? transAndShared=transientAndShared? gpuVarQualifier? valvar=(VAL|VAR)? prefix=('-'|'+'|'~')?    (refname = refName type) ( assStyle=assignStyle ( rhsAnnotShurtcut = annotation | rhsAssignment = assignmentForcedRHS | rhsExpr = expr_stmt_tuple ) | onchangeEveryShorthand )
	| (annotations NEWLINE? )? ppp? (override='override')? transAndShared=transientAndShared? gpuVarQualifier?  valvar=(VAL|VAR)? prefix=('-'|'+'|'~')?  refname = refName (refCnt+=':')* ( assStyle=assignStyle (  rhsAnnotShurtcut = annotation | rhsAssignment = assignmentForcedRHS | rhsExpr = expr_stmt_tuple  )   | onchangeEveryShorthand)
	| LPARA assignmentTupleDereflhsOrNothing (',' assignmentTupleDereflhsOrNothing)+ RPARA ( assStyle=assignStyle (  rhsAssignment = assignmentForcedRHS | rhsExpr = expr_stmt_tuple  )   | onchangeEveryShorthand)
	| ppp? (override='override')? transAndShared=transientAndShared? gpuVarQualifier? valvar=(VAL|VAR)? prefix=('-'|'+'|'~')?  assignee=expr_stmt ( assStyle=assignStyle  (rhsAnnotShurtcut = annotation | rhsAssignment = assignmentForcedRHS | rhsExpr = expr_stmt_tuple )  | onchangeEveryShorthand)
	| lonleyannotation = annotation
	;

assignmentTupleDereflhsOrNothing:
	assignmentTupleDereflhs?
;

assignmentTupleDereflhs:
	(annotations NEWLINE? )? ppp? (override='override')? transAndShared=transientAndShared? valvar=(VAL|VAR)? prefix=('-'|'+'|'~')?    (refname = refName type)
	| (annotations NEWLINE? )? ppp? (override='override')? transAndShared=transientAndShared? valvar=(VAL|VAR)? prefix=('-'|'+'|'~')?  refname = refName (refCnt+=':')*
	| ppp? transAndShared=transientAndShared? valvar=(VAL|VAR)? prefix=('-'|'+'|'~')?  assignee=expr_stmt
;


//assignmentNamdAndTypeOnly: (annotations NEWLINE? )? ppp? trans='transient'? gpuVarQualifier? valvar=(VAL|VAR)? prefix=('-'|'+'|'~')?    (refname = refName type);

onchangeEveryShorthand : 
	('<-' | isEvery='<=') (LPARA onChangeEtcArgs RPARA)? expr_stmt_tuple
	;

assert_stmt 
	: 'assert' e=expr_stmt_ s=stringNode?
	; 

delete_stmt
	: 'del' expr_stmt (',' expr_stmt)* ','?
	;
	
flow_stmt 
	: 'break' expr_stmt_tuple?   
	| 'continue' expr_stmt_tuple? 
	;
	
throw_stmt : 'throw' expr_stmt;
return_stmt : 'return' expr_stmt_tuple? ;

import_stmt 
	: import_stmt_impot
	| import_stmt_from
	;

import_stmt_impot 
	: ('import' | using='using') prim=dotted_as_name (',' sec+=dotted_as_name )* ','? 
	| ('import' | using='using') dotted_name DOT star='*'
	;

import_stmt_from 
  	: 'from' dotted_name ('import' | using='using') (import_as_name (',' import_as_name )* ','? | star='*')
	; 

import_as_name 
    : NAME ('as' NAME)? 
	;

dotted_as_name 
	: dotted_name ('as' NAME)? 
	;

typedef_stmt 
  : pppNoInject? 'typedef'  NAME typedefArgs? '='  type
  ;

typedefArgs: '<' NAME  (',' NAME )* ','? '>';


await_stmt
 :  'await' LPARA onChangeEtcArgs  (  ';'  ( expr_stmt | block )?  )? RPARA //TODO: expand the syntax permitte here
;

///////////// compound stmt /////////////

compound_stmt
	:
	( (annotations NEWLINE*)?
		( 	
			 funcdef
		  	| constructorDef
			| classdef
			| objectProvider
			| annotationDef
			| enumdef
		)
	)
	| comp = compound_stmt_atomic_base 
;


compound_stmt_atomic_base 
  : fors      
  | match_stmt  
  | if_stmt      
  | async_block     
  | while_stmt   
  | loop_stmt     
  | try_stmt     
  | block_async     
  | with_stmt   
  | trans_block    
  | init_block    
  | sync_block    
  | onchange  
  | every    
  ;

funcdef 
    : ppp?  ( (override='override')? ('def' | gpuitem = 'gpudef' | gpuitem ='gpukernel' kerneldim = intNode) | (override='override') )  ( (extFuncOn ('|' extFuncOn)* )? funcDefName DOT?)?
    		genericQualiList?
    		LPARA funcParams? RPARA retTypeIncVoid? (  block | single_line_block )?
	;

funcDefName
	: NAME //now for operator overloading...
	| '='
	| '+' | '-'
	| '*'| '/'
	| '**'| '++' | '--'
	| 'mod' | 'or'| 'and'| 'not'
	| '<''<' | '>' '>' | '>' '>' '>'
	| 'comp' | 'band' | 'bor' | 'bxor'
	| '-=' | '*=' | '/=' | 'mod=' | '**=' | '+=' | 'or=' | 'and=' | '<<='
	| '>>=' | '>>>=' | 'band=' | 'bor=' | 'bxor='
	;


extFuncOn
	: extFunOn=type
	//: extFunOn=namedType_ExActor|extFunOnPrim=primitiveType
	;

funcParams
  : funcParam (',' funcParam)* (',')?  
  ;

funcParam:
	annotations? sharedOrLazy? (gpuVarQualifier gpuInOutFuncParamModifier?)? (isFinal=VAL|VAR)? 
	(
		NAME ( type isvararg='...'?)? ('=' expr_stmt )
		|
		NAME? ( type isvararg='...'?) ('=' expr_stmt )?
	)
	;


sharedOrLazy: 
	lazy = 'lazy' shared='shared'?
	| shared='shared' lazy='lazy'?
;

gpuVarQualifier: 'global'|'local'|'constant';

gpuInOutFuncParamModifier : 'in'|'out';

constructorDef 
  :  ppp? 'def'? 'this' LPARA funcParams? RPARA (block | single_line_block)
  ;



objectProviderArgs 
	: objectProviderArg (',' objectProviderArg )* (',')?
	;

objectProviderArg:
	annotations? pppNoInject? transAndShared=transientAndShared?   
		(isFinal=VAL|VAR)? NAME  ((type | (refCnt+=':')+) isvararg='...'? )? ( '='  expr_stmt )?
	;



objectProvider: pppNoInject? (trans='transient' | shared='shared')? 'provider' providerName=NAME
genericQualiList? 
(LPARA objectProviderArgs? RPARA)?
	objectProviderBlock
	;

objectProviderBlock: 
	LBRACE NEWLINE* linex+=objectProviderLine* RBRACE 
	;

objectProviderLine: 
	(pppNoInject? (single='single'|shared='shared')? 'provide' genericQualiList? lazy='lazy'? fieldName=stringNode? provName=NAME? provide=type ('=>' provideExpr=expr_stmt  | objectProviderNestedDeps )?
	| opdl=objectProviderLineDep) (';'|NEWLINE+)
	;

objectProviderNestedDeps : LBRACE ((';'|NEWLINE+)? nestedDep+=objectProviderLineDep ((';'|NEWLINE+) nestedDep+=objectProviderLineDep)* )? (';'|NEWLINE+)? RBRACE;

objectProviderLineDep: (single='single'|shared='shared')? lazy='lazy'? fieldName=stringNode? nameFrom=type ('=>' exprTo=expr_stmt | '<=' typeOnlyRHS=type objectProviderNestedDeps?)?;


classdef 
	: p=ppp? aoc=('abstract'|'open'|'closed')? (trans='transient' | shared='shared')? ('class'|istrait='trait'|isactor='actor') className=NAME
		genericQualiList?
      (LPARA classdefArgs? RPARA)?
		(
			istypedActor='of' ( typedActorOn=namedType_ExActor typeActeeExprList=expr_stmtList?  )   
		)?
		( 
			NEWLINE* ('extends'|'<') superCls=dotted_name ('<' superGenType+=type (',' superGenType+=type )* ','? '>')? 
		 	 extExpressions=expr_stmtList?
		)?
		(
			NEWLINE* ('with'|'~') implInstance ((',' implInstance)*) ','?
		)?
		(block | NEWLINE+ | EOF)
	;


implInstance:
	impli=dotted_name ('<' implType+=type (',' implType+=type )* ','? '>')?
	;

localclassdef 
	: (trans='transient' | shared='shared')? ('class'|isactor='actor')
		genericQualiList? 
		(LPARA  classdefArgs? RPARA)?
		( 
			istypedActor='of' ( typedActorOn=namedType_ExActor typeActeeExprList=expr_stmtList?  )   
		)?
		( 
			('extends'|'<') superCls=dotted_name ('<' type (',' type )* ','? '>')? 
		 	 extExpressions=expr_stmtList?
		)?
		(
			('with'|'~') implInstance ((',' implInstance)* | ','?)
		)?
		block
	;

anonclassdef 
	: ('new' isactor='actor'?  )
		( 
			superCls=dotted_name ('<' type (',' type )* ','? '>')? 
		)
		
		(
			('with'|'~') implInstance ((',' implInstance)* | ','?)
		)?
		block?
	;
	

expr_stmtList : (LPARA (expr_stmt ( ',' expr_stmt )*)? RPARA);
	

classdefArgs 
	: classdefArg (',' classdefArg )* (',')?
	;

classdefArg:
	annotations? pppNoInject? (override='override')? transAndShared=transientAndShared? (isFinal=VAL|VAR)? (prefix=('-'|'+'|'~'))? NAME  ((type | (refCnt+=':')+) isvararg='...'? )? ( '='  expr_stmt )?
	;


annotationDef 
  : pppNoInject? 'annotation' NAME  
      ( LPARA annotationArg  (',' annotationArg )* ','? RPARA )?
      block? 
  ;

annotationArg:
	annotations? NAME type? ('=' expr_stmt)?
	;


enumdef 
  : pppNoInject? 'enum' NAME (LPARA classdefArgs? RPARA)? enumblock
  ;

enumItem
  : annotations? NAME pureFuncInvokeArgs? block?//dont need to have args?
  ; 
 
enumblock
	: LBRACE NEWLINE* enumItem ( NEWLINE* ',' NEWLINE* enumItem )* (';'|NEWLINE*) line* NEWLINE* RBRACE
	;
	
///////////// compound stmt:compound_stmt_atomic_base /////////////

fors 
  : for_stmt     
  | for_stmt_old 
  ;
  
for_stmt 
	: forblockvariant LPARA ( (localVarName=NAME localVarType=type?) | ( LPARA forVarTupleOrNothing (',' forVarTupleOrNothing)+ RPARA))  'in' expr=expr_stmt_tuple 
	    (';'  (idxName=NAME idxType=type? (('\\=' | '=') idxExpr=expr_stmt)?) )? RPARA mainblock=block 
	    (NEWLINE* 'else' elseblock=block )?
	;

forVarTupleOrNothing: forVarTuple?;

forVarTuple: localVarName=NAME localVarType=type?;

for_stmt_old 
  : forblockvariant LPARA ( (NAME type? assignStyle assigFrom=expr_stmt_tuple) | assignExpr=expr_stmt_tuple )? 
					  	';' check=expr_stmt?  
					  	';' postExpr=expr_stmt?   
				  	RPARA
  	  	mainblock=block 
    ( NEWLINE* 'else' elseblock=block  )?
  ;

forblockvariant:'for'|'parfor' |'parforsync';


match_stmt
  :
  'match' NEWLINE* LPARA NEWLINE* simple_stmt NEWLINE* RPARA NEWLINE* 
   LBRACE NEWLINE*
	   match_case_stmt*
	   ( NEWLINE* 'else'  (elseb=block | elsebs = single_line_block))?
    NEWLINE* RBRACE 
  ;

match_case_stmt: match_case_stmt_case | match_case_stmt_nocase;

match_case_stmt_case:
    (NEWLINE* 'case' LPARA 
    	  (	((  match_case_stmt_typedCase //CaseExpressionAssign
		    |  match_case_stmt_assign  //TypedCaseExpression
		    |  match_case_stmt_assignTuple
		    |  case_expr_chain_Tuple
		    |  case_expr_chain_or //, passthrough
		    | match_case_assign_typedObjectAssign
	      	) matchAlso=match_also_attachment?) 
	      | justAlso=match_also_attachment//needs an also...
	      )  RPARA
	    (block | single_line_block) 
	) 
   ;
   
match_case_stmt_nocase:
    (NEWLINE* 
    	  (	(( match_case_stmt_typedCase //TypedCaseExpression 
		    |  match_case_stmt_assign  //CaseExpressionAssign
		    |  match_case_stmt_assignTuple
		    |  case_expr_chain_Tuple
		    |  case_expr_chain_or //, passthrough
		    | match_case_assign_typedObjectAssign
	      	) matchAlso=match_also_attachment?) 
	      | justAlso=match_also_attachment//needs an also...
	      )  
	    (block | single_line_block) 
	) 
   ;

match_also_attachment: 'also' expr_stmt;

match_case_stmt_typedCase: (type  ( 'or'  type  )* ( ';'  (case_expr_chain_Tuple | case_expr_chain_or) )? ); //TypedCaseExpression

match_case_stmt_assign: ( (var='var'|isfinal='val')? NAME ( type  ( 'or'  type  )* )? ( ';'  expr_stmt)? );//CaseExpressionAssign

match_case_assign_typedObjectAssign: (var='var'|isfinal='val')? NAME bitwise_or;

match_case_stmt_assignTuple:  ( LPARA matchTupleAsignOrNone (',' matchTupleAsignOrNone)+ RPARA) ( ';'  expr_stmt)? ;//CaseExpressionAssign

matchTupleAsignOrNone: matchTupleAsign?;

matchTupleAsign: NAME type;

case_expr_chain_Tuple:
	LPARA case_expr_chain_orOrNone (',' case_expr_chain_orOrNone )+  RPARA
;

case_expr_chain_orOrNone: case_expr_chain_or?;

case_expr_chain_or
  :
   ce = case_expr_chain_and ( 'or' case_expr_chain_and )* 
  ;

case_expr_chain_and
  :
   ce = case_expr ( 'and' case_expr )* 
  ;

case_expr 
  : bitwise_or ( case_operator)? 
  | case_operator_pre bitwise_or 
  ;


case_operator : '==' | '<' | '<>' | '&==' | '&<>' | '>' | '>==' | '<==' ;
  
case_operator_pre 
  :  case_operator | 'in' | 'not' 'in' 
  ;



if_stmt
	: 
	  'if'  LPARA  ifexpr=expr_stmt  RPARA  ifblk=block
		elifUnit* 
		(  ( NEWLINE* 'else'   elseblk=block) )? 
	;

elifUnit :  NEWLINE* ('elif' | 'else' 'if') LPARA  expr_stmt  RPARA  block ;

async_block 
 : 'async' LBRACE
  (
    line 
    | 'pre' preblk+=block 
    | 'post' postblk+=block  
  )* 
 RBRACE
 ;

while_stmt 
	: 'while' LPARA  mainExpr=expr_stmt
						(';'  (idxName=NAME idxType=type? (('\\=' | '=') idxExpr=expr_stmt)?) )?// | nameAlone=NAME
	RPARA mainBlock=block  
	  ( NEWLINE* 'else' elseblock=block )?
	;

loop_stmt 
  : 'loop'  
  (LPARA  (idxName=NAME idxType=type? (('\\=' | '=') idxExpr=expr_stmt)?)  RPARA )? mainBlock=block  
  ; 

try_stmt 
	:	'try'  (LPARA  simple_stmt  (';'? simple_stmt  )* ';'?  RPARA)? mainblock=block 
	  catchBlock*
	  ( NEWLINE* 'finally'  finblock=block )?
	;

catchBlock: NEWLINE* 'catch'  LPARA   NAME  (type   ('or'  type   )* )? RPARA  block ;

block_async
  : block_ ( async='!'( LPARA executor=expr_stmt RPARA )? )?
  ;
  
  
with_stmt
	: 'with' 
	LPARA expr_stmt RPARA  
	block  
	;

trans_block 
  : 'trans'  b=block
  ;

init_block 
  : 'init' block
  ;

sync_block
  : 'sync' b=block
  ;

onchange 
 :  'onchange' (LPARA onChangeEtcArgs (';' opts+=NAME (',' opts+=NAME )* (',')? )? RPARA)? (block )//| single_line_block
 ;
 

every 
 :  'every' (LPARA onChangeEtcArgs (';' opts+=NAME (',' opts+=NAME )* (',')? )? RPARA)? (block )//| single_line_block
;

///////////// annotations /////////////

annotations 
  : annotation (NEWLINE* ','? NEWLINE* annotation  )* 
  ;

annotation 
  :'@' (LBRACK loc+=('this'|NAME ) (',' loc+=('this'|NAME ) )* (',' )? RBRACK )?
     dotted_name 
    ( LPARA (namedAnnotationArgList |  expr_stmt  )?  RPARA )?
  ;

namedAnnotationArgList 
  : n2expr (',' n2expr )*  ','?
  ;
  
n2expr : NAME '=' expr_stmt;


///////////// common /////////////

genericQualiList: '<' nameAndUpperBound  (',' nameAndUpperBound )* ','? '>';

nameAndUpperBound: NAME namedType? nullable='?'?;

dottedNameList 
	: dotted_name (',' dotted_name  )* ','?
	;

inoutGenericModifier : 'in' | 'out';

onChangeEtcArgs
 : onChangeEtcArg (',' onChangeEtcArg)*     
 ;
 
onChangeEtcArg
	: valvar=(VAL|VAR)? NAME (type | (refCnt+=':')+)? '=' expr_stmt
	| expr_stmt
	;

dotted_name
	: NAME ( DOT  NAME )*
	;

assignStyle : '\\=' | '=' | '+=' | '-=' | '*=' | '/=' | 'mod=' | '**=' | 'or=' | 'and=' | '<<=' | '>>=' | '>>>=' | 'band=' | 'bor='| 'bxor=';

block 
  : NEWLINE* block_
  ;	
  
block_
  : LBRACE  line* RBRACE
  ;	
  
pureFuncInvokeArgs 
	: LPARA (pureFuncInvokeArg (',' pureFuncInvokeArg)* ','? )?  RPARA
	;

pureFuncInvokeArg
	: (NAME  '=' )? ( expr_stmt  |  primitiveType |  funcType | tupleType)
	;
	
	
funcRefArgs
  : LPARA (  funcRefArg (  ',' funcRefArg )* ','? )? RPARA
  ;

funcRefArg
	: (NAME '=')? ( '?' lazy='lazy'? type | lazy='lazy'? primitiveType | lazy='lazy'? funcType nullable='?'? | lazy='lazy'? LPARA tupleType RPARA nullable='?'? | lazy='lazy'? namedType nullable='?' | expr_stmt | (refcnt+=':')+ )
	;
	
genTypeList 
  :
	'<' genTypeListElemnt ( ',' genTypeListElemnt )* ','? '>'  
  ;
  
genTypeListElemnt
	: '?'   | ( inoutGenericModifier?  type )
	;	
	
	
///////////// types /////////////
trefOrArrayRef:
		hasAr=LBRACK (arLevels=intNode) RBRACK
	| (hasArAlt+=LBRACK RBRACK)+
	| refOrNullable
	;

refOrNullable: ':' dotted_name?
	| nullable='?'
	| nullableErr='??'
	;


type:
	 bareTypeParamTuple ('|' bareTypeParamTuple)* trefOrArrayRef*
	 ;


bareTypeParamTuple:
	pointerQualifier? primitiveType
	| namedType
	| funcType
	| LPARA tupleType RPARA
	;


typeNoNTTuple:
	 bareTypeParamTupleNoNT ('|' bareTypeParamTupleNoNT)* trefOrArrayRef*
	 ;

bareTypeParamTupleNoNT:
	pointerQualifier? primitiveType
	| namedType
	| funcType
	| LPARA tupleTypeNoNT RPARA
	;

tupleTypeNoNT : bareButTupleNoNT (',' bareButTupleNoNT )+ ;

bareButTupleNoNT: primitiveType | funcType;


ppp: inject=INJECT?  pp=(PRIVATE | PROTECTED | PUBLIC | PACKAGE)
	| inject=INJECT pp=(PRIVATE | PROTECTED | PUBLIC | PACKAGE)?;

pppNoInject: PRIVATE | PROTECTED | PUBLIC | PACKAGE;

pointerQualifier : (cnt+='*'|cnt2+='**')+;

//typeNoPrim : namedType | funcType | tupleType;

namedType 
  : isactor='actor' namedType_ExActor?
  | namedType_ExActor 
  ;
  
  
namedType_ExActor 
  :  primaryName=dotted_name priamryGens=genTypeList? (DOT nameAndgens)* ( 'of' of=namedType)? //'of' namedType ?
  ;
  
nameAndgens : NAME genTypeList?;

tupleType : bareButTuple (',' bareButTuple )+ ;

bareButTuple: (primitiveType | namedType | funcType ) trefOrArrayRef*;

funcType :
	funcType_ 
	| LPARA funcType_ RPARA
	; 

funcType_ 
	: genericQualiList?  
	    LPARA ( (type (',' type)*)? ','?  | constr='*' ) RPARA retTypeIncVoid
	;

retTypeIncVoid 
  :  type
  |  'void' 
  ;

primitiveType: ('boolean'|'bool') | 'size_t' | 'int' | 'long' | 'float' | 'double'	| 'byte' | 'short' | 'char' | 'lambda'; 

///////////// expresssions /////////////

expr_stmt_tuple : expr_stmt ( (',' expr_stmt)+ ','? )?;

expr_stmt 
	: for_list_comprehension
	;

for_list_comprehension 
	: mainExpr=expr_list ( flc_forStmt_+  ('if' condexpr=expr_stmt)?)?
	;

flc_forStmt_:
	forblockvariant (localVarName=NAME localVarType=type?  | ( LPARA forVarTupleOrNothing (',' forVarTupleOrNothing)+ RPARA))   'in' expr=expr_list
	;



expr_list 
	: /*block_async |*/  lambdadefOneLine | lambdadef | anonLambdadef | expr_stmt_+ ;//shortcut in the lambdadef as it ends with a newline so cannot be an expr stmt


lambdadefOneLine : annotations? 'def' genericQualiList?  LPARA  funcParams? RPARA retTypeIncVoid? single_line_block ;

lambdadef  
    : annotations? 'def' genericQualiList?  LPARA  funcParams? RPARA retTypeIncVoid? (block | (single_line_block NEWLINE+))  ;


anonLambdadef : ((NAME (',' NAME)*) | LPARA ( typeAnonParam (',' typeAnonParam)*) RPARA) retType=type? single_line_block;

typeAnonParam: NAME type?;

expr_stmt_: if_expr//for_list_comprehension
 	;

if_expr: op1=expr_stmt_or ('if' test=expr_stmt_or 'else' op2=expr_stmt_or )?;

expr_stmt_or: head=expr_stmt_and ( 'or'  ors+=expr_stmt_and)*;

expr_stmt_and: head=bitwise_or ( 'and'  ands+=bitwise_or)*;


bitwise_or: head=bitwise_xor ( 'bor'  ands+=bitwise_xor)*;
bitwise_xor: head=bitwise_and ( 'bxor'  ands+=bitwise_and)*;
bitwise_and: head=expr_stmt_BelowEQ ( 'band'  ands+=expr_stmt_BelowEQ)*;


expr_stmt_BelowEQ : head=instanceof_expr ( eqAndExpression_)*; 
eqAndExpression_: equalityOperator instanceof_expr;

instanceof_expr : castExpr (( 'is' | invert='isnot' | ('is' invert='not')) type ('or' type)* )?;

castExpr: lTGTExpr ('as' type)*;

lTGTExpr : shiftExpr ( relOpAndExpression_)*;
relOpAndExpression_: relationalOperator  shiftExpr;


shiftExpr: additiveExpr ( shiftExprOp_)*;
shiftExprOp_: (lshift='<' '<' | rshift='>' '>' | rshiftu='>' '>' '>')  additiveExpr;


additiveExpr: divisiveExpr ( additiveOp_)*;
additiveOp_ : op=('+'|'-')  divisiveExpr;

divisiveExpr: powExpr ( divisiveExprOP_)*;
divisiveExprOP_:op=('*'|'/'|'mod')  powExpr;

powExpr :  lhs=notExpr ( '**'  rhs+=notExpr)*; 

notExpr: isnot='not'? containsExpr;

containsExpr : lhs=prefixExpr ( (invert='not'? 'in')  rhs=prefixExpr)?;

prefixExpr : prefixOp=('++' | '--' | '-' | '+' | 'comp')?	 postfixExpr;

postfixExpr : sizeOfExpr postfixOp=('++' | '--')?;

sizeOfExpr: (sizeof='sizeof'  ('<' variant=dotted_name '>')? )? asyncSpawnExpr;

asyncSpawnExpr: notNullAssertion (isAsync='!' (LPARA expr_stmt RPARA)? )?;


notNullAssertion :  elvisOperator ( nna='??')?;
elvisOperator :  lhsExpr=vectorize ( '?:'  elsExpr=if_expr)?  ;


vectorize: primary=vectorize vectorize_element+
	| passthrough=dotOperatorExpr
	;

vectorize_element:
	nullsafe='?'? ('^' (doubledot='^')? ) (constru=constructorInvoke | arrayRefElements+ | afterVecExpr=refName genTypeList? (pureFuncInvokeArgs | '&' funcRefArgs?)? )? 
	;

dotOperatorExpr: ((pntUnrefCnt+='*'|pntUnrefCnt2+='**' )+ | address='~')? copyExpr ( NEWLINE* dotOpArg NEWLINE* copyExpr)*;

copyExpr : expr_stmt_BelowDot (isCopy='@' (hasCopier=LPARA ( (copyExprItem  (',' copyExprItem)* ','?)? (';' modifier+=NAME (',' modifier+=NAME)* ','?  )? ) RPARA )? )?;

copyExprItem: ename=NAME '=' expr_stmt
	|  incName=NAME
	| '<' exclName+=NAME (',' exclName+=NAME)* ','? '>'
	| (copyName=NAME | superCopy='super' )'@' ( hasCopier=LPARA ( (copyExprItem  (',' copyExprItem)* ','?)?  (';' modifier+=NAME (',' modifier+=NAME)* ','? )? ) RPARA )? 
	;


expr_stmt_BelowDot //seperate rule for match operations - basically atoms
	: (isthis='this' pureFuncInvokeArgs | 'super' pureFuncInvokeArgs) #superOrThisConstructorInvoke
	| NAME genTypeList? pureFuncInvokeArgs #FuncInvokeExprName
	| expr_stmt_BelowDot genTypeList? pureFuncInvokeArgs #FuncInvokeExpr
	
	| NAME genTypeList #RefQualifiedGeneric //{ $ret = new RefQualifiedGenericNamedType(getLine(input), getColumn(input), $namedT.text, $gg.genTypes);   }//ret namedType
    | LPARA 'actor' dotted_name genTypeList? RPARA #RefQualifiedGenericActor //{ $ret = gg==null? new RefQualifiedGenericNamedType(getLine(input), getColumn(input), $namedTa.ret, true) : new RefQualifiedGenericNamedType(getLine(input), getColumn(input), $namedTa.ret, $gg.genTypes, true);   }//ret namedType
    
	| expr_stmt_BelowDot genTypeList? '&' funcRefArgs? #FuncRefExpr
	| expr_stmt_BelowDot (refCnt+=':')* arrayRefElements+ (extraEmptyBracks+=LBRACK RBRACK)* #ArrayRefExpr
	| main=expr_stmt_BelowDot (refCnt+=':')+ post=expr_stmt_BelowDot? #RefExpr
	| notNullAssertion2 #AtomPassThrough
	;


arrayRefElements:  (nullSafe='?'? LBRACK arrayRefElement (',' arrayRefElement)* (trailcomma+=',')* RBRACK )  ;

notNullAssertion2 :  atom (NEWLINE* nna='??')?;

atom 
	: classNode 
	| thisNode
	| outNode
	| refName
	| superNode
	| ofNode
	| annotation
	| booleanNode
	| changedNode
	| arrayDef
	| mapDef
	| constructorInvoke
	| compound_stmt_atomic_base //set ret.setShouldBePresevedOnStack(true); after extraction
	| localclassdef
	| anonclassdef
	| lambdadef
	| intNode
	| longNode
	| shortNode
	| floatNode
	| doubleNode
	| nullNode
	| stringNode
	| regexStringNode
	| langExtNode
	| nestedNode
	;//move to add Labels above

nestedNode: LPARA expr_stmt_tuple RPARA;

classNode: type '.' 'class';
superNode : 'super' (LBRACK superQuali=dotted_name RBRACK)? ( dotOpArg expr_stmt_BelowDot )+;
changedNode:'changed';
outNode:'out';
thisNode:'this' (LBRACK (thisQuali=dotted_name| thisQualiPrim=primitiveType) RBRACK)?;
nullNode: 'null';
intNode : INT;
longNode : LONGINT;
shortNode : SHORTINT;
floatNode : FLOAT;
doubleNode : DOUBLE;
booleanNode : 'true' | 'false';
stringNode : STRING_ITMcit | isQuote=STRING_ITMquot;
langExtNode: name=NAME body=LANG_EXT;
regexStringNode : REGEX_STRING_ITM;
ofNode : 'of';

arrayRefElement
	: (lhs=expr_stmt DDD rhs=expr_stmt)
	| (post=expr_stmt DDD )
	| ( DDD pre=expr_stmt)
	| ( simple=expr_stmt )
	;

refName
	: NAME 
	;
	

dotOpArg
	: '.'|'\\.'|'..'|'?.'
	;

arrayDef 
	: LBRACK RBRACK
	| (isArray=ALBRACK | LBRACK) NEWLINE* ((expr_stmt  ( ( NEWLINE* ',' NEWLINE* expr_stmt )+  ','? | NEWLINE*',')  ) | ',') NEWLINE* RBRACK //list def or single element array
	| arrayDefComplex
	;

arrayDefComplex: (ALBRACK | LBRACK) expr_stmt_+ arrayDefComplexNPLus1Row* (NEWLINE* ';' NEWLINE*)? RBRACK
	;

arrayDefComplexNPLus1Row
	: (';' NEWLINE* | NEWLINE+) expr_stmt_+
	;

mapDef 
	: LBRACE NEWLINE*  mapDefElement  (NEWLINE* ',' NEWLINE* mapDefElement )* (NEWLINE* ',')?  NEWLINE* RBRACE
	;

mapDefElement: (isDefault='default' | key=expr_stmt) NEWLINE*  '->' NEWLINE*  value=expr_stmt;


///////////// expresssions:constructors/////////////

constructorInvoke 
	: namedActorConstructor 
	| ( 'new'  ( namedConstructor | arrayConstructor |  primNamedOrFuncType refOrNullable+  ) ) //
	| arrayConstructorPrimNoNew
	| newreftypeOnOwn 
	;

namedConstructor
	: type  ( ( isConsRef='&' funcRefArgs) | isConsRef='&' | pureFuncInvokeArgs)
	;

namedActorConstructor
  : isNewDefiend='new'? 'actor' namedType_ExActor ( (isConsRef='&' funcRefArgs) | isConsRef='&' | pureFuncInvokeArgs)
  ;

arrayConstructor
  : 
   primNamedOrFuncType ('|' primNamedOrFuncType)* 
   		 (LBRACK  arconExprsSubsection RBRACK)+ (nullEnd+=LBRACK RBRACK)* 
   		 (LPARA expr_stmt_tuple RPARA )?
   		 //constructor args...
   		 
  ;
  
primNamedOrFuncType : (pointerQualifier? primitiveType | namedType |  funcType | tupleType) refOrNullable*;

arrayConstructorPrimNoNew:
   primitiveType (LBRACK  arconExprsSubsection RBRACK)+ (nullEnd+=LBRACK RBRACK)*
   	(LPARA expr_stmt_tuple RPARA )?
	;

arconExprsSubsection:
	expr_stmt (',' expr_stmt  )* (commaEnd+=',')*
	;
	

newreftypeOnOwn 
  : typex=typeEclRef trefOrArrayRef+ pureFuncInvokeArgs
  ;

typeEclRef
  : primitiveType | namedType  | funcType | tupleType
  ;


///////////// expresssions:operators /////////////
 
equalityOperator 
  : '==' | '&==' | '<>' | '&<>' 
  ;

relationalOperator 
  : '<' | '>' | '>==' | '<==' 
  ;

//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~//TOKEN Names//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~//

VAL: 'val';
VAR: 'var';


PRIVATE: 'private';
PUBLIC: 'public';
INJECT: 'inject';
PROTECTED: 'protected';
PACKAGE:'package';





/////////////////////////////////////////////////////////lexer/////////////////////////////////////////////////////////



DOT: '.';

DOTDOT: '..';

DDD: '...';


LONGINT
    :   INT ('l'|'L')
    ;
    
SHORTINT
    :   INT ('s'|'S')
    ;

fragment HexDigits
    : HexDigit ((HexDigit | '_')* HexDigit)?
    ;

fragment HexDigit
    : [0-9a-fA-F]
    ;

	
fragment
HexIntegerLiteral
	:	'0' [xX] HexDigits
	;

fragment
DIGITS : ( '0' .. '9' )+ ;

INT :  HexIntegerLiteral 
    |   (
    	'0' ( 'b' | 'B' ) ( '0' .. '9'  )+
	    |   '0' DIGITS*
	    |   '1'..'9' DIGITS* //TODO: starting with as many 0 as you like is ok
	    )
    ;


fragment DBL_FRAG
  :  '.' DIGITS (Exponent)?
    |   DIGITS ( '.' (DIGITS (Exponent)?)? | Exponent)
    ;

fragment
Exponent
	:	('e' | 'E') ( '+' | '-' )? DIGITS
	;
	

FLOAT :  (DBL_FRAG | INT) ('f' |'F');

DOUBLE:  DBL_FRAG ('d'|'D')? | INT ('d'|'D');

NAME: //{permitDollarPrefixRefName}? => '$'+ NAME_ITMS |
	 NAME_ITMS//a nice list of keywords...!!! <- do this before release!
	 //TODO: add other keywords here
	 | '\\and' {setText("and");} 
	 | '\\or' {setText("or");}
	 | '\\not' {setText("not");} 
	 | '\\mod' {setText("mod");} 
	 | '\\comp' {setText("comp");}  
	 | '\\annotation' {setText("annotation");} 
	 | '\\assert' {setText("assert");} 
	 | '\\del' {setText("del");} 
	 | '\\open' {setText("open");}
	 | '\\in' {setText("contains");}
	 | '\\out' {setText("out");}
	 | '\\private' {setText("private");}
	 | '\\public' {setText("public");}
	 | '\\class' {setText("class");}
	 | '\\def' {setText("def");}
	 | '\\fun' {setText("fun");}
	 | '\\new' {setText("new");}
	 | '\\also' {setText("also");}
	 | '\\val' {setText("val");}
	 | '\\var' {setText("var");}
	 | '\\to' {setText("to");}
	 | '\\sizeof' {setText("sizeof");}
	 | '\\single' {setText("single");}
	 | '\\provide' {setText("provide");}
	 | '\\provider' {setText("provider");}
	 | '\\inject' {setText("inject");}
	 | '\\for' {setText("for");}
	 | '\\constant' {setText("constant");}
	 | '\\local' {setText("local");}
	 | '\\global' {setText("global");}
	 //TODO: add backslash as a general escape pattern
	;

fragment
NAME_ITMS: {permitDollarPrefixRefName}? ('0' .. '9')* ( 'a' .. 'z' | 'A' .. 'Z' | '_' | '$' | '\u0080'..'\ufffe') ( 'a' .. 'z' | '$' | 'A' .. 'Z' | '_' | '0' .. '9' | '\u0080'..'\ufffe' )* 
  | ('0' .. '9')* ( 'a' .. 'z' | 'A' .. 'Z' | '_' | '\u0080'..'\ufffe') ( 'a' .. 'z' | 'A' .. 'Z' | '_' | '0' .. '9' | '\u0080'..'\ufffe' )* 
  ;

fragment EscapeSequence
    : '\\' [btnfr{"'\\]
    | '\\' ([0-3]? [0-7])? [0-7]
    | '\\' 'u'+ HexDigit HexDigit HexDigit HexDigit
    ;


STRING_ITMcit
  : '"' (   ~( '\\' | '"'  ) | EscapeSequence )*'"' 
  ;
STRING_ITMquot
  : '\'' (   ~( '\\' | '\''  ) | EscapeSequence )*'\'' 
  ;

fragment EscapeSequenceLE
    : '\\' [{|\\]
	;
	
	
LANG_EXT
  :  '||' (   ~( '\\' | '|'  ) | EscapeSequenceLE )* '||'  
  ;

    
REGEX_STRING_ITM
  : 'r"' (   ~( '\\' | '"' )  )*'"' 
  | 'r\'' (  ~( '\\' | '\'' )  )*'\''
  ;

MULTILINE_COMMENT
    :   '/*'
        ( (MULTILINE_COMMENT | .) )*?
        '*/' -> skip
    ;

LINE_COMMENT
    : '//' ~('\n'|'\r')*  -> skip
    ;
    
IGNORE_NEWLINE  :  '\r'? '\n' {skipNewLine}? -> skip ;	
NEWLINE  :  '\r'? '\n';	

	
LPARA: '(' { prevskip.add(skipNewLine); skipNewLine=true; } ;
RPARA: ')' { skipNewLine=prevskip.isEmpty()?false:prevskip.pop(); };
LBRACK: '['{ prevskip.add(skipNewLine); skipNewLine=false; } ;
ALBRACK: 'a['{ prevskip.add(skipNewLine); skipNewLine=false; } ;
RBRACK: ']'{ skipNewLine=prevskip.isEmpty()?false:prevskip.pop(); };	

LBRACE:'{'{ prevskip.add(skipNewLine); skipNewLine=false; } ;
RBRACE:'}'{ skipNewLine=prevskip.isEmpty()?false:prevskip.pop(); };	
	
	
WS  :  (' ' | '\t' | '\f' )+ -> skip	;
	

	

WS2 : 
	'\\' (' ' | '\t' | '\f' | LINE_COMMENT|MULTILINE_COMMENT )* ('\r'? '\n')+ -> skip
	;//ignore newline if prefixed with \ just like in python

