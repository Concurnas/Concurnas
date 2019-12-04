//##69. Lambda - creation def 1
x69Lambdacreationdef1.conc line 3:16 cannot assign type of java.lang.Integer to com.concurnas.bootstrap.lang.Lambda$Function1<java.lang.Integer, out java.lang.Integer>
x69Lambdacreationdef1.conc line 5:17 cannot assign type of java.lang.Integer to com.concurnas.bootstrap.lang.Lambda$Function1<java.lang.Integer, out java.lang.Integer>
 
~~~~~
//##69. Lambda - creation def 2
x69Lambdacreationdef2.conc line 4:20 Generic Type argument type mismatch: java.lang.Double vs java.lang.Float
x69Lambdacreationdef2.conc line 5:21 Generic Type argument type mismatch: java.lang.Double vs java.lang.Boolean
 
~~~~~
//##69. Lambda - creation def 3 - ensure subbj correctly
x69Lambdacreationdef3ensuresubbjcorrectly.conc line 4:18 Generic Type argument type mismatch: java.lang.Object vs java.lang.String
x69Lambdacreationdef3ensuresubbjcorrectly.conc line 5:18 Generic Type argument type mismatch: java.lang.String vs java.lang.Object
x69Lambdacreationdef3ensuresubbjcorrectly.conc line 12:14 Generic Type argument type mismatch: x69Lambdacreationdef3ensuresubbjcorrectly.A vs x69Lambdacreationdef3ensuresubbjcorrectly.B
x69Lambdacreationdef3ensuresubbjcorrectly.conc line 13:14 Generic Type argument type mismatch: x69Lambdacreationdef3ensuresubbjcorrectly.B vs x69Lambdacreationdef3ensuresubbjcorrectly.A
 
~~~~~
//##69. Lambda - creation def 4 - arrays
x69Lambdacreationdef4arraysasargs.conc line 4:20 Generic Type argument type mismatch: int[2] vs int[]
x69Lambdacreationdef4arraysasargs.conc line 5:21 Generic Type argument type mismatch: int[6] vs int[5]
 
~~~~~
//##69. Lambda - creation def 5 - arrays of funcs -> wow!
 
~~~~~
//##69. Lambda - creation def 5.2 - generics parameters 



~~~~~
//##69. Lambda - creation def 6 - lambda catch all
 
~~~~~
//##69. Lambda - creation def 7 - object is master of all

~~~~~
//##69. Lambda - currying 1 - curry existing lambda
x69Lambdacurrying1curryexistinglambda.conc line 3:22 Unable to resolve type corresponding to name: doulble
x69Lambdacurrying1curryexistinglambda.conc line 3:32 Unable to determine type of: doulble
x69Lambdacurrying1curryexistinglambda.conc line 4:13 Unable to resolve type corresponding to name: iint
x69Lambdacurrying1curryexistinglambda.conc line 12:35 Unable to find method with matching name: masterSimple and arguments (int, int, java.lang.Object)
x69Lambdacurrying1curryexistinglambda.conc line 13:39 Generic Type argument type mismatch: java.lang.String vs java.lang.Integer
x69Lambdacurrying1curryexistinglambda.conc line 18:34 Type array levels don't match. Expected: 0 vs 1
 
~~~~~
//##69. Lambda - currying 11. - curry chain


~~~~~
//##69. Lambda - currying 2 - curry existing function

~~~~~
//##69. Lambda - currying 2.1 - simple case
x69Lambdacurrying21simplecase.conc line 7:19 For variable reference on: 'innaproperiate' it must be a method or method reference not: int
 
~~~~~
//##69. Lambda - currying 2.2 - avoid masking
x69Lambdacurrying22avoidmasking.conc line 5:0 Method reference variable 'mask' hides existing named method or method reference definition
 
~~~~~
//##69. Lambda - currying 2.3 - search upper nest
x69Lambdacurrying23searchuppernest.conc line 42:1 Method ffo is hidden by existing variable declared as a method reference variable
 
~~~~~
//##69. Lambda - currying 2.4 - search upper nest layered




~~~~~
//##69. Lambda - currying 3 - curry existing method on object


~~~~~
//##69. Lambda - currying 4 - curry existing method on class
x69Lambdacurrying4curryexistingmethodonclass.conc line 19:36 Unable to find method with matching name: foo
 
~~~~~
//##69. Lambda - currying 5 - setting class members etc

~~~~~
//##69. Lambda - currying 6 - generics

~~~~~
//##69. Lambda - erasure 1 - simple
x69Lambdaerasure1simple.conc line 8:1 Method x with matching argument definition exists already in current Scope - generic types are erased at runtime
x69Lambdaerasure1simple.conc line 14:1 Constructor with matching argument definition exists already in current Scope - generic types are erased at runtime
 
~~~~~
//##69. Lambda - erasure 2 - simple overrides
x69Lambdaerasure2simpleoverrides.conc line 10:1 Method: 'def x(def (java.lang.Integer) out java.lang.Integer) void' of class: 'x69Lambdaerasure2simpleoverrides.My' should be explicitly overriden in subclass: x69Lambdaerasure2simpleoverrides.BadChild. Use the override keyword
 
~~~~~
//##69. Lambda - erasure 3 - simple abstract
x69Lambdaerasure3simpleabstract.conc line 14:1 In order for the method 'def x(def (java.lang.Integer) out java.lang.Integer) void' of class: 'x69Lambdaerasure3simpleabstract.BadChild' to be overriden it must be defined in the superclass: x69Lambdaerasure3simpleabstract.My - its been declared abstract
 
~~~~~
//##69. Lambda - erasure 3 - super has thing already
x69Lambdaerasure3superhasthingalready.conc line 10:1 Method def (def (java.lang.Double) out java.lang.Integer) void with matching argument definition exists already in supertype or traits as: def (def (java.lang.Integer) out java.lang.Integer) void - generic types are erased at runtime
 
~~~~~
//##69. Lambda - generics mist - amazing this works

~~~~~
//##69. Lambda - equals is always false - should this warn?


~~~~~
//##69. Lambda - cast
x69Lambdacast.conc line 6:4 Cannot cast from com.concurnas.bootstrap.lang.Lambda$Function1<java.lang.Integer, java.lang.Integer> to def (java.lang.Integer, java.lang.Double) out java.lang.Integer
x69Lambdacast.conc line 9:7 Cannot cast from com.concurnas.bootstrap.lang.Lambda$Function1<java.lang.Integer, java.lang.Integer> to def (java.lang.Integer) out java.lang.String
x69Lambdacast.conc line 11:7 Cannot cast from java.lang.String to def (java.lang.Integer) out java.lang.String
 
~~~~~
//##69. Lambda - is isnot
x69Lambdaisisnot.conc line 5:4 Cannot compare an instance of com.concurnas.bootstrap.lang.Lambda$Function1<java.lang.Integer, java.lang.Integer> with def (java.lang.Integer, java.lang.Double) out java.lang.Integer
x69Lambdaisisnot.conc line 8:7 Cannot compare an instance of com.concurnas.bootstrap.lang.Lambda$Function1<java.lang.Integer, java.lang.Integer> with def (java.lang.Integer) out java.lang.String
x69Lambdaisisnot.conc line 10:7 Cannot compare an instance of java.lang.String with def (java.lang.Integer) out java.lang.String
 
~~~~~
//##69. Lambda - example

~~~~~
//##69. Lambda - FunctionN type...
x69LambdaFunctionNtype.conc line 9:11 cannot assign type of com.concurnas.bootstrap.lang.Lambda$Function3v<java.lang.String, java.lang.String, java.lang.String> to com.concurnas.bootstrap.lang.Lambda$Function3<java.lang.String, java.lang.String, java.lang.String, java.lang.Void>?
x69LambdaFunctionNtype.conc line 11:14 com.concurnas.bootstrap.lang.Lambda$Function0<java.lang.Void> is not a subtype of com.concurnas.bootstrap.lang.Lambda$Function0v
x69LambdaFunctionNtype.conc line 13:5 Void is not a valid input type
 
~~~~~
//##69. Lambda - of a inner function...



~~~~~
//##69. Lambda - misc voids
x69Lambdamiscvoids.conc line 5:1 cannot assign type of com.concurnas.bootstrap.lang.Lambda$Function1v<java.lang.Integer> to com.concurnas.bootstrap.lang.Lambda$Function1<java.lang.Integer, out java.lang.Integer>
x69Lambdamiscvoids.conc line 5:27 void methods cannot return a value
 
~~~~~
//##69. Lambda - call externally a function


~~~~~
//##69. Lambda - make sure both maskings are present
x69Lambdamakesurebothmaskingsarepresent.conc line 5:1 Method reference variable 'xxx' hides existing named method or method reference definition
x69Lambdamakesurebothmaskingsarepresent.conc line 10:1 Method xxx is hidden by existing variable declared as a method reference variable
 
~~~~~
//##69. if defo exception, then u can ignore that fact that it doesnt return properly

~~~~~
//##70. rhs of dot operator is ok

