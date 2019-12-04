//##1. basic trans operation
[A Balance: 20, B Balance: 0]

~~~~~
//##1b. bigger example
[A Balance: 2000, B Balance: 0]

~~~~~
//##2. check no ret gets popped off
999

~~~~~
//##3. ensure parent block navigated correctly
[A Balance: 1010, B Balance: 990]

~~~~~
//##4. this works lovely
[A Balance: 20, B Balance: 0]

~~~~~
//##5. transactions trigger notification once only and full state preserved
[10, ~, [9, 1] => 10[8, 2] => 10[7, 3] => 10[6, 4] => 10[5, 5] => 10[4, 6] => 10[3, 7] => 10[2, 8] => 10[1, 9] => 10[0, 10] => 10]

~~~~~
//##z6 ensure transactions are in order
[0, 10000, true]

~~~~~
//##z7 count returns in order
[123, 101, 66, [123, 22, 101, ]]

~~~~~
//##z8 ret null
[123, 101, 66]

~~~~~
//##z9 ret nothing
[123, 101, 66]

~~~~~
//##z10 post called on ret
[123, 101, 66]

~~~~~
//##z11 supress output of plus plus
[123, 123, 66]

~~~~~
//##z11.b supress output of plus plus
[123, 123, 66]

~~~~~
//##z11.c supress output of plus plus as above but with pre
[123, 123, 66]

~~~~~
//##z12 close is tagged in transaction
[[2, 98, true, true]]

~~~~~
//##z13 excep is tagged in transaction
[[2, 98, true, java.lang.RuntimeException: java.lang.RuntimeException: hi, true, true, la eq: [true, true]]]

~~~~~
//##z14 trans order from getchange respected
[[true, true][true, true]]

~~~~~
//##z15 nested transactions easier than i thought
[11, [11, true, true, 12]]

~~~~~
//##z16 trans can return thigngs
[55, 10]

~~~~~
//##z17 was a bug with parfor in the past
[20, 10]

~~~~~
//##z18 parfor operates on refarrays of refs now
[100]

~~~~~
//##z19. trans can return a ref
10

~~~~~
//##z20. trans can return a ref more advanced
[100, 10]

~~~~~
//##z21. trans can return a ref when called inside bang operator
[55, 10]

~~~~~
//##z22. process trans one at a time in order
result: [rhs: [1, [1:]], rhs: [2, [2:]], rhs: [3, [3:]]]

~~~~~
//##z23. ensure values are locked in place inside call
result: [[rhs: [1, 1, [1:]], rhs: [2, 2, [2:]]], 2, 2]

~~~~~
//##z24. ensure that initial and later values are correctly captured for every
result: [rhs: 99, rhs: 2, rhs: 3]

~~~~~
//##z25. above but with ref array
result: [rhs: [99 99], rhs: [2 99], rhs: [3 99]]

~~~~~
//##z26. ensure that onchange etc can be used as function arguments and that pass through works as expected 
works

~~~~~
//##z27. this is never going to work
k[turds[1 1]]
