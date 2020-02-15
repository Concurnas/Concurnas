//##1. find gpu devices
true

~~~~~
//##2. choose approperiate device group
ok

~~~~~
//##3. allocate buffers
[GPUBufferManagedInput, GPUBufferManagedOutput, GPUBufferManagedMixed]

~~~~~
//##4. copy to device
res: true

~~~~~
//##5. chain of writes to device
res: [true true]

~~~~~
//##6. too long and too short
res: [Attempting to write to gpu an object larger than buffer size of: 40 bytes, Attempting to write an object to gpu which is of size: 24 bytes, this is smaller than buffer size of: 40 bytes, great]

~~~~~
//##7. write 1d array to 2d strucutre
res: true

~~~~~
//##8. write 1d array to 2d strucutre obj variant
res: true

~~~~~
//##9. write all the types to the gpu
res: [true, true, true, true, true, true, true, true]

~~~~~
//##10. gpus auto import
auto import

~~~~~
//##11. 2d to 2d write
res: true

~~~~~
//##12. 2d to 1d write
res: true

~~~~~
//##13. 1d to 2d write
res: true

~~~~~
//##14. simple decode
res: [true, [1 2 3 4 5 6]]

~~~~~
//##15. simple decode boxed type
res: [true, [1 2 3 4 5 6]]

~~~~~
//##16. simple decode 2d
res: [true, [1 2 3 ; 4 5 6]]

~~~~~
//##17. decode all types
res: [1 2 3 4 5 6 7 8 9 10]
[1 2 3 4 5 6 7 8 9 10]
[1 2 3 4 5 6 7 8 9 10]
[1.0 2.0 3.0 4.0 5.0 6.0 7.0 8.0 9.0 10.0]
[1.0 2.0 3.0 4.0 5.0 6.0 7.0 8.0 9.0 10.0]
[true false]
[a b c]
[1 2 3 4]

~~~~~
//##18. copy from one device to another
res: [1 2 3 4 5 6 7 8 9 10]

~~~~~
//##19. copy from one device to another size must match
Direct GPU copy from one buffer to another without subregion specified requires matching buffer sizes. From buffer size of: 40 does not equal to buffer size of: 44

~~~~~
//##20. sizeof with variants
12

~~~~~
//##21. gpu sizeof
80

~~~~~
//##22. sublist write to operations
done: [11 11 2 3 4 100 100 100 8 9 10 11 12 13 14 99 99 17 66 66]

~~~~~
//##23. sublist write to operations failure cases
done: [Sublist index from: 8 must be less than to: 5, Sublist index from: -9980 must be greater than or equal to zero, Sublist index to: 5000000 must be less than buffer size of: 20 elements, Expected input sublist to equal 16 bytes (4 elements), but resolves to: 12 bytes (3 elements)]

~~~~~
//##24. sublist read from buffers
done: [[5 6 7], [15 16], [18 19], [0 1]]

~~~~~
//##25. sublist read from buffers errors
done: [Sublist index from: 8 must be less than to: 5, Sublist index from: -9980 must be greater than or equal to zero, Sublist index to: 5000000 must be less than buffer size of: 20 elements]

~~~~~
//##26. copy subregion
res: [0 0 3 4 0 0 0 0 0 0]

~~~~~
//##27. copy subregion errors
Copy source index from: 5 must be less than to: 4
Copy source index from: -990 must be greater than or equal to zero
Copy source index to: 4000 must be less than buffer size of: 10 elements
Copy source index from: 5 must be less than to: 4
Copy destination effective index to: 50 must be less than buffer size of: 10 elements

~~~~~
//##28. gpu single write read copy simple
res: (true, 456)

~~~~~
//##29. gpu single write read copy simple conv types
res: (true, 4623113902481840538, 12.3)

~~~~~
//##30. gpu single errors
Buffer assignment of type: class java.lang.String is not supported
Attempting to write an object to gpu which is of size: 4 bytes, this is smaller than buffer size of: 8 bytes

~~~~~
//##31. copy between same buffer
res: [1 2 3 0 0 0 0 1 2 0]

~~~~~
//##32. on copy to self buffer regions may not overlapp
When copying to a different region of the same buffer, copy region may not overlap. 2 to 5 overlaps with 4 to 7
When copying to a different region of the same buffer, copy region may not overlap. 2 to 5 overlaps with 0 to 3

~~~~~
//##33. gpu copy buffers must be on same device
ok?: true

~~~~~
//##34. gpu platform details strings
[true, true]

~~~~~
//##35. gpu device details strings
true

~~~~~
//##36. basic 1d kernel
nice: [101 202 303 404 505 606 707 808 909 1010]

~~~~~
//##37. basic 1d kernel with in out params
nice: [101 202 303 404 505 606 707 808 909 1010]

~~~~~
//##38. basic 1d kernel with in out params - simple reference
nice: [101 202 303 404 505 606 707 808 909 1010]

~~~~~
//##39. graceful fail after delete called
nice: [[101 202 303 404 505 606 707 808 909 1010], Device Group has already been deleted, Buffer has already been deleted, Device has already been deleted]

~~~~~
//##40. ensure little big endian correct
nice: [4 6 8 10 12 14 16 18 20 22]

~~~~~
//##41. single arg private memory
nice: [5 8 11 14 17 20 23 26 29 32]

~~~~~
//##42. mixed buffer can be used for in or out params
nice: [5 8 11 14 17 20 23 26 29 32]

~~~~~
//##43. correct use of stub functions
ok

~~~~~
//##44. use pre defined funcs
nice: [2.0 5.0 6.0 9.0 4.0 2.0 2.0 5.0 8.0 5.0]

~~~~~
//##45. stub function wish clashing funcparam except for global local etc
ok

~~~~~
//##46. using mod in kernels
nice: [0.0 1.0 1.0 0.0 1.0 0.0 1.0 0.0 0.0 1.0]

~~~~~
//##47. a dependency
nice: [4.0 7.0 3.0 6.0 5.0 4.0 3.0 7.0 8.0 5.1]

~~~~~
//##48. a dependency chain, ordered
nice: [24.0 27.0 23.0 26.0 25.0 24.0 23.0 27.0 28.0 25.1]

~~~~~
//##49. no recursion for you
nice: Recursion, either direct or indirect, is not permitted in gpu kernel invocation chains. 'fib' is recursive

~~~~~
//##50. dependancy in stub
nice: [4.0 7.0 3.0 6.0 5.0 4.0 3.0 7.0 8.0 5.1]

~~~~~
//##51. dependancy in stub in file
nice: [4.0 7.0 3.0 6.0 5.0 4.0 3.0 7.0 8.0 5.1]

~~~~~
//##52. fibonacci 1
nice: [1 1 2 3 5 8 13 21 34 55 89 144 233 377 610 987 1597 2584 4181 6765]

~~~~~
//##53. fibonacci 1 bugfix on for
nice: [1 1 2 3 5 8 13 21 34 55 89 144 233 377 610 987 1597 2584 4181 6765]

~~~~~
//##54. no overloading permitted
Function overloading is not permitted within gpu execution chains. The following functions are overloaded: plus

~~~~~
//##55. ensure kernel dims match
Global Work Size dimensions specified of 2 does not match kernel dimensions to be executed of 1

~~~~~
//##56. kernel calling a kernel
nice: [101 202 303 404 505 606 707 808 909 1010]

~~~~~
//##57. all kernels in chain must be of same dimention
Global Work Size dimensions specified of 2 does not match kernel dimensions to be executed of 1

~~~~~
//##58. simple matrix multilplcation 
nice: [7.0 10.0 ; 15.0 22.0]

~~~~~
//##59. ensure types match
[Passed object to write to buffer of type: int[]. Component type must match float but it is: int, When copying, destination must be of the same component type int <> float]

~~~~~
//##60. mat mult and conventional version
nice: [[7.0 10.0 ; 15.0 22.0], [7.0 10.0 ; 15.0 22.0]]

~~~~~
//##61. on write comparrison is done on unboxed types
nice

~~~~~
//##62. finish
nice

~~~~~
//##63. call profiling
true

~~~~~
//##64. array creation
ok[1 2 3 4 5 6 7 8 9 10]

~~~~~
//##65. local const and global variables
ok[1 2 3 4 5 6 7 8 9 10]

~~~~~
//##66. pointers
pntInUtilFunc: [12 12 12 12 12]
basicPointers: [4 4 4 4 4]
pointerDerefs: [12 12 12 12 12]
pointerDerefsTypeDefined: [12 12 12 12 12]
pointerArith: [33 33 33 33 33]
pointerMath: [484 484 484 484 484]
arrayOfPointers: [24 24 24 24 24]
twodarrofpointer: [12 12 12 12 12]
two2versionpntarray: [14 14 14 14 14]
infixOpsOk: [14 14 14 14 14]
nullPointer: [12 12 12 12 12]

~~~~~
//##67. pointer assignment
ok[99 99 99 99 99 99 99 99 99 99]

~~~~~
//##68. pointer assignment - arrays
ok[99 99 99 99 99 99 99 99 99 99]

~~~~~
//##69. pointer assignment arithmetic
ok[99 99 99 99 99 99 99 99 99 99]

~~~~~
//##70. array of pointer assignment
ok[99 99 99 99 99 99 99 99 99 99]

~~~~~
//##71. address pointer as array
ok[99 99 99 99 99 99 99 99 99 99]

~~~~~
//##72. creating pointers from pointers
ok[12 12 12 12 12]

~~~~~
//##73. refer to global pnt and do some arithmetic
ok[2 2 2 2 2]

~~~~~
//##74. combine things in same memory space
ok[2 2 2 2 2]

~~~~~
//##75. wite to individual buffer
ok 99

~~~~~
//##76. double precision
ok 99.0

~~~~~
//##77. refer to constant val from module level
ok 100.0

~~~~~
//##78. constants
nice: [5 8 11 14 17 20 23 26 29 32]

~~~~~
//##79. local memory
PASS! result: 1024399

~~~~~
//##80. local memory matrix mult
pass? [true, true]

~~~~~
//##81. buffer size checks
Local Work Size dimensions specified of 2 does not match kernel dimensions to be executed of 1
local dimension of: 1093 in position: 0 must less than or equal to the max work item size of: 1024
Local Work Size dimensions specified of 2 does not match kernel dimensions to be executed of 1
local buffers specified (of 393216 bytes) are larger than max local memory of: 49152 bytes
Buffer size (of 51539607544 bytes) is greater than max allocation size of: 805306368 bytes true
Number of elements in buffer must be greater than zero
Number of elements in Local buffer must be greater than zero

~~~~~
//##82. ref to gpu
nice: [5 8 11 14 17 20 23 26 29 32 ; 5 8 11 14 17 20 23 26 29 32]

~~~~~
//##83. gpu boolean
nice: [99 2 3 99 99 99 7 8 99 10]

~~~~~
//##84. null in the events slot
nice: [99 2 3 99 99 99 7 8 99 10]

~~~~~
//##85. no copy of gpu objects
true

~~~~~
//##86. multi assign
nice: [103 206 309 412 515 618 721 824 927 1030]

~~~~~
//##87. constants defined at top level
ok[201 202 203 204 205 206 207 208 209 210]
