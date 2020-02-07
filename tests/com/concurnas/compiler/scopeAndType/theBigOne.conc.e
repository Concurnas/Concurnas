//##3. Lexical Structure




~~~~~
//##3.1. Unicode





~~~~~
//##3.2. Lexical Translations





~~~~~
//##3.3. Unicode Escapes





~~~~~
//##3.4. Line Terminators





~~~~~
//##3.5. Input Elements and Tokens





~~~~~
//##3.6. White Space





~~~~~
//##3.7. Comments





~~~~~
//##3.8. Identifiers





~~~~~
//##3.9. Keywords





~~~~~
//##3.10. Literals





~~~~~
//##3.10.1. Integer Literals





~~~~~
//##3.10.2. FloatingPoint Literals





~~~~~
//##3.10.3. Boolean Literals





~~~~~
//##3.10.4. Character Literals





~~~~~
//##3.10.5. String Literals



~~~~~
//##3.10.6. Escape Sequences for Character and String Literals



~~~~~
//##3.10.7. The Null Literal
x3107TheNullLiteral.conc line 4:0 Variable d has already been defined in current scope

~~~~~
//##3.11. Separators

~~~~~
//##3.12. Operators


~~~~~
//##4. Types, Values, and Variables
x4TypesValuesandVariables.conc line 3:9 extraneous input '=' expecting {<EOF>, ';', 'transient', 'shared', 'lazy', 'override', '-', '+', '~', 'assert', 'del', 'break', 'continue', 'throw', 'return', 'import', 'using', '*', 'from', 'typedef', '<', 'await', 'def', 'gpudef', 'gpukernel', '**', '++', '--', 'not', 'comp', 'global', 'local', 'constant', 'out', 'this', 'provider', 'abstract', 'open', 'closed', 'class', 'trait', 'actor', 'of', 'with', 'new', 'annotation', 'enum', 'for', 'parfor', 'parforsync', 'match', 'if', 'async', 'while', 'loop', 'try', 'trans', 'init', 'sync', 'onchange', 'every', '@', 'boolean', 'bool', 'size_t', 'int', 'long', 'float', 'double', 'byte', 'short', 'char', 'lambda', 'sizeof', 'super', 'changed', 'null', 'true', 'false', 'val', 'var', 'private', 'public', 'inject', 'protected', 'package', LONGINT, SHORTINT, INT, FLOAT, DOUBLE, NAME, STRING_ITMcit, STRING_ITMquot, REGEX_STRING_ITM, NEWLINE, '(', '[', 'a[', '{'}
x4TypesValuesandVariables.conc line 3:16 missing ';' at '\n'
 
~~~~~
//##4.1. The Kinds of Types and Values


~~~~~
//##4.2. Primitive Types and Values
x42PrimitiveTypesandValues.conc line 5:4 Type mismatch: cannot convert from double to int
x42PrimitiveTypesandValues.conc line 7:0 Variable tolong has already been defined in current scope

~~~~~
//##4.2. Primitive Types and Values  no void 4u
x42PrimitiveTypesandValuesnovoid4u.conc line 3:2 extraneous input 'void' expecting {<EOF>, ';', 'transient', 'shared', 'lazy', 'override', '-', '+', '~', 'assert', 'del', 'break', 'continue', 'throw', 'return', 'import', 'using', '*', 'from', 'typedef', '<', 'await', 'def', 'gpudef', 'gpukernel', '**', '++', '--', 'not', 'comp', 'global', 'local', 'constant', 'out', 'this', 'provider', 'abstract', 'open', 'closed', 'class', 'trait', 'actor', 'of', 'with', 'new', 'annotation', 'enum', 'for', 'parfor', 'parforsync', 'match', 'if', 'async', 'while', 'loop', 'try', 'trans', 'init', 'sync', 'onchange', 'every', '@', 'boolean', 'bool', 'size_t', 'int', 'long', 'float', 'double', 'byte', 'short', 'char', 'lambda', 'sizeof', 'super', 'changed', 'null', 'true', 'false', 'val', 'var', 'private', 'public', 'inject', 'protected', 'package', LONGINT, SHORTINT, INT, FLOAT, DOUBLE, NAME, STRING_ITMcit, STRING_ITMquot, REGEX_STRING_ITM, NEWLINE, '(', '[', 'a[', '{'}
 
~~~~~
//##4.2.1. Integral Types and Values
x421IntegralTypesandValues.conc line 6:6 Type mismatch: cannot convert from long to int
x421IntegralTypesandValues.conc line 7:6 Type mismatch: cannot convert from long to int

~~~~~
//##4.2.1. Integral Types and ValuesTooLong
x421IntegralTypesandValuesTooLong.conc line 3:18 Literal '287349827349827348723942389479' is not a valid integer or long

~~~~~
//##4.2.2. Integer Operations
x422IntegerOperations.conc line 43:0 numerical operation cannot be performed on type java.lang.String. No overloaded 'minusAssign' operator found for type java.lang.String with signature: '(java.lang.String) java.lang.String'
 
~~~~~
//##4.2.3. Floating-Point Types, Formats, and Values
x423FloatingPointTypesFormatsandValues.conc line 16:11 Type mismatch: cannot convert from double to float




~~~~~
//##4.2.4. FloatingPoint Operations
x424FloatingPointOperations.conc line 18:4 Type mismatch: cannot convert from double to float




~~~~~
//##4.2.5. The boolean Type and boolean Values





~~~~~
//##4.3. Reference Types and Values



~~~~~
//##4.3.1. Objects System.out.println



~~~~~
//##4.3.1. Objects





~~~~~
//##4.3.2. The Class Object
x432TheClassObject.conc line 10:12 Moi cannot be resolved to a variable
x432TheClassObject.conc line 18:2 The method finalize is not visible
x432TheClassObject.conc line 19:2 Generic parameter count of: 0 does not equal: 1 - generic type parameters must be defined
x432TheClassObject.conc line 22:11 incompatible type: int vs java.lang.String

~~~~~
//##4.3.2.b The Class Object  prevent forbidden method calls
x432bTheClassObjectpreventforbiddenmethodcalls.conc line 8:2 Unable to find method with matching name: wait
x432bTheClassObjectpreventforbiddenmethodcalls.conc line 9:2 Unable to find method with matching name: wait
x432bTheClassObjectpreventforbiddenmethodcalls.conc line 10:2 Unable to find method with matching name: wait
x432bTheClassObjectpreventforbiddenmethodcalls.conc line 11:2 Unable to find method with matching name: notify
x432bTheClassObjectpreventforbiddenmethodcalls.conc line 12:2 Unable to find method with matching name: notifyAll
x432bTheClassObjectpreventforbiddenmethodcalls.conc line 16:2 Unable to find method with matching name: wait
x432bTheClassObjectpreventforbiddenmethodcalls.conc line 17:2 Unable to find method with matching name: wait
x432bTheClassObjectpreventforbiddenmethodcalls.conc line 18:2 Unable to find method with matching name: wait
x432bTheClassObjectpreventforbiddenmethodcalls.conc line 19:2 Unable to find method with matching name: notify
x432bTheClassObjectpreventforbiddenmethodcalls.conc line 20:2 Unable to find method with matching name: notifyAll
 
~~~~~
//##4.3.3. The Class String





~~~~~
//##4.3.4. When Reference Types Are the Same





~~~~~
//##4.4. Type Variables





~~~~~
//##4.5. Parameterized Types





~~~~~
//##4.5.1. Type Arguments and Wildcards





~~~~~
//##4.5.2. Members and Constructors of Parameterized Types





~~~~~
//##4.6. Type Erasure





~~~~~
//##4.7. Reifiable Types





~~~~~
//##4.8. Raw Types
x48RawTypes.conc line 11:11 Generic Type argument type mismatch: java.lang.String vs java.lang.Object
x48RawTypes.conc line 13:28 Generic Type argument type mismatch: java.lang.Object vs java.lang.String
x48RawTypes.conc line 15:9 Generic parameter count of: 0 does not equal: 1 - generic type parameters must be defined
x48RawTypes.conc line 18:24 Generic Type argument type mismatch: java.lang.Object vs java.lang.String
x48RawTypes.conc line 20:20 Class: java.util.List<java.lang.Object> is not instantiable
x48RawTypes.conc line 21:2 Generic parameter count of: 0 does not equal: 1 - generic type parameters must be defined
 
~~~~~
//##4.9. Intersection Types





~~~~~
//##4.10. Subtyping
~~~~~
//##4.10.1. Subtyping among Primitive Types

~~~~~
//##4.10.2. Subtyping among Class and Interface Types

~~~~~
//##4.10.3. Subtyping among Array Types
x4103SubtypingamongArrayTypes.conc line 8:21 Type mismatch: cannot convert from int[] to long[]
x4103SubtypingamongArrayTypes.conc line 9:21 Type mismatch: cannot convert from long[] to int[]
 
~~~~~
//##4.11. Where Types Are Used





~~~~~
//##4.12. Variables





~~~~~
//##4.12.1. Variables of Primitive Type





~~~~~
//##4.12.2. Variables of Reference Type





~~~~~
//##4.12.3. Kinds of Variables





~~~~~
//##4.12.4. final Variables





~~~~~
//##4.12.5. Initial Values of Variables





~~~~~
//##4.12.6. Types, Classes, and Interfaces





~~~~~
//##5. Conversions and Promotions





~~~~~
//##5.1. Kinds of Conversion





~~~~~
//##5.1.1. Identity Conversion





~~~~~
//##5.1.2. Widening Primitive Conversion





~~~~~
//##5.1.3. Narrowing Primitive Conversion
x513NarrowingPrimitiveConversion.conc line 15:10 Type mismatch: cannot convert from short to byte
x513NarrowingPrimitiveConversion.conc line 16:10 Type mismatch: cannot convert from short to char
x513NarrowingPrimitiveConversion.conc line 18:11 Type mismatch: cannot convert from char to byte
x513NarrowingPrimitiveConversion.conc line 19:12 Type mismatch: cannot convert from char to short
x513NarrowingPrimitiveConversion.conc line 25:14 Type mismatch: cannot convert from long to byte
x513NarrowingPrimitiveConversion.conc line 26:14 Type mismatch: cannot convert from long to short
x513NarrowingPrimitiveConversion.conc line 27:14 Type mismatch: cannot convert from long to char
x513NarrowingPrimitiveConversion.conc line 28:13 Type mismatch: cannot convert from long to int
x513NarrowingPrimitiveConversion.conc line 30:15 Type mismatch: cannot convert from float to byte
x513NarrowingPrimitiveConversion.conc line 31:15 Type mismatch: cannot convert from float to short
x513NarrowingPrimitiveConversion.conc line 32:15 Type mismatch: cannot convert from float to char
x513NarrowingPrimitiveConversion.conc line 33:14 Type mismatch: cannot convert from float to int
x513NarrowingPrimitiveConversion.conc line 34:15 Type mismatch: cannot convert from float to long
x513NarrowingPrimitiveConversion.conc line 37:16 Type mismatch: cannot convert from double to byte
x513NarrowingPrimitiveConversion.conc line 38:16 Type mismatch: cannot convert from double to short
x513NarrowingPrimitiveConversion.conc line 39:16 Type mismatch: cannot convert from double to char
x513NarrowingPrimitiveConversion.conc line 40:15 Type mismatch: cannot convert from double to int
x513NarrowingPrimitiveConversion.conc line 41:16 Type mismatch: cannot convert from double to long
x513NarrowingPrimitiveConversion.conc line 42:17 Type mismatch: cannot convert from double to float
 





~~~~~
//##5.1.4. Widening and Narrowing Primitive Conversion





~~~~~
//##5.1.5. Widening Reference Conversion


~~~~~
//##5.1.6. Narrowing Reference Conversion


~~~~~
//##5.1.7. Boxing Conversion

~~~~~
//##5.1.8. Unboxing Conversion
x518UnboxingConversion.conc line 9:16 numerical operation cannot be performed on type boolean. No overloaded 'plus' operator found for type java.lang.Boolean with signature: '(java.lang.Boolean)'

~~~~~
//##5.1.9. Unchecked Conversion

~~~~~
//##5.1.10. Capture Conversion



~~~~~
//##5.1.11. String Conversion

~~~~~
//##5.1.12. Forbidden Conversions
 
~~~~~
//##5.1.13. Value Set Conversion

~~~~~
//##5.2. Assignment Conversion

x52AssignmentConversion.conc line 21:13 Type mismatch: cannot convert from short to char
x52AssignmentConversion.conc line 22:8 Type mismatch: cannot convert from char to short

~~~~~
//##5.3. Method Invocation Conversion
x53MethodInvocationConversion.conc line 7:1 Method exalready with matching argument definition exists already in current Scope
x53MethodInvocationConversion.conc line 23:24 void methods cannot return a value
x53MethodInvocationConversion.conc line 24:25 void methods cannot return a value
x53MethodInvocationConversion.conc line 26:29 java.lang.Object is not a subtype of java.lang.String
x53MethodInvocationConversion.conc line 28:31 int is not a subtype of java.lang.String
x53MethodInvocationConversion.conc line 35:12 void is not an instantiable type
x53MethodInvocationConversion.conc line 44:15 incompatible type: boolean vs java.lang.String
x53MethodInvocationConversion.conc line 57:15 Ambiguous method detected 'cho2'. More than one indirect match made
x53MethodInvocationConversion.conc line 62:0 Variable res has already been defined in current scope
 
~~~~~
//##5.4. String Conversion

~~~~~
//##5.5. Casting Conversion
x55CastingConversion.conc line 24:5 Cannot cast from java.lang.Long to java.lang.Boolean

~~~~~
//##5.5.1. Reference Type Casting
x551ReferenceTypeCasting.conc line 6:5 Cannot cast from java.lang.Integer to java.lang.String
 
~~~~~
//##5.5.2. Checked Casts and Unchecked Casts





~~~~~
//##5.5.3. Checked Casts at Runtime





~~~~~
//##5.6. Numeric Promotions





~~~~~
//##5.6.1. Unary Numeric Promotion

~~~~~
//##5.6.2. Binary Numeric Promotion
x562BinaryNumericPromotion.conc line 17:18 Type mismatch: cannot convert from float to byte

~~~~~
//##6. Names





~~~~~
//##6.1. Declarations





~~~~~
//##6.2. Names and Identifiers





~~~~~
//##6.3. Scope of a Declaration





~~~~~
//##6.4. Shadowing and Obscuring
x64ShadowingandObscuring.conc line 4:0 Variable definitions cannot override classneames. String has already been defined as an imported class
x64ShadowingandObscuring.conc line 5:0 Variable definitions cannot override classneames. Object has already been defined as an imported class
x64ShadowingandObscuring.conc line 8:8 Class: java.util.List<java.lang.String> is not instantiable
x64ShadowingandObscuring.conc line 9:0 Variable definitions cannot override classneames. List has already been defined as an imported class
x64ShadowingandObscuring.conc line 15:1 Variable a has already been defined in current scope
x64ShadowingandObscuring.conc line 16:1 Variable a has already been defined in current scope
 
~~~~~
//##6.4.1. Shadowing
x641Shadowing.conc line 14:1 Variable z has already been defined in current scope
x641Shadowing.conc line 19:7 localThing cannot be resolved to a variable
x641Shadowing.conc line 23:0 defonewvari cannot be resolved to a variable
x641Shadowing.conc line 26:36 scopedint cannot be resolved to a variable
 
~~~~~
//##6.4.2. Obscuring





~~~~~
//##6.5. Determining the Meaning of a Name





~~~~~
//##6.5.1. Syntactic Classification of a Name According to Context





~~~~~
//##6.5.2. Reclassification of Contextually Ambiguous Names





~~~~~
//##6.5.3. Meaning of Package Names





~~~~~
//##6.5.3.1. Simple Package Names





~~~~~
//##6.5.3.2. Qualified Package Names





~~~~~
//##6.5.4. Meaning of PackageOrTypeNames





~~~~~
//##6.5.4.1. Simple PackageOrTypeNames





~~~~~
//##6.5.4.2. Qualified PackageOrTypeNames





~~~~~
//##6.5.5. Meaning of Type Names





~~~~~
//##6.5.5.1. Simple Type Names





~~~~~
//##6.5.5.2. Qualified Type Names





~~~~~
//##6.5.6. Meaning of Expression Names





~~~~~
//##6.5.6.1. Simple Expression Names





~~~~~
//##6.5.6.2. Qualified Expression Names
x6562QualifiedExpressionNames.conc line 14:14 int is not a subtype of java.lang.String
x6562QualifiedExpressionNames.conc line 25:1 blocks cannot be nested directly within classes
x6562QualifiedExpressionNames.conc line 25:1 line cannot be present within class definition
x6562QualifiedExpressionNames.conc line 31:1 blocks cannot be nested directly within classes
x6562QualifiedExpressionNames.conc line 31:1 line cannot be present within class definition
x6562QualifiedExpressionNames.conc line 38:0 Unable to resolve reference to variable name: c
x6562QualifiedExpressionNames.conc line 39:3 c cannot be resolved to a variable
x6562QualifiedExpressionNames.conc line 53:8 Unable to resolve reference to variable name: x
x6562QualifiedExpressionNames.conc line 55:10 Unable to find method with matching name: nPoints
x6562QualifiedExpressionNames.conc line 79:10 Unable to resolve reference to constructor for: 'x6562QualifiedExpressionNames.Two'(int)
x6562QualifiedExpressionNames.conc line 81:4 Unable to find method with matching name: beards
x6562QualifiedExpressionNames.conc line 101:11 Ambiguous method detected 'constructor'. More than one indirect match made
 
~~~~~
//##CONC this constructors
xCONCthisconstructors.conc line 9:9 Constructor call must be the first call in a constructor
xCONCthisconstructors.conc line 10:10 Super constructor call must be the first call in a constructor
xCONCthisconstructors.conc line 15:2 Unable to resolve reference to super constructor for: 'java.lang.Object'(int)
 
~~~~~
//##6.5.7. Meaning of Method Names

~~~~~
//##6.5.7.1. Simple Method Names

~~~~~
//##6.5.7.2. Qualified Method Names

~~~~~
//##6.6. Access Control




~~~~~
//##6.6.1. Determining Accessibility




~~~~~
//##6.6.2. Details on protected Access





~~~~~
//##6.6.2.1. Access to a protected Member





~~~~~
//##6.6.2.2. Qualified Access to a protected Constructor
x6622QualifiedAccesstoaprotectedConstructor.conc line 24:7 x6622QualifiedAccesstoaprotectedConstructor.C.B is not a subtype of x6622QualifiedAccesstoaprotectedConstructor.C.Ca
x6622QualifiedAccesstoaprotectedConstructor.conc line 25:25 Nested Class: x6622QualifiedAccesstoaprotectedConstructor.C.Ca cannot be directly instantiated. Can only be instantiated via reference of class: x6622QualifiedAccesstoaprotectedConstructor.C


~~~~~
//##Classes cannot ext self
xClassescannotextself.conc line 4:18 The nested type: 'C' cannot hide an enclosing type: 'C'
 
~~~~~
//##6.7. Fully Qualified Names and Canonical Names



~~~~~
//##8. Classes




~~~~~
//##8.1.a1 Simple class stuff
x81a1Simpleclassstuff.conc line 5:4 Unable to find method with matching name: ddd
x81a1Simpleclassstuff.conc line 6:4 Unable to find method with matching name: charAt and arguments (java.lang.String)
x81a1Simpleclassstuff.conc line 15:2 Unable to find method with matching name: setCar and arguments (int)
 
~~~~~
//##8.1.a2 getters settters
x81a2getterssettters.conc line 20:8 Unable to find method with matching name: getCar
x81a2getterssettters.conc line 22:2 Unable to find method with matching name: setCar
x81a2getterssettters.conc line 25:2 Unable to find method with matching name: setCar
x81a2getterssettters.conc line 26:8 Unable to find method with matching name: getCar
 
~~~~~
//##8.1.a3 getters settters fields

~~~~~
//##8.1.a4 getters settters fields - specifically arrays
x81a4getterssetttersfieldsspecificallyarrays.conc line 16:20 Type mismatch: cannot convert from java.lang.Object[] to java.lang.String[]?
x81a4getterssetttersfieldsspecificallyarrays.conc line 17:3 Unable to find method with matching name: setSomething and arguments (java.lang.Object[])
x81a4getterssetttersfieldsspecificallyarrays.conc line 21:15 Type mismatch: cannot convert from float[] to double[]?
x81a4getterssetttersfieldsspecificallyarrays.conc line 22:3 Unable to find method with matching name: setPrim and arguments (float[])
x81a4getterssetttersfieldsspecificallyarrays.conc line 23:10 Cannot cast from float[] to double[]
x81a4getterssetttersfieldsspecificallyarrays.conc line 25:3 Unable to find method with matching name: setPrim and arguments (int[])
 
~~~~~
//##8.1.a5 generic cast
x81a5genericcast.conc WARN line 8:9 Unable to determine type for generic: T for as operation. Wil use upper bounds of: java.lang.Object instead
 
~~~~~
//##8.1.a6 dont allow duplicate vars
x81a6dontallowduplicatevars.conc line 3:18 Cannot define variable: x more than once
x81a6dontallowduplicatevars.conc line 5:2 Variable x has already been defined in current scope
x81a6dontallowduplicatevars.conc line 11:2 Variable x has already been defined in current scope
x81a6dontallowduplicatevars.conc line 12:2 Variable x has already been defined in current scope
x81a6dontallowduplicatevars.conc line 13:2 Variable x has already been defined in current scope
x81a6dontallowduplicatevars.conc line 14:2 Variable x has already been defined in current scope
x81a6dontallowduplicatevars.conc line 15:2 Variable x has already been defined in current scope
x81a6dontallowduplicatevars.conc line 21:1 Variable var2 has already been defined in current scope
x81a6dontallowduplicatevars.conc line 23:1 Variable car has already been defined in current scope
 
~~~~~
//##CONC dup cls name
xCONCdupclsname.conc line 5:0 Class name has already been declared in current scope: 'A'

~~~~~
//##CONC class name variants
xCONCclassnamevariants.conc line 23:32 Cannot cast from xCONCclassnamevariants.Cont.SubA to int
 
~~~~~
//##CONC outer class overrides nested
xCONCouterclassoverridesnested.conc line 12:18 xCONCouterclassoverridesnested.Cont.SubA is not a subtype of xCONCouterclassoverridesnested.SubA
xCONCouterclassoverridesnested.conc line 13:27 xCONCouterclassoverridesnested.SubA is not a subtype of xCONCouterclassoverridesnested.Cont.SubA
xCONCouterclassoverridesnested.conc line 14:58 xCONCouterclassoverridesnested.SubA is not a subtype of xCONCouterclassoverridesnested.Cont.SubA
 
~~~~~
//##CONC nested classes via parent only
xCONCnestedclassesviaparentonly.conc line 12:12 Nested Class: xCONCnestedclassesviaparentonly.Cont.SubA cannot be directly instantiated. Can only be instantiated via reference of class: xCONCnestedclassesviaparentonly.Cont
xCONCnestedclassesviaparentonly.conc line 13:13 Nested Class: xCONCnestedclassesviaparentonly.Cont.SubA cannot be directly instantiated. Can only be instantiated via reference of class: xCONCnestedclassesviaparentonly.Cont
xCONCnestedclassesviaparentonly.conc line 21:3 Variable alsook has already been defined in current scope
xCONCnestedclassesviaparentonly.conc line 22:3 Variable alsook has already been defined in current scope
xCONCnestedclassesviaparentonly.conc line 29:29 Nested Class: xCONCnestedclassesviaparentonly.Outer2.InnerL1.InnterLevel2 cannot be directly instantiated. Can only be instantiated via reference of class: xCONCnestedclassesviaparentonly.Outer2.InnerL1
 
~~~~~
//##CONC class lines restricted
xCONCclasslinesrestricted.conc line 6:1 Unable to find method with matching name: func
xCONCclasslinesrestricted.conc line 6:1 line cannot be present within class definition
xCONCclasslinesrestricted.conc line 7:1 line cannot be present within class definition
 
~~~~~
//##CONC generic class must link to something
xCONCgenericclassmustlinktosomething.conc line 4:10 Unable to resolve type corresponding to name: Inteddger

~~~~~
//##CONC good news a new hope autocasts on params

~~~~~
//##CONC duplicate vars
xCONCduplicatevars.conc line 4:14 Duplicate parameter: b in arguments of: bb
xCONCduplicatevars.conc line 5:0 Duplicate parameter: b in arguments of: bb
xCONCduplicatevars.conc line 8:17 Cannot define variable: a more than once
xCONCduplicatevars.conc line 9:13 Duplicate parameter: b
 
~~~~~
//##CONC abstract classes - 1
xCONCabstractclasses1.conc line 16:10 Parameter names must be defined for non abstract method definition 'oops'
xCONCabstractclasses1.conc line 20:0 Abstract method definition of 'abast' can only exist within (abstract) classes
xCONCabstractclasses1.conc line 32:32 Class: java.util.List<java.lang.Integer> is not instantiable
xCONCabstractclasses1.conc line 33:33 Class: xCONCabstractclasses1.Abstr is not instantiable
xCONCabstractclasses1.conc line 34:34 Class: xCONCabstractclasses1.EmptyAbst is not instantiable
xCONCabstractclasses1.conc line 37:0 Class 'xCONCabstractclasses1.naughtyChild' is missing implementations of abstract method definitions inherited: xCONCabstractclasses1.Abstr:{def ab(int) int, def ac(int) int, def ac2(int) void}
xCONCabstractclasses1.conc line 39:0 Class 'xCONCabstractclasses1.goodChild' is missing implementations of abstract method definitions inherited: xCONCabstractclasses1.Abstr:{def ac(int) int, def ac2(int) void}
xCONCabstractclasses1.conc line 41:1 Method: 'def overme() void' of class: 'xCONCabstractclasses1.Abstr' should be explicitly overriden in subclass: xCONCabstractclasses1.goodChild. Use the override keyword
 
~~~~~
//##CONC abstract classes - 2
xCONCabstractclasses2.conc line 9:0 Class 'xCONCabstractclasses2.ChildBadGenericStr' is missing implementations of abstract method definitions inherited: xCONCabstractclasses2.Sup:{def ss(java.lang.String) java.lang.String}
xCONCabstractclasses2.conc line 11:0 Class 'xCONCabstractclasses2.ChildBadGeneric' is missing implementations of abstract method definitions inherited: xCONCabstractclasses2.Sup:{def ss(B) B}
 
~~~~~
//##CONC abstract classes - 3
xCONCabstractclasses3.conc line 20:0 Class 'xCONCabstractclasses3.Child' is missing implementations of abstract method definitions inherited: xCONCabstractclasses3.ChildAbst:{def foo() void}, xCONCabstractclasses3.SupAbstr:{def goo() void}
xCONCabstractclasses3.conc line 40:0 Class 'xCONCabstractclasses3.ChildGeneric' is missing implementations of abstract method definitions inherited: xCONCabstractclasses3.ChildAbstGeneric:{def foo() java.lang.String}, xCONCabstractclasses3.SupAbstrGeneric:{def goo(java.lang.String) void}
xCONCabstractclasses3.conc line 46:0 Class 'xCONCabstractclasses3.ChildGenericUndef' is missing implementations of abstract method definitions inherited: xCONCabstractclasses3.ChildAbstGeneric:{def foo() Z}, xCONCabstractclasses3.SupAbstrGeneric:{def goo(Z) void}
 
~~~~~
//##CONC override function (inc abstr)
xCONCoverridefunction(incabstr).conc line 6:0 Function: def doings() void which has been declared as overriden can only be defined within a subclass
xCONCoverridefunction(incabstr).conc line 10:1 In order for the method 'def doings() void' of class: 'xCONCoverridefunction(incabstr).NaugtySup' to be overriden it must be defined in superclass: java.lang.Object
xCONCoverridefunction(incabstr).conc line 16:1 Method: 'def equals(java.lang.Object) boolean' of class: 'java.lang.Object' should be explicitly overriden in subclass: xCONCoverridefunction(incabstr).BadExtendObjectAbstr. Use the override keyword
xCONCoverridefunction(incabstr).conc line 21:1 Method: 'def equals(java.lang.Object) boolean' of class: 'java.lang.Object' should be explicitly overriden in subclass: xCONCoverridefunction(incabstr).BadExtendObjectDefined. Use the override keyword
xCONCoverridefunction(incabstr).conc line 34:0 Class 'xCONCoverridefunction(incabstr).OkExtendObjectAbstrChildBad' is missing implementations of abstract method definitions inherited: xCONCoverridefunction(incabstr).OkExtendObjectAbstr:{def equals(java.lang.Object) boolean}
xCONCoverridefunction(incabstr).conc line 44:1 Method: 'def equals(java.lang.Object) boolean' of class: 'xCONCoverridefunction(incabstr).OKObjectExtendObjectDefined' should be explicitly overriden in subclass: xCONCoverridefunction(incabstr).OkExtendObjectAbstrChildOKNeedKW. Use the override keyword
xCONCoverridefunction(incabstr).conc line 54:0 Class 'xCONCoverridefunction(incabstr).Sup' is missing implementations of abstract method definitions inherited: xCONCoverridefunction(incabstr).SupAbst:{def goo() void}
xCONCoverridefunction(incabstr).conc line 60:0 Class 'xCONCoverridefunction(incabstr).Child' is missing implementations of abstract method definitions inherited: xCONCoverridefunction(incabstr).SupAbst:{def goo() void}
 
~~~~~
//##CONC function ret type overrides
xCONCfunctionrettypeoverrides.conc line 19:1 The return type of method 'def fooi() float' in class xCONCfunctionrettypeoverrides.Child cannot be matched with method: 'def fooi() int' in superclass: xCONCfunctionrettypeoverrides.Sup
xCONCfunctionrettypeoverrides.conc line 20:1 The return type of method 'def foo2i() float' in class xCONCfunctionrettypeoverrides.Child cannot be matched with method: 'def foo2i() int' in superclass: xCONCfunctionrettypeoverrides.Sup
xCONCfunctionrettypeoverrides.conc line 22:1 In order for the method 'def fooObj() java.lang.Float' of class: 'xCONCfunctionrettypeoverrides.Child' to be overriden it must be defined in the superclass: xCONCfunctionrettypeoverrides.Sup - its been declared abstract
xCONCfunctionrettypeoverrides.conc line 23:1 The return type of method 'def fooObjLessGen() java.lang.Object' in class xCONCfunctionrettypeoverrides.Child cannot be matched with method: 'def fooObjLessGen() java.lang.String' in superclass: xCONCfunctionrettypeoverrides.Sup
xCONCfunctionrettypeoverrides.conc line 24:1 The return type of method 'def voidray() int' in class xCONCfunctionrettypeoverrides.Child cannot be matched with method: 'def voidray() void' in superclass: xCONCfunctionrettypeoverrides.Sup
xCONCfunctionrettypeoverrides.conc line 38:38 Class: xCONCfunctionrettypeoverrides.Child is not instantiable
 
~~~~~
//##CONC cannot det sup abs type so make err
xCONCcannotdetsupabstypesomakeerr.conc line 11:1 The return type of method 'def voidray() int' in class xCONCcannotdetsupabstypesomakeerr.Child cannot be matched with method: 'def voidray() void' in superclass: xCONCcannotdetsupabstypesomakeerr.Sup
 
~~~~~
//##CONC check supclass pass through generics ok


~~~~~
//##CONC generic types can be primatives
xCONCgenerictypescanbeprimatives.conc line 10:0 Child cannot extend: Sup as it is closed
xCONCgenerictypescanbeprimatives.conc line 16:8 Expected to invoke a function reference
 
~~~~~
//##CONC even more def with generics etc
xCONCevenmoredefwithgenericsetc.conc line 40:0 The type FF is not generic; it cannot be parameterized with arguments
xCONCevenmoredefwithgenericsetc.conc line 40:0 Unable to resolve type corresponding to name: ? FF<java.lang.String>
xCONCevenmoredefwithgenericsetc.conc line 43:0 Cycle detected: the class xCONCevenmoredefwithgenericsetc.Oops cannot extend itself
xCONCevenmoredefwithgenericsetc.conc line 45:0 Cycle detected: the class xCONCevenmoredefwithgenericsetc.Oops2 cannot extend itself
xCONCevenmoredefwithgenericsetc.conc line 59:1 The return type of method 'def g() java.lang.Integer' in class xCONCevenmoredefwithgenericsetc.SupSupLA cannot be matched with method: 'def g() int' in superclass: xCONCevenmoredefwithgenericsetc.SupLA
 
~~~~~
//##CONC generics use the question mark and force coloring
xCONCgenericsusethequestionmarkandforcecoloring.conc line 8:19 Generic Type argument type mismatch: B vs A
xCONCgenericsusethequestionmarkandforcecoloring.conc line 10:27 Generic Type argument type mismatch: java.util.List<B> vs java.util.ArrayList<A>
xCONCgenericsusethequestionmarkandforcecoloring.conc line 11:4 Generic type refernces cannot have generic qualifications themselves
xCONCgenericsusethequestionmarkandforcecoloring.conc line 17:18 Cannot perform is or isnot check against parameterized type java.util.List<java.lang.String>. Use the form java.util.List<?> instead since further generic type information will be erased at runtime 
xCONCgenericsusethequestionmarkandforcecoloring.conc line 19:21 Generic parameter count of: 0 does not equal: 1 - generic type parameters must be defined
xCONCgenericsusethequestionmarkandforcecoloring.conc WARN line 8:19 Unable to determine type for generic: A for as operation. Wil use upper bounds of: java.lang.Object instead
xCONCgenericsusethequestionmarkandforcecoloring.conc WARN line 9:20 Unable to determine type for generic: B for as operation. Wil use upper bounds of: java.lang.Object instead
xCONCgenericsusethequestionmarkandforcecoloring.conc WARN line 10:27 Unable to determine type for generic: A for as operation. Wil use upper bounds of: java.lang.Object instead
 
~~~~~
//##CONC generics must match 1:1
xCONCgenericsmustmatch1:1.conc line 7:27 Generic Type argument type mismatch: xCONCgenericsmustmatch1:1.Supsup vs xCONCgenericsmustmatch1:1.Childchild
xCONCgenericsmustmatch1:1.conc line 8:28 Generic Type argument type mismatch: xCONCgenericsmustmatch1:1.Supsup vs java.lang.Object
xCONCgenericsmustmatch1:1.conc line 9:28 Generic Type argument type mismatch: java.lang.Object vs xCONCgenericsmustmatch1:1.Supsup
 
~~~~~
//##CONC generics qualified cannot exist in heirachy
xCONCgenericsqualifiedcannotexistinheirachy.conc line 4:1 generic qualifier 'X' has been defined already further up hierarchy
 
~~~~~
//##CONC generic nested classes def and games
xCONCgenericnestedclassesdefandgames.conc line 30:11 incompatible type: int vs xCONCgenericnestedclassesdefandgames.Child<java.lang.String>.Boy<java.lang.Float>
xCONCgenericnestedclassesdefandgames.conc line 31:11 incompatible type: int vs xCONCgenericnestedclassesdefandgames.Child<java.lang.String>.Boy<java.lang.Float>.LittleBoy<xCONCgenericnestedclassesdefandgames.Sup<java.lang.String, java.lang.Integer>>
xCONCgenericnestedclassesdefandgames.conc line 35:8 Nested inner class parent generic parameter count of: 2 does not equal: 1
xCONCgenericnestedclassesdefandgames.conc line 36:5 Nested inner class parent: xCONCgenericnestedclassesdefandgames.Child for: xCONCgenericnestedclassesdefandgames.Child.Boy requies 1 generic paramaters
xCONCgenericnestedclassesdefandgames.conc line 46:33 Parent nestor type argument type mismatch: xCONCgenericnestedclassesdefandgames.Child<java.lang.Float> vs xCONCgenericnestedclassesdefandgames.Child<java.lang.String>
 
~~~~~
//##CONC parent nestor generics must match
xCONCparentnestorgenericsmustmatch.conc line 12:35 Generic Type argument type mismatch: java.lang.String vs java.lang.Integer
xCONCparentnestorgenericsmustmatch.conc line 15:36 Generic Type argument type mismatch: java.lang.String vs java.lang.Integer
xCONCparentnestorgenericsmustmatch.conc line 18:36 Parent nestor type argument type mismatch: xCONCparentnestorgenericsmustmatch.Child<java.lang.String> vs xCONCparentnestorgenericsmustmatch.Child<java.lang.Integer>
 
~~~~~
//##CONC more parent nestor generics partly defined constructors...
xCONCmoreparentnestorgenericspartlydefinedconstructors.conc line 39:38 Parent nestor type argument type mismatch: xCONCmoreparentnestorgenericspartlydefinedconstructors.Parent<java.lang.String> vs xCONCmoreparentnestorgenericspartlydefinedconstructors.Parent<java.lang.Integer>
 
~~~~~
//##CONC generic constructors

~~~~~
//##CONC generic nested extends supertype...

~~~~~
//##CONC construcors exist at own level only... 
xCONCconstrucorsexistatownlevelonly.conc line 8:0 Implicit super constructor for superclass xCONCconstrucorsexistatownlevelonly.Parent of xCONCconstrucorsexistatownlevelonly.Child is undefined. Must explicitly invoke another constructor or invoke superconstructor
xCONCconstrucorsexistatownlevelonly.conc line 12:8 Unable to resolve reference to constructor for: 'xCONCconstrucorsexistatownlevelonly.Child'(java.lang.String)
xCONCconstrucorsexistatownlevelonly.conc line 13:9 Unable to resolve reference to constructor for: 'xCONCconstrucorsexistatownlevelonly.NoConst'(java.lang.String)
 
~~~~~
//##CONC generic const and method erasure 1
xCONCgenericconstandmethoderasure1.conc line 8:1 Method gg with matching argument definition exists already in current Scope - generic types are erased at runtime
xCONCgenericconstandmethoderasure1.conc line 14:1 Method gg with matching argument definition exists already in current Scope - generic types are erased at runtime

~~~~~
//##CONC generic const and method erasure 2
xCONCgenericconstandmethoderasure2.conc line 12:1 Method gg with matching argument definition exists already in current Scope - generic types are erased at runtime
xCONCgenericconstandmethoderasure2.conc line 17:1 Method gg with matching argument definition exists already in current Scope - generic types are erased at runtime
xCONCgenericconstandmethoderasure2.conc line 22:1 Method gg with matching argument definition exists already in current Scope - generic types are erased at runtime

~~~~~
//##CONC generic const and method erasure 3 simple genclash
xCONCgenericconstandmethoderasure3simplegenclash.conc line 13:1 Constructor with matching argument definition exists already in current Scope - generic types are erased at runtime
xCONCgenericconstandmethoderasure3simplegenclash.conc line 21:1 Method xxx with matching argument definition exists already in current Scope
xCONCgenericconstandmethoderasure3simplegenclash.conc line 25:1 Method xxx with matching argument definition exists already in current Scope
 
~~~~~
//##CONC generic must be totally formalized
xCONCgenericmustbetotallyformalized.conc line 11:1 The return type of method 'def doing(java.lang.String) X' in class xCONCgenericmustbetotallyformalized.Childa cannot be matched with method: 'def doing(java.lang.String) java.lang.String' in superclass: xCONCgenericmustbetotallyformalized.Sup
 
~~~~~
//##CONC generic override in child, prevents accidental  very cool

~~~~~
//##CONC generic const and method erasure 4  complex with supertypes erasure
xCONCgenericconstandmethoderasure4complexwithsupertypeserasure.conc line 21:1 The return type of method 'def xxx(ZZ) ZZ' in class xCONCgenericconstandmethoderasure4complexwithsupertypeserasure.ChildClsFail is incompatible with method: 'def xxx(java.lang.String) java.lang.String' in superclass: xCONCgenericconstandmethoderasure4complexwithsupertypeserasure.GenClass
xCONCgenericconstandmethoderasure4complexwithsupertypeserasure.conc line 26:1 Method def (X) Y with matching argument definition exists already in supertype or traits as: def (Y) Y - generic types are erased at runtime
 
~~~~~
//##CONC generic const and method erasure 4  complex with supertypes erasure part 2 with 3 levels
xCONCgenericconstandmethoderasure4complexwithsupertypeserasurepart2with3levels.conc line 13:1 The return type of method 'def go(M) M' in class xCONCgenericconstandmethoderasure4complexwithsupertypeserasurepart2with3levels.L3 is incompatible with method: 'def go(java.lang.String) java.lang.String' in superclass: xCONCgenericconstandmethoderasure4complexwithsupertypeserasurepart2with3levels.L2
 
~~~~~
//##CONC generic const and method erasure 4  complex with supertypes erasure part 3  qualifcation of generics
xCONCgenericconstandmethoderasure4complexwithsupertypeserasurepart3qualifcationofgenerics.conc line 12:1 Method def (java.util.ArrayList<java.lang.Integer>) java.lang.String with matching argument definition exists already in supertype or traits as: def (java.util.ArrayList<java.lang.String>) java.lang.String - generic types are erased at runtime
 
~~~~~
//##CONC just check some more override and abstract
xCONCjustchecksomemoreoverrideandabstract.conc line 8:0 Class 'xCONCjustchecksomemoreoverrideandabstract.ChildFails' is missing implementations of abstract method definitions inherited: xCONCjustchecksomemoreoverrideandabstract.SupAbst:{def toString() java.lang.String}
xCONCjustchecksomemoreoverrideandabstract.conc line 12:1 In order for the method 'def toString() java.lang.String' of class: 'xCONCjustchecksomemoreoverrideandabstract.ChildAlsoFails' to be overriden it must be defined in the superclass: xCONCjustchecksomemoreoverrideandabstract.SupAbst - its been declared abstract
xCONCjustchecksomemoreoverrideandabstract.conc line 29:1 Method: 'def toString() java.lang.String' of class: 'xCONCjustchecksomemoreoverrideandabstract.SupAbst2' should be explicitly overriden in subclass: xCONCjustchecksomemoreoverrideandabstract.ChildFialThrough3rdLevel. Use the override keyword
 
~~~~~
//##CONC thissuper classes functions and variables  1  pickup super type

~~~~~
//##CONC thissuper classes functions and variables  2  choose from this and superdef


~~~~~
//##CONC thissuper classes functions and variables  3  choose from this and superdef functions


~~~~~
//##CONC thissuper classes functions and variables  4  choose from this and superdef functions generics


~~~~~
//##CONC thissuper classes functions and variables  5  choose from this and superdef functions generics unqualified

~~~~~
//##CONC super constructor invokation, 1  explicit
xCONCsuperconstructorinvokation1explicit.conc line 5:17 Unable to find method with matching name: constructor and arguments (float)
 
~~~~~
//##CONC super constructor invokation, 2  helper autogen
xCONCsuperconstructorinvokation2helperautogen.conc line 9:12 Unable to resolve reference to variable name: x
 
~~~~~
//##CONC super constructor invokation, 3  gen default
xCONCsuperconstructorinvokation3gendefault.conc line 6:8 Unable to resolve reference to constructor for: 'xCONCsuperconstructorinvokation3gendefault.Som'()
xCONCsuperconstructorinvokation3gendefault.conc line 9:8 xCONCsuperconstructorinvokation3gendefault.Two is not a subtype of xCONCsuperconstructorinvokation3gendefault.Som
 
~~~~~
//##CONC super constructor invokation, 4  ensure correct stuff callable...
xCONCsuperconstructorinvokation4ensurecorrectstuffcallable.conc line 6:17 super references cannot be whilst invoking this or super constructors
xCONCsuperconstructorinvokation4ensurecorrectstuffcallable.conc line 7:5 this references cannot be whilst invoking this or super constructors
xCONCsuperconstructorinvokation4ensurecorrectstuffcallable.conc line 17:16 Cannot refer to an instance method while explicitly invoking a constructor
xCONCsuperconstructorinvokation4ensurecorrectstuffcallable.conc line 18:29 Cannot refer to an instance variable while explicitly invoking a constructor
xCONCsuperconstructorinvokation4ensurecorrectstuffcallable.conc line 19:39 Cannot refer to an instance method while explicitly invoking a constructor
 
~~~~~
//##CONC import name cannot mask existing type


~~~~~
//##CONC imports can go anywhere and have scope
xCONCimportscangoanywhereandhavescope.conc line 5:2 Unable to resolve type corresponding to name: List
xCONCimportscangoanywhereandhavescope.conc line 12:2 Unable to resolve type corresponding to name: List
xCONCimportscangoanywhereandhavescope.conc line 14:5 Unable to resolve type corresponding to name: HashMap
xCONCimportscangoanywhereandhavescope.conc line 20:5 Unable to resolve type corresponding to name: HashMap
 
~~~~~
//##8. Class simple abstract 
x8Classsimpleabstract.conc line 31:31 Class: x8Classsimpleabstract.Point is not instantiable

~~~~~
//##8. Class bit more interesting  why we need qualified gen types...
x8Classbitmoreinterestingwhyweneedqualifiedgentypes.conc line 9:25 Unable to find method with matching name: convert
x8Classbitmoreinterestingwhyweneedqualifiedgentypes.conc line 10:34 Unable to find method with matching name: convert
 
~~~~~
//##8. some more random stuff


~~~~~
//##8. no local classes
 
~~~~~
//##8. def with nested


~~~~~
//##8. final classes cannot be extended
x8finalclassescannotbeextended.conc line 5:0 Colored3DPoint cannot extend: ColoredPoint as it is closed
x8finalclassescannotbeextended.conc line 7:0 Colored3DPoint2 cannot extend: ColoredPoint as it is closed
 
~~~~~
//##8. what is the


~~~~~
//##8. class  no fwd reference

~~~~~
//##8. class  some exceptions



~~~~~
//##8. class  cannot directly invoke abst
x8classcannotdirectlyinvokeabst.conc line 10:21 Cannot directly invoke the abstract method: toString
 
~~~~~
//##8. class  cannot directly invoke abst  the right way

~~~~~
//##8. class  nested classes cannot be superclass
x8classnestedclassescannotbesuperclass.conc line 8:0 Class: AnotherM cannot be the subclass of nested class: Nestor.Neste - it must be nested within a subclass of: Nestor
 
~~~~~
//##8. Class dunno waht this does but looks cool

~~~~~
//##8. Class dunno waht this does but looks cool2

~~~~~
//##8. Class dunno waht this does but looks cool3

~~~~~
//##8. Class dunno waht this does but looks cool4

~~~~~
//##8. Class dunno waht this does but looks cool 5 - cool caught this
 
~~~~~
//##8. Class dunno waht this does but looks cool 6 - the final generics menace
x8Classdunnowahtthisdoesbutlookscool6thefinalgenericsmenace.conc line 7:4 Method def (java.lang.Object) java.lang.Object with matching argument definition exists already in supertype or traits as: def (T) T - generic types are erased at runtime
 
~~~~~
//##8. Class template

~~~~~
//##10. Arrays
x10Arrays.conc line 10:3 Unable to resolve type corresponding to name: Collection
x10Arrays.conc line 18:13 Type array levels don't match. Expected: 1 vs 2
 
~~~~~
//##10.1. Array Types





~~~~~
//##10.2. Array Variables





~~~~~
//##10.3. Array Creation
x103ArrayCreation.conc line 4:30 Type array levels don't match. Expected: 1 vs 2
x103ArrayCreation.conc line 6:25 incompatible type: int[] vs null[]
x103ArrayCreation.conc line 16:17 At least one element of an array declaration must be non empty in order to be instantiable
x103ArrayCreation.conc line 20:20 Type array levels don't match. Expected: 2 vs 1
x103ArrayCreation.conc line 26:16 Type array levels don't match. Expected: 3 vs 1
 
~~~~~
//##10.4. Array Access





~~~~~
//##10.5. Array Store Exception





~~~~~
//##10.6. Array Initializers
x106ArrayInitializers.conc line 12:13 Type mismatch: cannot convert from int[] to float[]
x106ArrayInitializers.conc line 13:14 Cannot cast from int[] to float[]
 
~~~~~
//##10.6.a Array Initializers  generics and more
x106aArrayInitializersgenericsandmore.conc line 8:18 Type mismatch: cannot convert from java.lang.Number[] to java.lang.Integer[]
x106aArrayInitializersgenericsandmore.conc line 12:13 Cannot create a generic array of T
x106aArrayInitializersgenericsandmore.conc line 13:13 Cannot instantiate generic type T
 
~~~~~
//##10.7. Array Members





~~~~~
//##10.8. Class Objects for Arrays





~~~~~
//##10.9. An Array of Characters is Not a String





~~~~~
//##11. Exceptions



~~~~~
//##11.a Exceptions thrown ext throwable etc
x11aExceptionsthrownextthrowableetc.conc line 9:10 Exception type thrown must be a subtype of Throwable: x11aExceptionsthrownextthrowableetc.Excep2 is not
x11aExceptionsthrownextthrowableetc.conc line 10:10 Exception type thrown must be a subtype of Throwable not: int
x11aExceptionsthrownextthrowableetc.conc line 11:10 Exception type thrown must be a subtype of Throwable not: def (java.lang.Integer) void
x11aExceptionsthrownextthrowableetc.conc line 16:2 Generic Exception type upper bound must be of type or subtype of Throwable: java.lang.Object is not
 
~~~~~
//##11.b generic classes cannot subclass throwable
x11bgenericclassescannotsubclassthrowable.conc line 4:0 The generic class x11bgenericclassescannotsubclassthrowable.Excep cannot superclass java.lang.Throwable
x11bgenericclassescannotsubclassthrowable.conc line 5:0 The generic class x11bgenericclassescannotsubclassthrowable.Oops cannot superclass java.lang.Throwable
 
~~~~~
//##11.c Unreachable catch block
x11cUnreachablecatchblock.conc line 4:4 Use of try catch finally is inappropriate - try block is empty
x11cUnreachablecatchblock.conc line 5:52 Unreachable catch block for java.lang.Exception. It is already handled by the catch block for java.lang.Throwable 
 
~~~~~
//##14. CONC continue and break in right place
x14CONCcontinueandbreakinrightplace.conc line 45:0 continue cannot be used outside of a loop or inside a parallel for loop
x14CONCcontinueandbreakinrightplace.conc line 49:1 break cannot be used outside of a loop, onchange, every, await or async block or inside a parallel for loop
x14CONCcontinueandbreakinrightplace.conc line 49:1 line cannot be present within class definition
x14CONCcontinueandbreakinrightplace.conc line 58:3 break cannot be used outside of a loop, onchange, every, await or async block or inside a parallel for loop
 
~~~~~
//##14. CONC assert 


~~~~~
//##14. CONC withable 
x14CONCwithable.conc line 8:4 Resource specified in try with resource block must implement close method, type: java.lang.String does not
x14CONCwithable.conc line 9:0 Use of try catch finally is inappropriate - try block is empty
 
~~~~~
//##14. CONC old for loop 
x14CONColdforloop.conc line 30:0 Unreachable code
x14CONColdforloop.conc line 30:8 n cannot be resolved to a variable
x14CONColdforloop.conc line 34:0 Unreachable code
x14CONColdforloop.conc line 34:4 n cannot be resolved to a variable
 
~~~~~
//##14. CONC return stmt in special place
x14CONCreturnstmtinspecialplace.conc line 3:0 return statement is not valid here
x14CONCreturnstmtinspecialplace.conc line 7:2 line cannot be present within class definition
x14CONCreturnstmtinspecialplace.conc line 7:2 return statement is not valid here
x14CONCreturnstmtinspecialplace.conc line 14:4 return statement is not valid here
x14CONCreturnstmtinspecialplace.conc line 19:2 Unreachable code after return statement
x14CONCreturnstmtinspecialplace.conc line 23:2 Unreachable code after return statement
x14CONCreturnstmtinspecialplace.conc line 27:2 Unreachable code after return statement
x14CONCreturnstmtinspecialplace.conc line 32:2 Unreachable code after return statement
x14CONCreturnstmtinspecialplace.conc line 42:1 return statement is not valid here
 
~~~~~
//##15.1. CONC  simple exp

~~~~~
//##15.16. Misc one

~~~~~
//##15.16. Misc arrays
x1516Miscarrays.conc line 14:0 A2 cannot extend: A as it is closed
 
~~~~~
//##15.16. Misc arrays tests 2


~~~~~
//##15.16. Misc arrays tests 3


~~~~~
//##15.16. Var resolution 1


~~~~~
//##15.16. Var resolution 2  methods

~~~~~
//##15.16. Var resolution 3  supes

~~~~~
//##15.16. werid not supported
x1516weridnotsupported.conc line 7:4 void methods cannot return a value
x1516weridnotsupported.conc line 10:23 Unable to resolve reference to variable name: mountain
 
~~~~~
//##15.16.  not supported  u cant have static class methods


~~~~~
//##15.16.  some random stuff with btyes

~~~~~
//##15.16.  ambigious function stuff long winded
x1516ambigiousfunctionstufflongwinded.conc line 17:0 Ambiguous method detected 'test'. More than one indirect match made
 
~~~~~
//##15.16. return type not considered when resolving fun
x1516returntypenotconsideredwhenresolvingfun.conc line 14:10 int is not a subtype of java.lang.String
 
~~~~~
//##15.16. cool random stuff
x1516coolrandomstuff.conc line 8:4 Method: 'def toString() java.lang.String' of class: 'java.lang.Object' should be explicitly overriden in subclass: x1516coolrandomstuff.Point. Use the override keyword
 
~~~~~
//##15.16. random stuff 2 

~~~~~
//##15.16. thissuper method resolution



~~~~~
//##15.16. some more virtual stuff and subtypeing of generic thingy


~~~~~
//##15.16. more random stuff


~~~~~
//##15.16. vartypes


~~~~~
//##15.16. random stuff


~~~~~
//##15.16. random stuff 2 



~~~~~
//##15.16. no threads for you

~~~~~
//##15.16. lets do some assignment 1

~~~~~
//##15.16. lets do some assignment 2

~~~~~
//##15.16. inc dec post prefix only on special stuff
x1516incdecpostprefixonlyonspecialstuff.conc line 23:0 Invalid argument to operation ++/--
x1516incdecpostprefixonlyonspecialstuff.conc line 25:4 Invalid argument to operation ++/--
 
~~~~~
//##15.16. Cast Expressions
x1516CastExpressions.conc line 5:15 Cannot cast from int[] to double[]
x1516CastExpressions.conc line 15:12 Cannot compare an instance of x1516CastExpressions.Element with x1516CastExpressions.Point
x1516CastExpressions.conc line 17:16 Cannot cast from x1516CastExpressions.Element to x1516CastExpressions.Point
 
~~~~~
//##15.16. equality
x1516equality.conc line 45:4 Incompatible operand types int[] and int[2]
x1516equality.conc line 47:4 Incompatible operand types int[] and int[2]
 
~~~~~
//##15.16. conditional ? : 

~~~~~
//##15.16. array type inference
x1516arraytypeinference.conc line 11:19 Type mismatch: cannot convert from java.lang.Integer[] to int[]
x1516arraytypeinference.conc line 16:21 Type mismatch: cannot convert from x1516arraytypeinference.A[] to x1516arraytypeinference.C[]
x1516arraytypeinference.conc line 22:0 Variable ugh has already been defined in current scope
x1516arraytypeinference.conc line 23:27 Type mismatch: cannot convert from java.util.ArrayList<java.lang.String>[] to java.util.ArrayList<java.lang.Object>[]
x1516arraytypeinference.conc line 28:10 Type array levels don't match. Expected: 0 vs 1
 
~~~~~
//##16. Definite Assignment





~~~~~
//##16.1. Definite Assignment and Expressions





~~~~~
//##16.1.1. Boolean Constant Expressions





~~~~~
//##16.1.2. ConditionalAnd Operator &&





~~~~~
//##16.1.3. ConditionalOr Operator ||





~~~~~
//##16.1.4. Logical Complement Operator !





~~~~~
//##16.1.5. Conditional Operator ? :





~~~~~
//##16.1.6. Conditional Operator ? :





~~~~~
//##16.1.7. Other Expressions of Type boolean





~~~~~
//##16.1.8. Assignment Expressions





~~~~~
//##16.1.9. Operators ++ and 





~~~~~
//##16.1.10. Other Expressions





~~~~~
//##16.2. Definite Assignment and Statements





~~~~~
//##16.2.1. Empty Statements





~~~~~
//##16.2.2. Blocks





~~~~~
//##16.2.3. Local Class Declaration Statements





~~~~~
//##16.2.4. Local Variable Declaration Statements





~~~~~
//##16.2.5. Labeled Statements





~~~~~
//##16.2.6. Expression Statements





~~~~~
//##16.2.7. if Statements





~~~~~
//##16.2.8. assert Statements





~~~~~
//##16.2.9. switch Statements





~~~~~
//##16.2.10. while Statements





~~~~~
//##16.2.11. do Statements





~~~~~
//##16.2.12. for Statements





~~~~~
//##16.2.12.1. Initialization Part of for Statement





~~~~~
//##16.2.12.2. Incrementation Part of for Statement





~~~~~
//##16.2.13. break, continue, return, and throw Statements





~~~~~
//##16.2.14. synchronized Statements





~~~~~
//##16.2.15. try Statements





~~~~~
//##16.3. Definite Assignment and Parameters





~~~~~
//##16.4. Definite Assignment and Array Initializers





~~~~~
//##16.5. Definite Assignment and Enum Constants





~~~~~
//##16.6. Definite Assignment and Anonymous Classes





~~~~~
//##16.7. Definite Assignment and Member Types





~~~~~
//##16.8. Definite Assignment and Static Initializers





~~~~~
//##16.9. Definite Assignment, Constructors, and Instance Initializers




~~~~~
//##69  Misc  ensure superconstructor is correctly generic colored



~~~~~
//##69  Misc  generics arrays
x69Miscgenericsarrays.conc line 7:16 Type mismatch: cannot convert from java.lang.Integer to int[]
x69Miscgenericsarrays.conc line 8:13 Type array levels don't match. Expected: 1 vs 0
x69Miscgenericsarrays.conc line 11:13 Type array levels don't match. Expected: 14 vs 1
x69Miscgenericsarrays.conc line 13:27 Generic Type argument type mismatch: int[] vs int[3]
x69Miscgenericsarrays.conc line 14:28 Generic Type argument type mismatch: int[6] vs int[5]
 
~~~~~
//##70  Withable 1
x70Withable1.conc line 3:4 Resource specified in try with resource block must implement close method, '89' does not
x70Withable1.conc line 5:0 WW cannot resolve reference to trait: com.concurnas.lang.Withable
x70Withable1.conc line 9:4 Resource specified in try with resource block must implement close method, '89' does not
 
~~~~~
//##70  misc -1trycatch
x70misc1trycatch.conc line 6:1 Try catch finnally block must consist of at least one catch block and/or one finnally block
x70misc1trycatch.conc line 11:1 Unreachable code after exception thrown
 
~~~~~
//##71  explicit constructor invokation
x71explicitconstructorinvokation.conc line 9:0 Implicit super constructor for superclass x71explicitconstructorinvokation.Sup of x71explicitconstructorinvokation.CFail1 is undefined. Must explicitly invoke another constructor or invoke superconstructor
x71explicitconstructorinvokation.conc line 15:1 Implicit super constructor for superclass x71explicitconstructorinvokation.Sup of x71explicitconstructorinvokation.CFail2 is undefined. Must explicitly invoke another constructor or invoke superconstructor
x71explicitconstructorinvokation.conc line 25:1 Constructor with matching argument definition exists already in current Scope
 
 ~~~~~
//##69. lhs must be a variable
x69lhsmustbeavariable.conc line 11:0 The left-hand side of an assignment must be a variable
x69lhsmustbeavariable.conc line 13:0 The left-hand side of an assignment must be a variable
 
~~~~~
//##81.1 ref eq only on objecst not prim
x811refeqonlyonobjecstnotprim.conc line 3:4 Refernce equality can only be applied to objects not: int, int

~~~~~
//##81.2 pow shortcut to math.pow returns double

~~~~~
//##81.3 a doesnt exist! check type dammit!
x813adoesntexist!checktypedammit!.conc line 5:15 a cannot be resolved to a variable
 

~~~~~
//##81.4 no, you cannot go from a boolean to a int in a for loop assignment
x814noyoucannotgofromabooleantoaintinaforloopassignment.conc line 5:8 Type mismatch: cannot convert from int to boolean
 
~~~~~
//##81.4 plus eq is ok in for loops

~~~~~
//##81.5 only eq can be used on new var in for loop
x815onlyeqcanbeusedonnewvarinforloop.conc line 5:1 Assignment type not permitted for new variable 'n'
 
~~~~~
//##81.6 you cannot have matixes of int and double (ie differing types)

~~~~~
//##81.6b initialization rhs transformation
x816binitializationrhstransformation.conc line 7:15 Type mismatch: cannot convert from int[] to java.lang.Object[]
x816binitializationrhstransformation.conc line 8:17 Type mismatch: cannot convert from int[] to java.lang.Object[]
x816binitializationrhstransformation.conc line 11:17 Type mismatch: cannot convert from int[] to java.lang.Object[]
x816binitializationrhstransformation.conc line 14:0 Type mismatch: cannot convert from int[] to java.lang.Object[]
 
~~~~~
//##81.7 cannot do plus etc on arrays
 
~~~~~
//##81.8 cannot assign to array length
x818cannotassigntoarraylength.conc line 10:1 The field length has been decalred as val and cannot be reassigned
x818cannotassigntoarraylength.conc line 11:1 Cannot change the length of an array
 
~~~~~
//##81.9 no ciruclar refernces in consttuors
x819nociruclarreferncesinconsttuors.conc line 4:1 Recursive constructor invocation is not permitted
 
~~~~~
//##81.9b no ciruclar refernces in consttuors via indirection
x819bnociruclarreferncesinconsttuorsviaindirection.conc line 4:1 Recursive constructor invocation is not permitted
x819bnociruclarreferncesinconsttuorsviaindirection.conc line 7:1 Recursive constructor invocation is not permitted
 
~~~~~
//##81.9c no ciruclar refernces in consttuors via indirection
x819cnociruclarreferncesinconsttuorsviaindirection.conc line 4:1 Recursive constructor invocation is not permitted
x819cnociruclarreferncesinconsttuorsviaindirection.conc line 8:1 Recursive constructor invocation is not permitted
x819cnociruclarreferncesinconsttuorsviaindirection.conc line 11:1 Recursive constructor invocation is not permitted
 
~~~~~
//##11. supernonexist blah
x11supernonexistblah.conc line 13:10 Array index operation can only be performed on object that is an array, Map, List or object having approperiate operator overloading 'get' method not: d cannot be resolved to a variable
 
~~~~~
//##82. cannot find function
x82cannotfindfunction.conc line 3:10 Parameter names must be defined for non abstract method definition 'range'
x82cannotfindfunction.conc line 8:11 Unable to resolve type corresponding to name: x
x82cannotfindfunction.conc line 10:8 Unable to find method with matching name: x
x82cannotfindfunction.conc line 15:4 Unable to find method with matching name: range and arguments (int)

~~~~~
//##82. cannot be resolved to variable... 
x82cannotberesolvedtovariable.conc line 7:4 a1 cannot be resolved to a variable
x82cannotberesolvedtovariable.conc line 8:3 a1 cannot be resolved to a variable
x82cannotberesolvedtovariable.conc line 9:14 a1 cannot be resolved to a variable
 

 
 ~~~~~
//##83. lhs must be var
x83lhsmustbevar.conc line 4:25 The left-hand side of an assignment must be a variable

~~~~~
//##84. this is ok, autogen we ignore all the override rules etc

 
~~~~~
//##85. is is only for objects
x85isisonlyforobjects.conc line 5:10 Cannot perform is or isnot check against expression of primative type
x85isisonlyforobjects.conc line 6:10 Cannot perform is or isnot check on primative type
x85isisonlyforobjects.conc line 7:10 Cannot perform is or isnot check on primative type
 
~~~~~
//##86. object array init weirdness
x86objectarrayinitweirdness.conc line 5:18 int[] is not a subtype of java.lang.Integer[]
 
~~~~~
//##87. misc  mis aligned generic type inf shouldn't blow up the whole world
x87miscmisalignedgenerictypeinfshouldn'tblowupthewholeworld.conc line 3:41 Unable to resolve type corresponding to name: Z
x87miscmisalignedgenerictypeinfshouldn'tblowupthewholeworld.conc line 9:3 numerical operation cannot be performed on type java.lang.Object
 
~~~~~
//##88. fiddly nesting not permitted
x88fiddlynestingnotpermitted.conc line 10:10 Nested Class: x88fiddlynestingnotpermitted.Outer.Inner.GG cannot be directly instantiated. Can only be instantiated via reference of class: x88fiddlynestingnotpermitted.Outer.Inner
 
~~~~~
//##89. funcref from boxed to unboxed type not permitted

~~~~~
//##90. cannot assign to null
x90cannotassigntonull.conc line 8:1 void is not an instantiable type
x90cannotassigntonull.conc line 9:1 void is not an instantiable type
 
~~~~~
//##91. lambda cannot be directly invoked
x91lambdacannotbedirectlyinvoked.conc line 7:1 For variable reference on: 'xxx' it must be a method or method reference not: com.concurnas.bootstrap.lang.Lambda. No overloaded 'invoke' operator found for type com.concurnas.bootstrap.lang.Lambda with signature: '()'
 
~~~~~
//##92. lambda moans about this generical assingment
x92lambdamoansaboutthisgenericalassingment.conc line 7:11 Unable to find method with matching name: constructor and arguments (def () java.lang.String)
 
~~~~~
//##92. lambda moans about this generical assingment part 2 other way around
 
~~~~~
//##93. ensure sublist in array sublist op returns list and not arraylist
x93ensuresublistinarraysublistopreturnslistandnotarraylist.conc line 12:29 cannot assign type of java.util.List<java.lang.Integer> to java.util.ArrayList<java.lang.Integer>
 
~~~~~
//##94. err on ret type cause moan
x94erronrettypecausemoan.conc line 4:13 Unable to resolve type corresponding to name: Stirng
x94erronrettypecausemoan.conc line 10:1 Unable to determine type of: Stirng
 
~~~~~
//##95. assignment fails i forgot about before
x95assignmentfailsiforgotaboutbefore.conc line 3:16 Unable to resolve type corresponding to name: ArrayList
 
~~~~~
//##96. nested scope overwrite

~~~~~
//##97. cannot call method on array directly
 
~~~~~
//##98. def with null lists

~~~~~
//##99. misc stuff with keysets and generic types etc
x99miscstuffwithkeysetsandgenerictypesetc.conc line 11:14 Cannot find get function on java.util.Set<java.lang.Integer> with arguments: [int]
x99miscstuffwithkeysetsandgenerictypesetc.conc line 12:21 Cannot find get function on java.util.Set<java.lang.Integer> with arguments: [int]
 
~~~~~
//##100. array moan
x100arraymoan.conc line 5:10 Array index operation can only be performed on object that is an array, Map, List or object having approperiate operator overloading 'get' method not: int
 
~~~~~
//##101. no escaping from the erasure rules you sneeky git
x101noescapingfromtheerasurerulesyousneekygit.conc line 8:0 Function x with matching argument definition exists already in current Scope
 
~~~~~
//##102. sealed classes by default
x102sealedclassesbydefault.conc line 8:0 F1 cannot extend: ClosedOne as it is closed
x102sealedclassesbydefault.conc line 9:0 F2 cannot extend: ClosedOne2 as it is closed
x102sealedclassesbydefault.conc line 11:0 class Fail3 is implictly abstract and cannot be closed
x102sealedclassesbydefault.conc line 30:0 JJ cannot extend: String as it is closed
 
~~~~~
//##103. final methods
x103finalmethods.conc line 8:0 JJ cannot extend: String as it is closed
x103finalmethods.conc line 17:1 Method: 'def finfun() java.lang.String' of class: 'x103finalmethods.MyCls' has been defined as final and cannot be be overriden in subclass: x103finalmethods.Chi
x103finalmethods.conc line 21:1 Method: 'def finfun() java.lang.String' of class: 'x103finalmethods.MyCls' has been defined as final and cannot be be overriden in subclass: x103finalmethods.Chi2
x103finalmethods.conc line 25:1 Abstract method definition of 'finfun' cannnot be final
 
~~~~~
//##104. final var tests  1  gen 
x104finalvartests1gen.conc line 7:0 Variable a has been decalred as val and cannot be reassigned
x104finalvartests1gen.conc line 8:0 Variable b has been decalred as val and cannot be reassigned
x104finalvartests1gen.conc line 11:0 Variable finVar has already been defined in current scope
x104finalvartests1gen.conc line 12:0 Variable finVar has been decalred as val and cannot be reassigned
x104finalvartests1gen.conc line 13:0 Variable finVar has already been defined in current scope
x104finalvartests1gen.conc line 14:0 Variable finVar has already been defined in current scope
x104finalvartests1gen.conc line 17:6 Variable finVar has been decalred as val and cannot be reassigned
x104finalvartests1gen.conc line 23:0 Variable x has already been defined in current scope
x104finalvartests1gen.conc line 24:0 Variable x has already been defined in current scope
x104finalvartests1gen.conc line 50:1 Variable golo has been decalred as val and cannot be reassigned
x104finalvartests1gen.conc line 52:2 Variable golo has been decalred as val and cannot be reassigned
x104finalvartests1gen.conc line 61:2 Variable golo3 has been decalred as val and cannot be reassigned
x104finalvartests1gen.conc line 64:2 Variable golo3 has been decalred as val and cannot be reassigned
x104finalvartests1gen.conc line 68:0 Variable xfga has been decard val at top level and so must have a value assigned to it
x104finalvartests1gen.conc line 86:1 Variable x has been decalred as val and cannot be reassigned
 
~~~~~
//##104. final var tests  2  classes a
x104finalvartests2classesa.conc line 9:2 Variable x has been decalred as val and cannot be reassigned
x104finalvartests2classesa.conc line 10:2 Variable y has been decalred as val and cannot be reassigned
x104finalvartests2classesa.conc line 16:0 These variables have been declared val but have not been assigned a value in the auto gennerated constructor: a
x104finalvartests2classesa.conc line 18:1 Variable z has already been defined in current scope
x104finalvartests2classesa.conc line 22:2 Variable y has been decalred as val and cannot be reassigned
x104finalvartests2classesa.conc line 23:2 Variable z has been decalred as val and cannot be reassigned
x104finalvartests2classesa.conc line 24:2 Variable a has been decalred as val and cannot be reassigned
x104finalvartests2classesa.conc line 32:1 The field y has been decalred as val and cannot be reassigned
x104finalvartests2classesa.conc line 33:1 The field z has been decalred as val and cannot be reassigned
 
~~~~~
//##104. final var tests  3  classes b
x104finalvartests3classesb.conc line 4:11 Cannot autodefine setter for field x that has been declared val
x104finalvartests3classesb.conc line 5:3 Cannot autodefine setter for field y that has been declared val
x104finalvartests3classesb.conc line 8:1 Cannot autodefine setter for field x2 that has been declared val
x104finalvartests3classesb.conc line 9:1 Cannot autodefine setter for field y2 that has been declared val
 
~~~~~
//##104. final var tests  4  cls a
x104finalvartests4clsa.conc line 4:0 These variables have been declared val but have not been assigned a value in the auto gennerated constructor: x
x104finalvartests4clsa.conc line 10:2 Variable x has been decalred as val and cannot be reassigned
x104finalvartests4clsa.conc line 11:2 Variable y has been decalred as val and cannot be reassigned
 
~~~~~
//##105. final var tests  5 constructors set only  1
x105finalvartests5constructorssetonly1.conc line 4:0 These variables have been declared val but have not been assigned a value in the auto gennerated constructor: y
x105finalvartests5constructorssetonly1.conc line 8:0 These variables have been declared val but have not been assigned a value in the auto gennerated constructor: yp
x105finalvartests5constructorssetonly1.conc line 12:0 These variables have been declared val but have not been assigned a value in the auto gennerated constructor: x, y

~~~~~
//##105. final var tests  5 constructors set only  2

~~~~~
//##105. final var tests  5 constructors set only  3
x105finalvartests5constructorssetonly3.conc line 21:1 These variables have been declared val but have not been assigned a value in this constructor: x, y
x105finalvartests5constructorssetonly3.conc line 24:1 These variables have been declared val and can only be set once in constructor call hierarchy: x, y
x105finalvartests5constructorssetonly3.conc line 29:1 These variables have been declared val and can only be set once in constructor call hierarchy: y
 
~~~~~
//##105. final var tests  5 constructors set only  4
x105finalvartests5constructorssetonly4.conc line 4:0 These variables have been declared val but have not been assigned a value in the auto gennerated constructor: x
x105finalvartests5constructorssetonly4.conc line 10:2 Variable x has been decalred as val and cannot be reassigned
x105finalvartests5constructorssetonly4.conc line 11:2 Variable y has been decalred as val and cannot be reassigned
 
~~~~~
//##105. final var tests  6 this is fine
x105finalvartests6thisisfine.conc line 4:0 Return statement in method must return type of void
x105finalvartests6thisisfine.conc line 5:1 void is not an instantiable type
x105finalvartests6thisisfine.conc line 13:1 void is not an instantiable type
x105finalvartests6thisisfine.conc line 14:20 The variable g is not visible
 
~~~~~
//##106. imported final var cannot be fiddled with
x106importedfinalvarcannotbefiddledwith.conc line 7:0 The field black has been decalred as val and cannot be reassigned
 
~~~~~
//##107. Dunno where this belongs - abst if not impl all
x107Dunnowherethisbelongsabstifnotimplall.conc line 16:16 Class: x107Dunnowherethisbelongsabstifnotimplall.Molly is not instantiable
x107Dunnowherethisbelongsabstifnotimplall.conc line 17:17 Class: x107Dunnowherethisbelongsabstifnotimplall.Dave is not instantiable
 
~~~~~
//##107. nested final class logic
x107nestedfinalclasslogic.conc line 21:2 These variables have been declared val but have not been assigned a value in this constructor: x, y
x107nestedfinalclasslogic.conc line 24:2 These variables have been declared val and can only be set once in constructor call hierarchy: x, y
x107nestedfinalclasslogic.conc line 29:2 These variables have been declared val and can only be set once in constructor call hierarchy: y
x107nestedfinalclasslogic.conc line 35:2 These variables have been declared val but have not been assigned a value in the auto gennerated constructor: x
x107nestedfinalclasslogic.conc line 41:4 Variable x has been decalred as val and cannot be reassigned
x107nestedfinalclasslogic.conc line 42:4 Variable y has been decalred as val and cannot be reassigned
 
~~~~~
//##110. ppp  abstract no privates
x110pppabstractnoprivates.conc line 5:1 The abstract method johnny cannot be declared private
 
~~~~~
//##111. ppp  cannot narrow scope
x111pppcannotnarrowscope.conc line 15:1 Method: 'def somethng() java.lang.String' of class: 'x111pppcannotnarrowscope.Sup' has been declared with accesability: public but implementation in subclass: x111pppcannotnarrowscope.FAIL attempts to narrow scope to: private - this cannot be done
x111pppcannotnarrowscope.conc line 19:1 Method: 'def somethng() java.lang.String' of class: 'x111pppcannotnarrowscope.Sup' has been declared with accesability: public but implementation in subclass: x111pppcannotnarrowscope.FAIL2 attempts to narrow scope to: protected - this cannot be done
x111pppcannotnarrowscope.conc line 32:1 Method: 'def somethng() java.lang.String' of class: 'x111pppcannotnarrowscope.Sup2' has been declared with accesability: protected but implementation in subclass: x111pppcannotnarrowscope.FAIL3 attempts to narrow scope to: private - this cannot be done
 
~~~~~
//##112. ppp  cannot narrow scope  already compiled class
x112pppcannotnarrowscopealreadycompiledclass.conc line 10:1 Method: 'def somethng() java.lang.String' of class: 'com.concurnas.lang.precompiled.TestHelperClasses$Sup' has been declared with accesability: public but implementation in subclass: x112pppcannotnarrowscopealreadycompiledclass.FAIL attempts to narrow scope to: private - this cannot be done
x112pppcannotnarrowscopealreadycompiledclass.conc line 14:1 Method: 'def somethng() java.lang.String' of class: 'com.concurnas.lang.precompiled.TestHelperClasses$Sup' has been declared with accesability: public but implementation in subclass: x112pppcannotnarrowscopealreadycompiledclass.FAIL2 attempts to narrow scope to: protected - this cannot be done
x112pppcannotnarrowscopealreadycompiledclass.conc line 23:1 Method: 'def somethng() java.lang.String' of class: 'com.concurnas.lang.precompiled.TestHelperClasses$Sup2' has been declared with accesability: public but implementation in subclass: x112pppcannotnarrowscopealreadycompiledclass.FAIL3 attempts to narrow scope to: private - this cannot be done
 
~~~~~
//##113. ppp  calling methods whith are ppp
x113pppcallingmethodswhithareppp.conc line 45:3 The method methprivat is not visible
x113pppcallingmethodswhithareppp.conc line 53:8 The method methprivat is not visible
x113pppcallingmethodswhithareppp.conc line 57:2 The method methprivat is not visible
 
~~~~~
//##113.b ppp  calling methods whith are ppp  calling precompiled class
x113bpppcallingmethodswhitharepppcallingprecompiledclass.conc line 24:3 The method methprotect is not visible
x113bpppcallingmethodswhitharepppcallingprecompiledclass.conc line 25:3 The method methprivat is not visible
x113bpppcallingmethodswhitharepppcallingprecompiledclass.conc line 26:3 The method methdefault is not visible
x113bpppcallingmethodswhitharepppcallingprecompiledclass.conc line 34:8 The method methprivat is not visible
x113bpppcallingmethodswhitharepppcallingprecompiledclass.conc line 38:2 The method methprivat is not visible
x113bpppcallingmethodswhitharepppcallingprecompiledclass.conc line 39:2 The method methdefault is not visible
 
~~~~~
//##113.c ppp  calling methods whith are ppp  dynamic binding to one which is visible

~~~~~
//##114.a ppp  accessability of constructors
x114apppaccessabilityofconstructors.conc line 20:5 The constructor is not visible
x114apppaccessabilityofconstructors.conc line 21:5 The constructor is not visible
 
~~~~~
//##114.b ppp  accessability of constructors  precomp class
x114bpppaccessabilityofconstructorsprecompclass.conc line 8:5 Unable to resolve reference to constructor for: 'com.concurnas.lang.precompiled.PrivatePublicProtected$MollyPreDef'(int)
x114bpppaccessabilityofconstructorsprecompclass.conc line 9:5 The constructor is not visible
 
~~~~~
//##114.c ppp  accessability of constructors  extend in childclass
x114cpppaccessabilityofconstructorsextendinchildclass.conc line 28:2 The constructor is not visible
 
~~~~~
//##114.d ppp  accessability of constructors  precomp class extend in childclass
x114dpppaccessabilityofconstructorsprecompclassextendinchildclass.conc line 15:2 Unable to resolve reference to super constructor for: 'com.concurnas.lang.precompiled.PrivatePublicProtected$MollyPreDef'(int)
 
~~~~~
//##115.a ppp  local vars
x115appplocalvars.conc line 34:5 The variable z is not visible
x115appplocalvars.conc line 41:10 The variable z is not visible
x115appplocalvars.conc line 45:6 The variable z is not visible
x115appplocalvars.conc line 60:8 The variable z is not visible
x115appplocalvars.conc line 65:1 Local variables may not have an access modifier defined
x115appplocalvars.conc line 69:10 The variable z is not visible
 
~~~~~
//##115.b ppp  local vars imported from precompiled
x115bppplocalvarsimportedfromprecompiled.conc line 13:7 The variable privModLevel is not visible

~~~~~
//##115.c ppp  local vars imported from precompiled fails a few times
x115cppplocalvarsimportedfromprecompiledfailsafewtimes.conc line 25:5 The variable z is not visible
x115cppplocalvarsimportedfromprecompiledfailsafewtimes.conc line 32:10 The variable z is not visible
x115cppplocalvarsimportedfromprecompiledfailsafewtimes.conc line 36:6 The variable z is not visible
x115cppplocalvarsimportedfromprecompiledfailsafewtimes.conc WARN line 6:0 Class name overwrites imported class: Vars
 
~~~~~
//##115.d ppp  static functions imported access modifiers get respected
x115dpppstaticfunctionsimportedaccessmodifiersgetrespected.conc line 13:8 The method aFunctionPrivate is not visible
x115dpppstaticfunctionsimportedaccessmodifiersgetrespected.conc line 14:8 The method aFunctionProtected is not visible
 
~~~~~
//##116.a ppp  vars can increase scope  

~~~~~
//##116.b ppp  and also overide scope
x116bpppandalsooveridescope.conc line 9:11 The variable g is not visible

~~~~~
//##117. something i overlooked re parent types being genericalsz

~~~~~
//##117.b something i overlooked re parent types being genericalsz  reson i found prev one

~~~~~
//##118. ambigousness checks

~~~~~
//##119. more ambigousness checks

~~~~~
//##120. cannot dup local fields
x120cannotduplocalfields.conc line 6:1 Variable ss has already been defined in current scope
x120cannotduplocalfields.conc line 11:1 Variable ss has already been defined in current scope
 
~~~~~
//##121. search no further than parent when doing assignment checking
x121searchnofurtherthanparentwhendoingassignmentchecking.conc line 12:18 The variable lam1 is not visible

~~~~~
//##122. search no further than parent when doing assignment checking  lambda version

~~~~~
//##123. null lists on setters
x123nulllistsonsetters.conc line 15:4 Unable to find method with matching name: setX and arguments (null[])

~~~~~
//##124. null lists on setters of obj type

~~~~~
//##125. null lists on on if expr stmts
x125nulllistsononifexprstmts.conc line 7:18 incompatible type: int[] vs null[]
x125nulllistsononifexprstmts.conc line 8:19 incompatible type: int[] vs null[]
 
~~~~~
//##126. generic qmark owns all
x126genericqmarkownsall.conc line 9:6 Unable to find method with matching name: add and arguments (int)
 
~~~~~
//##127.1 null lists in maps  basic


~~~~~
//##127.2 null lists in maps  as obj

~~~~~
//##127.3 null lists in maps  2d int

~~~~~
//##127.4 null lists in maps  moan not doable to simple array
x1274nulllistsinmapsmoannotdoabletosimplearray.conc line 6:32 Generic Type argument type mismatch: int[] vs null[]
 
~~~~~
//##127.5 null lists in maps  maps

~~~~~
//##127.6 null lists in maps  2d maps

~~~~~
//##127.7 null lists in maps  maps in functions
 
~~~~~
//##128. finally blocks cant have return, break or continue
x128finallyblockscanthavereturnbreakorcontinue.conc line 7:2 Fianlly blocks cannot contain return statements
x128finallyblockscanthavereturnbreakorcontinue.conc line 22:3 Fianlly blocks cannot contain break statements
x128finallyblockscanthavereturnbreakorcontinue.conc line 32:3 Fianlly blocks cannot contain continue statements
x128finallyblockscanthavereturnbreakorcontinue.conc line 35:1 Unreachable code
x128finallyblockscanthavereturnbreakorcontinue.conc line 60:1 Unreachable code
 
~~~~~
//##129 avoid the more pathalogical try catch excep cases
x129avoidthemorepathalogicaltrycatchexcepcases.conc line 4:4 Use of try catch finally is inappropriate - just use return
x129avoidthemorepathalogicaltrycatchexcepcases.conc line 14:4 Use of try catch finally is inappropriate - just use return
x129avoidthemorepathalogicaltrycatchexcepcases.conc line 24:4 Use of try catch finally is inappropriate - try block is empty
 
~~~~~
//##130 fwd references
x130fwdreferences.conc line 5:36 d cannot be resolved to a variable

~~~~~
//##131 check private constructor cant be called
x131checkprivateconstructorcantbecalled.conc line 6:11 The constructor is not visible

~~~~~
//##132 no u cant instantiate this private guy
x132noucantinstantiatethisprivateguy.conc line 9:9 The constructor is not visible

~~~~~
//##133 java system lib classes cannot be extended
 
~~~~~
//##134.a primatives can be generic qualifiers

~~~~~
//##134.b primatives can be generic qualifiers fix bool bug

~~~~~
//##134.c the joy of lambdas
x134cthejoyoflambdas.conc line 11:9 Unable to find method with matching name: constructor and arguments (def () java.lang.Integer)
x134cthejoyoflambdas.conc line 12:9 Unable to find method with matching name: constructor and arguments (def () java.lang.Integer)
 
~~~~~
//##135. copy function only on object types
x135copyfunctiononlyonobjecttypes.conc line 7:6 Copies can only be made of Object types not: int
x135copyfunctiononlyonobjecttypes.conc line 10:9 Copies can only be made of Object types not: null[]
 
~~~~~
//##136. int to Integer

~~~~~
//##137. check this works

~~~~~
//##138. misc err
x138miscerr.conc line 10:2 The left-hand side of an assignment must be a variable
 
~~~~~
//##139. compiler used to blow up when postfix augmenter visitor was in wrong place
 
~~~~~
//##140. double cehck boolean operations
x140doublecehckbooleanoperations.conc line 10:7 Incompatible operand types boolean and int
x140doublecehckbooleanoperations.conc line 11:7 Incompatible operand types boolean and int
x140doublecehckbooleanoperations.conc line 12:7 Incompatible operand types boolean and int
 
~~~~~
//##141. Cannot refer to an instance field y while explicitly invoking a constructor
x141Cannotrefertoaninstancefieldywhileexplicitlyinvokingaconstructor.conc line 7:7 Cannot refer to instance variable y while explicitly invoking a constructor
x141Cannotrefertoaninstancefieldywhileexplicitlyinvokingaconstructor.conc line 14:8 Cannot refer to instance variable y2 while explicitly invoking a constructor
x141Cannotrefertoaninstancefieldywhileexplicitlyinvokingaconstructor.conc line 23:8 Cannot refer to instance variable ok while explicitly invoking a constructor
 
~~~~~
//##142. if throws an exception then doesnt return anything
 
~~~~~
//##142.b if throws an exception then doesnt return anything  in plus minus
  
~~~~~
//##143. in for loop last thing must be defined (and cannot be void)
x143inforlooplastthingmustbedefined(andcannotbevoid).conc line 7:29 for loop must return something
x143inforlooplastthingmustbedefined(andcannotbevoid).conc line 8:24 for loop must return something
x143inforlooplastthingmustbedefined(andcannotbevoid).conc line 9:29 for loop must return something
x143inforlooplastthingmustbedefined(andcannotbevoid).conc line 10:24 for loop must return something
 
~~~~~
//##144. break and continue can return stuff
x144breakandcontinuecanreturnstuff.conc line 6:18 continue statement cannot return value
x144breakandcontinuecanreturnstuff.conc line 8:21 while loop must return something
x144breakandcontinuecanreturnstuff.conc line 9:18 break statement cannot return value
x144breakandcontinuecanreturnstuff.conc line 11:21 while loop must return something
x144breakandcontinuecanreturnstuff.conc line 16:20 continue statement cannot return value
x144breakandcontinuecanreturnstuff.conc line 18:6 Generic Type argument type mismatch: java.lang.Integer vs void
x144breakandcontinuecanreturnstuff.conc line 19:20 break statement cannot return value
x144breakandcontinuecanreturnstuff.conc line 21:6 Generic Type argument type mismatch: java.lang.Integer vs void
x144breakandcontinuecanreturnstuff.conc line 26:20 continue statement cannot return value
x144breakandcontinuecanreturnstuff.conc line 28:23 for loop must return something
x144breakandcontinuecanreturnstuff.conc line 29:20 break statement cannot return value
x144breakandcontinuecanreturnstuff.conc line 31:23 for loop must return something
 
~~~~~
//##145. tweak imports
x145tweakimports.conc line 7:0 Import name has already been declared: ArrayList as: java.util.ArrayList
 
~~~~~
//##146. rubbish line imp used to think this was ok
x146rubbishlineimpusedtothinkthiswasok.conc line 7:6 The left-hand side of an assignment must be a variable

~~~~~
//##147. ensure blow up only on f missing
x147ensureblowuponlyonfmissing.conc line 5:12 f cannot be resolved to a variable
x147ensureblowuponlyonfmissing.conc line 7:8 f cannot be resolved to a variable
 
~~~~~
//##148. no iterate on obj
x148noiterateonobj.conc line 32:57 for loop must return something
x148noiterateonobj.conc line 33:4 Expected Iterable object or array for for loop not: java.lang.Object
 
~~~~~
//##149. variables dont return anything yet

~~~~~
//##150. wrong type ret
x150wrongtyperet.conc line 9:42 Generic Type argument type mismatch: java.util.List<java.lang.String> vs java.util.List<java.lang.Integer>
 
~~~~~
//##151. ensure all paths return properly
 
~~~~~
//##152. ensure no hide from dero analysis, even for block inner blocks are caught
x152ensurenohidefromderoanalysisevenforblockinnerblocksarecaught.conc line 7:28 The variable a may not have been initialized

~~~~~
//##153. inifinite loop deadcode  fors
x153inifiniteloopdeadcodefors.conc line 9:1 Unreachable code
x153inifiniteloopdeadcodefors.conc line 19:1 Unreachable code
x153inifiniteloopdeadcodefors.conc line 49:1 Unreachable code
 
~~~~~
//##154. cleanup func overrides
x154cleanupfuncoverrides.conc line 12:1 The return type of method 'def fail() double' in class x154cleanupfuncoverrides.Child cannot be matched with method: 'def fail() int' in superclass: x154cleanupfuncoverrides.Parent
x154cleanupfuncoverrides.conc line 14:1 Method: 'def fail2() java.lang.String' of class: 'x154cleanupfuncoverrides.Parent' should be explicitly overriden in subclass: x154cleanupfuncoverrides.Child. Use the override keyword
x154cleanupfuncoverrides.conc line 15:1 The return type of method 'def fail3() int' in class x154cleanupfuncoverrides.Child cannot be matched with method: 'def fail3() java.lang.Object' in superclass: x154cleanupfuncoverrides.Parent
 
~~~~~
//##155. prim type and Obj type non compatible in ret type matching
x155primtypeandObjtypenoncompatibleinrettypematching.conc line 9:1 The return type of method 'def noes() java.lang.Integer' in class x155primtypeandObjtypenoncompatibleinrettypematching.ClsOther cannot be matched with method: 'def noes() int' in superclass: x155primtypeandObjtypenoncompatibleinrettypematching.Sup
 
~~~~~
//##156. nested child privates are visible  fields

~~~~~
//##156.b nested child privates are visible  methods


~~~~~
//##157.a nested class extension - declaration
x157anestedclassextensiondeclaration.conc line 10:1 Nested class: ChiIn1 cannot be the subclass of private nested class: Inner
x157anestedclassextensiondeclaration.conc line 14:0 Class: Fail cannot be the subclass of nested class: Parent.InnerPubDef - it must be nested within a subclass of: Parent
x157anestedclassextensiondeclaration.conc line 17:1 Nested class: ChiIn3 cannot be the subclass of: Parent.InnerPubDef - parent nestor must be subclass of: Parent
 
~~~~~
//##157.b nested class extension - reference
x157bnestedclassextensionreference.conc line 29:16 Private nested class: Priv is not visible
x157bnestedclassextensionreference.conc line 35:35 Nested Class: x157bnestedclassextensionreference.Child2.Priv cannot be directly instantiated. Can only be instantiated via reference of class: x157bnestedclassextensionreference.Child2
x157bnestedclassextensionreference.conc line 36:36 Nested Class: x157bnestedclassextensionreference.Child2.Pro cannot be directly instantiated. Can only be instantiated via reference of class: x157bnestedclassextensionreference.Child2
x157bnestedclassextensionreference.conc line 37:37 Nested Class: x157bnestedclassextensionreference.Child2.Pub cannot be directly instantiated. Can only be instantiated via reference of class: x157bnestedclassextensionreference.Child2
 
~~~~~
//##157.c nested class extension - constru

~~~~~
//##157.d nested class extension  private nested constructor invokation
x157dnestedclassextensionprivatenestedconstructorinvokation.conc line 34:16 Private nested class: Priv is not visible
x157dnestedclassextensionprivatenestedconstructorinvokation.conc line 37:11 Private nested class: Priv is not visible
 
~~~~~
//##158. no void in plus minus permitted
x158novoidinplusminuspermitted.conc line 7:18 Invalid type void
x158novoidinplusminuspermitted.conc line 9:16 Invalid type void
 
~~~~~
//##159. nested class superclass must be marked open
x159nestedclasssuperclassmustbemarkedopen.conc line 16:1 MyClass2 cannot extend: MyClass as it is closed
 
~~~~~
//##160. invoke superconstructor of an abstract class

~~~~~
//##161. invalid array type
x161invalidarraytype.conc line 13:22 Invalid type void
 
~~~~~
//##162. CAN convert from int: to int:: if one is locked etc
 
~~~~~
//##163. call setter dependant on ref levels

~~~~~
//##164. ensure ref levels are correct on deref
x164ensurereflevelsarecorrectonderef.conc line 7:9 type java.lang.Integer: has fewer than 2 ref levels
x164ensurereflevelsarecorrectonderef.conc line 8:9 type java.lang.Integer: has fewer than 2 ref levels
x164ensurereflevelsarecorrectonderef.conc line 9:11 type java.lang.Integer has fewer than 1 ref levels
x164ensurereflevelsarecorrectonderef.conc line 10:12 type java.lang.Integer has fewer than 1 ref levels
 
~~~~~
//##407  165. mist err used to npe
x165misterrusedtonpe.conc line 3:13 x cannot be resolved to a variable
x165misterrusedtonpe.conc line 8:7 Return type expected
x165misterrusedtonpe.conc line 8:12 Array index operation can only be performed on object that is an array, Map, List or object having approperiate operator overloading 'get' method not: int
 
~~~~~
//##408  165. basic onchange
x165basiconchange.conc line 9:1 onchange statements can only operate upon either refernces or arrays, lists, maps, sets of references or ReferenceSets, not: int
 
~~~~~
//##409  166. basic onchange  must ret sth
x166basiconchangemustretsth.conc line 11:20 onchange must return something
 
~~~~~
//##410  167. await  basic syntax
x167awaitbasicsyntax.conc line 29:1 where condition is specified for await, this must evaluate to type boolean not: int
 
~~~~~
//##411  168. onchange vars
x168onchangevars.conc line 6:8 type specified for variable: java.lang.Integer:: does not match right hand side of assignment: java.lang.Integer:
x168onchangevars.conc line 7:8 type specified for variable: int does not match right hand side of assignment: java.lang.Integer:
x168onchangevars.conc line 8:8 type specified for variable: float does not match right hand side of assignment: java.lang.Integer:
 
~~~~~
//##412  169. onchange vars
x169onchangevars.conc line 7:27 Type mismatch: cannot convert from java.lang.String to java.lang.Integer:
 
~~~~~
//##413  170. await missing var
x170awaitmissingvar.conc line 20:13 done2 cannot be resolved to a variable
x170awaitmissingvar.conc line 20:29 done2 cannot be resolved to a variable
 
~~~~~
//##414  171. correct working on type errror
x171correctworkingontypeerrror.conc line 7:1 every statements can only operate upon either refernces or arrays, lists, maps, sets of references or ReferenceSets, not: int
x171correctworkingontypeerrror.conc line 12:1 await statements can only operate upon either refernces or arrays, lists, maps, sets of references or ReferenceSets, not: java.lang.String
 
~~~~~
//##415  172. onchange return type tagged and expected
 
~~~~~
//##416  173. auto iterate on keyset for map
 
~~~~~
//##174. async scope basics
x174asyncscopebasics.conc line 17:3 Variable a3 has already been defined in current scope

~~~~~
//##175. ensure only onchange every at top level
x175ensureonlyonchangeeveryattoplevel.conc line 14:2 async may contain only onchange or every instances
x175ensureonlyonchangeeveryattoplevel.conc line 19:3 Variable a3 has already been defined in current scope
x175ensureonlyonchangeeveryattoplevel.conc line 21:2 async may contain only onchange or every instances
 
~~~~~
//##176. no npe on await being wrong no ret
x176nonpeonawaitbeingwrongnoret.conc line 9:3 block must return something
 
~~~~~
//##177. at least one required
x177atleastonerequired.conc line 8:2 async must contain at least one onchange or every instance
 
~~~~~
//##178 vars cannot be redefined more than once inside onchange every block
x178varscannotberedefinedmorethanonceinsideonchangeeveryblock.conc line 13:3 Variable f has already been defined in current scope
x178varscannotberedefinedmorethanonceinsideonchangeeveryblock.conc line 17:2 Variable f has already been defined in current scope
 
~~~~~
//##179 correct err thrown on missing ret type within async
x179correcterrthrownonmissingrettypewithinasync.conc line 14:14 every must return something
x179correcterrthrownonmissingrettypewithinasync.conc line 18:17 onchange must return something
 
~~~~~
//##185 check valid return 1 for onchange
x185checkvalidreturn1foronchange.conc line 12:4 return statement within onchange cannot return a value
 
~~~~~
//##188 pre post cannot have return and break continue
x188prepostcannothavereturnandbreakcontinue.conc line 10:14 break cannot be used outside of a loop, onchange, every, await or async block or inside a parallel for loop
x188prepostcannothavereturnandbreakcontinue.conc line 11:9 continue cannot be used outside of a loop or inside a parallel for loop
x188prepostcannothavereturnandbreakcontinue.conc line 18:14 break cannot be used outside of a loop, onchange, every, await or async block or inside a parallel for loop
x188prepostcannothavereturnandbreakcontinue.conc line 19:9 continue cannot be used outside of a loop or inside a parallel for loop
 
~~~~~
//##189 ref of array strict type check
x189refofarraystricttypecheck.conc line 9:4 Type mismatch: cannot convert from int[] to double[]:

~~~~~
//##190 trans break continue
x190transbreakcontinue.conc line 7:14 return statement is not valid here
x190transbreakcontinue.conc line 8:2 return statement is not valid here
x190transbreakcontinue.conc line 13:2 continue cannot be used outside of a loop or inside a parallel for loop
x190transbreakcontinue.conc line 18:2 break cannot be used outside of a loop, onchange, every, await or async block or inside a parallel for loop
 
~~~~~
//##191 no void left of ref
x191novoidleftofref.conc line 8:13 type on left hand side of : cannot be void
 
~~~~~
//##192 ensure that we cannot cast to Ref interface type, but to ref it ok
x192ensurethatwecannotcasttoRefinterfacetypebuttorefitok.conc line 9:0 Type mismatch: cannot extract value from ref type: java.lang.Object:com.concurnas.bootstrap.runtime.ref.Ref as it does not implement the DirectlyGettable interface
 
~~~~~
//##193 ensure that we cannot cast to Ref interface type, but to ref it ok - multilevel
x193ensurethatwecannotcasttoRefinterfacetypebuttorefitokmultilevel.conc line 9:0 Type mismatch: cannot extract value from ref type: java.lang.Object:com.concurnas.bootstrap.runtime.ref.Ref:com.concurnas.bootstrap.runtime.ref.Ref as it does not implement the DirectlyGettable interface
x193ensurethatwecannotcasttoRefinterfacetypebuttorefitokmultilevel.conc line 10:0 Type mismatch: cannot extract value from ref type: java.lang.Object:com.concurnas.bootstrap.runtime.ref.Ref: as it does not implement the DirectlyGettable interface
x193ensurethatwecannotcasttoRefinterfacetypebuttorefitokmultilevel.conc line 11:0 Type mismatch: cannot extract value from ref type: java.lang.Object::com.concurnas.bootstrap.runtime.ref.Ref as it does not implement the DirectlyGettable interface
 
~~~~~
//##194 multilevel as before  these are all ok
x194multilevelasbeforetheseareallok.conc line 9:0 Type mismatch: cannot extract value from ref type: java.lang.Object:com.concurnas.bootstrap.runtime.ref.Ref:com.concurnas.bootstrap.runtime.ref.Ref as it does not implement the DirectlyGettable interface
x194multilevelasbeforetheseareallok.conc line 10:0 Type mismatch: cannot extract value from ref type: java.lang.Object:com.concurnas.bootstrap.runtime.ref.Ref: as it does not implement the DirectlyGettable interface
x194multilevelasbeforetheseareallok.conc line 11:0 Type mismatch: cannot extract value from ref type: java.lang.Object::com.concurnas.bootstrap.runtime.ref.Ref as it does not implement the DirectlyGettable interface
 
~~~~~
//##194 multilevel as before  some ok some not
x194multilevelasbeforesomeoksomenot.conc line 9:0 Type mismatch: cannot extract value from ref type: java.lang.Object:com.concurnas.bootstrap.runtime.ref.Ref:com.concurnas.bootstrap.runtime.ref.Ref as it does not implement the DirectlyGettable interface
x194multilevelasbeforesomeoksomenot.conc line 10:0 Type mismatch: cannot extract value from ref type: java.lang.Object:com.concurnas.bootstrap.runtime.ref.Ref: as it does not implement the DirectlyGettable interface
x194multilevelasbeforesomeoksomenot.conc line 11:0 Type mismatch: cannot extract value from ref type: java.lang.Object::com.concurnas.bootstrap.runtime.ref.Ref as it does not implement the DirectlyGettable interface
 
~~~~~
//##195 no break continue in pre post blocks
x195nobreakcontinueinprepostblocks.conc line 17:13 continue cannot be used outside of a loop or inside a parallel for loop
x195nobreakcontinueinprepostblocks.conc line 18:16 break cannot be used outside of a loop, onchange, every, await or async block or inside a parallel for loop
x195nobreakcontinueinprepostblocks.conc line 26:13 continue cannot be used outside of a loop or inside a parallel for loop
x195nobreakcontinueinprepostblocks.conc line 27:16 break cannot be used outside of a loop, onchange, every, await or async block or inside a parallel for loop
 
~~~~~
//##196 no return in sync blocks
 
~~~~~
//##197 more than one default no allow map
x197morethanonedefaultnoallowmap.conc line 4:7 Map may only have one default definition
 
~~~~~
//##198 return a funcref incorrectly
x198returnafuncrefincorrectly.conc line 6:22 Expected to invoke a function reference
 
~~~~~
//##199 funcref one void but not all
x199funcrefonevoidbutnotall.conc line 5:7 All method types must either return something or void
x199funcrefonevoidbutnotall.conc line 5:72 b cannot be resolved to a variable
x199funcrefonevoidbutnotall.conc line 7:22 Expected to invoke a function reference
 
~~~~~
//##200 we can infer the type here

~~~~~
//##200 we can infer the type here

~~~~~
//##202 no new and gen inf

~~~~~
//##203 gen inf - missing some generics
x203geninfmissingsomegenerics.conc line 21:11 Generic parameter count of: 2 does not equal: 2 - Generic qualification inferred from called consturctor for: X but not Y
x203geninfmissingsomegenerics.conc line 23:20 Cannot find get function on java.lang.Object? with arguments: [int]

~~~~~
//##204 erasure check performed on generic types of classes for consturctor and methods
x204erasurecheckperformedongenerictypesofclassesforconsturctorandmethods.conc line 12:1 Constructor with matching argument definition exists already in current Scope - generic types are erased at runtime
x204erasurecheckperformedongenerictypesofclassesforconsturctorandmethods.conc line 20:1 Method do with matching argument definition exists already in current Scope - generic types are erased at runtime
 
~~~~~
//##205 some complex constructor invokations are too complex
x205somecomplexconstructorinvokationsaretoocomplex.conc line 18:11 Unable to find method with matching name: constructor and arguments (x205somecomplexconstructorinvokationsaretoocomplex.MyAR, x205somecomplexconstructorinvokationsaretoocomplex.MyARInt)
 
~~~~~
//##206 what if not all the generics got qualified...
x206whatifnotallthegenericsgotqualified.conc line 14:9 Unable to find method with matching name: myGen and arguments (java.lang.String, x206whatifnotallthegenericsgotqualified.MyAR)
x206whatifnotallthegenericsgotqualified.conc line 15:10 Unable to find method with matching name: myGen and arguments (java.lang.String, x206whatifnotallthegenericsgotqualified.MyAR)
 
~~~~~
//##207 what if not all the generics got qualified...
x207whatifnotallthegenericsgotqualifiedwhenexplicit.conc line 14:10 Unable to find method with matching name: myGen
 
~~~~~
//##207.b avoid accidental binding
x207bavoidaccidentalbinding.conc line 14:10 Unable to find method with matching name: myGen and arguments (java.lang.String, x207bavoidaccidentalbinding.MyAR)
x207bavoidaccidentalbinding.conc line 20:10 Unable to find method with matching name: myGen and arguments (java.lang.String, x207bavoidaccidentalbinding.MyAR)

~~~~~
//##208 method locals do get ereased at runtime
 
~~~~~
//##209 no new ints
x209nonewints.conc line 4:4 Cannot instantiate type int

~~~~~
//##210. class ref oh no 1
x210classrefohno1.conc line 7:8 Unable to resolve type corresponding to name: ll
 
~~~~~
//##211. class ref oh no 2
x211classrefohno2.conc line 5:5 extraneous input '=' expecting {<EOF>, ';', 'transient', 'shared', 'lazy', 'override', '-', '+', '~', 'assert', 'del', 'break', 'continue', 'throw', 'return', 'import', 'using', '*', 'from', 'typedef', '<', 'await', 'def', 'gpudef', 'gpukernel', '**', '++', '--', 'not', 'comp', 'global', 'local', 'constant', 'out', 'this', 'provider', 'abstract', 'open', 'closed', 'class', 'trait', 'actor', 'of', 'with', 'new', 'annotation', 'enum', 'for', 'parfor', 'parforsync', 'match', 'if', 'async', 'while', 'loop', 'try', 'trans', 'init', 'sync', 'onchange', 'every', '@', 'boolean', 'bool', 'size_t', 'int', 'long', 'float', 'double', 'byte', 'short', 'char', 'lambda', 'sizeof', 'super', 'changed', 'null', 'true', 'false', 'val', 'var', 'private', 'public', 'inject', 'protected', 'package', LONGINT, SHORTINT, INT, FLOAT, DOUBLE, NAME, STRING_ITMcit, STRING_ITMquot, REGEX_STRING_ITM, NEWLINE, '(', '[', 'a[', '{'}
x211classrefohno2.conc line 7:1 no viable alternative at input '{\ngot=class\n\n""'
x211classrefohno2.conc line 8:0 extraneous input '}' expecting {<EOF>, ';', 'transient', 'shared', 'lazy', 'override', '-', '+', '~', 'assert', 'del', 'break', 'continue', 'throw', 'return', 'import', 'using', '*', 'from', 'typedef', '<', 'await', 'def', 'gpudef', 'gpukernel', '**', '++', '--', 'not', 'comp', 'global', 'local', 'constant', 'out', 'this', 'provider', 'abstract', 'open', 'closed', 'class', 'trait', 'actor', 'of', 'with', 'new', 'annotation', 'enum', 'for', 'parfor', 'parforsync', 'match', 'if', 'async', 'while', 'loop', 'try', 'trans', 'init', 'sync', 'onchange', 'every', '@', 'boolean', 'bool', 'size_t', 'int', 'long', 'float', 'double', 'byte', 'short', 'char', 'lambda', 'sizeof', 'super', 'changed', 'null', 'true', 'false', 'val', 'var', 'private', 'public', 'inject', 'protected', 'package', LONGINT, SHORTINT, INT, FLOAT, DOUBLE, NAME, STRING_ITMcit, STRING_ITMquot, REGEX_STRING_ITM, NEWLINE, '(', '[', 'a[', '{'}
 
~~~~~
//##212. couple of errors now gracefuly handlered
x212coupleoferrorsnowgracefulyhandlered.conc line 12:5 Unable to resolve type corresponding to name: Sting
x212coupleoferrorsnowgracefulyhandlered.conc line 18:9 incompatible type: int vs java.lang.String
x212coupleoferrorsnowgracefulyhandlered.conc line 38:1 await statements can only operate upon either refernces or arrays, lists, maps, sets of references or ReferenceSets, not: int
x212coupleoferrorsnowgracefulyhandlered.conc line 41:24 Incompatible operand types int and java.lang.String
 
~~~~~
//##213. super constructor must be public or protected
x213superconstructormustbepublicorprotected.conc line 8:0 Implicit super constructor for superclass x213superconstructormustbepublicorprotected.Sup of x213superconstructormustbepublicorprotected.Child is private. Must explicitly invoke another non private (e.g. public or protected) constructor in supertype
 
~~~~~
//##214. no parfor transation if block not meant to return but does
x214noparfortransationifblocknotmeanttoreturnbutdoes.conc line 14:2 Expression cannot appear on its own line

~~~~~
//##215. no parfor transation if block not meant to return but does 2

~~~~~
//##216. generic param missing on this constructor invokation
x216genericparammissingonthisconstructorinvokation.conc line 9:20 Generic parameter count of: 1 does not equal: 0
x216genericparammissingonthisconstructorinvokation.conc line 14:6 Generic parameter count of: 1 does not equal: 0
 
~~~~~
//##217. function takes constructor func ref but missing generic params
x217functiontakesconstructorfuncrefbutmissinggenericparams.conc line 9:19 Generic parameter count of: 0 does not equal: 1 - generic type parameters must be defined
x217functiontakesconstructorfuncrefbutmissinggenericparams.conc line 16:5 Unable to find method with matching name: taker and arguments (def () out x217functiontakesconstructorfuncrefbutmissinggenericparams.MyClass<java.lang.String>)
 
~~~~~
//##218. ensure that when locally qualified but wrong args passed then this fails
x218ensurethatwhenlocallyqualifiedbutwrongargspassedthenthisfails.conc line 13:10 Unable to find method with matching name: proc
  
~~~~~
//##219. type can only be used as funcref in this way
x219typecanonlybeusedasfuncrefinthisway.conc line 12:6 type cannot be used in this way
 
~~~~~
//##220. ensure that functype is being invoked without array levels on funcrefinvoke
x220ensurethatfunctypeisbeinginvokedwithoutarraylevelsonfuncrefinvoke.conc line 18:5 Expected to invoke a function reference
 
~~~~~
//##221. named type useage check  cant go on its own
x221namedtypeuseagecheckcantgoonitsown.conc line 14:7 type cannot be used in this way
x221namedtypeuseagecheckcantgoonitsown.conc line 20:8 type cannot be used in this way
x221namedtypeuseagecheckcantgoonitsown.conc line 21:7 type cannot be used in this way
 
~~~~~
//##222. incorrect generics passed in unbound func ref
x222incorrectgenericspassedinunboundfuncref.conc line 13:45 Unable to find method with matching name: getLa and arguments (java.lang.String, int)
 
~~~~~
//##223. used to fail
x223usedtofail.conc line 15:7 Unable to find method with matching name: x223usedtofail.Parent
x223usedtofail.conc line 16:10 Unable to find method with matching name: MyClass
x223usedtofail.conc line 20:5 Unable to resolve reference to variable name: x
 
~~~~~
//##224. constructor ref generic type inference wrong type passed
x224constructorrefgenerictypeinferencewrongtypepassed.conc line 16:39 Generic Type argument type mismatch: out x224constructorrefgenerictypeinferencewrongtypepassed.Parent.MyClass<java.lang.Float> vs x224constructorrefgenerictypeinferencewrongtypepassed.Parent<java.lang.Integer>.MyClass<java.lang.String>
 
~~~~~
//##225. should fail
x225shouldfail.conc line 15:6 type cannot be used in this way

~~~~~
//##226. used to blow up...
x226usedtoblowup.conc line 6:22 Unable to resolve type corresponding to name: F
 
~~~~~
//##227. funcref visibility
x227funcrefvisibility.conc line 10:9 The method getter is not visible
 
~~~~~
//##228. ensure generic bindings
x228ensuregenericbindings.conc line 9:1 Type must match generic type: H
x228ensuregenericbindings.conc line 15:9 Unable to find method with matching name: myGen and arguments (java.lang.String, x228ensuregenericbindings.MyAR)
x228ensuregenericbindings.conc line 16:10 Unable to find method with matching name: myGen and arguments (java.lang.String, x228ensuregenericbindings.MyAR)
 
~~~~~
//##229. missing local bindings
x229missinglocalbindings.conc line 10:21 Missing generic binding for: P on method: something2
 
~~~~~
//##230. missing generic type qualification in funcType definition T
x230missinggenerictypequalificationinfuncTypedefinitionT.conc line 4:13 Unable to resolve type corresponding to name: T
x230missinggenerictypequalificationinfuncTypedefinitionT.conc line 7:6 Unable to find method with matching name: thefunc and arguments (int)
x230missinggenerictypequalificationinfuncTypedefinitionT.conc line 10:6 Unable to find method with matching number of arguments with name: ff
 
~~~~~
//##231. complains on lambda funcref with local gens
x231complainsonlambdafuncrefwithlocalgens.conc line 7:6 Unable to find reference function Type for: thefunc
x231complainsonlambdafuncrefwithlocalgens.conc line 9:6 Unable to find method with matching number of arguments with name: ff
 
~~~~~
//##232. something we may not extend
x232somethingwemaynotextend.conc line 4:0 Class com.concurnas.lang.Actor cannot be extended
x232somethingwemaynotextend.conc line 15:9 Unable to find method with matching name: plus
 
~~~~~
//##232.b TypedActor we may not extend
x232bTypedActorwemaynotextend.conc line 4:0 Class com.concurnas.lang.TypedActor cannot be extended
 
~~~~~
//##233. non existant actor method
x233nonexistantactormethod.conc line 12:9 Unable to find method with matching name: hi
 
~~~~~
//##234. some actor errors
x234someactorerrors.conc line 16:5 The method notAllowed is not visible
x234someactorerrors.conc line 33:5 Unable to find method with matching name: doStuff and arguments (DefaultActor$0)
x234someactorerrors.conc line 34:5 Unable to find method with matching name: doStuffToMe and arguments (DefaultActor$1)
 
~~~~~
//##235. actor fields are private
x235actorfieldsareprivate.conc line 10:11 The variable a is not visible
 
~~~~~
//##236. actors cant call private or protected methods on other instances of themselves
x236actorscantcallprivateorprotectedmethodsonotherinstancesofthemselves.conc line 17:5 Actors cannot call private or protected methods on instances other than those preceeded with a this or super reference, method: notAllowed
x236actorscantcallprivateorprotectedmethodsonotherinstancesofthemselves.conc line 21:5 Actors cannot call private or protected methods on instances other than those preceeded with a this or super reference, method: notAllowed
x236actorscantcallprivateorprotectedmethodsonotherinstancesofthemselves.conc line 22:5 Actors cannot call private or protected methods on instances other than those preceeded with a this or super reference, method: notAllowed2
 
~~~~~
//##236. private and public actor fields are not visible
x236privateandpublicactorfieldsarenotvisible.conc line 10:10 The variable x is not visible
x236privateandpublicactorfieldsarenotvisible.conc line 11:10 The variable y is not visible
x236privateandpublicactorfieldsarenotvisible.conc line 13:12 The variable y is not visible
 
~~~~~
//##237. bugfix on unknown type
x237bugfixonunknowntype.conc line 4:17 Unable to resolve type corresponding to name: X
 
~~~~~
//##238. check local method binding count for both func invoke and references
x238checklocalmethodbindingcountforbothfuncinvokeandreferences.conc line 10:11 Unable to find method with matching name: getX
x238checklocalmethodbindingcountforbothfuncinvokeandreferences.conc line 17:11 Unable to find reference function Type for: getX
x238checklocalmethodbindingcountforbothfuncinvokeandreferences.conc line 24:7 Invalid type void
 
~~~~~
//##239. lhs generic rhs must match or be null
x239lhsgenericrhsmustmatchorbenull.conc line 6:2 Type must match generic type: H
 
~~~~~
//##240. more generic errors as par above
x240moregenericerrorsasparabove.conc line 7:6 Type must match generic type: XX
x240moregenericerrorsasparabove.conc line 8:4 Unable to find method with matching name: getX
x240moregenericerrorsasparabove.conc line 9:4 Unable to find reference function Type for: getX
 
~~~~~
//##241. more local binding errors
x241morelocalbindingerrors.conc line 13:11 Unable to find method with matching name: getMe
x241morelocalbindingerrors.conc line 20:11 Unable to find reference function Type for: getMe
x241morelocalbindingerrors.conc line 27:7 Invalid type void
 
~~~~~
//##242. use generics in - as
x242usegenericsinas.conc WARN line 6:29 Unable to determine type for generic: Y for as operation. Wil use upper bounds of: java.lang.Object instead
 
~~~~~
//##243. use generics in - is
x243usegenericsinis.conc WARN line 6:29 Unable to determine type for generic: Y for is operation. Wil use upper bounds of: java.lang.Object instead
 
~~~~~
//##244. creation of actors with missing generics
x244creationofactorswithmissinggenerics.conc WARN line 8:28 Unable to determine type for generic: Y for use as argument in constructor of actor operation. Wil use upper bounds of: java.lang.Object instead
x244creationofactorswithmissinggenerics.conc WARN line 12:36 Unable to determine type for generic: Y for use as argument in constructor of actor operation. Wil use upper bounds of: java.lang.Object instead
 
~~~~~
//##245. aside
x245aside.conc line 4:33 Unable to resolve type corresponding to name: ZZ
x245aside.conc line 5:9 Unable to resolve reference to this constructor for: 'x245aside.MyClass'(null, null, null)
x245aside.conc line 8:50 Generic parameter count of: 3 does not equal: 2
 
~~~~~
//##246. warning on creation of generic ref tpye
x246warningoncreationofgenericreftpye.conc WARN line 4:32 Unable to determine type for generic: Y for : operation. Wil use upper bounds of: java.lang.Object instead
 
~~~~~
//##247. more than on qualifying thing for gens assocaited with actor
x247morethanonqualifyingthingforgensassocaitedwithactor.conc WARN line 6:30 Unable to determine type for generic: Y for use as argument in constructor of actor operation. Wil use upper bounds of: java.lang.Object instead. Cannot infer type from arguments due to ambiguity arising from more than one argument to infer type from.
 
~~~~~
//##248. more warning on crappy defitions
x248morewarningoncrappydefitions.conc WARN line 8:28 Unable to determine type for generic: Y for : operation. Wil use upper bounds of: java.lang.Object instead
 
~~~~~
//##249. bug with superclasses having args themselves being nested classes with generic params
x249bugwithsuperclasseshavingargsthemselvesbeingnestedclasseswithgenericparams.conc line 16:26 Unable to resolve type corresponding to name: AF
 
~~~~~
//##250. bug used to npe on constructor ref not existing
x250bugusedtonpeonconstructorrefnotexisting.conc line 11:13 Unable to resolve type corresponding to name: getA
x250bugusedtonpeonconstructorrefnotexisting.conc line 12:5 Unable to find method with matching name: bind
x250bugusedtonpeonconstructorrefnotexisting.conc line 14:6 For variable reference on: 'ff2' it must be a method or method reference not: void
 
~~~~~
//##251. a does not exist
x251adoesnotexist.conc line 16:7 Cannot access actor field: a
x251adoesnotexist.conc line 23:19 Cannot access actor field: a
 
~~~~~
//##252. actor field access no confuse with actee
x252actorfieldaccessnoconfusewithactee.conc line 15:15 Unable to resolve reference to variable name: xa
 
~~~~~
//##253. actor constructor args
x253actorconstructorargs.conc line 8:0 Unable to find method with matching number of arguments with name: MyClass
 
~~~~~
//##254. another actor ttest
x254anotheractorttest.conc line 13:12 Unable to resolve reference to constructor for: 'x254anotheractorttest.MyActor'(java.lang.Class<? java.lang.Object>[])
 
~~~~~
//##255. untyped actor gen
x255untypedactorgen.conc line 4:0 generic qualifier 'Y' identifier has already been declared in generic parameter list
 
~~~~~
//##256. abstract typed actee gen already defined
x256abstracttypedacteegenalreadydefined.conc line 4:0 generic qualifier for actee 'XXX' has already been declared in generic parameter list for abstract typed actor
 
~~~~~
//##257. error could be better here
x257errorcouldbebetterhere.conc line 4:32 Unable to resolve type corresponding to name: YYY
x257errorcouldbebetterhere.conc line 5:13 Unable to resolve type corresponding to name: YYY
x257errorcouldbebetterhere.conc line 6:2 Unable to find method with matching name: constructor and arguments (java.lang.Class<? java.lang.Object>[], def () out YYY)
 
~~~~~
//##258. this was a bug before
x258thiswasabugbefore.conc line 12:1 Unable to find method with matching name: something
x258thiswasabugbefore.conc line 20:19 Unable to find method with matching name: something
 
~~~~~
//##259. abstract typed actors cannot have actee arguments
x259abstracttypedactorscannothaveacteearguments.conc line 8:0 abstract typed actors cannot have actee arguments
x259abstracttypedactorscannothaveacteearguments.conc line 16:0 Unable to resolve reference to super constructor for: 'x259abstracttypedactorscannothaveacteearguments.AbstractActor' as its super class is abstract
 
~~~~~
//##260 no actors of actors
x260noactorsofactors.conc line 10:18 actors cannot have actors as actees. x260noactorsofactors.MyActor is already an actor
x260noactorsofactors.conc line 14:7 Unable to resolve reference to variable name: xa
 
~~~~~
//##261. missing actee show correct line
x261missingacteeshowcorrectline.conc line 13:7 Unable to resolve type corresponding to name: MyActor2
 
~~~~~
//##262. actors can only extend actors
x262actorscanonlyextendactors.conc line 10:0 untyped actors can only extend untyped actors, not class: java.lang.Object
x262actorscanonlyextendactors.conc line 11:0 typed actors can only extend typed actors, not class: java.lang.Object
 
~~~~~
//##263. classes cannot extend actors
x263classescannotextendactors.conc line 10:0 classes can only extend classes, not typed actor: x263classescannotextendactors.MyActor
x263classescannotextendactors.conc line 13:7 Unable to resolve reference to constructor for: 'x263classescannotextendactors.MyActor2'(java.lang.Class<? java.lang.Object>[], int)
 
~~~~~
//##264. of syntax can be used on abstract typed actors only
x264ofsyntaxcanbeusedonabstracttypedactorsonly.conc line 17:10 expected abstract typed actor not class: java.lang.String
x264ofsyntaxcanbeusedonabstracttypedactorsonly.conc line 18:10 expected abstract typed actor not class: java.util.Set
 
~~~~~
//##265. of syntax can be used on abstract typed actors only missing stuff
x265ofsyntaxcanbeusedonabstracttypedactorsonlymissingstuff.conc line 17:11 abstract typed must use of qualification
 
~~~~~
//##266. four errors with actors
x266fourerrorswithactors.conc line 4:0 closed actor SupActorFail of XXX cannot be closed as it is abstract
x266fourerrorswithactors.conc line 5:0 LeNonActor must be declared as an actor to act on: x266fourerrorswithactors.MyClass
x266fourerrorswithactors.conc line 6:0 LeNonActor2 must be declared as an actor to act on: XXX
x266fourerrorswithactors.conc line 14:19 The left-hand side of an assignment must be a variable
 
~~~~~
//##267. nested classes cannot be actors
x267nestedclassescannotbeactors.conc line 8:1 actor classes cannot be nested
 
~~~~~
//##268. used to blow up now it doesnt, great
x268usedtoblowupnowitdoesntgreat.conc line 9:1 actor classes cannot be nested
 
~~~~~
//##269. actors and classes cannot extend stuff willy nilly
x269actorsandclassescannotextendstuffwillynilly.conc line 12:0 untyped actors can only extend untyped actors, not class: java.lang.Object
x269actorsandclassescannotextendstuffwillynilly.conc line 14:0 typed actors can only extend typed actors, not class: java.lang.Object
x269actorsandclassescannotextendstuffwillynilly.conc line 16:0 typed actor cannot extend untyped actor: x269actorsandclassescannotextendstuffwillynilly.AnUntyped
x269actorsandclassescannotextendstuffwillynilly.conc line 20:0 classes can only extend classes, not untyped actor: x269actorsandclassescannotextendstuffwillynilly.AnUntyped
x269actorsandclassescannotextendstuffwillynilly.conc line 22:0 classes can only extend classes, not typed actor: x269actorsandclassescannotextendstuffwillynilly.ATyped
 
~~~~~
//##270. used to blow up
x270usedtoblowup.conc line 6:22 Unable to find method with matching name: identityHashcode
x270usedtoblowup.conc line 11:4 Invalid type void
 
~~~~~
//##271. toBoolean void check
x271toBooleanvoidcheck.conc line 7:9 unexpected type: void for if
x271toBooleanvoidcheck.conc line 9:8 unexpected type: void for elif
x271toBooleanvoidcheck.conc line 11:13 unexpected type: void for if
x271toBooleanvoidcheck.conc line 12:5 unexpected type: void for and
x271toBooleanvoidcheck.conc line 13:5 unexpected type: void for or
x271toBooleanvoidcheck.conc line 14:8 unexpected type: void for assert
x271toBooleanvoidcheck.conc line 15:7 unexpected type: void for while
x271toBooleanvoidcheck.conc line 16:10 unexpected type: void for for
 
~~~~~
//##272. invalid regex picked up at runtime
x272invalidregexpickedupatruntime.conc line 5:6 Invalid regex: Dangling meta character '*'

~~~~~
//##273. invalid format strings
x273invalidformatstrings.conc line 5:14 v cannot be resolved to a variable
x273invalidformatstrings.conc line 9:30 asddsa cannot be resolved to a variable
 
~~~~~
//##274. in and not in errors
x274inandnotinerrors.conc line 14:8 inside in expression - Unable to find method with matching name: contains and arguments (int)
x274inandnotinerrors.conc line 15:14 unexpected type void inside in expression
 
~~~~~
//##275. restrictions on parallel for use
x275restrictionsonparallelforuse.conc line 7:21 parrallel for loop cannot contain else block
x275restrictionsonparallelforuse.conc line 8:11 continue cannot be used outside of a loop or inside a parallel for loop
x275restrictionsonparallelforuse.conc line 9:11 break cannot be used outside of a loop, onchange, every, await or async block or inside a parallel for loop
x275restrictionsonparallelforuse.conc line 17:37 parrallel for loop cannot contain else block
x275restrictionsonparallelforuse.conc line 19:11 continue cannot be used outside of a loop or inside a parallel for loop
x275restrictionsonparallelforuse.conc line 20:11 break cannot be used outside of a loop, onchange, every, await or async block or inside a parallel for loop
 
~~~~~
//##276. for else must return something
x276forelsemustreturnsomething.conc line 9:18 for loop else block must return something
x276forelsemustreturnsomething.conc line 18:34 for loop else block must return something
x276forelsemustreturnsomething.conc line 29:16 while loop else block must return something
 
~~~~~
//##277. for, while etc incrementors
x277forwhileetcincrementors.conc line 7:1 Expected numerical type in block incrementer
x277forwhileetcincrementors.conc line 8:1 Expected numerical type in block incrementer
x277forwhileetcincrementors.conc line 9:1 Expected numerical type in block incrementer
x277forwhileetcincrementors.conc line 10:1 Expected numerical type in block incrementer
 
~~~~~
//##278. werid bug
x278weridbug.conc line 8:1 Expression cannot appear on its own line
x278weridbug.conc line 8:10 non suitable element detected to right hand side of dot: VarInt
 
~~~~~
//##279. js style maps bugs
x279jsstylemapsbugs.conc line 9:5 Unable to resolve reference to variable name: one
x279jsstylemapsbugs.conc line 12:5 Cannot find put function on java.util.HashMap<java.lang.String, java.lang.Integer> with arguments: [java.lang.String, java.lang.String]
x279jsstylemapsbugs.conc line 13:1 Existing variable is not a ref. It is of type: java.lang.Integer
 
~~~~~
//##280. boolean infix operations
x280booleaninfixoperations.conc line 7:1 Expected boolean. No overloaded 'orAssign' operator found for type java.lang.String with signature: '(boolean) java.lang.String'
x280booleaninfixoperations.conc line 8:1 Expected boolean. No overloaded 'andAssign' operator found for type java.lang.String with signature: '(boolean) java.lang.String'
 
~~~~~
//##281. operator overloader error messages
x281operatoroverloadererrormessages.conc line 31:6 numerical operation cannot be performed on type x281operatoroverloadererrormessages.Complex. No overloaded 'plus' operator found for type x281operatoroverloadererrormessages.Complex with signature: '(x281operatoroverloadererrormessages.Complex)'
x281operatoroverloadererrormessages.conc line 32:6 numerical operation cannot be performed on type x281operatoroverloadererrormessages.Complex. No overloaded 'minus' operator found for type x281operatoroverloadererrormessages.Complex with signature: '(x281operatoroverloadererrormessages.Complex)'
x281operatoroverloadererrormessages.conc line 34:8 numerical operation cannot be performed on type x281operatoroverloadererrormessages.Complex. No overloaded 'mul' operator found for type x281operatoroverloadererrormessages.Complex with signature: '(x281operatoroverloadererrormessages.Complex)'
x281operatoroverloadererrormessages.conc line 35:8 numerical operation cannot be performed on type x281operatoroverloadererrormessages.Complex. No overloaded 'div' operator found for type x281operatoroverloadererrormessages.Complex with signature: '(x281operatoroverloadererrormessages.Complex)'
x281operatoroverloadererrormessages.conc line 36:8 numerical operation cannot be performed on type x281operatoroverloadererrormessages.Complex. No overloaded 'mod' operator found for type x281operatoroverloadererrormessages.Complex with signature: '(x281operatoroverloadererrormessages.Complex)'
x281operatoroverloadererrormessages.conc line 44:11 numerical operation cannot be performed on type x281operatoroverloadererrormessages.Complex. No overloaded 'compareTo' operator found for type x281operatoroverloadererrormessages.Complex with signature: '(x281operatoroverloadererrormessages.Complex) int'
x281operatoroverloadererrormessages.conc line 45:15 numerical operation cannot be performed on type x281operatoroverloadererrormessages.Complex. No overloaded 'compareTo' operator found for type x281operatoroverloadererrormessages.Complex with signature: '(x281operatoroverloadererrormessages.Complex) int'
x281operatoroverloadererrormessages.conc line 46:11 numerical operation cannot be performed on type x281operatoroverloadererrormessages.Complex. No overloaded 'compareTo' operator found for type x281operatoroverloadererrormessages.Complex with signature: '(x281operatoroverloadererrormessages.Complex) int'
x281operatoroverloadererrormessages.conc line 47:15 numerical operation cannot be performed on type x281operatoroverloadererrormessages.Complex. No overloaded 'compareTo' operator found for type x281operatoroverloadererrormessages.Complex with signature: '(x281operatoroverloadererrormessages.Complex) int'
x281operatoroverloadererrormessages.conc line 49:12 numerical operation cannot be performed on type x281operatoroverloadererrormessages.Complex. No overloaded 'compareTo' operator found for type x281operatoroverloadererrormessages.Complex with signature: '(x281operatoroverloadererrormessages.Complex) int'
x281operatoroverloadererrormessages.conc line 50:16 numerical operation cannot be performed on type x281operatoroverloadererrormessages.Complex. No overloaded 'compareTo' operator found for type x281operatoroverloadererrormessages.Complex with signature: '(x281operatoroverloadererrormessages.Complex) int'
x281operatoroverloadererrormessages.conc line 51:12 numerical operation cannot be performed on type x281operatoroverloadererrormessages.Complex. No overloaded 'compareTo' operator found for type x281operatoroverloadererrormessages.Complex with signature: '(x281operatoroverloadererrormessages.Complex) int'
x281operatoroverloadererrormessages.conc line 52:16 numerical operation cannot be performed on type x281operatoroverloadererrormessages.Complex. No overloaded 'compareTo' operator found for type x281operatoroverloadererrormessages.Complex with signature: '(x281operatoroverloadererrormessages.Complex) int'
x281operatoroverloadererrormessages.conc line 55:10 Cannot find get function on x281operatoroverloadererrormessages.Complex with arguments: [java.lang.String]
x281operatoroverloadererrormessages.conc line 56:11 Cannot find get function on x281operatoroverloadererrormessages.Complex with arguments: [int]
x281operatoroverloadererrormessages.conc line 58:14 Sublist cannot be operated on. No overloaded 'sub' operator found for type x281operatoroverloadererrormessages.Complex with signature: '(int, int)'
x281operatoroverloadererrormessages.conc line 59:14 Sublist cannot be operated on. No overloaded 'subfrom' operator found for type x281operatoroverloadererrormessages.Complex with signature: '(int)'
x281operatoroverloadererrormessages.conc line 60:15 Sublist cannot be operated on. No overloaded 'subto' operator found for type x281operatoroverloadererrormessages.Complex with signature: '(int)'
x281operatoroverloadererrormessages.conc line 63:10 Array index operation can only be performed on object that is an array, Map, List or object having approperiate operator overloading 'put' method not: myMap cannot be resolved to a variable
x281operatoroverloadererrormessages.conc line 64:4 Cannot find put function on x281operatoroverloadererrormessages.Complex with arguments: [java.lang.String, int]
x281operatoroverloadererrormessages.conc line 65:4 Cannot find put function on x281operatoroverloadererrormessages.Complex with arguments: [int, int]
x281operatoroverloadererrormessages.conc line 67:17 Unable to resolve reference to variable name: one
x281operatoroverloadererrormessages.conc line 68:1 Unable to resolve reference to variable name: one
x281operatoroverloadererrormessages.conc line 70:9 inside in expression - Unable to find method with matching name: contains
x281operatoroverloadererrormessages.conc line 71:9 inside not in expression - Unable to find method with matching name: contains
x281operatoroverloadererrormessages.conc line 73:1 Expected type numerical not: x281operatoroverloadererrormessages.Complex. No overloaded 'inc' operator found for type x281operatoroverloadererrormessages.Complex with signature: '() x281operatoroverloadererrormessages.Complex'
x281operatoroverloadererrormessages.conc line 74:1 Expected type numerical not: x281operatoroverloadererrormessages.Complex. No overloaded 'dec' operator found for type x281operatoroverloadererrormessages.Complex with signature: '() x281operatoroverloadererrormessages.Complex'
x281operatoroverloadererrormessages.conc line 75:1 Expected type numerical not: x281operatoroverloadererrormessages.Complex. No overloaded 'inc' operator found for type x281operatoroverloadererrormessages.Complex with signature: '() x281operatoroverloadererrormessages.Complex'
x281operatoroverloadererrormessages.conc line 76:1 Expected type numerical not: x281operatoroverloadererrormessages.Complex. No overloaded 'dec' operator found for type x281operatoroverloadererrormessages.Complex with signature: '() x281operatoroverloadererrormessages.Complex'
x281operatoroverloadererrormessages.conc line 78:4 Expected type numerical not: x281operatoroverloadererrormessages.Complex. No overloaded 'inc' operator found for type x281operatoroverloadererrormessages.Complex with signature: '() x281operatoroverloadererrormessages.Complex'
x281operatoroverloadererrormessages.conc line 79:4 Expected type numerical not: x281operatoroverloadererrormessages.Complex. No overloaded 'dec' operator found for type x281operatoroverloadererrormessages.Complex with signature: '() x281operatoroverloadererrormessages.Complex'
x281operatoroverloadererrormessages.conc line 80:4 Expected type numerical not: x281operatoroverloadererrormessages.Complex. No overloaded 'inc' operator found for type x281operatoroverloadererrormessages.Complex with signature: '() x281operatoroverloadererrormessages.Complex'
x281operatoroverloadererrormessages.conc line 81:4 Expected type numerical not: x281operatoroverloadererrormessages.Complex. No overloaded 'dec' operator found for type x281operatoroverloadererrormessages.Complex with signature: '() x281operatoroverloadererrormessages.Complex'
x281operatoroverloadererrormessages.conc line 83:8 Expected type numerical not: x281operatoroverloadererrormessages.Complex. No overloaded 'neg' operator found for type x281operatoroverloadererrormessages.Complex with signature: '() x281operatoroverloadererrormessages.Complex'
x281operatoroverloadererrormessages.conc line 84:9 Expected type numerical not: x281operatoroverloadererrormessages.Complex. No overloaded 'plus' operator found for type x281operatoroverloadererrormessages.Complex with signature: '() x281operatoroverloadererrormessages.Complex'
x281operatoroverloadererrormessages.conc line 88:9 For variable reference on: 'c1' it must be a method or method reference not: x281operatoroverloadererrormessages.Complex. No overloaded 'invoke' operator found for type x281operatoroverloadererrormessages.Complex with signature: '()'
x281operatoroverloadererrormessages.conc line 89:9 For variable reference on: 'c1' it must be a method or method reference not: x281operatoroverloadererrormessages.Complex. No overloaded 'invoke' operator found for type x281operatoroverloadererrormessages.Complex with signature: '(java.lang.String)'
x281operatoroverloadererrormessages.conc line 90:9 For variable reference on: 'c1' it must be a method or method reference not: x281operatoroverloadererrormessages.Complex. No overloaded 'invoke' operator found for type x281operatoroverloadererrormessages.Complex with signature: '(java.lang.String, int)'
x281operatoroverloadererrormessages.conc line 92:1 numerical operation cannot be performed on type x281operatoroverloadererrormessages.Complex. No overloaded 'plusAssign' operator found for type x281operatoroverloadererrormessages.Complex with signature: '(x281operatoroverloadererrormessages.Complex) x281operatoroverloadererrormessages.Complex'
 
~~~~~
//##282. used to be an error
x282usedtobeanerror.conc line 8:4 type int has fewer than 1 ref levels
 
~~~~~
//##283. operator overloaded get and put types must amtch
x283operatoroverloadedgetandputtypesmustamtch.conc line 22:4 Cannot find put function on x283operatoroverloadedgetandputtypesmustamtch.Complex with arguments: [java.lang.String, int]
 
~~~~~
//##284. operator overloading of inc and dec
x284operatoroverloadingofincanddec.conc line 15:1 Expected type numerical not: x284operatoroverloadingofincanddec.Complex. No overloaded 'inc' operator found for type x284operatoroverloadingofincanddec.Complex with signature: '() x284operatoroverloadingofincanddec.Complex'
x284operatoroverloadingofincanddec.conc line 16:1 Expected type numerical not: x284operatoroverloadingofincanddec.Complex. No overloaded 'dec' operator found for type x284operatoroverloadingofincanddec.Complex with signature: '() x284operatoroverloadingofincanddec.Complex'
 
~~~~~
//##285. enums can have protected stuff

~~~~~
//##286. enum cannot define own values and valueOf methods
x286enumcannotdefineownvaluesandvalueOfmethods.conc line 6:1 Method values with matching argument definition exists implicitly already in current Scope
x286enumcannotdefineownvaluesandvalueOfmethods.conc line 7:1 Method values with matching argument definition exists already in current Scope
x286enumcannotdefineownvaluesandvalueOfmethods.conc line 8:1 Method valueOf with matching argument definition exists implicitly already in current Scope
 
~~~~~
//##287. enum fields private only for elemnts
x287enumfieldsprivateonlyforelemnts.conc line 6:2 fields of enum elements may only be private
 
~~~~~
//##288. inner item enum methods
x288inneritemenummethods.conc line 6:2 fields of enum elements may only be private
x288inneritemenummethods.conc line 8:2 public enum item method 'something2' must be defined in containing enum
x288inneritemenummethods.conc line 9:2 protected method 'myprotected' cannot be defined in enum element (only public and private are permitted)
x288inneritemenummethods.conc line 10:2 Method: 'def motok() int' of class: 'x288inneritemenummethods.MyEnum' has been declared with accesability: public but implementation in subclass: x288inneritemenummethods.MyEnum$0 attempts to narrow scope to: private - this cannot be done
x288inneritemenummethods.conc line 12:2 protected method 'sdsdasd' cannot be defined in enum element (only public and private are permitted)
 
~~~~~
//##289. enum super cannot invoke
x289enumsupercannotinvoke.conc line 12:12 Super constructor cannot be directly invoked for enums
 
~~~~~
//##290. enum constructors can be private only
x290enumconstructorscanbeprivateonly.conc line 11:1 enum constructors can only be private
 
~~~~~
//##291. no access of private stuff from the non nested class ok
x291noaccessofprivatestufffromthenonnestedclassok.conc line 14:9 The variable a is not visible
x291noaccessofprivatestufffromthenonnestedclassok.conc line 15:5 The method afunc is not visible
x291noaccessofprivatestufffromthenonnestedclassok.conc line 18:44 The variable a is not visible
x291noaccessofprivatestufffromthenonnestedclassok.conc line 28:1 enum constructors can only be private
 
~~~~~
//##292. enum at top level only if unnested else must go via thingy
x292enumattoplevelonlyifunnestedelsemustgoviathingy.conc line 13:7 Unable to resolve reference to variable name: MyEnum.ONE
 
~~~~~
//##293. nested enum cannot access nestor class variables
x293nestedenumcannotaccessnestorclassvariables.conc line 12:8 The variable xxx is not visible
x293nestedenumcannotaccessnestorclassvariables.conc line 13:8 The variable xxx2 is not visible

~~~~~
//##294. nested enum cannot access nestor class functions
x294nestedenumcannotaccessnestorclassfunctions.conc line 12:8 The method xxx is not visible
x294nestedenumcannotaccessnestorclassfunctions.conc line 13:8 The method xxx2 is not visible
 
~~~~~
//##295. nested enum enum cannot access  stuff
x295nestedenumenumcannotaccessstuff.conc line 17:6 The variable h is not visible
x295nestedenumenumcannotaccessstuff.conc line 18:6 The method sdf is not visible
 
~~~~~
//##296. inner thing protected by default no create for you
x296innerthingprotectedbydefaultnocreateforyou.conc line 9:19 Private nested class: Inner is not visible
x296innerthingprotectedbydefaultnocreateforyou.conc line 11:14 Private nested class: Inner is not visible
x296innerthingprotectedbydefaultnocreateforyou.conc line 13:14 Private nested class: Inner is not visible
x296innerthingprotectedbydefaultnocreateforyou.conc line 14:10 Private nested class: Inner is not visible
 
~~~~~
//##297. inner thing no instiation like this
x297innerthingnoinstiationlikethis.conc line 9:9 Nested Class: x297innerthingnoinstiationlikethis.Outer.Inner cannot be directly instantiated. Can only be instantiated via reference of class: x297innerthingnoinstiationlikethis.Outer
x297innerthingnoinstiationlikethis.conc line 10:10 Nested Class: x297innerthingnoinstiationlikethis.Outer.Inner cannot be directly instantiated. Can only be instantiated via reference of class: x297innerthingnoinstiationlikethis.Outer
 
~~~~~
//##298. nested class in enum no
x298nestedclassinenumno.conc line 16:16 Nested Class: x298nestedclassinenumno.MyEnum.MyClass cannot be directly instantiated. Can only be instantiated via reference of class: x298nestedclassinenumno.MyEnum
 
~~~~~
//##299. nested enum private by default
x299nestedenumprivatebydefault.conc line 14:12 The variable InnerEnum is not visible
 
~~~~~
//##300. no weird stuff in enum subelements
x300noweirdstuffinenumsubelements.conc line 6:2 classes cannot be defined inside enum elements
x300noweirdstuffinenumsubelements.conc line 9:2 actors cannot be defined inside enum elements
x300noweirdstuffinenumsubelements.conc line 10:2 enums cannot be defined inside enum elements
 
~~~~~
//##301. no nesting of actors
x301nonestingofactors.conc line 8:1 actor classes cannot be nested
 
~~~~~
//##302. no actors of enums
x302noactorsofenums.conc line 11:0 Obay cannot extend class: MyEnum as it is an enum
x302noactorsofenums.conc line 13:17 actors can only be created for classes not enums
x302noactorsofenums.conc line 17:6 Unable to resolve reference to constructor for: 'DefaultActor$0'(java.lang.Class<? java.lang.Object>[])
x302noactorsofenums.conc line 17:24 actors can only be created for classes not enums
 
~~~~~
//##303. enums items have got to explicity call this or super on entry
x303enumsitemshavegottoexplicitycallthisorsuperonentry.conc line 6:2 First line of constructor for enum element must be a this or super call
 
~~~~~
//##304. some dupe enum bugs
x304somedupeenumbugs.conc line 4:29 Duplicate enum value declaration: ONE
x304somedupeenumbugs.conc line 7:0 enum MYDullEnum has already been declared in current scope
 
~~~~~
//##305. placement of inits
x305placementofinits.conc line 6:0 init block may only be declared within classes or enums having declaration level parameters
x305placementofinits.conc line 11:1 init block may only be declared within classes or enums having declaration level parameters
x305placementofinits.conc line 19:1 init block may only be declared within classes or enums having declaration level parameters
x305placementofinits.conc line 24:0 init block may only be declared within classes or enums having declaration level parameters
 
~~~~~
//##306. placement of inits re enums
x306placementofinitsreenums.conc line 9:2 init block may only be declared within classes or enums having declaration level parameters
 
~~~~~
//##307. annotation basic errors
x307annotationbasicerrors.conc line 4:0 Unable to resolve type corresponding to name: Stringx
x307annotationbasicerrors.conc line 8:0 Thread is not an annotation
x307annotationbasicerrors.conc line 15:0 MYClass is not an annotation
 
~~~~~
//##308. annotation no duplicates for you
x308annotationnoduplicatesforyou.conc line 8:1 Duplicate annotation AH.SimpleAnnotation
x308annotationnoduplicatesforyou.conc line 10:1 Duplicate annotation AH.SimpleAnnotation2
x308annotationnoduplicatesforyou.conc line 11:1 Duplicate annotation AH.SimpleAnnotation2
 
~~~~~
//##309. annotation missing args
x309annotationmissingargs.conc line 7:1 Missing annotation arguments for: name
 
~~~~~
//##310. annotation single arg case
x310annotationsingleargcase.conc line 7:1 More than one value to qualify from single annotation arugment. Could be one of: name, name2
 
~~~~~
//##311. annotation single arg incorrect arg
x311annotationsingleargincorrectarg.conc line 7:1 Annotation type mismatch: cannot convert from int to java.lang.String
 
~~~~~
//##312. annotation single must resolve to constant
x312annotationsinglemustresolvetoconstant.conc line 9:17 Argument passed to annotation must resolve to constant at compilation time. This does not: dave
x312annotationsinglemustresolvetoconstant.conc line 13:27 Argument passed to annotation must resolve to constant at compilation time. This does not: " const " + dave
 
~~~~~
//##313. annotation multi key args
x313annotationmultikeyargs.conc line 8:1 Missing argument for attribute name2 in annotation
x313annotationmultikeyargs.conc line 11:1 Duplicate attribute name in annotation
x313annotationmultikeyargs.conc line 14:1 Unknown attribute namex in annotation
x313annotationmultikeyargs.conc line 17:1 Annotation type mismatch: cannot convert from int to java.lang.String
 
~~~~~
//##314. annotation multi kv args must resolve to constants
x314annotationmultikvargsmustresolvetoconstants.conc line 10:1 Argument passed to annotation must resolve to constant at compilation time. This does not: name = xxx + " 9 "
 
~~~~~
//##315. err takes annot as arg
x315errtakesannotasarg.conc line 11:1 Annotation type mismatch: cannot convert from java.lang.String to com.concurnas.lang.precompiled.AnnotationHelper$TakesEnum
 
~~~~~
//##316. annotations only to new stuff
x316annotationsonlytonewstuff.conc line 18:2 annotations can only be defined on new variable assignments
 
~~~~~
//##317. invalid annotation location
x317invalidannotationlocation.conc line 11:1 Invalid location specified for annotation: dog, valid locations are: setter, getter, field
 
~~~~~
//##318. invalid annotation location x2
x318invalidannotationlocationx2.conc line 7:0 Invalid location specified for annotation: dog, valid locations are: this
 
~~~~~
//##319. invalid annotation location x3
x319invalidannotationlocationx3.conc line 8:1 Invalid location specified for annotation: param, valid locations are: setter, getter, field
 
~~~~~
//##320. can only use setter getter locations for annotations where they are defined
x320canonlyusesettergetterlocationsforannotationswheretheyaredefined.conc line 8:1 setter location can only be used where a setter is defined
x320canonlyusesettergetterlocationsforannotationswheretheyaredefined.conc line 9:1 getter location can only be used where a getter is defined
 
~~~~~
//##321. annotations all defaults
x321annotationsalldefaults.conc line 9:1 More than one value to qualify from single annotation arugment. Could be one of: name, name2
 
~~~~~
//##322. annotations on classdef args
x322annotationsonclassdefargs.conc line 9:0 Invalid location specified for annotation: dsddd, valid locations are: setter, getter, field, param
x322annotationsonclassdefargs.conc line 10:0 getter location can only be used where a getter is defined
 
~~~~~
//##323. annotations on class level applying to constructor
x323annotationsonclasslevelapplyingtoconstructor.conc line 7:0 Invalid location specified for annotation: thss, valid locations are: this
 
~~~~~
//##324. annotations on class level need defaultarg to use this
x324annotationsonclasslevelneeddefaultargtousethis.conc line 7:0 class level arguments must be provided in order to use 'this' annnotation location
 
~~~~~
//##325. target applies restrictions on to where annotations can be used
x325targetappliesrestrictionsontowhereannotationscanbeused.conc line 8:1 The cannot use annotation @AH.FieldAnnot at this location. It is restricted to use at these locations only: FIELD
 
~~~~~
//##326.nested enum double check
x326nestedenumdoublecheck.conc line 9:3 The variable a is not visible

~~~~~
//##327. annotation default values must resolve to constants
x327annotationdefaultvaluesmustresolvetoconstants.conc line 8:1 Annotation field default value must resolve to constant at compilation time. This does not: b = 99 + f
x327annotationdefaultvaluesmustresolvetoconstants.conc line 9:1 Annotation field defualt value must resolve to constant at compilation time. This does not: e = 199 + f
 
~~~~~
//##328. annotation fields must resolve to approperiate types
x328annotationfieldsmustresolvetoapproperiatetypes.conc line 8:1 Annotation field default value must resolve to constant at compilation time. This does not: b = new java.util.HashMap < java.lang.String , java.lang.String > ( )
x328annotationfieldsmustresolvetoapproperiatetypes.conc line 9:1 Invalid type java.util.HashMap<java.lang.String, java.lang.String>? for the annotation attribute bc; only primitive type, String, Class, annotation, enumeration are permitted or 1-dimensional arrays thereof
 
~~~~~
//##329. annotation fields validation checks of formattings
x329annotationfieldsvalidationchecksofformattings.conc line 7:1 Annotation field ba cannot have an access modifier
x329annotationfieldsvalidationchecksofformattings.conc line 8:1 Annotation field noval cannot be declared val
x329annotationfieldsvalidationchecksofformattings.conc line 9:1 Annotation field noprfix cannot be declared with a prefix
 
~~~~~
//##330. annotations cannot have methods or init
x330annotationscannothavemethodsorinit.conc line 6:1 Method: notallowedhere cannot be defined within annotation
x330annotationscannothavemethodsorinit.conc line 7:1 init block may only be declared within classes or enums having declaration level parameters
 
~~~~~
//##331. validate attributes
x331validateattributes.conc line 11:0 Unknown attribute f in annotation
 
~~~~~
//##332. custom annotations no boxed types for you
x332customannotationsnoboxedtypesforyou.conc line 7:1 Annotation field default value must resolve to constant at compilation time. This does not: check = [ new java.lang.Integer ( 1 )   new java.lang.Integer ( 2 ) ]
x332customannotationsnoboxedtypesforyou.conc line 8:1 Invalid type java.lang.Integer[]? for the annotation attribute check2; only primitive type, String, Class, annotation, enumeration are permitted or 1-dimensional arrays thereof
x332customannotationsnoboxedtypesforyou.conc line 11:0 Argument passed to annotation must resolve to constant at compilation time. This does not: check2 = [ new java.lang.Integer ( 33 )   new java.lang.Integer ( 44 ) ]
 
~~~~~
//##333. annotation cannot be instantiated
x333annotationcannotbeinstantiated.conc line 7:7 Instances of annotation: x333annotationcannotbeinstantiated.MyAnnotation cannot be created
x333annotationcannotbeinstantiated.conc line 8:7 Unable to find method with matching name: x333annotationcannotbeinstantiated.MyAnnotation
x333annotationcannotbeinstantiated.conc line 9:7 Annotation: MyAnnotation cannot be used at this location
x333annotationcannotbeinstantiated.conc line 11:10 Annotation: MyAnnotation cannot be used at this location
x333annotationcannotbeinstantiated.conc line 11:26 Expected type numerical not: java.lang.String. No overloaded 'plus' operator found for type java.lang.String with signature: '() java.lang.String'

~~~~~
//##334. annotation fields checks
x334annotationfieldschecks.conc line 4:0 Cannot define annotation field: b more than once
x334annotationfieldschecks.conc line 8:0 Cannot define annotation field: ab more than once
x334annotationfieldschecks.conc line 11:0 Either a type or default value must be specified for annotation field: xx
 
~~~~~
//##335. custom annotation field useage restriction
x335customannotationfielduseagerestriction.conc line 11:0 The cannot use annotation @MYAnnotation1 at this location. It is restricted to use at these locations only: FIELD
 
~~~~~
//##336. custom annotation field useage restriction converted to array
x336customannotationfielduseagerestrictionconvertedtoarray.conc line 11:0 The cannot use annotation @MYAnnotation1 ( c = 1 ) at this location. It is restricted to use at these locations only: FIELD
 
~~~~~
//##337. annotation can have inner enum but this must be public
x337annotationcanhaveinnerenumbutthismustbepublic.conc line 12:1 enums within annotations can only be defined as public
 
~~~~~
//##338. no nested classes thanks
x338nonestedclassesthanks.conc line 9:1 class: Nope cannot be defined within annotation
 
~~~~~
//##339. suppress warnings params that can be used
x339suppresswarningsparamsthatcanbeused.conc line 8:1 Invalid SuppressWarnings arguments: 'allx' valid options are one or many of: 'enum-match-non-exhaustive, generic-cast, redefine-import, typedef-arg-use' or 'all'
x339suppresswarningsparamsthatcanbeused.conc line 13:1 Invalid SuppressWarnings arguments: 'allc' valid options are one or many of: 'enum-match-non-exhaustive, generic-cast, redefine-import, typedef-arg-use' or 'all'
x339suppresswarningsparamsthatcanbeused.conc line 18:1 Invalid SuppressWarnings arguments: 'generic-castc' valid options are one or many of: 'enum-match-non-exhaustive, generic-cast, redefine-import, typedef-arg-use' or 'all'
x339suppresswarningsparamsthatcanbeused.conc line 23:1 Invalid SuppressWarnings arguments: 'pants, generic-castx' valid options are one or many of: 'enum-match-non-exhaustive, generic-cast, redefine-import, typedef-arg-use' or 'all'
x339suppresswarningsparamsthatcanbeused.conc line 29:0 Invalid SuppressWarnings arguments: 'all, generic-cast' valid options are one or many of: 'enum-match-non-exhaustive, generic-cast, redefine-import, typedef-arg-use' or 'all' - 'all' cannot be used in conjunction with other options
x339suppresswarningsparamsthatcanbeused.conc line 35:0 Invalid SuppressWarnings arguments: 'alla' valid options are one or many of: 'enum-match-non-exhaustive, generic-cast, redefine-import, typedef-arg-use' or 'all'
x339suppresswarningsparamsthatcanbeused.conc line 42:0 Invalid SuppressWarnings arguments: 'generic-castd' valid options are one or many of: 'enum-match-non-exhaustive, generic-cast, redefine-import, typedef-arg-use' or 'all'
x339suppresswarningsparamsthatcanbeused.conc WARN line 10:2 Unable to determine type for generic: T for as operation. Wil use upper bounds of: java.lang.Object instead
x339suppresswarningsparamsthatcanbeused.conc WARN line 15:4 Unable to determine type for generic: T for as operation. Wil use upper bounds of: java.lang.Object instead
x339suppresswarningsparamsthatcanbeused.conc WARN line 20:2 Unable to determine type for generic: T for as operation. Wil use upper bounds of: java.lang.Object instead
x339suppresswarningsparamsthatcanbeused.conc WARN line 25:4 Unable to determine type for generic: T for as operation. Wil use upper bounds of: java.lang.Object instead
x339suppresswarningsparamsthatcanbeused.conc WARN line 38:2 Unable to determine type for generic: T for as operation. Wil use upper bounds of: java.lang.Object instead
x339suppresswarningsparamsthatcanbeused.conc WARN line 45:2 Unable to determine type for generic: T for as operation. Wil use upper bounds of: java.lang.Object instead
 
~~~~~
//##340. try w resrouces
x340trywresrouces.conc line 27:3 Resource specified in try with resource block must implement close method, type: java.lang.String does not
 
~~~~~
//##341. try w resrouces var decl
x341trywresroucesvardecl.conc line 34:6 The variable b may not have been initialized
 
~~~~~
//##342. try w resrouces var decl respected
x342trywresroucesvardeclrespected.conc line 27:2 Variable c has been decalred as val and cannot be reassigned
 
~~~~~
//##343. try w resrouces must impl close
x343trywresroucesmustimplclose.conc line 7:3 Resource specified in try with resource block must implement close method, 'assert true' does not
x343trywresroucesmustimplclose.conc line 8:4 Resource specified in try with resource block must implement close method, type: x343trywresroucesmustimplclose.MyClass does not
 
~~~~~
//##344. try w assign existing must resolve to refname
x344trywassignexistingmustresolvetorefname.conc line 20:3 assignment specified in try with resource block must resolve to a variable, 'uhoh [ 0 ]' is not a variable
 
~~~~~
//##345. exception caught must be of exception type
x345exceptioncaughtmustbeofexceptiontype.conc line 9:10 Attempted to catch Type: x345exceptioncaughtmustbeofexceptiontype.MyExcep which is not a subtype of throwable
 
~~~~~
//##346. exception caught does not exist
x346exceptioncaughtdoesnotexist.conc line 31:9 Unable to resolve type corresponding to name: MyExcep
 
~~~~~
//##347. multi catch one each please
x347multicatchoneeachplease.conc line 25:19 Attempted to catch Type: x347multicatchoneeachplease.Excep1 more than once in same catch block
 
~~~~~
//##348. potential reassignment of val in loop not permitted
x348potentialreassignmentofvalinloopnotpermitted.conc line 8:3 The field a has been decalred as val and cannot be reassigned
 
~~~~~
//##349. is cannot reuse same type name
x349iscannotreusesametypename.conc line 14:29 Attempted to compare instance of type: java.lang.Integer more than once in the same is not statement
 
~~~~~
//##350. double dot usage
x350doubledotusage.conc line 7:13 .. can only be used with method calls
 
~~~~~
//##351. double dot usage restrict assignment
x351doubledotusagerestrictassignment.conc line 16:1 .. cannot be used for assignment
x351doubledotusagerestrictassignment.conc line 18:1 .. cannot be used for assignment
x351doubledotusagerestrictassignment.conc line 19:1 .. cannot be used for assignment
 
~~~~~
//##351. param name cannot be directly declared
x351paramnamecannotbedirectlydeclared.conc line 10:11 Instances of com.concurnas.lang.ParamName cannot be created
 
~~~~~
//##352. param name cannot be dupe
x352paramnamecannotbedupe.conc line 13:36 Duplicate named parameter a
 
~~~~~
//##353. named param checks
x353namedparamchecks.conc line 8:12 Unable to find method with matching number of arguments with name: afunc
x353namedparamchecks.conc line 9:12 Unable to find method with matching number of arguments with name: afunc
x353namedparamchecks.conc line 10:12 Unable to find method with matching number of arguments with name: afuncLackInfo
 
~~~~~
//##354. named type too many gens
x354namedtypetoomanygens.conc line 13:10 Generic parameter count of: 2 does not equal: 0
 
~~~~~
//##355. constructor with named param doesnt exist
x355constructorwithnamedparamdoesntexist.conc line 13:10 Generic parameter count of: 0 does not equal: 1 - generic type parameters must be defined
 
~~~~~
//##356. amibous named parameter mapping
x356amibousnamedparametermapping.conc line 21:11 Ambiguous method detected 'afunc'. More than one direct match made - due to ambiguous named parameter mapping
 
~~~~~
//##357. lambda param information
 
~~~~~
//##358. was a bug on thing creation const not existing
x358wasabugonthingcreationconstnotexisting.conc line 9:10 Unable to resolve reference to constructor for: 'DefaultActor$0'(java.lang.Class<? java.lang.Object>[], int, int, a = int)
x358wasabugonthingcreationconstnotexisting.conc line 10:11 Unable to resolve reference to constructor for: 'DefaultActor$0'(java.lang.Class<? java.lang.Object>[], int, int, int)
 
~~~~~
//##359. incorrect number of arguments w no defaults
x359incorrectnumberofargumentswnodefaults.conc line 7:4 Unable to find method with matching number of arguments with name: hasDefaults
 
~~~~~
//##360. lhs must be supertype of rhs
x360lhsmustbesupertypeofrhs.conc line 4:17 int is not a subtype of java.lang.String
 
~~~~~
//##361. defaults arg mach
x361defaultsargmach.conc line 10:4 Unable to find method with matching number of arguments with name: hasDefaults
x361defaultsargmach.conc line 11:4 Unable to find method with matching number of arguments with name: hasDefaults
x361defaultsargmach.conc line 13:4 Unable to find method with matching number of arguments with name: hasDefaults
x361defaultsargmach.conc line 14:4 Unable to find method with matching number of arguments with name: hasDefaults
x361defaultsargmach.conc line 15:4 Unable to find method with matching number of arguments with name: hasDefaults
x361defaultsargmach.conc line 16:5 Unable to find method with matching number of arguments with name: hasDefaults
 
~~~~~
//##362. default param or type must be provided for classdef args
x362defaultparamortypemustbeprovidedforclassdefargs.conc line 4:21 Either a type or default value must be specified for parameter a
x362defaultparamortypemustbeprovidedforclassdefargs.conc line 7:2 Unable to resolve reference to this constructor for: 'x362defaultparamortypemustbeprovidedforclassdefargs.MyClass'(int, int)
x362defaultparamortypemustbeprovidedforclassdefargs.conc line 17:6 Unable to find method with matching name: x362defaultparamortypemustbeprovidedforclassdefargs.MyClass
 
~~~~~
//##363. minor bug
x363minorbug.conc line 14:7 Unable to find method with matching name: lefunc
 
~~~~~
//##364. ambigious with default params
x364ambigiouswithdefaultparams.conc line 5:0 Function myFunc with matching argument definition exists already in current Scope when default arguments are ignored
x364ambigiouswithdefaultparams.conc line 7:0 Function myFunc with matching argument definition exists already in current Scope
 
~~~~~
//##365. ambigious with default params constr
x365ambigiouswithdefaultparamsconstr.conc line 6:1 Constructor with matching argument definition exists already in current Scope when default arguments are ignored
x365ambigiouswithdefaultparamsconstr.conc line 7:1 Constructor with matching argument definition exists already in current Scope
x365ambigiouswithdefaultparamsconstr.conc line 13:9 Ambiguous method detected 'constructor'. More than one direct match made
 
~~~~~
//##365. default value besides null cannot be used for generic type assignment
x365defaultvaluebesidesnullcannotbeusedforgenerictypeassignment.conc line 4:14 default value besides null cannot be used for generic type assignment
x365defaultvaluebesidesnullcannotbeusedforgenerictypeassignment.conc line 7:5 Unable to find method with matching name: myFunc and arguments ()
 
~~~~~
//##366. default value besides null cannot be used for generic type assignment classdef
x366defaultvaluebesidesnullcannotbeusedforgenerictypeassignmentclassdef.conc line 4:0 default value besides null cannot be used for generic type assignment
x366defaultvaluebesidesnullcannotbeusedforgenerictypeassignmentclassdef.conc line 4:17 default value besides null cannot be used for generic type assignment
 
~~~~~
//##367. error when rhs does not match lhs 
x367errorwhenrhsdoesnotmatchlhs.conc line 4:17 int is not a subtype of java.lang.String
 
~~~~~
//##368. error when rhs does not match lhs classdefargs meh good enough
x368errorwhenrhsdoesnotmatchlhsclassdefargsmehgoodenough.conc line 4:0 int is not a subtype of java.lang.String
x368errorwhenrhsdoesnotmatchlhsclassdefargsmehgoodenough.conc line 4:19 int is not a subtype of java.lang.String
 
~~~~~
//##369. error when no type or dv specified for classdefarg
x369errorwhennotypeordvspecifiedforclassdefarg.conc line 4:19 Either a type or default value must be specified for parameter a
 
~~~~~
//##370. only one vararg for you
x370onlyonevarargforyou.conc line 4:31 Only one parameter per method may be declared as a vararg
 
~~~~~
//##371. default param may not follow vararg immediatly
x371defaultparammaynotfollowvarargimmediatly.conc line 4:31 Parameter s with default value may not immediately follow vararg parameter: bs having type of: int which can be included in the vararg
 
~~~~~
//##372. vararg type checking must be subtypes
x372varargtypecheckingmustbesubtypes.conc line 9:8 Unable to find method with matching name: myMethod and arguments (int, double[], int)
x372varargtypecheckingmustbesubtypes.conc line 10:9 Unable to find method with matching name: myMethod and arguments (int, int, double, int, int)
 
~~~~~
//##373. vararg default value may not preceed vararg

~~~~~
//##374. this is not a match
x374thisisnotamatch.conc line 9:8 Unable to find method with matching name: myMethod and arguments (int, int, int, int, double)
 
~~~~~
//##375. this is not a match either
x375thisisnotamatcheither.conc line 13:1 Unable to find method with matching name: asList and arguments (int, int[])
x375thisisnotamatcheither.conc line 14:6 Unable to find method with matching name: asList and arguments (int[], int)
 
~~~~~
//##376. vararg ambiguity

~~~~~
//##377. must have diff type seperator between vararg and default param
x377musthavedifftypeseperatorbetweenvararganddefaultparam.conc line 4:21 Parameter g with default value may not immediately follow vararg parameter: b having type of: int which can be included in the vararg
x377musthavedifftypeseperatorbetweenvararganddefaultparam.conc line 5:20 Vararg parameter b may not immediately follow parameter with default value: g having type of: int which can be included in the vararg
 
~~~~~
//##378. as above
x378asabove.conc line 4:23 Vararg parameter b may not immediately follow parameter with default value: a having type of: int which can be included in the vararg
 
~~~~~
//##379. ambigous wihtout seperating parameter on these varargs
x379ambigouswihtoutseperatingparameteronthesevarargs.conc line 4:27 Parameter g with default value may not immediately follow vararg parameter: b having type of: int which can be included in the vararg
x379ambigouswihtoutseperatingparameteronthesevarargs.conc line 9:25 Vararg parameter b may not immediately follow parameter with default value: g having type of: int which can be included in the vararg
 
~~~~~
//##380. typedef misc bugs
x380typedefmiscbugs.conc line 9:8 type cannot be used in this way
x380typedefmiscbugs.conc line 10:6 Cannot find put function on java.util.HashMap<java.lang.String, java.lang.Integer> with arguments: [int, java.lang.String]
 
~~~~~
//##381. typedef takes the provided arg and applies if possible, also throwns warning when unused
x381typedeftakestheprovidedargandappliesifpossiblealsothrownswarningwhenunused.conc line 12:6 Unable to find method with matching name: add and arguments (java.lang.String)
x381typedeftakestheprovidedargandappliesifpossiblealsothrownswarningwhenunused.conc WARN line 6:0 typedef qualifier is unused in right hand side definition: x
x381typedeftakestheprovidedargandappliesifpossiblealsothrownswarningwhenunused.conc WARN line 7:0 typedef qualifiers are unused in right hand side definition: x, y
 
~~~~~
//##382. typedef can only be used in blocks
x382typedefcanonlybeusedinblocks.conc line 5:1 line cannot be present within class definition
 
~~~~~
//##383. typedef ppp
 
~~~~~
//##384. typedef ppp at module level only
 
~~~~~
//##385. typedef cehcks
x385typedefcehcks.conc line 7:0 typedef myok has already been definted with 1 arguments
x385typedefcehcks.conc line 8:0 typedef qualifier 'x' has already been declared in qualifier parameter list
x385typedefcehcks.conc line 9:0 typedef qualifier 'String' overrides an existing type - this is confusing
x385typedefcehcks.conc line 10:0 typedef qualifier 'mylistQ' overrides an existing type - this is confusing
x385typedefcehcks.conc line 11:0 typedef 'String' overrides an existing type - this is confusing
x385typedefcehcks.conc line 13:16 Unable to resolve type corresponding to name: amyb
x385typedefcehcks.conc line 14:16 Unable to resolve type corresponding to name: amyc
x385typedefcehcks.conc line 15:16 Unable to resolve type corresponding to name: nope
x385typedefcehcks.conc line 25:2 Cannot perform is or isnot check on primative type
x385typedefcehcks.conc WARN line 10:0 typedef qualifier is unused in right hand side definition: mylistQ
 
~~~~~
//##386. match at least one case
x386matchatleastonecase.conc line 5:1 match must have at least one case
 
~~~~~
//##387. match else block redundant
x387matchelseblockredundant.conc line 9:7 case will match all inputs hence else block will never be entered into
x387matchelseblockredundant.conc line 12:7 case will match all inputs hence else block will never be entered into
 
~~~~~
//##388. match else block because catch all always
x388matchelseblockbecausecatchallalways.conc line 6:7 case will match all inputs hence else block will never be entered into
 
~~~~~
//##389. match catch all stuff after never triggered
x389matchcatchallstuffafternevertriggered.conc line 6:7 case will match all inputs, cases after the one will never be triggered
x389matchcatchallstuffafternevertriggered.conc line 9:7 case will match all inputs, cases after the one will never be triggered
 
~~~~~
//##390. looks about right 
x390looksaboutright.conc line 12:7 incompatible type: int vs java.lang.Object
 
~~~~~
//##391. two double matches
x391twodoublematches.conc line 6:7 case will match all inputs, cases after the one will never be triggered

~~~~~
//##392. cannot match on unrelated class
x392cannotmatchonunrelatedclass.conc line 8:7 case will match all inputs, cases after the one will never be triggered
x392cannotmatchonunrelatedclass.conc line 11:7 case will never be matched as x392cannotmatchonunrelatedclass.MyClass and java.lang.Number are not subtypes of each other
 
~~~~~
//##393. case epr always false
x393caseepralwaysfalse.conc line 6:7 case expression always resolves to false, hence will never be triggered
 
~~~~~
//##394. else would be better
x394elsewouldbebetter.conc line 9:7 typed case expression without variable assingment is redundant all inputs will be matched, use else statement
 
~~~~~
//##395. defo returns needs an else
x395deforeturnsneedsanelse.conc line 5:3 else block or catch all case must be provided for match statement returns which something
 
~~~~~
//##396. flag longers on match no ret
x396flaglongersonmatchnoret.conc line 7:3 Expression cannot appear on its own line
x396flaglongersonmatchnoret.conc line 9:7 Expression cannot appear on its own line
 
~~~~~
//##397. exhcaustive enum match no else needed

~~~~~
//##398. exhcaustive enum match no else needed

~~~~~
//##399. else block missing when non exhaustive enum raise WARNING only
x399elseblockmissingwhennonexhaustiveenumraiseWARNINGonly.conc WARN line 9:1 match returns something and is acting on an enum, all enum elements must be matched if no catch all case or else block is provided. These enum elements are not matched in any cases: CASE3
 
~~~~~
//##400. one case and it matches everything
x400onecaseanditmatcheseverything.conc line 6:7 case will match all inputs hence else block will never be entered into

~~~~~
//##401. bugfix nasty blowup
x401bugfixnastyblowup.conc line 6:14 Unable to resolve type corresponding to name: MyEnum
x401bugfixnastyblowup.conc line 10:15 Unable to resolve reference to variable name: MyEnum.CASE1
  
~~~~~
//##402. match assign final cannot be reassigned
x402matchassignfinalcannotbereassigned.conc line 7:3 Variable a has been decalred as val and cannot be reassigned
 
~~~~~
//##403. cannot use increment decrement on vals
x403cannotuseincrementdecrementonvals.conc line 7:1 Variable a has been decalred as val and cannot be incremented or decremented
x403cannotuseincrementdecrementonvals.conc line 10:3 Variable a has been decalred as val and cannot be incremented or decremented
 
~~~~~
//##404. ref thing captures all
x404refthingcapturesall.conc line 8:7 typed case expression without variable assingment is redundant all inputs will be matched, use else statement
 
~~~~~
//##405. sizeof
x405sizeof.conc line 7:5 sizeof can only operate on objects, not primative types such as int
x405sizeof.conc line 8:6 Invalid type void
 
~~~~~
//##406. transient
x406transient.conc line 6:1 Local variables may not be declared transient
 
~~~~~
//##407. enum field restrictions
x407enumfieldrestrictions.conc line 10:12 Unable to resolve reference to variable name: ONE
x407enumfieldrestrictions.conc line 11:12 Unable to resolve reference to variable name: d
 
~~~~~
//##408. controls on delete
x408controlsondelete.conc line 17:5 Invalid type void
x408controlsondelete.conc line 18:5 Can only delete local variables or list/map references
x408controlsondelete.conc line 19:5 Only local variables can be deleted
x408controlsondelete.conc line 23:3 d cannot be resolved to a variable
 
~~~~~
//##409. controls on delete double check
x409controlsondeletedoublecheck.conc line 21:1 Unable to resolve reference to variable name: mc.thing
 
~~~~~
//##410. delete needs override  
x410deleteneedsoverride.conc line 6:1 Method: 'def delete() void' of class: 'java.lang.Object' should be explicitly overriden in subclass: x410deleteneedsoverride.MyClass. Use the override keyword
 
~~~~~
//##411. branch operation del invalidates rest
x411branchoperationdelinvalidatesrest.conc line 13:6 thing cannot be resolved to a variable
 
~~~~~
//##412. no range delete
x412norangedelete.conc line 15:5 ranges cannot be deleted
 
~~~~~
//##413. custom dma enc and dec
x413customdmaencanddec.conc line 6:0 If public toBinary(Encoder) is defined then public fromBinary(Decoder) must also be defined
x413customdmaencanddec.conc line 11:0 If public fromBinary(Decoder) is defined then public toBinary(Encoder) must also be defined
x413customdmaencanddec.conc line 17:1 Method: 'def toBinary(com.concurnas.bootstrap.lang.offheap.Encoder) void' of class: 'java.lang.Object' should be explicitly overriden in subclass: x413customdmaencanddec.ErrorCls3. Use the override keyword
x413customdmaencanddec.conc line 18:1 Method: 'def fromBinary(com.concurnas.bootstrap.lang.offheap.Decoder) void' of class: 'java.lang.Object' should be explicitly overriden in subclass: x413customdmaencanddec.ErrorCls3. Use the override keyword
 
~~~~~
//##414. ref name bugfix
x414refnamebugfix.conc line 7:30 MyEnum cannot be resolved to a variable
 
~~~~~
//##415. was a bug on null arg funcref
x415wasabugonnullargfuncref.conc line 8:10 Unable to find method with matching name: alovelyone
 
~~~~~
//##416. ambigious no arg match
x416ambigiousnoargmatch.conc line 8:10 Ambiguous method detected 'whoislovely'. More than one direct match made
 
~~~~~
//##417. ambigious no arg match on cons
x417ambigiousnoargmatchoncons.conc line 13:17 Ambiguous method detected 'MyClass'. More than one direct match made
 
~~~~~
//##418. we need generic binding on this funcref
 
~~~~~
//##419. we need generic binding on this construref
x419weneedgenericbindingonthisconstruref.conc line 11:6 Missing generic binding for: X on module method: MyClass
 
~~~~~
//##420. in out params - out param useage
x420inoutparamsoutparamuseage.conc line 18:30 Generic Type argument type mismatch: java.lang.Object vs java.lang.String
x420inoutparamsoutparamuseage.conc line 21:9 Method: add is not callable as generic arguments: 1 have been qualified as out types and so cannot be used as method inputs
x420inoutparamsoutparamuseage.conc line 23:12 Method: add is not callable as generic arguments: 1 have been qualified as out types and so cannot be used as method inputs
x420inoutparamsoutparamuseage.conc line 28:1 Cannot assign to generic parameter qualified as an out type: out java.lang.Integer?
 
~~~~~
//##421. in out params - in param useage
x421inoutparamsinparamuseage.conc line 21:30 Generic Type argument type mismatch: java.lang.Object vs java.lang.Number
x421inoutparamsinparamuseage.conc line 22:33 Generic Type argument type mismatch: in java.lang.Object vs java.lang.Number
x421inoutparamsinparamuseage.conc line 26:21 Method: get is not callable as generic return type: in java.lang.Integer has been qualified as in type and so cannot be used as method output
x421inoutparamsinparamuseage.conc line 27:25 Method: get is not callable as generic return type: in java.lang.Integer has been qualified as in type and so cannot be used as method output
x421inoutparamsinparamuseage.conc line 29:15 Method: get is not callable as generic return type: in java.lang.Integer has been qualified as in type and so cannot be used as method output
x421inoutparamsinparamuseage.conc line 35:17 java.lang.Object is not a subtype of java.lang.Integer
 
~~~~~
//##422. in out params - imported from existing class
x422inoutparamsimportedfromexistingclass.conc line 11:12 Method: setX is not callable as generic arguments: 1 have been qualified as out types and so cannot be used as method inputs
x422inoutparamsimportedfromexistingclass.conc line 12:1 Cannot assign to generic parameter qualified as an out type: out java.lang.Number
x422inoutparamsimportedfromexistingclass.conc line 15:26 Method: getX is not callable as generic return type: in java.lang.Integer has been qualified as in type and so cannot be used as method output
x422inoutparamsimportedfromexistingclass.conc line 16:28 Method: getX is not callable as generic return type: in java.lang.Integer has been qualified as in type and so cannot be used as method output
 
~~~~~
//##423. in out params convert from in out etc
x423inoutparamsconvertfrominoutetc.conc line 8:30 Generic Type argument type mismatch: java.lang.Number vs out java.lang.Number
x423inoutparamsconvertfrominoutetc.conc line 9:31 Generic Type argument type mismatch: java.lang.Integer vs out java.lang.Number
x423inoutparamsconvertfrominoutetc.conc line 13:32 Generic Type argument type mismatch: java.lang.Number vs out java.lang.Number
x423inoutparamsconvertfrominoutetc.conc line 14:33 Generic Type argument type mismatch: in java.lang.Object vs out java.lang.Number
 
~~~~~
//##424. in out params convert from in out etc imported
x424inoutparamsconvertfrominoutetcimported.conc line 7:44 Generic Type argument type mismatch: java.lang.Number vs out java.lang.Number
x424inoutparamsconvertfrominoutetcimported.conc line 9:10 Method: expiri is not callable as generic arguments: 1 have been qualified as out types and so cannot be used as method inputs
 
~~~~~
//##425. no inout on constructors
x425noinoutonconstructors.conc line 7:7 Generic qualifications for constructors may not use in out
  
~~~~~
//##426. all generics must be qualified inc nested
x426allgenericsmustbequalifiedincnested.conc line 9:20 Generic parameter count of: 0 does not equal: 1 - generic type parameters must be defined
 
~~~~~
//##427. generics must have subgenerics in typedef quantified
 
~~~~~
//##427. typedef no inout and validate valid structures 
x427typedefnoinoutandvalidatevalidstructures.conc line 6:0 definition of typedef alist may not contain out generic qualification
x427typedefnoinoutandvalidatevalidstructures.conc line 7:0 definition of typedef something may not contain out generic qualification
x427typedefnoinoutandvalidatevalidstructures.conc line 8:0 typedef impossible1 variable: x useage has inconsistent generic arguments in definition
x427typedefnoinoutandvalidatevalidstructures.conc line 9:0 typedef impossible2 variable: x useage has inconsistent generic arguments in definition
x427typedefnoinoutandvalidatevalidstructures.conc line 10:0 typedef impossible3 variable: x useage has inconsistent generic arguments in definition
 
~~~~~
//##428. use of class refs on actors etc
x428useofclassrefsonactorsetc.conc line 16:30 Generic Type argument type mismatch: out x428useofclassrefsonactorsetc.MyClass<java.lang.String> vs out DefaultActor$0<java.lang.String>
x428useofclassrefsonactorsetc.conc line 17:30 cannot assign type of com.concurnas.bootstrap.lang.Lambda$ClassRef<out x428useofclassrefsonactorsetc.MyClass<java.lang.String>> to com.concurnas.bootstrap.lang.Lambda$Function0<out x428useofclassrefsonactorsetc.MyClass<java.lang.String>>
x428useofclassrefsonactorsetc.conc line 18:31 cannot assign type of com.concurnas.bootstrap.lang.Lambda$ClassRef<out x428useofclassrefsonactorsetc.MyClass<java.lang.String>> to com.concurnas.bootstrap.lang.Lambda$Function0<out x428useofclassrefsonactorsetc.MyClass<java.lang.String>>
x428useofclassrefsonactorsetc.conc line 19:35 cannot assign type of com.concurnas.bootstrap.lang.Lambda$ClassRef<out DefaultActor$0<java.lang.String>> to com.concurnas.bootstrap.lang.Lambda$Function0<out com.concurnas.lang.TypedActor<x428useofclassrefsonactorsetc.MyClass<java.lang.String>>>
x428useofclassrefsonactorsetc.conc line 20:35 cannot assign type of com.concurnas.bootstrap.lang.Lambda$Function0<DefaultActor$0<java.lang.String>> to com.concurnas.bootstrap.lang.Lambda$ClassRef<out com.concurnas.lang.TypedActor<x428useofclassrefsonactorsetc.MyClass<java.lang.String>>>
 
~~~~~
//##429. lambdas from java code cannot contain wildcard generic qualifications
x429lambdasfromjavacodecannotcontainwildcardgenericqualifications.conc line 8:5 Unable to resolve reference to variable name: Constructor, Method or Class references cannot return or take as an input wildcards parameters
x429lambdasfromjavacodecannotcontainwildcardgenericqualifications.conc line 9:21 Unable to find method with matching name: illegalclassref2 and arguments (def (*) java.lang.String)
 
~~~~~
//##430. classsref only can use public constrictors 
x430classsrefonlycanusepublicconstrictors.conc line 18:8 Unable to find method with matching name: mc and arguments (int)
x430classsrefonlycanusepublicconstrictors.conc line 19:9 Unable to find method with matching name: mc and arguments (int)
x430classsrefonlycanusepublicconstrictors.conc line 23:9 Unable to find method with matching name: mc and arguments (int)
 
~~~~~
//##431. local classes are not nested
x431localclassesarenotnested.conc line 18:52 Method b in: x431localclassesarenotnested.Thing cannot be called from within local class: MiniClass (local classes are not nested)
x431localclassesarenotnested.conc line 20:4 Method b in: x431localclassesarenotnested.Thing cannot be called from within local class: MiniClass (local classes are not nested)
 
~~~~~
//##431. local classes are not nested
x432localclassescanonlybeprivate.conc line 6:1 local class: 'MiniClass' can only be declared private
 
~~~~~
//##432. local classes can only be private
x433localclassescannotaccessfieldsasnotnested.conc line 12:3 Field pp in: x433localclassescannotaccessfieldsasnotnested.MyClass cannot be called from within local class: MMM (local classes are not nested)
 
~~~~~
//##434. local classes cannot access funcs of class
x434localclassescannotaccessfuncsofclass.conc line 10:46 Method NIF$0 in: x434localclassescannotaccessfuncsofclass.MyClass cannot be called from within local class: NIC$1 (local classes are not nested)
 
~~~~~
//##435. inf copy bug
x435infcopybug.conc line 14:36 a cannot be resolved to a variable
 
~~~~~
//##436. no npe

~~~~~
//##437. new oo  type returned must be compatible
x437newootypereturnedmustbecompatible.conc line 26:13 Operator overloading of: x437newootypereturnedmustbecompatible.MyProvider produces type of: x437newootypereturnedmustbecompatible.MyClass? for requested class: x437newootypereturnedmustbecompatible$MyOtherClass which is not compatible
 
~~~~~
//##438. new oo  type returned must be namedType
x438newootypereturnedmustbenamedType.conc line 14:13 Operator overloading of: x438newootypereturnedmustbenamedType.MyProvider produces type of: void for requested class: D.MyOtherClass which is not compatible
 
~~~~~
//##439. new oo  no class refs allowed
x439newoonoclassrefsallowed.conc line 21:15 Operator overloading of new cannot produce a class reference
 
~~~~~
//##440. new oo  no actors
x440newoonoactors.conc line 21:11 Operator overloading of: x440newoonoactors.MyProvider cannot be used to produce an actor
 
~~~~~
//##441. used to npe now it doesnt
x441usedtonpenowitdoesnt.conc line 13:2 Generic parameter count of: 0 does not equal: 1 - generic type parameters must be defined
x441usedtonpenowitdoesnt.conc line 13:13 ars cannot be resolved to a variable
 
~~~~~
//##442. bug on accessablility of protected items
x442bugonaccessablilityofprotecteditems.conc line 10:4 The method something is not visible
 
~~~~~
//##443. fix bugs on calling methods with boxed unboxed arguments
x443fixbugsoncallingmethodswithboxedunboxedarguments.conc line 11:8 Unable to find method with matching name: call1 and arguments (int[], int)
x443fixbugsoncallingmethodswithboxedunboxedarguments.conc line 12:8 Unable to find method with matching name: call2 and arguments (java.lang.Integer[], int)
x443fixbugsoncallingmethodswithboxedunboxedarguments.conc line 13:8 Unable to find method with matching name: call3 and arguments (java.lang.Integer[])
x443fixbugsoncallingmethodswithboxedunboxedarguments.conc line 14:8 Unable to find method with matching name: call4 and arguments (int[])
 
~~~~~
//##444. final block throws exception so no return for main try
x444finalblockthrowsexceptionsonoreturnformaintry.conc line 16:2 Expression cannot appear on its own line
 
~~~~~
//##445. final block throws exception so no return for catches of try
x445finalblockthrowsexceptionsonoreturnforcatchesoftry.conc line 18:2 Expression cannot appear on its own line

~~~~~
//##446. bugfix
x446bugfix.conc line 10:7 async must contain at least one onchange or every instance
 
~~~~~
//##447. redefine class
x447redefineclass.conc line 9:0 Class name has already been declared in current scope: 'HashSet'
x447redefineclass.conc WARN line 6:0 Class name overwrites imported class: HashSet
 
~~~~~
//##448. bugfix on misnamed generics
x448bugfixonmisnamedgenerics.conc line 4:21 Unable to resolve type corresponding to name: Ta
x448bugfixonmisnamedgenerics.conc line 10:21 Missing generic binding for: P on method: something2
x448bugfixonmisnamedgenerics.conc line 15:6 Generic parameter count of: 0 does not equal: 1 - generic type parameters must be defined
 
~~~~~
//##449. bugfix on parent nestor name incorrect
x449bugfixonparentnestornameincorrect.conc line 15:23 Nested inner class: out x449bugfixonparentnestornameincorrect.Parent.MyClass<java.lang.String> parent cannot be found: ParentM
 
~~~~~
//##450. show error on missing import
x450showerroronmissingimport.conc line 5:5 Fiber cannot be resolved to a variable
 
~~~~~
//##451. in our param test
x451inourparamtest.conc line 14:17 java.lang.Object is not a subtype of java.lang.Integer
x451inourparamtest.conc line 21:1 Cannot assign to generic parameter qualified as an out type: out java.lang.Integer
 
~~~~~
//##452. was a bug before concerning args for Holder
x452wasabugbeforeconcerningargsforHolder.conc line 8:9 Unable to find method with matching name: constructor and arguments (java.lang.String)
 
~~~~~
//##453. in param cant be used to qualify generic input to method or constructor
x453inparamcantbeusedtoqualifygenericinputtomethodorconstructor.conc line 15:12 java.lang.Object is not a subtype of java.lang.String
x453inparamcantbeusedtoqualifygenericinputtomethodorconstructor.conc line 18:23 Generic Type argument type mismatch: java.lang.String vs java.lang.Object
 
~~~~~
//##454. ref array is directly settable

~~~~~
//##455. Cannot create ref of type: ref as it is uninstantiable
x455Cannotcreaterefoftype:refasitisuninstantiable.conc line 7:0 Cannot create ref of type: java.lang.Integer:com.concurnas.bootstrap.runtime.ref.Ref as it is uninstantiable
 
~~~~~
//##456. used to blow up
x456usedtoblowup.conc line 7:4 Unable to find method with matching name: g

~~~~~
//##457. cannot cast from x array to x ref array 
x457cannotcastfromxarraytoxrefarray.conc line 10:1 Unable to find method with matching name: suum and arguments (java.lang.Integer[])
 
~~~~~
//##458. ret when should and shouldnt
x458retwhenshouldandshouldnt.conc line 5:7 Expression cannot appear on its own line
x458retwhenshouldandshouldnt.conc line 6:3 trans must return something
 
~~~~~
//##459. ambigious thanks to generic qualification constructors
x459ambigiousthankstogenericqualificationconstructors.conc line 17:11 Ambiguous method detected 'constructor'. More than one indirect match made
 
~~~~~
//##460. ambigious thanks to generic qualification method calls
x460ambigiousthankstogenericqualificationmethodcalls.conc line 17:11 Ambiguous method detected 'ambigola'. More than one indirect match made
 
~~~~~
//##461. type error on match
x461typeerroronmatch.conc line 15:8 Incompatible operand types java.lang.String and int
 
~~~~~
//##462. classloaders return type Object
x462classloadersreturntypeObject.conc line 47:10 Unable to find method with matching name: thing
 
~~~~~
//##462. match case always resolve to true
x463matchcasealwaysresolvetotrue.conc line 6:7 case will match all inputs hence else block will never be entered into
 
~~~~~
//##463. this qualifier must exist and be in nest path
x464thisqualifiermustexistandbeinnestpath.conc line 25:12 qualifier for this must be either current class or parent nestor: String resolves to: java.lang.String which is not
x464thisqualifiermustexistandbeinnestpath.conc line 26:12 qualifier for this does not exist: Innerclasssdf
 
~~~~~
//##464. extension function only access public stuff
x465extensionfunctiononlyaccesspublicstuff.conc line 14:7 The variable myvar is not visible
x465extensionfunctiononlyaccesspublicstuff.conc line 15:8 The variable myvar is not visible
x465extensionfunctiononlyaccesspublicstuff.conc line 16:3 amethod cannot be resolved to a variable
x465extensionfunctiononlyaccesspublicstuff.conc line 17:8 The method amethod is not visible
 
~~~~~
//##466. extension methods only private or protected
x466extensionmethodsonlyprivateorprotected.conc line 16:1 Extension methods can only be declared protected or private

~~~~~
//##467. extension functions only called on extendee
x467extensionfunctionsonlycalledonextendee.conc line 18:1 Unable to find method with matching name: myFunc
x467extensionfunctionsonlycalledonextendee.conc line 19:1 Unable to find method with matching name: myFunc
 
~~~~~
//##468. arg clash on ext func duplicate
x468argclashonextfuncduplicate.conc line 16:0 Function myFunc with matching argument definition exists already in current Scope
x468argclashonextfuncduplicate.conc line 22:5 Unable to find method with matching name: myFunc

~~~~~
//##469. bit shift operations only on integral types
x469bitshiftoperationsonlyonintegraltypes.conc line 10:15 bit shift operation cannot be performed on type boolean
x469bitshiftoperationsonlyonintegraltypes.conc line 11:13 bit shift operation cannot be performed on type double
x469bitshiftoperationsonlyonintegraltypes.conc line 12:14 bit shift operation cannot be performed on type float
x469bitshiftoperationsonlyonintegraltypes.conc line 14:1 bit shift operation cannot be performed on type boolean
x469bitshiftoperationsonlyonintegraltypes.conc line 15:1 bit shift operation cannot be performed on type double
x469bitshiftoperationsonlyonintegraltypes.conc line 16:1 bit shift operation cannot be performed on type float
 
~~~~~
//##470. bitwise operators and or xor and comp type erros need to be integrals
x470bitwiseoperatorsandorxorandcomptypeerrosneedtobeintegrals.conc line 9:6 bitwise operation cannot be performed on type boolean
x470bitwiseoperatorsandorxorandcomptypeerrosneedtobeintegrals.conc line 10:6 bitwise operation cannot be performed on type double
x470bitwiseoperatorsandorxorandcomptypeerrosneedtobeintegrals.conc line 11:6 bitwise operation cannot be performed on type float
x470bitwiseoperatorsandorxorandcomptypeerrosneedtobeintegrals.conc line 13:1 bitwise operation cannot be performed on type boolean
x470bitwiseoperatorsandorxorandcomptypeerrosneedtobeintegrals.conc line 14:1 bitwise operation cannot be performed on type double
x470bitwiseoperatorsandorxorandcomptypeerrosneedtobeintegrals.conc line 15:1 bitwise operation cannot be performed on type float
x470bitwiseoperatorsandorxorandcomptypeerrosneedtobeintegrals.conc line 17:6 Expected type integral not: boolean
x470bitwiseoperatorsandorxorandcomptypeerrosneedtobeintegrals.conc line 18:6 Expected type integral not: double
x470bitwiseoperatorsandorxorandcomptypeerrosneedtobeintegrals.conc line 19:6 Expected type integral not: float
 
~~~~~
//##471. asis - avoid accidental is cast
x471asisavoidaccidentaliscast.conc line 11:5 Unable to find method with matching name: athing
x471asisavoidaccidentaliscast.conc line 14:5 Unable to find method with matching name: athing
x471asisavoidaccidentaliscast.conc line 17:29 Unable to find method with matching name: athing
 
~~~~~
//##472. asis - not auto cast as short cirucuited
x472asisnotautocastasshortcirucuited.conc line 12:5 Unable to find method with matching name: athing
x472asisnotautocastasshortcirucuited.conc line 15:9 Unable to find method with matching name: athing
 
~~~~~
//##473. no valid expression list interpreatation
x473novalidexpressionlistinterpreatation.conc line 9:9 Unable to interpret entire expression list, best interpretations so far:~-> mc ...
 
~~~~~
//##474. ambigious expression list
x474ambigiousexpressionlist.conc line 11:11 Ambiguous expression list defined, more than one valid interpretation possible:~-> foo ( bar ( 4 ) )~-> foo ( bar ( ) , 4 )
 
~~~~~
//##475. unable to intepret return best match so far
x475unabletointepretreturnbestmatchsofar.conc line 13:13 Unable to interpret entire expression list, best interpretations so far:~-> getMc ( ) . afunc ( ) ...
 
~~~~~
//##476. no valid interp

 
~~~~~
//##477. matrix contatinations cannot vary by more than one ar level
x477matrixcontatinationscannotvarybymorethanonearlevel.conc line 8:6 Individual expressions being concatinated cannot vary by more than one array level
 
~~~~~
//##478. graceful failure on no ambiguate here
x478gracefulfailureonnoambiguatehere.conc line 4:10 b cannot be resolved to a variable
 
~~~~~
//##479. for compri with parfor and gate no no
x479forcompriwithparforandgatenono.conc line 5:7 block must return something
x479forcompriwithparforandgatenono.conc line 5:7 if statement must have else block when used in this way
x479forcompriwithparforandgatenono.conc line 5:25 if check can only be used for a 'for' list comprehension statement, not a 'parforsync'
 
~~~~~
//##480. list compri type error
x480listcompritypeerror.conc line 6:20 int is not a subtype of java.lang.String
 
~~~~~
//##481. horizontal concat fail

~~~~~
//##482. array dec may not have more than one null
x482arraydecmaynothavemorethanonenull.conc line 6:12 Only the last dimension(s) of an array constructor may be unqualified
 
~~~~~
//##483. array dec may not have more than one null 2
x483arraydecmaynothavemorethanonenull2.conc line 5:12 Only the last dimension(s) of an array constructor may be unqualified

~~~~~
//##483. array dec may not have more than one null 2
x484defaultvaluetypebeingincompatiblewithtahtprovidedshoulderr.conc line 9:5 Unable to find method with matching name: fella and arguments (double[])
 
~~~~~
//##485. invalid vectorization
x485invalidvectorization.conc line 7:7 Incompatible operand types int[] and int
 
~~~~~
//##486. bugfix on funcinvoke
x486bugfixonfuncinvoke.conc line 11:6 Unable to find method with matching name: thing
 
~~~~~
//##487. bugfix on funcinvoke 2
x487bugfixonfuncinvoke2.conc line 10:6 Unable to find method with matching name: thing

~~~~~
//##488. ambigious func invocation
x488ambigiousfuncinvocation.conc line 13:1 Ambiguous method detected 'foo'. More than one direct match made - due to ambiguous named parameter mapping

~~~~~
//##489. no double nested vect for you
x489nodoublenestedvectforyou.conc line 18:8 Array index operation can only be performed on object that is an array, Map, List or object having approperiate operator overloading 'get' method not: int
 
~~~~~
//##490. only left or right may be vect
x490onlyleftorrightmaybevect.conc line 17:15 Only arrays and lists can be vectorized, not: x490onlyleftorrightmaybevect.Myclass
 
~~~~~
//##491. field not accessible to thing of arraylevels though present on individual items

~~~~~
//##492. Only single ^ may be used for vectorized field operations
x492Onlysingle^maybeusedforvectorizedfieldoperations.conc line 14:7 Only single ^ may be used for vectorized field operations
 
~~~~~
//##493. Field referece cannot be vectorized at this location
x493Fieldreferececannotbevectorizedatthislocation.conc line 16:1 Expression cannot appear on its own line
 
~~~~~
//##494. Only single ^ may be used for vectorized new operations
x494Onlysingle^maybeusedforvectorizednewoperations.conc line 19:7 Only single ^ may be used for vectorized new operations
 
~~~~~
//##495. no more than one dot for auto vect
x495nomorethanonedotforautovect.conc line 16:8 Unable to find method with matching name: afunc
x495nomorethanonedotforautovect.conc line 17:9 Unable to find method with matching name: afunc
 
~~~~~
//##496. wtf is null in a list all by itself

~~~~~
//##497. cannot be just all empty arrays
x497cannotbejustallemptyarrays.conc line 4:8 At least one element of an array declaration must be non empty in order to be instantiable
x497cannotbejustallemptyarrays.conc line 5:8 At least one element of an array declaration must be non empty in order to be instantiable
 
~~~~~
//##498. missing commas
x498missingcommas.conc line 8:8 Unable to interpret entire expression list, best interpretations so far:~-> 0 ...
x498missingcommas.conc line 9:9 Unable to interpret entire expression list, best interpretations so far:~-> 0 ...
x498missingcommas.conc line 10:10 Unable to interpret entire expression list, best interpretations so far:~-> 0 ...
x498missingcommas.conc line 11:11 Unable to interpret entire expression list, best interpretations so far:~-> 0 ...
x498missingcommas.conc line 16:16 Unable to interpret entire expression list, best interpretations so far:~-> 0 ...
x498missingcommas.conc line 17:17 Unable to interpret entire expression list, best interpretations so far:~-> 0 ...
x498missingcommas.conc line 18:18 Unable to interpret entire expression list, best interpretations so far:~-> 0 ...
x498missingcommas.conc line 19:19 Unable to interpret entire expression list, best interpretations so far:~-> 0 ...
 
~~~~~
//##499. no array like this
x499noarraylikethis.conc line 4:13 At least one element of an array declaration must be non null in order to be instantiable
 
~~~~~
//##500. not supported nested null arrays
x500notsupportednestednullarrays.conc line 4:23 At least one element of an array declaration must be non null in order to be instantiable
x500notsupportednestednullarrays.conc line 5:13 At least one element of an array declaration must be non null in order to be instantiable
 
~~~~~
//##500. was a bug now its fine
x500wasabugnowitsfine.conc line 8:4 type int has fewer than 1 ref levels

~~~~~
//##501. was a bug now its fine on thing non vectable
x501wasabugnowitsfineonthingnonvectable.conc line 14:8 Only array and lists can be vectorized
 
~~~~~
//##502. used to blow up
x502usedtoblowup.conc line 12:17 Unable to resolve type corresponding to name: xx cannot be resolved to a variable
 
~~~~~
//##503. used to blow up now ok
x503usedtoblowupnowok.conc line 7:10 Unable to resolve type corresponding to name: HashMap<java.lang.Boolean, java.lang.Integer[]>
 
~~~~~
//##504. Only single hat may be used for vectorized array assignment operations
x504Onlysinglehatmaybeusedforvectorizedarrayassignmentoperations.conc line 8:3 Only single ^ may be used for vectorized array refernce operations

~~~~~
//##505. Vectorized array references may not make use  of the double hat operator ^^ denoting self vectorization
x505Vectorizedarrayreferencesmaynotmakeuseofthedoublehatoperator^^denotingselfvectorization.conc line 9:1 Expression cannot appear on its own line
x505Vectorizedarrayreferencesmaynotmakeuseofthedoublehatoperator^^denotingselfvectorization.conc line 9:1 Vectorized array references may not make use  of the double hat operator ^^ denoting self vectorization
x505Vectorizedarrayreferencesmaynotmakeuseofthedoublehatoperator^^denotingselfvectorization.conc line 10:1 Expression cannot appear on its own line
x505Vectorizedarrayreferencesmaynotmakeuseofthedoublehatoperator^^denotingselfvectorization.conc line 10:1 Vectorized array references may not make use  of the double hat operator ^^ denoting self vectorization
 
~~~~~
//##506. Vectorized array ref errors
x506Vectorizedarrayreferrors.conc line 8:1 Vectorized epxression must have more than: 1 levels of dimentionality, it has: 1
x506Vectorizedarrayreferrors.conc line 9:1 Only single ^ may be used for vectorized array operations
x506Vectorizedarrayreferrors.conc line 10:5 The vectorized arguments of a vectorized array reference may have only one degree of dimentionality
 
~~~~~
//##507. lhs rhs of range
 
~~~~~
//##508. bugfix join not exist
x508bugfixjoinnotexist.conc line 5:7 Unable to find method with matching name: join and arguments (java.lang.String, java.lang.Object[])
x508bugfixjoinnotexist.conc line 13:4 Unable to find method with matching name: join and arguments (java.lang.String, char, int[], int[], java.util.ArrayList<java.lang.Object>)
 
~~~~~
//##509. class refs cant be used like this
x509classrefscantbeusedlikethis.conc line 5:19 System cannot be resolved to a variable
 
~~~~~
//##509. onchange no refs to monitor
x510onchangenorefstomonitor.conc line 8:6 At least one ref to monitor must be specified for onchange

~~~~~
//##511. funcInvoke and args differing levels
x511funcInvokeandargsdifferinglevels.conc line 15:9 Type array levels don't match. Expected: 0 vs 1
 
~~~~~
//##512. correct error on long used as array index
 
~~~~~
//##513. used to blow up
x513usedtoblowup.conc line 23:19 Unable to find method with matching name: makeOffHeapArrayIn and arguments (int)
x513usedtoblowup.conc line 24:19 Unable to find method with matching name: makeOffHeapArrayOut and arguments (int)
x513usedtoblowup.conc line 25:19 Unable to find method with matching name: makeOffHeapArrayMixed and arguments (int)
x513usedtoblowup.conc line 27:31 Expected Iterable object or array for list comprehension not: void
 
~~~~~
//##514. used to blow up 2

~~~~~
//##515. bugfix on missing refname rhs of dot
x515bugfixonmissingrefnamerhsofdot.conc line 12:11 ad cannot be resolved to a variable
 
~~~~~
//##516. better error message for import override
x516bettererrormessageforimportoverride.conc line 6:1 variable name 'String' overrides an existing type name imported from: 'java.lang.String' - this is confusing
 
~~~~~
//##517. inf loop mitigation
x517infloopmitigation.conc line 7:7 No valid interpretation of expression list available
x517infloopmitigation.conc line 8:2 Expression cannot appear on its own line
 
~~~~~
//##518. DeleteOnUnusedReturn annotation can only be used on methods which
x518DeleteOnUnusedReturnannotationcanonlybeusedonmethodswhich.conc line 20:0 @DeleteOnUnusedReturn annotation can only be used on methods which return non array objects 'invalid1' does not
x518DeleteOnUnusedReturnannotationcanonlybeusedonmethodswhich.conc line 25:0 @DeleteOnUnusedReturn annotation can only be used on methods which return non array objects 'invalid2' does not
 
~~~~~
//##519. forced assign operator
x519forcedassignoperator.conc line 4:1 Only = may be used for new variable assignment not: \=
x519forcedassignoperator.conc line 5:1 Assignment type: \= not permitted for new variable
 
~~~~~
//##520. Sublist cannot be operated on
x520Sublistcannotbeoperatedon.conc line 11:4 Sublist cannot be operated on. No overloaded 'subAssign' operator found for type x520Sublistcannotbeoperatedon.MyClass with signature: '(int, int, java.lang.String)'
 
~~~~~
//##521. sizeof with variant
x521sizeofwithvariant.conc line 30:3 Unable to find method with matching name: nope
x521sizeofwithvariant.conc line 31:3 qualifier for sizeof must return an int
x521sizeofwithvariant.conc line 32:3 qualifier for sizeof must return an int
 
~~~~~
//##522. something to non local ref implicit not permitted

~~~~~
//##523. gpu restrictions in satc
x523gpurestrictionsinsatc.conc line 9:10 kernel dimentions must be a number between 1 and 3
x523gpurestrictionsinsatc.conc line 14:0 gpu kernel functions may only return type void, not: int
x523gpurestrictionsinsatc.conc line 19:0 gpu kernel functions may only return type void, not: int
x523gpurestrictionsinsatc.conc line 25:1 gpu kernels or gpu functions may not be defined within classes
x523gpurestrictionsinsatc.conc line 31:1 gpu kernels or gpu functions may not be nested
x523gpurestrictionsinsatc.conc line 37:0 gpu kernel or gpu functions may not have annotations
x523gpurestrictionsinsatc.conc line 41:0 gpu kernels or gpu functions may not be extension functions
x523gpurestrictionsinsatc.conc line 45:21 gpu parameter may not have default value
x523gpurestrictionsinsatc.conc line 49:21 gpu parameter may not be a vararg
x523gpurestrictionsinsatc.conc line 53:21 all parameters to gpu functions or kernels must be of primative type
x523gpurestrictionsinsatc.conc line 65:13 gpu qulifier is only applicable to gpu kernels or gpu functions
x523gpurestrictionsinsatc.conc line 70:1 super references can only be made within a class or extension function not gpu functions or kernels
x523gpurestrictionsinsatc.conc line 74:9 Unable to find method with matching name: acall
x523gpurestrictionsinsatc.conc line 77:21 parameters to gpu functions or kernels may not have annotations
x523gpurestrictionsinsatc.conc line 82:1 Unable to find method with matching name: myFunc17 and arguments (int)
x523gpurestrictionsinsatc.conc line 88:1 When invoking within a gpu kernal or gpu function only gpu kernels or gpu functions can be called, okFunc is neither


~~~~~
//##524. gpu restrictions in satc - part 2
x524gpurestrictionsinsatcpart2.conc line 5:1 functions may not be nested within gpu kernels or gpu functions
x524gpurestrictionsinsatcpart2.conc line 11:1 class definition may not be used within a gpudef or gpukernel
 
~~~~~
//##525. ensure that precompile classes abstract methods correctly captured
x525ensurethatprecompileclassesabstractmethodscorrectlycaptured.conc line 6:0 Class 'x525ensurethatprecompileclassesabstractmethodscorrectlycaptured.TrueHolder' is missing implementations of abstract method definitions inherited: com.concurnas.lang.precompiled.SimpleAbstractClass:{def getThing() java.lang.String}
 
~~~~~
//##526. private class cannot be used like this
x526privateclasscannotbeusedlikethis.conc line 7:7 MyAbstract has been marked private
 
~~~~~
//##527. in out only with global
x527inoutonlywithglobal.conc line 4:54 gpu parameter may not be qualified with in as it's not marked global
x527inoutonlywithglobal.conc line 6:22 Invalid operator in expression involving pointer
 
~~~~~
//##528. what can be tagged as a stub function
x528whatcanbetaggedasastubfunction.conc line 16:0 gpudef stub functions or kernels can only be abstract
x528whatcanbetaggedasastubfunction.conc line 21:0 gpudef stub functions or kernels can only be abstract
 
~~~~~
//##529. gpu kernel ambiguity
x529gpukernelambiguity.conc line 10:0 Function myfunc1 with matching argument definition exists already in current Scope
x529gpukernelambiguity.conc line 14:0 Function myfunc1 with matching argument definition exists already in current Scope
x529gpukernelambiguity.conc line 18:0 Function myfunc1 with matching argument definition exists already in current Scope
 
~~~~~
//##530. gpu kerenel type erasure
x530gpukereneltypeerasure.conc line 23:1 Type mismatch: cannot convert from long to int
x530gpukereneltypeerasure.conc line 26:0 Function myFunc with matching argument definition exists already in current Scope - generic types are erased at runtime
 
~~~~~
//##531. was a bug now its fine
x531wasabugnowitsfine.conc line 44:8 Unable to find method with matching name: matMult and arguments (int, int, int, com.concurnas.lang.gpus$GPUBufferManagedInput<float[2]>, com.concurnas.lang.gpus$GPUBufferManagedInput<float[2]>, com.concurnas.lang.gpus$GPUBufferManagedOutput<float[2]>)
x531wasabugnowitsfine.conc line 45:19 Unable to find method with matching name: exe and arguments (def (int, int, int, com.concurnas.lang.GPUBufferInput<float[]>, com.concurnas.lang.GPUBufferInput<float[]>, com.concurnas.lang.GPUBufferOutput<float[]>) com.concurnas.lang.gpus$Kernel, int[], null, java.lang.Boolean or java.lang.Boolean:com.concurnas.lang.gpus$GPURef, java.lang.Boolean or java.lang.Boolean:com.concurnas.lang.gpus$GPURef)
x531wasabugnowitsfine.conc line 48:14 Unable to find method with matching name: readFromBuffer and arguments (void)
x531wasabugnowitsfine.conc line 53:5 Invalid type void
 
~~~~~
//##532. gens
x532gens.conc line 8:30 Generic Type argument type mismatch: java.lang.Object vs java.lang.String
 
~~~~~
//##533. restrictions with arrays in gpus
x533restrictionswitharraysingpus.conc line 6:16 unequal n dimentional array subarray instantiation may not be used within a gpudef or gpukernel
x533restrictionswitharraysingpus.conc line 14:10 unequal n dimentional array subarray instantiation may not be used within a gpudef or gpukernel
x533restrictionswitharraysingpus.conc line 15:1 non primative types may not be used within a gpudef or gpukernel
x533restrictionswitharraysingpus.conc line 24:1 Array items must be individually assigned when used within a gpudef or gpukernel
 
~~~~~
//##534. use of global local etc
x534useofgloballocaletc.conc line 4:1 Variable a can only be qualified with global inside a gpu kernel or function
x534useofgloballocaletc.conc line 5:1 Variable b can only be qualified with global inside a gpu kernel or function
x534useofgloballocaletc.conc line 6:1 Qualifier: global can only be used for new variables
 
~~~~~
//##535. local array with initalizer
x535localarraywithinitalizer.conc line 8:1 local varaible array instantiation may not be used within a gpudef or gpukernel
x535localarraywithinitalizer.conc line 9:1 local varaible array instantiation may not be used within a gpudef or gpukernel
 
~~~~~
//##536. global variables may only be of pointer type
x536globalvariablesmayonlybeofpointertype.conc line 7:1 global variables may only be of pointer type
x536globalvariablesmayonlybeofpointertype.conc line 8:1 global variables may only be of pointer type
 
~~~~~
//##537. no const reassign
x537noconstreassign.conc line 7:1 GPU constant variable: v1 may only be defined at top level
x537noconstreassign.conc line 8:1 GPU constant variable: v1a may only be defined at top level
x537noconstreassign.conc line 10:1 GPU constant variable: v2 may only be defined at top level
x537noconstreassign.conc line 11:1 GPU constant variable: v2a may only be defined at top level
 
~~~~~
//##538. size_t and pointer useage
x538size_tandpointeruseage.conc line 6:15 Pointers cannot be used outside of gpu functions or gpu kernels
x538size_tandpointeruseage.conc line 7:7 Pointers cannot be used outside of gpu functions or gpu kernels
x538size_tandpointeruseage.conc line 9:5 Pointer address operator & can only be used within gpu kernels or gpu functions
x538size_tandpointeruseage.conc line 10:3 size_t cannot be used outside of gpu functions or gpu kernels
x538size_tandpointeruseage.conc line 11:8 Pointer address operator & can only be used within gpu kernels or gpu functions
x538size_tandpointeruseage.conc line 12:8 Pointer address dereferencing ** can only be used within gpu kernels or gpu functions
x538size_tandpointeruseage.conc line 18:16 incompatible type: long vs com.concurnas.bootstrap.lang.Lambda$Function0<java.lang.Integer>
x538size_tandpointeruseage.conc line 19:15 incompatible type: int vs com.concurnas.bootstrap.lang.Lambda$Function0<java.lang.Integer>
x538size_tandpointeruseage.conc line 20:15 incompatible type: private *size_t vs com.concurnas.bootstrap.lang.Lambda$Function0<java.lang.Integer>
x538size_tandpointeruseage.conc line 21:8 Attempting to dereference a pointer type with: 1 levels by: 2 levels
x538size_tandpointeruseage.conc line 23:8 Attempting to dereference type which is not a pointer: int
x538size_tandpointeruseage.conc line 32:9 Invalid operator in expression involving pointer
x538size_tandpointeruseage.conc line 45:9 Only one pointer may be included in an expression involving pointer arithmetic
x538size_tandpointeruseage.conc line 57:3 Invalid operator: - in expression involving pointer
x538size_tandpointeruseage.conc line 73:1 Invalid operator in expression involving pointer
 
~~~~~
//##539.pointer type muyst match
x539pointertypemuystmatch.conc line 9:13 Type mismatch: cannot convert from private *float to private *int
 
~~~~~
//##540. always check lhs on assignment
x540alwayschecklhsonassignment.conc line 10:1 Attempting to dereference type which is not a pointer: int
 
~~~~~
//##541. no array init on its own
x541noarrayinitonitsown.conc line 7:1 Array items must be individually assigned when used within a gpudef or gpukernel
x541noarrayinitonitsown.conc line 7:7 array definitions outside of array initalizers may not be used within a gpudef or gpukernel
 
~~~~~
//##542. Cannot combine differing degrees of pointer type
x542Cannotcombinedifferingdegreesofpointertype.conc line 7:7 Cannot combine differing degrees of pointer type
x542Cannotcombinedifferingdegreesofpointertype.conc line 8:11 Unable to find method with matching name: tf
 
~~~~~
//##543. custom refs ensure correct subtyping
x543customrefsensurecorrectsubtyping.conc line 16:17 Type mismatch: cannot convert from boolean: to java.lang.Boolean:x543customrefsensurecorrectsubtyping.MyRef
 
~~~~~
//##544. custom refs ensure correct subtyping
x544customrefsensurecorrectsubtyping.conc line 9:1 GPU memory space mismatch, variables must be declared in the same memory space: local <> global
x544customrefsensurecorrectsubtyping.conc line 10:1 GPU memory space mismatch, variables must be declared in the same memory space: local <> global
 
~~~~~
//##545. memory space control
x545memoryspacecontrol.conc line 6:1 GPU memory space mismatch, variables must be declared in the same memory space: local <> private
 
~~~~~
//##546. memory space control 2
x546memoryspacecontrol2.conc line 7:10 Unable to find method with matching name: ss
 
~~~~~
//##547. more gpu restrictions
x547moregpurestrictions.conc line 10:1 **= may not be used within a gpudef or gpukernel
x547moregpurestrictions.conc line 11:1 mod= with floating point variables may not be used within a gpudef or gpukernel
x547moregpurestrictions.conc line 12:1 mod= with floating point variables may not be used within a gpudef or gpukernel
 
~~~~~
//##548. module restructions on class packages etc
com\myorg\code.conc line 31:20 The constructor is not visible
com\myorg\code.conc line 34:23 The constructor is not visible
com\myorg\code.conc line 43:12 The constructor is not visible
com\myorg\code2.conc line 13:20 The constructor is not visible
com\myorg\code2.conc line 19:23 The constructor is not visible
com\myorg\code2.conc line 20:40 The constructor is not visible
com\myorg\code2.conc line 29:8 Invalid type void
com\myorg\more\package\code.conc line 11:4 Unable to resolve type corresponding to name: PrivClass
com\myorg\more\package\code.conc line 12:5 Unable to resolve type corresponding to name: PrivClass
com\myorg\more\package\code.conc line 14:14 PackageClass has been marked package
com\myorg\more\package\code.conc line 15:15 PackageClass has been marked package
com\myorg\code.conc line 31:20 The constructor is not visible
com\myorg\code.conc line 34:23 The constructor is not visible
com\myorg\code.conc line 43:12 The constructor is not visible
com\myorg\code2.conc line 13:20 The constructor is not visible
com\myorg\code2.conc line 19:23 The constructor is not visible
com\myorg\code2.conc line 20:40 The constructor is not visible
com\myorg\code2.conc line 29:8 Invalid type void
com\myorg\more\package\code.conc line 11:4 Unable to resolve type corresponding to name: PrivClass
com\myorg\more\package\code.conc line 12:5 Unable to resolve type corresponding to name: PrivClass
com\myorg\more\package\code.conc line 14:14 PackageClass has been marked package
com\myorg\more\package\code.conc line 15:15 PackageClass has been marked package
com\myorg\code.conc line 31:20 The constructor is not visible
com\myorg\code.conc line 34:23 The constructor is not visible
com\myorg\code.conc line 43:12 The constructor is not visible
com\myorg\code2.conc line 13:20 The constructor is not visible
com\myorg\code2.conc line 19:23 The constructor is not visible
com\myorg\code2.conc line 20:40 The constructor is not visible
com\myorg\code2.conc line 29:8 Invalid type void
com\myorg\more\package\code.conc line 11:4 Unable to resolve type corresponding to name: PrivClass
com\myorg\more\package\code.conc line 12:5 Unable to resolve type corresponding to name: PrivClass
com\myorg\more\package\code.conc line 14:14 PackageClass has been marked package
com\myorg\more\package\code.conc line 15:15 PackageClass has been marked package
 
~~~~~
//##549. better checks on array assignment
x549betterchecksonarrayassignment.conc line 8:1 Type mismatch: cannot convert from int[] to java.lang.Integer
x549betterchecksonarrayassignment.conc line 9:1 Type array levels don't match. Expected: 0 vs 1
 
~~~~~
//##550. this should fail as wrong type
x550thisshouldfailaswrongtype.conc line 9:8 Unable to find method with matching name: myMethod and arguments (java.util.ArrayList<java.lang.Integer>)

~~~~~
//##551. better error message
x551bettererrormessage.conc line 11:1 The return type of method 'def iterator() java.util.Iterator<T>' in class x551bettererrormessage.IntSequence cannot be matched with method: 'def iterator() java.util.Iterator<java.lang.Integer>' in superclass: java.lang.Object or traits: java.lang.Iterable
x551bettererrormessage.conc line 12:8 Local class is missing implementations of abstract method definitions inherited: java.util.Iterator:{def hasNext() boolean, def next() java.lang.Integer}
x551bettererrormessage.conc WARN line 6:0 Class name overwrites imported class: IntSequence
 
~~~~~
//##552. to used to blow up
x552tousedtoblowup.conc line 7:8 Only arrays and lists can be vectorized, not: com.concurnas.lang.ranges$IntSequence
 
~~~~~
//##553. gpu multi assign restrictions
x553gpumultiassignrestrictions.conc line 8:9 assignors in multi assign statements other than = may not be used within a gpudef or gpukernel
x553gpumultiassignrestrictions.conc line 9:1 global may not be used in a multi assign
x553gpumultiassignrestrictions.conc line 9:1 global variables may only be of pointer type
x553gpumultiassignrestrictions.conc line 9:12 global may not be used in a multi assign
x553gpumultiassignrestrictions.conc line 9:12 global variables may only be of pointer type
 
~~~~~
//##554. tuples no more than 24 
x554tuplesnomorethan24.conc line 5:5 Maxium number of elements for a tuple is: 24, provided tuple has: 26 elements
 
~~~~~
//##555. tuples rhs must be tuple in decomp
x555tuplesrhsmustbetupleindecomp.conc line 5:1 Right hand side of tuple decomposition assignment must be a tuple, not: int
 
~~~~~
//##556. tuples rhs no
x556tuplesrhsno.conc line 5:2 a cannot be resolved to a variable
x556tuplesrhsno.conc line 6:2 c cannot be resolved to a variable
 
~~~~~
//##557. no tuples in gpu 
x557notuplesingpu.conc line 7:5 tuples may not be used within a gpudef or gpukernel
x557notuplesingpu.conc line 8:1 tuple decomposition may not be used within a gpudef or gpukernel
x557notuplesingpu.conc line 13:2 non primative types may not be used within a gpudef or gpukernel
 
~~~~~
//##558. anon lambda ret type incompatible
 
~~~~~
//##558. anon lambda ret type incompatible
x559anonlambdarettypeincompatible2.conc line 13:23 Generic Type argument type mismatch: java.lang.Long vs java.lang.Integer

~~~~~
//##560. anon lambda in gpu
x560anonlambdaingpu.conc line 5:20 lambdas may not be used within a gpudef or gpukernel
 
~~~~~
//##561. lambdas wrong rhs type
x561lambdaswrongrhstype.conc line 7:32 Unable to infer type for lambda function
x561lambdaswrongrhstype.conc line 8:32 com.concurnas.bootstrap.lang.Lambda$Function1<java.lang.Double, java.lang.Double> is not a subtype of com.concurnas.lang.precompiled.CompiledSAMFunc$MySAM
 
~~~~~
//##562. fields missing on copy of precompile type
x562fieldsmissingoncopyofprecompiletype.conc line 16:13 attempting to override field: c which does not exist
x562fieldsmissingoncopyofprecompiletype.conc line 17:4 attempting to override field: a which does not exist
 
~~~~~
//##563. copy specification on copiers
x563copyspecificationoncopiers.conc line 18:11 copier specification can only be applied to object types
x563copyspecificationoncopiers.conc line 19:19 field: a may be copied only once in copier
x563copyspecificationoncopiers.conc line 20:6 Field: ff declared in include specification of copier does not exist
x563copyspecificationoncopiers.conc line 21:13 cannot assign value of type: java.lang.String to override field: a of type: int
x563copyspecificationoncopiers.conc line 22:6 Duplicate field names: a declared in exclude specification of copier
x563copyspecificationoncopiers.conc line 23:6 Field: ff declared in exclude specification of copier does not exist
x563copyspecificationoncopiers.conc line 24:6 Fields in copier cannot be present for both inclusion and exclusion: a
x563copyspecificationoncopiers.conc line 25:6 Field: a declared in exclude specification of copier cannot also have an override or copier defined
x563copyspecificationoncopiers.conc line 26:6 Field: a declared in exclude specification of copier cannot also have an override or copier defined
x563copyspecificationoncopiers.conc line 27:7 only one exclude specification may be included in a copier
x563copyspecificationoncopiers.conc line 28:7 Field: ff with copier does not exist
x563copyspecificationoncopiers.conc line 29:7 Only one super copier may be defined
x563copyspecificationoncopiers.conc line 30:7 unknown keyword in copier: sdfsdf, dfgfdg
x563copyspecificationoncopiers.conc line 31:7 unchecked may not be repeated
x563copyspecificationoncopiers.conc line 32:7 nodefault may not be repeated
 
~~~~~
//##564. incorrectly qualified supertype generic having an upper bound
x564incorrectlyqualifiedsupertypegenerichavinganupperbound.conc line 9:0 Generic parameter qualifcation mismatch: 'X Number' cannot be qualified with: '? java.lang.Object'
x564incorrectlyqualifiedsupertypegenerichavinganupperbound.conc line 9:0 Generic type refernce at index: 0 is not compatible with superclass definiton. X Number is not equal to or a subtype of XX
x564incorrectlyqualifiedsupertypegenerichavinganupperbound.conc line 10:0 Generic type refernce at index: 0 is not compatible with superclass definiton. X Number is not equal to or a subtype of XX String
x564incorrectlyqualifiedsupertypegenerichavinganupperbound.conc line 11:0 Generic type refernce at index: 0 is not compatible with superclass definiton. X Number is not equal to or a subtype of java.lang.String
 
~~~~~
//##565. qualifciation of generic types with upper bound
x565qualifciationofgenerictypeswithupperbound.conc line 10:9 Generic parameter qualifcation mismatch: 'X Number' cannot be qualified with: 'java.lang.String'
 
~~~~~
//##566. qualify local generics check upper bound
x566qualifylocalgenericscheckupperbound.conc line 9:6 Unable to find method with matching name: myThing and arguments (java.lang.String)
x566qualifylocalgenericscheckupperbound.conc line 10:6 Unable to find method with matching name: myThing and arguments (java.lang.String)
 
~~~~~
//##566. qualify local generics check upper bound
x567wildcardgenerics.conc line 14:24 Generic Type argument type mismatch: java.lang.Number vs java.lang.Integer
 
~~~~~
//##568. transient implies new
x568transientimpliesnew.conc line 6:1 Variable myLists has already been defined in current scope

~~~~~
//##569. shared params may not be primatives
x569sharedparamsmaynotbeprimatives.conc line 4:9 shared variables may not be of non array primative type
x569sharedparamsmaynotbeprimatives.conc line 6:0 shared variables may not be of non array primative type
x569sharedparamsmaynotbeprimatives.conc line 6:0 shared variables of primative type must also be of array type
x569sharedparamsmaynotbeprimatives.conc line 9:1 shared variables of primative type must also be of array type
 
~~~~~
//##570. with expression must be an Object type
x570withexpressionmustbeanObjecttype.conc line 5:13 with expression must be an Object type

~~~~~
//##571. all return statements must return a value
x571allreturnstatementsmustreturnavalue.conc line 5:0 all return statements must return a value

~~~~~
//##572. all return statements must return a value
x572allreturnstatementsmustreturnavalue.conc line 45:45 Unable to interpret entire expression list, best interpretations so far:~-> _ ...
x572allreturnstatementsmustreturnavalue.conc line 46:6 Unable to find method with matching number of arguments with name: got
 
~~~~~
//##573. used to complain about incorrect line 
x573usedtocomplainaboutincorrectline.conc line 6:9 java.lang.Integer is not a subtype of java.lang.String
 
~~~~~
//##574. tuple typedef use
x574tupletypedefuse.conc line 7:19 Type mismatch: cannot convert from com.concurnas.lang.tuples$Tuple2<java.lang.Integer, java.lang.Comparable<? java.lang.Object>>[] to com.concurnas.lang.tuples$Tuple2<java.lang.Integer, java.lang.Integer>[]
 
~~~~~
//##575. tuple size mismatch on match stmt
x575tuplesizemismatchonmatchstmt.conc line 6:3 Cannot extract tuple components, expected: 3 elements but there are: 2
 
~~~~~
//##576. no match on differing tuple type
x576nomatchondifferingtupletype.conc line 6:2 case will never be matched as com.concurnas.lang.tuples$Tuple3<java.lang.Integer, java.lang.Integer, java.lang.Integer> and com.concurnas.lang.tuples$Tuple2<java.lang.Integer, java.lang.Integer> are not subtypes of each other
 
~~~~~
//##577. tuple size mismatch on match stmt pt 2
x577tuplesizemismatchonmatchstmtpt2.conc line 5:2 Cannot extract tuple components, expected: 3 elements but there are: 2
 
~~~~~
//##578. no actors on classrefs 
x578noactorsonclassrefs.conc line 12:0 Cannot create actor on non type entity
x578noactorsonclassrefs.conc line 12:8 Unable to resolve reference to constructor for: 'DefaultActor$0'(java.lang.Class<? java.lang.Object>[], def (*) localClassDef$0)
x578noactorsonclassrefs.conc line 16:11 Unable to find method with matching name: operate

~~~~~
//##579. no clever calls inside this or super invocation
x579noclevercallsinsidethisorsuperinvocation.conc line 14:16 Cannot refer to an instance method while explicitly invoking a constructor
x579noclevercallsinsidethisorsuperinvocation.conc line 17:12 this references cannot be whilst invoking this or super constructors
x579noclevercallsinsidethisorsuperinvocation.conc line 20:2 Unable to find method with matching name: constructor and arguments (java.lang.Object)
x579noclevercallsinsidethisorsuperinvocation.conc line 20:18 Cannot refer to an instance method while explicitly invoking a constructor
 
~~~~~
//##580. no lazy in super
x580nolazyinsuper.conc line 12:28 lazy variable qualification may not be defined in super or this constructor calls

~~~~~
//##581. ambigious lazy

~~~~~
//##582. asunc executor must be of approperiate type
x582asuncexecutormustbeofapproperiatetype.conc line 5:6 Unable to find method with matching name: com.concurnas.lang.dist.Remote
x582asuncexecutormustbeofapproperiatetype.conc line 6:7 Asynchonous executor must be a subtype of com.concurnas.runtime.cps.ISOExecutor: int is not
 
~~~~~
//##583. await cannot nest ref related items
 
~~~~~
//##584. bug on match with leg missing returns
x584bugonmatchwithlegmissingreturns.conc line 14:15 Return type expected
x584bugonmatchwithlegmissingreturns.conc line 14:27 Return type expected
 
~~~~~
//##585. double declare
x585doubledeclare.conc line 5:0 Variable var1 has already been defined in current scope
 
~~~~~
//##586. prohibited primative type casting
x586prohibitedprimativetypecasting.conc line 3:13 Cannot convert boxed primative types: java.lang.Double to java.lang.Integer

~~~~~
//##587. class marked shared no copy for you
x587classmarkedsharednocopyforyou.conc line 14:5 Cannot copy object as its class or superclass has been marked shared
x587classmarkedsharednocopyforyou.conc line 18:6 Cannot copy object as its class or superclass has been marked shared
 
~~~~~
//##588. shared trait no copy for you
x588sharedtraitnocopyforyou.conc line 12:5 Cannot copy object as its class or superclass has been marked shared
 
~~~~~
//##589. blow up on wrong name
x589blowuponwrongname.conc line 13:46 com.concurnas.lang.xxconcurrent cannot be resolved to a variable

~~~~~
//##590. used to blow up
x590usedtoblowup.conc line 11:11 No valid interpretation of expression list available

~~~~~
//##591. no copy actor
x591nocopyactor.conc line 9:8 Cannot copy object as its class or superclass has been marked shared

~~~~~
//##592. missing Annot
x592missingAnnot.conc line 11:0 Unable to resolve type corresponding to name: Annot

~~~~~
//##593. used to blow up now its fine
x593usedtoblowupnowitsfine.conc line 6:35 Unable to resolve type corresponding to name: ProtectionDomain
x593usedtoblowupnowitsfine.conc line 7:2 Unable to determine type of: PermissionCollection
x593usedtoblowupnowitsfine.conc line 10:24 Unable to resolve type corresponding to name: ProtectionDomain
x593usedtoblowupnowitsfine.conc line 14:36 Unable to resolve type corresponding to name: PermissionCollection
x593usedtoblowupnowitsfine.conc line 15:26 Unable to resolve type corresponding to name: Permissions
x593usedtoblowupnowitsfine.conc line 16:8 Unable to determine type of: PermissionCollection
x593usedtoblowupnowitsfine.conc line 19:41 Unable to resolve type corresponding to name: PermissionCollection
x593usedtoblowupnowitsfine.conc line 20:26 Unable to resolve type corresponding to name: Permissions
x593usedtoblowupnowitsfine.conc line 21:28 Unable to resolve type corresponding to name: AllPermission
x593usedtoblowupnowitsfine.conc line 22:8 Unable to determine type of: PermissionCollection

~~~~~
//##594. correct type inferece
x594correcttypeinferece.conc line 9:8 Generic parameter qualifcation mismatch: 'X Achi, Y Achi' cannot be qualified with: 'x594correcttypeinferece.Bsup, x594correcttypeinferece.Bsup'
x594correcttypeinferece.conc line 10:6 Generic parameter count of: 0 does not equal: 2 - generic type parameters must be defined
 
~~~~~
//##595. correct type inferece cannot use here
x595correcttypeinferececannotusehere.conc line 6:17 java.util.ArrayList<java.lang.String> is not a subtype of x595correcttypeinferececannotusehere.Thing
  
~~~~~
//##596. usage qualification even when lhs of new is not helpful
x596usagequalificationevenwhenlhsofnewisnothelpful.conc line 11:3 Unable to find method with matching name: add
  
~~~~~
//##597. gen type inference can only do one
x597gentypeinferencecanonlydoone.conc line 16:13 Generic parameter count of: 0 does not equal: 2 - generic type parameters must be defined
  
~~~~~
//##598. used to blow up
x598usedtoblowup.conc line 13:21 type cannot be used in this way
 
~~~~~
//##599. used to blow up
x599usedtoblowup.conc line 14:23 a cannot be resolved to a variable

~~~~~
//##600. no import all if thing does not exist
x600noimportallifthingdoesnotexist.conc line 3:0 Cannot import all assets from: com.myorg.code2 as it cannot be resolved to a path

~~~~~
//##601. no same path name
com\myorg\code2.conc line 2:0 Cannot import all assets from: com.myorg.code2 as path name is the same as where it is defined

~~~~~
//##602. validate onchange every options
x602validateonchangeeveryoptions.conc line 6:38 duplicate options specified: onlyclose
x602validateonchangeeveryoptions.conc line 11:41 invalid options specified: onlycloseasd - valid options are: "onlyclose"

~~~~~
//##603. in qualification cannot be used as output param
x603inqualificationcannotbeusedasoutputparam.conc line 7:11 Method: get is not callable as generic return type: in java.lang.String has been qualified as in type and so cannot be used as method output

~~~~~
//##604. smart casts respect this.me vs thing.me vs me
x604smartcastsrespectthismevsthingmevsme.conc line 12:11 Unable to find method with matching name: hi

~~~~~
//##605. trait needs to map generic types
x605traitneedstomapgenerictypes.conc line 6:15 numerical operation cannot be performed on type java.lang.String. No overloaded 'compareTo' operator found for type java.lang.String with signature: '(int) int'
 
~~~~~
//##606. trait needs to map generic types - vectorized version
x606traitneedstomapgenerictypesvectorizedversion.conc line 6:12 numerical operation cannot be performed on type java.lang.String[]. No overloaded 'compareTo' operator found for type java.lang.String[] with signature: '(int) int'
 
~~~~~
//##607. errors concerning gpu constants
x607errorsconcerninggpuconstants.conc line 5:0 constant may not be used in a multi assign
x607errorsconcerninggpuconstants.conc line 5:20 constant may not be used in a multi assign
 
~~~~~
//##608. errors concerning gpu constants - 2
x608errorsconcerninggpuconstants2.conc line 8:21 asdas is a gpu function, these can only be invoked from a gpu kernel or function
x608errorsconcerninggpuconstants2.conc line 12:1 GPU constant variable: hh may only be defined at top level
x608errorsconcerninggpuconstants2.conc line 17:1 GPU constant variable: hh may only be defined at top level
 
~~~~~
//##609. constant like val no reassign
x609constantlikevalnoreassign.conc line 6:1 GPU constant variable: tstdsf may only be defined at top level
x609constantlikevalnoreassign.conc line 10:1 Variable sdf has been decalred as constant and cannot be reassigned
x609constantlikevalnoreassign.conc line 14:1 Variable sdf has been decalred as constant and cannot be reassigned

~~~~~
//##610. import star scopes
x610importstarscopes.conc line 8:8 Unable to resolve type corresponding to name: ArrayList

~~~~~
//##611. top level del on func
x611topleveldelonfunc.conc line 4:4 thing resolves to something other than a local variable

~~~~~
//##612. missing typed used to blow up
x612missingtypedusedtoblowup.conc line 11:9 Unable to find method with matching name: BigDecimal
x612missingtypedusedtoblowup.conc line 18:10 Unable to find method with matching name: constructor and arguments (java.time.LocalDateTime, void)