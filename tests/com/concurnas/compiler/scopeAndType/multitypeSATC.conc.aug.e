//##Test: 0 - 1. this one is ok
 
~~~~~
//##2. cannot be used outside of func signautre
x2cannotbeusedoutsideoffuncsignautre.conc line 5:5 Multitype 'int|double' may not be used at this location
 
~~~~~
//##3. if ret then others must
x3ifretthenothersmust.conc line 3:0 method: plusTen return type has been declared as a multi type, multitype input arguments with matching count must be defined

~~~~~
//##4. multitype args count must match
x4multitypeargscountmustmatch.conc line 3:0 method: plusTen uses multitype arguments, these must all be of the same count of: 3. Return type has count: 2
x4multitypeargscountmustmatch.conc line 4:0 method: plusTen uses multitype arguments, these must all be of the same count of: 3. Input argument 2 has count: 2
 
~~~~~
//##5. not for use in lambda
x5notforuseinlambda.conc line 4:10 lambdas cannot have multitype arguments or return types
x5notforuseinlambda.conc line 7:12 For variable reference on: 'plusTen' it must be a method or method reference not: void
 
~~~~~
//##6. check count for multitypes referenced in body
x6checkcountformultitypesreferencedinbody.conc line 16:3 method: matmult uses multitype arguments, these must all be of the same count of: 7. Multitype referenced in body has count: 2
x6checkcountformultitypesreferencedinbody.conc line 17:4 method: matmult uses multitype arguments, these must all be of the same count of: 7. Multitype referenced in body has count: 2
 
~~~~~
//##7. All arry dimensions must be qualified in order to use an element wise initialiser
x7Allarrydimensionsmustbequalifiedinordertouseanelementwiseinitialiser.conc line 4:11 All arry dimensions must be qualified in order to use an element wise initialiser