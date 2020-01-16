//##1. pre defined seperate lang extensions
x1predefinedseperatelangextensions.conc line 3:0 Language extension to use: com.concurnas.tests.helpers.langExt.Normal must implement trait: com.concurnas.lang.LangExt.LanguageExtension
 
~~~~~
//##2. pre defined seperate lextensions import validation
x2predefinedseperatelangextensionsimportvalidation.conc line 6:0 Using name: com.concurnas.tests.helpers.langExt.NonExist cannot be resolved to a class
x2predefinedseperatelangextensionsimportvalidation.conc line 7:0 Language extension to use: com.concurnas.tests.helpers.langExt.Normal must implement trait: com.concurnas.lang.LangExt.LanguageExtension
x2predefinedseperatelangextensionsimportvalidation.conc line 10:10 Unable to resolve type corresponding to name: MyHiLang
x2predefinedseperatelangextensionsimportvalidation.conc WARN line 13:1 Class name overwrites imported class: MyHiLang
 
~~~~~
//##3. pre defined seperate lang extensions - same time declared modules
x3predefinedseperatelangextensionssametimedeclaredmodules.conc line 2:0 Using name: com.myorg.langs.lispLike.MyLisp cannot be resolved to a class
x3predefinedseperatelangextensionssametimedeclaredmodules.conc line 5:0 Using name: com.myorg.langs.lispLike.MyFaulty cannot be resolved to a class
x3predefinedseperatelangextensionssametimedeclaredmodules.conc line 8:10 Unable to resolve type corresponding to name: MyHiLang
x3predefinedseperatelangextensionssametimedeclaredmodules.conc line 2:0 Using name: com.myorg.langs.lispLike.MyLisp cannot be resolved to a class
x3predefinedseperatelangextensionssametimedeclaredmodules.conc line 5:0 Using name: com.myorg.langs.lispLike.MyFaulty cannot be resolved to a class
x3predefinedseperatelangextensionssametimedeclaredmodules.conc line 8:10 Unable to resolve type corresponding to name: MyHiLang

~~~~~
//##4. invalid code producted by lange ext
x4invalidcodeproductedbylangeext.conc line 6:8 Language extension: InvalidCodeOutput has produced invalid Concurnas code: Internal compiler error: [InvalidCodeOutput line 6:1 extraneous input '>' expecting {<EOF>, ';', 'transient', 'shared', 'lazy', 'override', '-', '+', '~', 'assert', 'del', 'break', 'continue', 'throw', 'return', 'import', 'using', '*', 'from', 'typedef', '<', 'await', 'def', 'gpudef', 'gpukernel', '**', '++', '--', 'not', 'comp', 'global', 'local', 'constant', 'out', 'this', 'provider', 'abstract', 'open', 'closed', 'class', 'trait', 'actor', 'of', 'with', 'new', 'annotation', 'enum', 'for', 'parfor', 'parforsync', 'match', 'if', 'async', 'while', 'loop', 'try', 'trans', 'init', 'sync', 'onchange', 'every', '@', 'boolean', 'bool', 'size_t', 'int', 'long', 'float', 'double', 'byte', 'short', 'char', 'lambda', 'sizeof', 'super', 'changed', 'null', 'true', 'false', 'val', 'var', 'private', 'public', 'inject', 'protected', 'package', LONGINT, SHORTINT, INT, FLOAT, DOUBLE, NAME, STRING_ITMcit, STRING_ITMquot, REGEX_STRING_ITM, NEWLINE, '(', '[', 'a[', '{'}]~Invalid code:~s><s

~~~~~
//##5. uncuaght exceptions in init and iterator
x5uncuaghtexceptionsininitanditerator.conc line 6:8 Error when executing initializer for: ThrowsExceptionInit as: java.lang.Exception: unexpected in init~~	at java.base/java.lang.Throwable.init(Throwable.java:270)~~	at java.base/java.lang.Exception.init(Exception.java:66)~~	at com.concurnas.tests.helpers.langExt$ThrowsExceptionInit.initialize(com.concurnas.tests.helpers.langExt.conc:51)~~	at ExtLangLoader$$Init1.apply(ExtLangLoader$$Init1.java)~~	at ExtLangLoader$$Init1.apply(ExtLangLoader$$Init1.java)~~	at java.base/com.concurnas.bootstrap.runtime.cps.IsoCore._runExecute(IsoCore.java:25)~~	at java.base/com.concurnas.bootstrap.runtime.cps.Iso.execute(Iso.java:48)~~	at java.base/com.concurnas.bootstrap.runtime.cps.Worker.run(Worker.java:56)~~
x5uncuaghtexceptionsininitanditerator.conc line 7:8 Error when executing iterator for: ThrowsExceptionIer as: java.lang.Exception: unexpected in iterate~~	at java.base/java.lang.Throwable.init(Throwable.java:270)~~	at java.base/java.lang.Exception.init(Exception.java:66)~~	at com.concurnas.tests.helpers.langExt$ThrowsExceptionIer.iterate(com.concurnas.tests.helpers.langExt.conc:66)~~	at ExtLangLoader$$Iter2.apply(ExtLangLoader$$Iter2.java)~~	at ExtLangLoader$$Iter2.apply(ExtLangLoader$$Iter2.java)~~	at java.base/com.concurnas.bootstrap.runtime.cps.IsoCore._runExecute(IsoCore.java:25)~~	at java.base/com.concurnas.bootstrap.runtime.cps.Iso.execute(Iso.java:48)~~	at java.base/com.concurnas.bootstrap.runtime.cps.Worker.run(Worker.java:56)~~
 
~~~~~
//##6. needs zero arg constructor
x6needszeroargconstructor.conc line 3:0 Language extension to use: com.concurnas.tests.helpers.langExt.NoZeoArg must provide a public zero argument constructor
x6needszeroargconstructor.conc line 7:8 Language extension: NoZeoArg has not been imported for use
x6needszeroargconstructor.conc line 8:8 NoZeoArgPriv has produced invalid Concurnas code: empty String
 
~~~~~
//##7. no empty string output
x7noemptystringoutput.conc line 6:8 RetNothing has produced invalid Concurnas code: empty String

~~~~~
//##8. throw normal errors - iter
x8thrownormalerrorsiter.conc line 7:8 SimpleLisp: Unknown variable: m

~~~~~
//##9. throw normal errors - init
x9thrownormalerrorsinit.conc line 7:8 SimpleLisp: unexpected token: n
 
~~~~~
//##10. type checking
x10typechecking.conc line 7:8 SimpleLisp: Variable: n is expected to be of numerical type

~~~~~
//##11. methods
x11methods.conc line 10:8 Imp: Cannot find method: asd
x11methods.conc line 12:8 Imp: Cannot find method: thing
 
~~~~~
//##12. errors reported with lang prefix
x12errorsreportedwithlangprefix.conc line 10:0 SimpleLisp: Unable to find method with matching name: asd and arguments (int)
 