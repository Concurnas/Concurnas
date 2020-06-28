//##69. Returns - 1. return must be possible - simple elifs
x69Returns1returnmustbepossiblesimpleelifs.conc line 5:1 Return statement in method must return type of int
x69Returns1returnmustbepossiblesimpleelifs.conc line 11:1 if statement must return something
x69Returns1returnmustbepossiblesimpleelifs.conc line 17:1 if statement must have else block when used in this way
x69Returns1returnmustbepossiblesimpleelifs.conc line 23:1 if statement must return something
x69Returns1returnmustbepossiblesimpleelifs.conc line 30:1 Return statement in method must return type of int
 
~~~~~
//##69. Returns - 2 - nested blocks

~~~~~
//##69. Returns - 3. return must be possible - simple exceptions
x69Returns3returnmustbepossiblesimpleexceptions.conc line 18:1 try catch must return something
x69Returns3returnmustbepossiblesimpleexceptions.conc line 18:16 catch block of try catch must return something
x69Returns3returnmustbepossiblesimpleexceptions.conc line 37:1 Unreachable code after return statement
x69Returns3returnmustbepossiblesimpleexceptions.conc line 51:1 Unreachable code after return statement
 
~~~~~
//##69. Returns - 4. deadcode analysis 1
x69Returns4deadcodeanalysis1.conc line 7:1 Unreachable code after return statement
x69Returns4deadcodeanalysis1.conc line 13:1 Unreachable code after return statement
x69Returns4deadcodeanalysis1.conc line 20:1 Unreachable code after exception thrown
x69Returns4deadcodeanalysis1.conc line 26:1 Unreachable code after exception thrown
x69Returns4deadcodeanalysis1.conc line 33:1 Unreachable code after exception thrown
x69Returns4deadcodeanalysis1.conc line 47:1 Unreachable code after exception thrown
x69Returns4deadcodeanalysis1.conc line 61:1 Unreachable code after exception thrown
 
~~~~~
//##69. Returns - 4. oh yeah, lambdas
x69Returns4ohyeahlambdas.conc line 4:19 if statement must return something
 
~~~~~
//##69. Returns - on other special stuff..
x69Returnsonotherspecialstuff.conc line 5:1 incompatible type: int vs java.util.List<java.lang.Integer>
x69Returnsonotherspecialstuff.conc line 11:5 Resource specified in try with resource block must implement close method, '{ null ; }' does not
x69Returnsonotherspecialstuff.conc line 17:5 Resource specified in try with resource block must implement close method, '{ null ; }' does not
x69Returnsonotherspecialstuff.conc line 24:5 Resource specified in try with resource block must implement close method, '{ null ; }' does not
x69Returnsonotherspecialstuff.conc line 28:1 Unreachable code after exception thrown
x69Returnsonotherspecialstuff.conc line 36:1 Unreachable code after exception thrown
 
~~~~~
//##69. Returns-deadcode - in classes and also nested functions et al
x69Returnsdeadcodeinclassesandalsonestedfunctionsetal.conc line 8:2 Return statement in method must return type of int
x69Returnsdeadcodeinclassesandalsonestedfunctionsetal.conc line 30:2 This method must return a result of type int
x69Returnsdeadcodeinclassesandalsonestedfunctionsetal.conc line 57:3 Unreachable code after exception thrown
x69Returnsdeadcodeinclassesandalsonestedfunctionsetal.conc line 70:3 Unreachable code after return statement
 
~~~~~
//##69. Returns-deadcode - 2 after break and cotinue
x69Returnsdeadcode2afterbreakandcotinue.conc line 7:2 Unreachable code after break
x69Returnsdeadcode2afterbreakandcotinue.conc line 14:2 Unreachable code after continue
x69Returnsdeadcode2afterbreakandcotinue.conc line 38:2 Unreachable code

~~~~~
//##69. break etc dont extend beyond the for block, or while etc
x69breaketcdontbleedbeyondtheforblockorwhileetc.conc line 26:2 Unreachable code after break
 
~~~~~
//##69. exceptions thrown in try catch make stuff unreachable
x69exceptionsthrownintrycatchmakestuffunreachable.conc line 10:2 Unreachable code after exception thrown
 
~~~~~
//##70. double check returns inside lambda

~~~~~
//##71. unreachable code after exception raised inside catch block
x71unreachablecodeafterexceptionraisedinsidecatchblock.conc line 12:8 Unreachable code after return statement
x71unreachablecodeafterexceptionraisedinsidecatchblock.conc WARN line 8:19 Unable to determine type for generic: T for as operation. Wil use upper bounds of: java.lang.Object instead
x71unreachablecodeafterexceptionraisedinsidecatchblock.conc WARN line 19:19 Unable to determine type for generic: T for as operation. Wil use upper bounds of: java.lang.Object instead
 
~~~~~
//##72. this is fine

~~~~~
//##73. this is also fine

~~~~~
//##74. defo returns in all catches means all returns

~~~~~
//##75. defo returns in some catches not all return

~~~~~
//##76.1 misc

~~~~~
//##76.1.1 inifinite loop - for - on own
x7611inifiniteloopforonown.conc line 6:1 Unreachable code
 
~~~~~
//##76.1.2 inifinite loop - for - tcf
x7612inifiniteloopfortcf.conc line 10:1 Unreachable code
x7612inifiniteloopfortcf.conc line 22:1 Unreachable code
 
~~~~~
//##76.1.3 inifinite loop - for - if
x7613inifiniteloopforif.conc line 11:1 Unreachable code
x7613inifiniteloopforif.conc line 23:1 Unreachable code
 
~~~~~
//##76.1.4 inifinite loop - for - for old
x7614inifiniteloopforforold.conc line 10:1 Unreachable code
 
~~~~~
//##76.1.5 inifinite loop - for - for old 
x7615inifiniteloopforforold.conc line 4:29 Unreachable code
x7615inifiniteloopforforold.conc line 4:29 n cannot be resolved to a variable
x7615inifiniteloopforforold.conc line 4:38 n cannot be resolved to a variable
x7615inifiniteloopforforold.conc line 7:1 Unreachable code

~~~~~
//##76.1.5.b inifinite loop - for - for old cond 
x7615binifiniteloopforforoldcond.conc line 5:32 Unreachable code
x7615binifiniteloopforforoldcond.conc line 8:1 Unreachable code
 
~~~~~
//##76.1.6 inifinite loop - for - for new
x7616inifiniteloopforfornew.conc line 10:1 Unreachable code
 
~~~~~
//##76.1.7 inifinite loop - for - while 
x7617inifiniteloopforwhile.conc line 16:1 Unreachable code
 
~~~~~
//##76.1.8 inifinite loop - for - ananon block 
x7618inifiniteloopforananonblock.conc line 8:1 Unreachable code
 
~~~~~
//##76.1.9 inifinite loop - for - async block

~~~~~
//##76.1.10 inifinite loop - for - barrier block and with block

~~~~~
//##76.1.1 inifinite loop - while - goes on forever cases
x7611inifiniteloopwhilegoesonforevercases.conc line 7:1 Unreachable code
x7611inifiniteloopwhilegoesonforevercases.conc line 12:1 Unreachable code
x7611inifiniteloopwhilegoesonforevercases.conc line 17:1 Unreachable code
x7611inifiniteloopwhilegoesonforevercases.conc line 22:1 Unreachable code
 
~~~~~
//##77. misc bug, last thing ret doesnt have to be an expression
x77miscbuglastthingretdoesnthavetobeanexpression.conc line 6:0 This method must return a result of type java.lang.String
 
~~~~~
//##78. onchange return analysis at least one must return here
x78onchangereturnanalysisatleastonemustreturnhere.conc line 7:19 onchange must return something

~~~~~
//##79. await validation for ret
x79awaitvalidationforret.conc line 6:0 Type mismatch: cannot convert from int to boolean
 
~~~~~
//##80. await validation for ret -2
x80awaitvalidationforret2.conc line 6:23 return statement must define something to return
 
~~~~~
//##81. this inf loop triggers no deadcode

~~~~~
//##82. if statement resolves always to true or false
x82ifstatementresolvesalwaystotrueorfalse.conc line 9:4 if test always resolves to true, so there is no need to use an if statement
x82ifstatementresolvesalwaystotrueorfalse.conc line 13:4 if test always resolves to false, so there is no need to use an if statement
x82ifstatementresolvesalwaystotrueorfalse.conc line 17:4 if test always resolves to true - as such elseif or else blocks will never be executed
x82ifstatementresolvesalwaystotrueorfalse.conc line 21:4 if test always resolves to false - as such code in if block will never be executed
x82ifstatementresolvesalwaystotrueorfalse.conc line 25:27 elif test always resolves to true, use an else block instead
x82ifstatementresolvesalwaystotrueorfalse.conc line 29:27 elif test always resolves to false, so there is no need to use an elif statement
x82ifstatementresolvesalwaystotrueorfalse.conc line 33:27 elif test always resolves to true - as such later elseif or else blocks will never be executed
x82ifstatementresolvesalwaystotrueorfalse.conc line 37:27 elif test always resolves to false - as such code in elif block will never be executed
 
~~~~~
//##83. if expresssion resolves always to true or false
x83ifexpresssionresolvesalwaystotrueorfalse.conc line 6:10 if expresssion always resolves to true, so there is no need to use an if expression
x83ifexpresssionresolvesalwaystotrueorfalse.conc line 10:10 if expresssion always resolves to false, so there is no need to use an if expression

~~~~~
//##84. impossible to assign on paths which return dont invalidate
