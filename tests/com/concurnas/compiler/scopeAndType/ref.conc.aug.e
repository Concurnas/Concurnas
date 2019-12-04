//##Test: 0 - 1. declare simple

~~~~~ 
//##Test: 1 - 1.b declare simple - check
 
~~~~~ 
//##Test: 2 - 2. array of ref cases fail
x2arrayofrefcasesfail.conc line 5:17 Type array levels don't match. Expected: 1 vs 0
x2arrayofrefcasesfail.conc line 6:18 int[] is not a subtype of java.lang.Integer:[]
 
~~~~~ 
//##Test: 3 - 3. array of ref cases ok
 
~~~~~ 
//##Test: 4 - 4. ref of array ok
 
~~~~~ 
//##Test: 5 - 4.b ref of array fail
x4brefofarrayfail.conc line 3:0 The left-hand side of an assignment must be a variable
 
~~~~~ 
//##Test: 6 - 5. ret from async function
 
~~~~~ 
//##Test: 7 - 5.a ret from async function - defined already fail
x5aretfromasyncfunctiondefinedalreadyfail.conc line 9:1 Variable f has already been defined in current scope
 
~~~~~ 
//##Test: 8 - 6. setting
 
~~~~~ 
//##Test: 9 - 7. useage
 
~~~~~ 
//##Test: 10 - 8. instanceof on refs is ok
 
~~~~~ 
//##Test: 11 - 9. lhs must be ref
 
~~~~~ 
//##Test: 12 - 10. more simple assignments
x10moresimpleassignments.conc line 8:8 incompatible type: int vs java.lang.Integer:
 
~~~~~ 
//##Test: 13 - 11. as above but checking the types
 
~~~~~ 
//##Test: 14 - 12. some random stuff which should be ok
 
~~~~~ 
//##Test: 15 - 13. lhs no ref
 
~~~~~ 
//##Test: 16 - 14. this is permitted but i think it's weird... cannot really prevent it
 
~~~~~ 
//##Test: 17 - 15. this is permitted but i think it's weird... cannot really prevent it
 
~~~~~ 
//##Test: 18 - 16. autocast from float to int
 
~~~~~ 
//##Test: 19 - 17. use single ref as function argument and in currying
 
~~~~~ 
//##Test: 20 - 18. ref logic checks
 
~~~~~ 
//##Test: 21 - 19. double ref to single
 
~~~~~ 
//##Test: 22 - 20. ops on ref ok 
 
~~~~~ 
//##Test: 23 - 21. ops on ref ok -fail
x21opsonrefokfail.conc line 7:14 Unable to find method with matching name: getasdasd
 
~~~~~ 
//##Test: 24 - 22. use, auto extract the get operation to unbox
 
~~~~~ 
//##Test: 25 - 23. async blocks
 
~~~~~ 
//##Test: 26 - 24. can assign to final as shell
x24canassigntofinalasshell.conc line 7:1 Variable a has been decalred as val and cannot be reassigned

~~~~~ 
//##Test: 27 - 24.b can assign to final as shell
x24bcanassigntofinalasshell.conc line 9:1 Variable a has been decalred as val and cannot be reassigned
 
~~~~~ 
//##Test: 28 - 25. ensure no ignore unboxing
 
~~~~~ 
//##Test: 29 - 26. more of prev case
 
~~~~~ 
//##Test: 30 - 27. instanceof moan
 
~~~~~ 
//##Test: 31 - 28. cast checks
 
~~~~~ 
//##Test: 32 - 29. cannot cast from ref to obj when locked in
x29cannotcastfromreftoobjwhenlockedin.conc line 10:27 java.lang.Integer: is not a subtype of java.lang.Integer
 
~~~~~ 
//##Test: 33 - 30. math on locked refs is ok
 
~~~~~ 
//##Test: 34 - 31. arrays of ref type must be init
x31arraysofreftypemustbeinit.conc line 7:6 The variable a may not have been initialized
 
~~~~~ 
//##Test: 35 - 32.a ambigous - ambig case
x32aambigousambigcase.conc line 9:1 Ambiguous method detected 'xxx'. More than one indirect match made
 
~~~~~ 
//##Test: 36 - 32.b ambigous - non ambig case
 
~~~~~ 
//##Test: 37 - 33. arrays of refs casting
x33arraysofrefscasting.conc line 8:19 Type mismatch: cannot convert from java.lang.Integer:[] to java.lang.Integer[]
x33arraysofrefscasting.conc line 9:18 Type mismatch: cannot convert from java.lang.Integer:[] to java.lang.Number[]
 
~~~~~ 
//##Test: 38 - 33.b arrays of refs casting - part 2
x33barraysofrefscastingpart2.conc line 7:6 Cannot cast from java.lang.Integer:[] to java.lang.Number[]
 
~~~~~ 
//##Test: 39 - 34. array of ref - some op fail more
x34arrayofrefsomeopfailmore.conc line 9:8 Cannot cast from java.lang.Object[] to java.lang.Number:[]
 
~~~~~ 
//##Test: 40 - 35. this should return a ref
 
~~~~~ 
//##Test: 41 - 36. check subtype
x36checksubtype.conc line 5:12 Type mismatch: cannot convert from int[]: to java.lang.Integer:
 
~~~~~ 
//##Test: 42 - 36.b check subtype
x36bchecksubtype.conc line 5:1 Type mismatch: cannot convert from int[]: to java.lang.Integer:
 
~~~~~ 
//##Test: 43 - 37. inc on raw not ret copy respects refness
x37inconrawnotretcopyrespectsrefness.conc line 7:8 token recognition error at: '#'
 
~~~~~ 
//##Test: 44 - 38. index op not valid on ref
x38indexopnotvalidonref.conc line 7:25 Expected type of int, Integer or size_t but recieved: java.lang.Integer:. Also, no approperiate overloaded setter/getter operator found
 
~~~~~ 
//##Test: 45 - 39 be careful with array ref types
x39becarefulwitharrayreftypes.conc line 5:12 Type mismatch: cannot convert from java.lang.Integer:[] to int[]:
x39becarefulwitharrayreftypes.conc line 11:12 Type mismatch: cannot convert from java.lang.Integer:[] to int[]:
 
~~~~~ 
//##Test: 46 - 40 asyncrefref levels
 
~~~~~ 
//##Test: 47 - 41. create an empty obj ref but not like this
x41createanemptyobjrefbutnotlikethis.conc line 5:1 a cannot be resolved to a variable
 
~~~~~ 
//##Test: 48 - 42. no refied types thus following cast is not possible
x42norefiedtypesthusfollowingcastisnotpossible.conc line 10:10 Cannot perform is or isnot check against parameterized type java.util.HashMap<java.lang.String, java.lang.Integer>. Use the form java.util.HashMap<?, ?> instead since further generic type information will be erased at runtime 
 
~~~~~ 
//##Test: 49 - 42.b no refied types thus following cast IS possible
 
~~~~~ 
//##Test: 50 - 42.c no refied types thus following cast IS possible - null not ref
 
~~~~~ 
//##Test: 51 - 42.d no refied types thus following cast IS possible - null
x42dnorefiedtypesthusfollowingcastISpossiblenull.conc line 11:5 Cannot compare an instance of java.lang.String: with java.util.HashMap<java.lang.String, java.lang.Integer>:
x42dnorefiedtypesthusfollowingcastISpossiblenull.conc line 12:6 Cannot compare an instance of java.lang.String: with java.util.HashMap<java.lang.String, java.lang.String>:
x42dnorefiedtypesthusfollowingcastISpossiblenull.conc line 14:5 Cannot compare an instance of java.lang.String: with java.util.HashMap<java.lang.String, java.lang.Integer>:
x42dnorefiedtypesthusfollowingcastISpossiblenull.conc line 15:6 Cannot compare an instance of java.lang.String: with java.util.HashMap<java.lang.String, java.lang.String>:
 
~~~~~ 
//##Test: 52 - 43. cast checks -sac
 
~~~~~ 
//##Test: 53 - 44. simple case
 
~~~~~ 
//##Test: 54 - 45. asign exsiting ref levels
x45asignexsitingreflevels.conc line 10:1 Too many assignment levels: 3 for existing ref of type: java.lang.Integer::
 
~~~~~ 
//##Test: 55 - 46. puzzle 1
 
~~~~~ 
//##Test: 56 - 46.b puzzle 2 - a tricky one where ref is of type Object:
 
~~~~~ 
//##Test: 57 - 47. cannot ref assign something that is not a ref
x47cannotrefassignsomethingthatisnotaref.conc line 7:1 Existing variable is not a ref. It is of type: java.lang.String:[]
 
~~~~~ 
//##Test: 58 - 48. obj: arg defo not upcast from int:
 
~~~~~ 
//##Test: 59 - 49. correct bevahour for ref creation
 
~~~~~ 
//##Test: 60 - 50. this async ref syntax is not permitted
 
~~~~~ 
//##Test: 61 - 51. cusr ref type must match
x51cusrreftypemustmatch.conc line 7:19 Type mismatch: cannot convert from java.lang.Integer: to java.lang.Integer:com.concurnas.runtime.ref.RefArray
x51cusrreftypemustmatch.conc line 8:22 Type mismatch: cannot convert from java.lang.Integer:: to java.lang.Integer:com.concurnas.runtime.ref.RefArray:
 
~~~~~ 
//##Test: 62 - 52. cannot get from a raw Ref type
x52cannotgetfromarawReftype.conc line 17:2 Type mismatch: cannot extract value from ref type: ? java.lang.Object:com.concurnas.bootstrap.runtime.ref.Ref as it does not implement the DirectlyGettable interface
x52cannotgetfromarawReftype.conc line 19:2 Type mismatch: cannot extract value from ref type: ? java.lang.Object:com.concurnas.bootstrap.runtime.ref.Ref as it does not implement the DirectlyGettable interface
x52cannotgetfromarawReftype.conc line 20:4 Type mismatch: cannot extract value from ref type: ? java.lang.Object:com.concurnas.bootstrap.runtime.ref.Ref as it does not implement the DirectlyGettable interface
x52cannotgetfromarawReftype.conc line 21:7 numerical operation cannot be performed on type ? java.lang.Object. No overloaded 'mul' operator found for type ? java.lang.Object with signature: '(? java.lang.Object)'
x52cannotgetfromarawReftype.conc line 23:10 Type mismatch: cannot convert to int as ref type: ? java.lang.Object:com.concurnas.bootstrap.runtime.ref.Ref does not support getting as it does not implement the DirectlyGettable or DirectlyArrayGettable interface
x52cannotgetfromarawReftype.conc line 27:15 Type mismatch: cannot extract value from ref type: ? java.lang.Object:com.concurnas.bootstrap.runtime.ref.Ref as it does not implement the DirectlyGettable interface
x52cannotgetfromarawReftype.conc line 29:17 Unable to find method with matching name: constructor and arguments (? java.lang.Object:com.concurnas.bootstrap.runtime.ref.Ref)
 
~~~~~ 
//##Test: 63 - 52.b cannot get from a raw Ref type as above
x52bcannotgetfromarawReftypeasabove.conc line 16:7 Type mismatch: cannot convert to ? java.lang.Object as ref type: ? java.lang.Object:com.concurnas.bootstrap.runtime.ref.Ref does not support getting as it does not implement the DirectlyGettable or DirectlyArrayGettable interface
x52bcannotgetfromarawReftypeasabove.conc line 17:4 Type mismatch: cannot convert to ? java.lang.Object as ref type: ? java.lang.Object:com.concurnas.bootstrap.runtime.ref.Ref does not support getting as it does not implement the DirectlyGettable or DirectlyArrayGettable interface
x52bcannotgetfromarawReftypeasabove.conc line 19:6 Type mismatch: cannot extract value from ref type: ? java.lang.Object:com.concurnas.bootstrap.runtime.ref.Ref as it does not implement the DirectlyGettable interface
 
~~~~~ 
//##Test: 64 - 52.c cannot get from a raw Ref type as above MORE
x52ccannotgetfromarawReftypeasaboveMORE.conc line 7:0 Type mismatch: cannot extract value from ref type: java.lang.Object:com.concurnas.bootstrap.runtime.ref.Ref: as it does not implement the DirectlyGettable interface
x52ccannotgetfromarawReftypeasaboveMORE.conc line 8:85 Type mismatch: cannot create ref from int to uninstantiable: java.lang.Object:com.concurnas.bootstrap.runtime.ref.Ref:
x52ccannotgetfromarawReftypeasaboveMORE.conc line 9:22 Type mismatch: cannot extract value from ref type: java.lang.Object:com.concurnas.bootstrap.runtime.ref.Ref: as it does not implement the DirectlyGettable interface
x52ccannotgetfromarawReftypeasaboveMORE.conc line 11:6 type java.lang.Object has fewer than 1 ref levels
 
~~~~~ 
//##Test: 65 - 52.d cannot get from a raw Ref type as above MORE MORE
x52dcannotgetfromarawReftypeasaboveMOREMORE.conc line 9:23 Type mismatch: cannot extract value from ref type: java.lang.Object::com.concurnas.bootstrap.runtime.ref.Ref as it does not implement the DirectlyGettable interface
 
~~~~~ 
//##Test: 66 - 52.e cannot get from a raw Ref type as above MORE MORE MORE
x52ecannotgetfromarawReftypeasaboveMOREMOREMORE.conc line 9:24 Type mismatch: cannot extract value from ref type: java.lang.Object::com.concurnas.bootstrap.runtime.ref.Ref as it does not implement the DirectlyGettable interface
 
~~~~~ 
//##Test: 67 - 52.F MORE COMPLAINTS
x52FMORECOMPLAINTS.conc line 9:24 Type mismatch: cannot extract value from ref type: java.lang.Object::com.concurnas.bootstrap.runtime.ref.Ref as it does not implement the DirectlyGettable interface
x52FMORECOMPLAINTS.conc line 14:9 Type mismatch: cannot extract value from ref type: java.lang.Object:com.concurnas.bootstrap.runtime.ref.Ref as it does not implement the DirectlyGettable interface
 
~~~~~ 
//##Test: 68 - 52. g doesnt like this
x52gdoesntlikethis.conc line 6:0 Type mismatch: cannot extract value from ref type: java.lang.Integer:com.concurnas.bootstrap.runtime.ref.Ref as it does not implement the DirectlyGettable interface
 
~~~~~ 
//##Test: 69 - 53. more cases like the above
x53morecasesliketheabove.conc line 9:0 Type mismatch: cannot extract value from ref type: java.lang.Object:com.concurnas.bootstrap.runtime.ref.Ref: as it does not implement the DirectlyGettable interface
 
~~~~~ 
//##Test: 70 - 54. can create local for ref type
x54cancreatelocalforreftype.conc line 4:0 Cannot create ref of type: java.lang.Integer:com.concurnas.bootstrap.runtime.ref.Ref as it is uninstantiable
x54cancreatelocalforreftype.conc line 5:2 Type mismatch: cannot create ref from int to uninstantiable: java.lang.Integer:com.concurnas.bootstrap.runtime.ref.Ref

~~~~~ 
//##Test: 71 - 55. some errs
x55someerrs.conc line 9:0 Type mismatch: cannot extract value from ref type: java.lang.Object:com.concurnas.bootstrap.runtime.ref.Ref:com.concurnas.bootstrap.runtime.ref.Ref as it does not implement the DirectlyGettable interface
x55someerrs.conc line 10:0 Type mismatch: cannot extract value from ref type: java.lang.Object:com.concurnas.bootstrap.runtime.ref.Ref: as it does not implement the DirectlyGettable interface
x55someerrs.conc line 11:0 Type mismatch: cannot extract value from ref type: java.lang.Object::com.concurnas.bootstrap.runtime.ref.Ref as it does not implement the DirectlyGettable interface
 
~~~~~ 
//##Test: 72 - 56. check ref type extraction errs in return statement
x56checkreftypeextractionerrsinreturnstatement.conc line 9:2 Type mismatch: cannot convert to int as ref type: java.lang.Integer:com.concurnas.bootstrap.runtime.ref.Ref does not support getting as it does not implement the DirectlyGettable or DirectlyArrayGettable interface
 
~~~~~ 
//##Test: 73 - 57. for while loop from Ref type not permitted
x57forwhileloopfromReftypenotpermitted.conc line 11:25 Type mismatch: cannot convert to java.lang.Integer as ref type: java.lang.Integer:com.concurnas.bootstrap.runtime.ref.Ref does not support getting as it does not implement the DirectlyGettable or DirectlyArrayGettable interface
x57forwhileloopfromReftypenotpermitted.conc line 16:2 Type mismatch: cannot convert to java.lang.Integer as ref type: java.lang.Integer:com.concurnas.bootstrap.runtime.ref.Ref does not support getting as it does not implement the DirectlyGettable or DirectlyArrayGettable interface
x57forwhileloopfromReftypenotpermitted.conc line 20:20 Type mismatch: cannot convert to java.lang.Integer as ref type: java.lang.Integer:com.concurnas.bootstrap.runtime.ref.Ref does not support getting as it does not implement the DirectlyGettable or DirectlyArrayGettable interface
x57forwhileloopfromReftypenotpermitted.conc line 25:2 Type mismatch: cannot convert to java.lang.Integer as ref type: java.lang.Integer:com.concurnas.bootstrap.runtime.ref.Ref does not support getting as it does not implement the DirectlyGettable or DirectlyArrayGettable interface
x57forwhileloopfromReftypenotpermitted.conc line 35:7 Type mismatch: cannot convert to java.lang.Integer as ref type: java.lang.Integer:com.concurnas.bootstrap.runtime.ref.Ref does not support getting as it does not implement the DirectlyGettable or DirectlyArrayGettable interface
x57forwhileloopfromReftypenotpermitted.conc line 40:3 Type mismatch: cannot convert to java.lang.Integer as ref type: java.lang.Integer:com.concurnas.bootstrap.runtime.ref.Ref does not support getting as it does not implement the DirectlyGettable or DirectlyArrayGettable interface
x57forwhileloopfromReftypenotpermitted.conc line 42:2 Type mismatch: cannot convert to java.lang.Integer as ref type: java.lang.Integer:com.concurnas.bootstrap.runtime.ref.Ref does not support getting as it does not implement the DirectlyGettable or DirectlyArrayGettable interface
 