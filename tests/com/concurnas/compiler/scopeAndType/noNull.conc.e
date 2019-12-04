//##1. basic null types and assignment
x1basicnulltypesandassignment.conc line 4:1 Assingment can be null, but assignment type is not nullable
x1basicnulltypesandassignment.conc line 5:1 Assingment can be null, but assignment type is not nullable
x1basicnulltypesandassignment.conc line 13:1 Assingment can be null, but assignment type is not nullable
x1basicnulltypesandassignment.conc line 16:1 Assingment can be null, but assignment type is not nullable
 
~~~~~
//##2. branching logic etc
x2branchinglogicetc.conc line 6:1 Assingment can be null, but assignment type is not nullable
x2branchinglogicetc.conc line 8:1 Assingment can be null, but assignment type is not nullable
 
~~~~~
//##3. assign fuctype and tuples
x3assignfuctypeandtuples.conc line 4:1 Assingment can be null, but assignment type is not nullable
x3assignfuctypeandtuples.conc line 7:1 Assingment can be null, but assignment type is not nullable

~~~~~
//##4. nullable passed to function
x4nullablepassedtofunction.conc line 12:12 Argument 1 passed to method (of funcNoNull) can be null (null), but method argument type([java.lang.String]) is not nullable
 
~~~~~
//##5. nullable funcref
x5nullablefuncref.conc line 12:15 Argument passed to method reference can be null, but method argument type is not nullable

~~~~~
//##6. nullable funcref 2
x6nullablefuncref2.conc line 12:15 Argument passed to method reference can be null, but method argument type is not nullable
 
~~~~~
//##7. nullable funcref 3
x7nullablefuncref3.conc line 12:16 Argument passed to method reference can be null, but method argument type is not nullable
 
~~~~~
//##8. nullable constructor
x8nullableconstructor.conc line 13:3 Argument 1 passed to constructor (of x8nullableconstructor.MyClass) can be null (null), but constructor argument type(java.lang.String) is not nullable
  
~~~~~
//##9. nullable constructor ref
x9nullableconstructorref.conc line 13:12 Argument passed to method reference can be null, but method argument type is not nullable
 
~~~~~
//##10. plus operands cannot be nullable
x10plusoperandscannotbenullable.conc line 8:5 Operand for operation might be null
x10plusoperandscannotbenullable.conc line 9:2 Operand for operation might be null
 
~~~~~
//##11. nullable on operators
x11nullableonoperators.conc line 0:0 expression before . might be null
x11nullableonoperators.conc line 21:5 Operand for operation might be null
x11nullableonoperators.conc line 22:2 Operand for operation might be null
x11nullableonoperators.conc line 25:1 Operand for operation might be null
x11nullableonoperators.conc line 26:3 Operand for operation might be null
x11nullableonoperators.conc line 28:5 Operand for operation might be null
x11nullableonoperators.conc line 30:2 Operand for operation might be null
x11nullableonoperators.conc line 32:6 Operand for operation might be null
x11nullableonoperators.conc line 34:2 Operand for operation might be null
x11nullableonoperators.conc line 36:6 Operand for operation might be null
x11nullableonoperators.conc line 38:2 Operand for operation might be null
x11nullableonoperators.conc line 40:5 Operand for operation might be null
x11nullableonoperators.conc line 42:2 Operand for operation might be null
x11nullableonoperators.conc line 44:5 Operand for operation might be null
x11nullableonoperators.conc line 46:2 Operand for operation might be null
x11nullableonoperators.conc line 48:6 Operand for operation might be null
x11nullableonoperators.conc line 50:2 Operand for operation might be null
x11nullableonoperators.conc line 52:6 Operand for operation might be null
x11nullableonoperators.conc line 56:6 Operand for operation might be null
x11nullableonoperators.conc line 58:2 Argument 1 passed to method (of minus) can be null (java.lang.Integer?), but method argument type([java.lang.Integer]) is not nullable
x11nullableonoperators.conc line 60:7 Operand for operation might be null
x11nullableonoperators.conc line 68:2 Operand for operation might be null
x11nullableonoperators.conc line 75:1 Operand for operation might be null
x11nullableonoperators.conc line 76:1 Assingment can be null, but assignment type is not nullable
x11nullableonoperators.conc line 77:1 Operand for operation might be null
x11nullableonoperators.conc line 79:5 Operand for operation might be null
 
~~~~~
//##12. expression before dot may be null
x12expressionbeforedotmaybenull.conc line 13:6 expression before . might be null
x12expressionbeforedotmaybenull.conc line 14:6 expression before . might be null

~~~~~
//##13. lists pt1
x13listspt1.conc line 16:2 Assingment can be null, but assignment type is not nullable
 
~~~~~
//##14. for loop iterable can be null
x14forloopiterablecanbenull.conc line 13:22 iterable for loop expression can be null
x14forloopiterablecanbenull.conc line 17:13 iterable for loop expression can be null
x14forloopiterablecanbenull.conc line 18:2 Assingment can be null, but assignment type is not nullable
x14forloopiterablecanbenull.conc line 22:2 Assingment can be null, but assignment type is not nullable
 
~~~~~
//##15. with expression may be null
x15withexpressionmaybenull.conc line 11:1 with expression may be null
 
~~~~~
//##16. return from try
x16returnfromtry.conc line 18:1 expression before . might be null
 
~~~~~
//##17.nullable to non null
x17nullabletononnull.conc line 5:1 Assingment can be null, but assignment type is not nullable
 
~~~~~
//##18. nullable due to else block for loops etc
x18nullableduetoelseblockforloopsetc.conc line 10:1 Assingment can be null, but assignment type is not nullable
x18nullableduetoelseblockforloopsetc.conc line 13:7 expression before . might be null
 
~~~~~
//##19. return from func
x19returnfromfunc.conc line 9:1 Assingment can be null, but assignment type is not nullable
 
~~~~~
//##20. safe dot op
x20safedotop.conc line 8:10 expression before ?. is expected to be nullable
x20safedotop.conc line 9:7 expression before . might be null
 
~~~~~
//##21. safe dot op chain
x21safedotopchain.conc line 16:14 expression before ?. is expected to be nullable
x21safedotopchain.conc line 17:11 expression before . might be null
x21safedotopchain.conc line 18:9 expression before ?. is expected to be nullable
 
~~~~~
//##22. not null assertion
 
~~~~~
//##23. elvis checks and void not approperiate
x23elvischecksandvoidnotapproperiate.conc line 8:8 Elvis operator ?: may only be used on nullable or potentially nullable types
x23elvischecksandvoidnotapproperiate.conc line 9:8 unexpected type: void for elvis operator ?:
x23elvischecksandvoidnotapproperiate.conc line 11:2 unexpected type: void for elvis operator ?:
x23elvischecksandvoidnotapproperiate.conc line 13:1 void is not an instantiable type

~~~~~
//##24. HasNoNullItems - test @NoNull annoation
x24HasNoNullItemstest@NoNullannoation.conc line 8:32 Argument 1 passed to method (of addToList) can be null (java.util.ArrayList<java.lang.String>?), but method argument type([java.util.List<java.lang.String>, java.lang.String]) is not nullable
x24HasNoNullItemstest@NoNullannoation.conc line 9:3 Argument 2 passed to method (of addToList) can be null (null), but method argument type([java.util.List<java.lang.String>, java.lang.String]) is not nullable
x24HasNoNullItemstest@NoNullannoation.conc line 11:1 Assingment can be null, but assignment type is not nullable
x24HasNoNullItemstest@NoNullannoation.conc line 13:35 Argument 1 passed to method (of addToListLG) can be null (java.util.ArrayList<java.lang.String>?), but method argument type([java.util.List<java.lang.String>, java.lang.String]) is not nullable
x24HasNoNullItemstest@NoNullannoation.conc line 14:3 Argument 2 passed to method (of addToListLG) can be null (null), but method argument type([java.util.List<java.lang.String>, java.lang.String]) is not nullable
x24HasNoNullItemstest@NoNullannoation.conc line 16:1 Assingment can be null, but assignment type is not nullable
x24HasNoNullItemstest@NoNullannoation.conc line 35:1 Assingment can be null, but assignment type is not nullable
x24HasNoNullItemstest@NoNullannoation.conc line 41:1 Assingment can be null, but assignment type is not nullable
x24HasNoNullItemstest@NoNullannoation.conc line 48:1 Assingment can be null, but assignment type is not nullable
x24HasNoNullItemstest@NoNullannoation.conc line 53:1 Assingment can be null, but assignment type is not nullable
 
~~~~~
//##25. HasNoNullItems - test @NoNull annoation fields
x25HasNoNullItemstest@NoNullannoationfields.conc line 8:2 Assingment can be null, but assignment type is not nullable
x25HasNoNullItemstest@NoNullannoationfields.conc line 9:2 Assingment can be null, but assignment type is not nullable
 
~~~~~
//##26. bytecode fields and methods are annotated with no null annotation
x26bytecodefieldsandmethodsareannotatedwithnonullannotation.conc line 8:1 Assingment can be null, but assignment type is not nullable
x26bytecodefieldsandmethodsareannotatedwithnonullannotation.conc line 9:15 Argument 1 passed to method (of conc) can be null (null), but method argument type([java.lang.String, java.lang.String?]) is not nullable
 
~~~~~
//##27. 2nd string op ok

~~~~~
//##28. nullable generics
x28nullablegenerics.conc line 14:8 Generic parameter qualifcation mismatch: 'X' cannot be qualified with: 'java.lang.String?' - nullability mismatch
 
~~~~~
//##29. restrictions on nullable types
x29restrictionsonnullabletypes.conc line 4:12 ? may not be used consecutively
 
~~~~~
//##30. basic nullable array assingment
x30basicnullablearrayassingment.conc line 11:1 Assingment can be null, but assignment type is not nullable
x30basicnullablearrayassingment.conc line 13:1 Array operation cannot be performed, array is nullable and might be null

~~~~~
//##31.generic qualifcation of type
 
~~~~~
//##32. no set for you on list
 
~~~~~
//##33. nullables on lists like arrays
x33nullablesonlistslikearrays.conc line 12:1 Array operation cannot be performed, array is nullable and might be null
 
~~~~~
//##34. nullable arrays and lists
x34nullablearraysandlists.conc line 10:1 Assingment can be null, but assignment type is not nullable
x34nullablearraysandlists.conc line 16:1 Assingment can be null, but assignment type is not nullable
 
~~~~~
//##34. vectorization
x35vectorization.conc line 4:7 Operand for operation might be null

~~~~~
//##36. applies to nullable only

~~~~~
//##37. nullable generics
x37nullablegenerics.conc line 25:7 Generic parameter qualifcation mismatch: 'X' cannot be qualified with: 'java.lang.String?' - nullability mismatch
x37nullablegenerics.conc line 26:7 Argument 1 passed to constructor (of x37nullablegenerics.MyClass<java.lang.String>) can be null (null), but constructor argument type(java.lang.String) is not nullable
x37nullablegenerics.conc line 27:8 Generic parameter qualifcation mismatch: 'X' cannot be qualified with: 'null' - nullability mismatch
x37nullablegenerics.conc line 30:7 Generic parameter qualifcation mismatch: 'X?' cannot be qualified with: 'java.lang.String' - nullability mismatch
x37nullablegenerics.conc line 31:8 Generic parameter qualifcation mismatch: 'X?' cannot be qualified with: 'java.lang.String' - nullability mismatch
 
~~~~~
//##38. generic type inference will preserve nullability
x38generictypeinferencewillpreservenullability.conc line 24:0 These variables have been declared non nullable but have not been assigned a value in the auto gennerated constructor: x
x38generictypeinferencewillpreservenullability.conc line 36:10 Generic parameter qualifcation mismatch: 'X' cannot be qualified with: 'null' - nullability mismatch

~~~~~
//##39. local generics respect nullability
x39localgenericsrespectnullability.conc line 8:7 Unable to find method with matching name: MyClass

~~~~~
//##40. local generics respect nullability full
x40localgenericsrespectnullabilityfull.conc line 16:7 Unable to find method with matching name: MyClass
x40localgenericsrespectnullabilityfull.conc line 17:23 Argument 1 passed to method (of MyClass) can be null (null), but method argument type([java.lang.String]) is not nullable
x40localgenericsrespectnullabilityfull.conc line 18:8 Unable to find method with matching name: MyClass
x40localgenericsrespectnullabilityfull.conc line 21:7 Unable to find method with matching name: MyClassNullable1
x40localgenericsrespectnullabilityfull.conc line 22:8 Unable to find method with matching name: MyClassNullable2
 
~~~~~
//##41. local generics respect nullability full mrefs
x41localgenericsrespectnullabilityfullmrefs.conc line 15:7 Unable to find reference function Type for: MyClass
x41localgenericsrespectnullabilityfullmrefs.conc line 16:24 Argument passed to method reference can be null, but method argument type is not nullable
x41localgenericsrespectnullabilityfullmrefs.conc line 17:8 Unable to find method with matching name: MyClass
x41localgenericsrespectnullabilityfullmrefs.conc line 20:7 Unable to find reference function Type for: MyClassNullable1
x41localgenericsrespectnullabilityfullmrefs.conc line 21:8 Unable to find reference function Type for: MyClassNullable2
 
~~~~~
//##42. local generics respect nullability lambda
x42localgenericsrespectnullabilitylambda.conc line 16:7 Unable to find method with matching name: MyClass
x42localgenericsrespectnullabilitylambda.conc line 17:23 Argument 1 passed to method (of MyClass) can be null (null), but method argument type([X]) is not nullable
x42localgenericsrespectnullabilitylambda.conc line 18:16 Argument 1 passed to method (of MyClass) can be null (null), but method argument type([X]) is not nullable
x42localgenericsrespectnullabilitylambda.conc line 21:7 Unable to find method with matching name: MyClassNullable1
x42localgenericsrespectnullabilitylambda.conc line 22:8 Unable to find method with matching name: MyClassNullable2
 
~~~~~
//##43. this was a bug now its ok
x43thiswasabugnowitsok.conc line 8:7 Type mismatch: cannot convert from java.lang.Integer[] to int[]
 
~~~~~
//##44. this was a bug now its ok
x44thiswasabugnowitsok.conc line 3:16 int[] is not a subtype of java.lang.Integer[]
x44thiswasabugnowitsok.conc line 8:7 int[] is not a subtype of java.lang.Integer[]

~~~~~
//##45. nullable generics respected in bytecode
x45nullablegenericsrespectedinbytecode.conc line 7:11 Generic parameter qualifcation mismatch: 'X' cannot be qualified with: 'java.lang.String?' - nullability mismatch
x45nullablegenericsrespectedinbytecode.conc line 9:10 Generic parameter qualifcation mismatch: 'X?' cannot be qualified with: 'java.lang.String' - nullability mismatch
 
~~~~~
//##46. nullable generics respected in bytecode - other way
x46nullablegenericsrespectedinbytecodeotherway.conc line 7:11 Generic parameter qualifcation mismatch: 'X' cannot be qualified with: 'java.lang.String?' - nullability mismatch
x46nullablegenericsrespectedinbytecodeotherway.conc line 9:10 Generic parameter qualifcation mismatch: 'X?' cannot be qualified with: 'java.lang.String' - nullability mismatch
 
~~~~~
//##47. nullable generics respected in bytecode - methods
x47nullablegenericsrespectedinbytecodemethods.conc line 7:8 Unable to find method with matching name: HolderNoNullMeth
x47nullablegenericsrespectedinbytecodemethods.conc line 9:8 Unable to find method with matching name: HodlerNullOkMeth
 
~~~~~
//##48. nullable generics respected in bytecode - methods - alt method
x48nullablegenericsrespectedinbytecodemethodsaltmethod.conc line 7:7 Unable to find method with matching name: ItsNoNullMeth
x48nullablegenericsrespectedinbytecodemethodsaltmethod.conc line 9:6 Unable to find method with matching name: TakesNullMeth
 
~~~~~
//##49. class fields declared defualt to null so flag if unset
x49classfieldsdeclareddefualttonullsoflagifunset.conc line 12:1 These variables have been declared non nullable but have not been assigned a value in this constructor: thing, x
 
~~~~~
//##50. no operation on nullable array
x50nooperationonnullablearray.conc line 7:9 Array operation cannot be performed, array is nullable and might be null
x50nooperationonnullablearray.conc line 8:9 Array operation cannot be performed, array is nullable and might be null
 
~~~~~
//##51. Null safe array operation may only be performed on array having a nullable type
x51Nullsafearrayoperationmayonlybeperformedonarrayhavinganullabletype.conc line 6:9 Null safe array operation may only be performed on array having a nullable type
 
~~~~~
//##52. used to crash now its ok
x52usedtocrashnowitsok.conc line 11:31 Array index operation can only be performed on object that is an array, Map, List or object having approperiate operator overloading 'get' method not: int
 
~~~~~
//##53. fun ret void still ret void
x53funretvoidstillretvoid.conc line 12:1 void is not an instantiable type
 
~~~~~
//##54. null check logic 
x54nullchecklogic.conc line 8:1 Lamdba: thing is nullable and may be null
x54nullchecklogic.conc line 24:1 Lamdba: thing is nullable and may be null
x54nullchecklogic.conc line 40:1 Lamdba: thing is nullable and may be null
 
~~~~~
//##55. no match on null if not nullable
x55nomatchonnullifnotnullable.conc line 8:7 cannot match against null as input is not nullable

~~~~~
//##56. advanved null inference - ae an
x56advanvednullinferenceaean.conc line 15:1 expression before . might be null
x56advanvednullinferenceaean.conc line 29:1 expression before . might be null
x56advanvednullinferenceaean.conc line 48:1 expression before . might be null
 
~~~~~
//##57. advanved null inference - class vars

~~~~~
//##58. advanved null inference - assert
x58advanvednullinferenceassert.conc line 16:1 expression before . might be null
 
~~~~~
//##59. isolated blocks
x59isolatedblocks.conc line 20:1 expression before . might be null
x59isolatedblocks.conc line 29:1 expression before . might be null
 
~~~~~
//##60. whiles
x60whiles.conc line 22:1 expression before . might be null
 
~~~~~
//##61. try catch fin
x61trycatchfin.conc line 25:1 expression before . might be null
x61trycatchfin.conc line 38:1 expression before . might be null
x61trycatchfin.conc line 50:1 expression before . might be null
 
~~~~~
//##62. if elif else
x62ifelifelse.conc line 22:1 expression before . might be null
x62ifelifelse.conc line 46:1 expression before . might be null
x62ifelifelse.conc line 58:1 expression before . might be null
 
~~~~~
//##63. another one
x63anotherone.conc line 10:5 expression before . might be null
 
~~~~~
//##64. class level vars - method call invalidates nullability
x64classlevelvarsmethodcallinvalidatesnullability.conc line 9:2 expression before . might be null
 
~~~~~
//##65. shared vars inf
x65sharedvarsinf.conc line 12:6 expression before . might be null
x65sharedvarsinf.conc line 13:6 expression before . might be null
x65sharedvarsinf.conc line 14:6 expression before . might be null
 
