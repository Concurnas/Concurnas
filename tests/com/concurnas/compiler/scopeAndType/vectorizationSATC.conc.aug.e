//##Test: 0 - 1. this one is ok
 
~~~~~
//##2. onyl one arg vec self
x2onyloneargvecself.conc line 13:7 Only one argument in vectorized call may be of '^^' form

~~~~~
//##3. all args same dimention
x3allargssamedimention.conc line 9:7 All vectorized arguments in call must be of the same dimention. First item is: 1 but item: 2 is of: 2 degree
 
~~~~~
//##4. correct type to write back cast needed
x4correcttypetowritebackcastneeded.conc line 20:1 Type mismatch: cannot convert from double to int
x4correcttypetowritebackcastneeded.conc line 22:5 Type mismatch: cannot convert from double to int
 
~~~~~
//##5. only one arg in ensted call may be of ^^ form
x5onlyonearginenstedcallmaybeof^^form.conc line 14:1 Expression element may not have vectorized call of '^^' form - only '^' can be used
 
~~~~~
//##6. only vec array and lists
x6onlyvecarrayandlists.conc line 14:14 Only arrays and lists can be vectorized, not: java.lang.String
 
~~~~~
//##7. only vectorize certain places
x7onlyvectorizecertainplaces.conc line 5:8 Expression cannot be vectorized at this location

~~~~~
//##8. expr element non self ref
x8exprelementnonselfref.conc line 17:6 Expression element may not have vectorized call of '^^' form - only '^' can be used
x8exprelementnonselfref.conc line 18:1 Expression element may not have vectorized call of '^^' form - only '^' can be used

~~~~~
//##9. Expression to vectorize has already been vectorized
x9Expressiontovectorizehasalreadybeenvectorized.conc line 10:13 Expression to vectorize has already been vectorized

~~~~~
//##10. Expression to vectorize has already been vectorized vect func invoke
x10Expressiontovectorizehasalreadybeenvectorizedvectfuncinvoke.conc line 11:7 Expression to vectorize has already been vectorized
 
~~~~~
//##11. Only array and lists can be vectorized vect func invoke
x11Onlyarrayandlistscanbevectorizedvectfuncinvoke.conc line 11:7 Only array and lists can be vectorized
 
~~~~~
//##12. Unable to find method with matching name func invoke
x12Unabletofindmethodwithmatchingnamefuncinvoke.conc line 11:7 Unable to find method with matching signature for vectorized argument: anOpx

~~~~~
//##13. only one self ref on func invoke vect chain
x13onlyoneselfrefonfuncinvokevectchain.conc line 15:7 Only one argument in nested vectorized call may be of '^^' form

~~~~~
//##14. Vectorized arguments must all be to the same degree
x14Vectorizedargumentsmustallbetothesamedegree.conc line 12:6 Vectorized arguments must all be to the same degree
 
~~~~~
//##15. When both elements of sublist array reference are vectorized, they must be vectorized to the same degree
x15Whenbothelementsofsublistarrayreferencearevectorizedtheymustbevectorizedtothesamedegree.conc line 9:19 When both elements of sublist array reference are vectorized, they must be vectorized to the same degree: 1 != 2
 
~~~~~
//##16. op overload only one may inplace
x16opoverloadonlyonemayinplace.conc line 7:7 Only one argument in nested vectorized call may be of '^^' form

~~~~~
//##17. Vectorized Expression cannot appear on its own line
x17VectorizedExpressioncannotappearonitsownline.conc line 7:1 Vectorized Expression cannot appear on its own line

~~~~~
//##18. vectorized assignment lhs must be array or list
x18vectorizedassignmentlhsmustbearrayorlist.conc line 8:1 numerical operation cannot be performed on type int vs int[]^
 
~~~~~
//##19. when vect no direct
x19whenvectnodirect.conc line 8:1 Direct assignment is not possible with vectoried assignment or assignee
 
~~~~~
//##20. all sides of operation need to be vectoried if arrays or lists

~~~~~
//##21. rhs must be vectoried if list ass
 
~~~~~
//##22. Vectorized calls in assignment may only be of
x22Vectorizedcallsinassignmentmayonlybeof.conc line 7:1 Vectorized calls in assignment may only be of '^' form

~~~~~
//##22. Vectorized calls in assignment may only be of
x23newmustactonclassnotafunctionormethod.conc line 9:13 Unable to resolve type corresponding to name: MyClass
 
~~~~~
//##23. cannot vectorize this 
x24cannotvectorizethis1.conc line 11:8 Expression cannot be vectorized at this location
 
~~~~~
//##24. cannot vectorize this 2
x25cannotvectorizethis2.conc line 12:7 Expression cannot be vectorized at this location

 