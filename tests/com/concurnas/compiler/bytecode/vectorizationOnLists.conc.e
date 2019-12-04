//##1. simple vectorization returns
[[[-0.3, 0.58], [0.89, 0.96]], 4]

~~~~~
//##2. simple vectorization no return
[4, 4]

~~~~~
//##3. in place vectorization with return
[[[-0.3, 0.58], [0.89, 0.96]], [[-0.3, 0.58], [0.89, 0.96]], 4, 4]

~~~~~
//##4. in place vectorization with no return
[[[-0.3, 0.58], [0.89, 0.96]], 4, 4]

~~~~~
//##5. simple to vector
[[-0.3, 0.58], [-0.3, 0.58], [60.0, 120.0], [-0.3, 0.58], [60.0, 120.0], [-0.3, 0.58], 8, 8]

~~~~~
//##6. simple to 3d
[[[[-0.3, 0.58]]], [[[-0.3, 0.58]]], [[[60.0, 120.0]]], [[[-0.3, 0.58]]], [[[60.0, 120.0]]], [[[-0.3, 0.58]]], 8, 8]

~~~~~
//##7. differing types
[[-0.3, 0.58], [60, 120], [60, 120], 4, 4]

~~~~~
//##8. boxing unboxing
[[-0.3, 0.58], [60.0, 120], [60.0, 120], 4, 4]

~~~~~
//##9.vectorization nested chains
[fcall: pre outer: a => 1, fcall: pre outer: b => 2]
[fcall: pre outer: a => 5, fcall: pre outer: b => 6]
~~
[a, b]
[a, b]
[fcall: pre outer: a => 5, fcall: pre outer: b => 6]
[fcall: pre outer: a => 7, fcall: pre outer: b => 8]

~~~~~
//##10.vectorization nested chains func invoke vectorized
[[ok 1 1 -, ok 2 2 -], [ok 3 3 -, ok 4 4 -]]

~~~~~
//##11. vectorization ambiguity resovled 
[2d: [1, 2], 2d: [3, 4]]

~~~~~
//##12. cast an array
[1, 2]

~~~~~
//##13. cast an amtrix
[[1, 2], [3, 4]]

~~~~~
//##14. cast an sub element
[1.0, 2.0]

~~~~~
//##15. use of cast
[101, 102]

~~~~~
//##16. more use of cast
[101, 102]

~~~~~
//##17. dot operator simple
[1, [11, 12, 13]]

~~~~~
//##18. dot operator simple - process residual post funcinvoke vec
[1, 3]

~~~~~
//##19. dot operator simple - process residual post funcinvoke vec pt 2
3

~~~~~
//##20. array ref post vect
13

~~~~~
//##21. array ref post vect pt 2
[1, 11]

~~~~~
//##22. vectorize super and this calls
[[11, 12, 13], [11, 12, 13], [111, 112, 113]]

~~~~~
//##23. ensure correct types captured
[[1 ; 1], [2 ; 2], [3 ; 3]]

~~~~~
//##24. ensure correct types captured v2
[[[1 ; 1], [2 ; 2], [3 ; 3]]]

~~~~~
//##25. vectorize dop imported function
asdasd[[11, 12], [13, 14]]

~~~~~
//##26. vectorize chain with dop argument 
[22, 24]

~~~~~
//##27. vectorize chain with dop argument  precidence bugfix
asdasd[[22, 24], [26, 28]]

~~~~~
//##28. vectorize extesion function 
[[[11, 12], [13, 14]], [[11, 12], [13, 14]]]

~~~~~
//##29. vectorize func invoke simple
[[22, 22], [22, 22]]

~~~~~
//##30. vectorize func invoke of array ret func
[[[22], [22]], [[22], [22]]]

~~~~~
//##31. vectorize func invoke of array ret func no ret
4

~~~~~
//##32. vectorize func invoke of array ret func no ret self set
2 [MyClass-anOp called, MyClass-anOp called] [MyClass-anOp called, MyClass-anOp called]

~~~~~
//##33. vectorize func invoke chain
2 [11, 11] [MyClass-ok, MyClass-ok]

~~~~~
//##34. vectorize func invoke chain indirect
2 [11, 11] [MyClass-ok, MyClass-ok]

~~~~~
//##35. vectorize func invoke chain self
2 [MyClass-11, MyClass-11] [MyClass-11, MyClass-11]

~~~~~
//##36. vectorize func invoke nested in others
2 [10, 11]

~~~~~
//##37. vectorize func invoke ext func 
2 [0, 1]

~~~~~
//##38. vectorixed named param
[hi100, hi90, hi80]

~~~~~
//##39. vectorixed constructor
[[MyClass1, MyClass2], [MyClass3, MyClass4]]

~~~~~
//##40. vectorixed constructor implicit
[[MyClass1, MyClass2], [MyClass3, MyClass4]]

~~~~~
//##41. vectorixed nested constructor implicit and explicit
[[[101, 102], [103, 104]] [[101, 102], [103, 104]]]

~~~~~
//##42. vectorixed func invoke chain
[[101, 102], [103, 104]]

~~~~~
//##43. vectorixed func invoke chain w constrcutor implicit and explicit
[[[101, 102], [103, 104]], [[101, 102], [103, 104]]]

~~~~~
//##44. construct an actor
[[MyClass1, MyClass2], [MyClass3, MyClass4]]

~~~~~
//##45. call actor method
[[[MyClass1, MyClass2], [MyClass3, MyClass4]], [[101, 102], [103, 104]]]

~~~~~
//##46. call on boxed primative
[[[101, 102], [103, 104]], [101, 102, 103, 104]]

~~~~~
//##47. refs of lists and lists of refs and etc
[[101 102 ; 103 104], [101 102 ; 103 104], [101 102 ; 103 104]]

~~~~~
//##48. funcref with binding
1101

~~~~~
//##49. funcref with binding x2
[1101, 1102, 1103, 1104]

~~~~~
//##50. funcref with no binding 
[101, 102, 103, 104]

~~~~~
//##51. funcref with no binding w named param 
[101, 102, 103, 104]

~~~~~
//##52. implicit constructor vect
[101, 102, 103, 104]

~~~~~
//##53. explicit constructor vect
[101, 102, 103, 104]

~~~~~
//##54. vectorized funcrefinvoke
[101, 102, 103, 104]

~~~~~
//##55. funcrefinvoke vectorized
[101, 102, 103, 104]

~~~~~
//##56. vectorized func invoke of funcrefinvoke
[k: 101, k: 102, k: 103, k: 104]

~~~~~
//##57. vectorized func ref of funcrefinvoke
[k: 101, k: 102, k: 103, k: 104]

~~~~~
//##58. no need for vect to operate on ifexpr
[yes, yes, no, no]

~~~~~
//##59. vect array ref
[1 4 6]

~~~~~
//##60. vect array ref postfix
[1 2 3 4 5 6 7 8 9 10 ; 4 5 6 7 8 9 10 ; 6 7 8 9 10]

~~~~~
//##61. vect array ref prefix
[ ; 1 2 3 ; 1 2 3 4 5]

~~~~~
//##62. vect array ref range
[ ; 1 2 3 ; 1 2 3 4 5]

~~~~~
//##63. vect array ref range other way around
[1 2 3 4 5 6 ; 4 5 6 ; 6]

~~~~~
//##64. vect array ref multi
[1 4 6]

~~~~~
//##65. vect array ref multi 2
[6 60 6 60 6 600]

~~~~~
//##66. more than one sublist vect
[ ;  ;  ;  ;  ; ]

~~~~~
//##67. vect operator plus
[2, 3, 4, 5, 6, 7, 8, 9, 10, 11]

~~~~~
//##68. vect operator plus many vect
[2, 4, 6, 8, 10, 12, 14, 16, 18, 20]

~~~~~
//##69. vect operator plus many vect three
[1, 3, 5, 7, 9, 11, 13, 15, 17, 19]

~~~~~
//##70. vect operator plus in place
[[2, 3, 4, 5, 6, 7, 8, 9, 10, 11], [2, 3, 4, 5, 6, 7, 8, 9, 10, 11]]

~~~~~
//##71. vect operator plus operator overload
[2, 3, 4]

~~~~~
//##72. vect operator plus operator string concat
[1abc, 2abc, 3abc, 4abc, 5abc, 6abc, 7abc, 8abc, 9abc, 10abc]

~~~~~
//##73. vect operator multiplier
[[2, 4, 6, 8, 10, 12, 14, 16, 18, 20] [1, 0, 1, 0, 1, 0, 1, 0, 1, 0] [0, 1, 1, 2, 2, 3, 3, 4, 4, 5]]

~~~~~
//##74. vect operator power
[1, 4, 9, 16, 25, 36, 49, 64, 81, 100]

~~~~~
//##75. vect operator reeq op
[true, true, true, true, false, false, false, false, false, false]

~~~~~
//##76. vect operator reeq op pt2
[false, false, false, false, true, false, false, false, false, false]

~~~~~
//##77. bitshift ops
[4, 8, 12, 16, 20, 24, 28, 32, 36, 40]

~~~~~
//##78. vect operator not
[false, false, false, false, false, true]

~~~~~
//##79. cool
[true, false, false, false]

~~~~~
//##80. vect operator instanceof
[[false, true, true, false] ; [true, false, false, true]]

~~~~~
//##81. vect operator and or
[[true, false, false, true, false] ; [true, true, false, true, true]]

~~~~~
//##82. vect operator bitmask ops
[[9, 9, 0] ; [9, 11, 11] ; [0, 2, 11]]

~~~~~
//##83. vect operator prefix operator
[[2, 3, 4] ; [2, 3, 4] ; [2, 3, 4]]

~~~~~
//##84. vect operator postfix operator
[[1, 2, 3] ; [0, 1, 2] ; [0, 1, 2]]

~~~~~
//##85. vect operator comp
[-2, -3, -4]

~~~~~
//##86. vect ops correct types returned
[[2, 3, 4, 5], [2, 4, 6, 8], [1, 4, 9, 16], [false, true, false, false], [4, 8, 12, 16], [false, false, false, true], [true, false, false, true], [true, true, true, false], [1, 0, 1, 0], [1, 2, 3, 4], [2, 3, 4, 5]]

~~~~~
//##87. vect assignment 
[[2, 4, 6, 8] ; [2, 3, 4, 5]]

~~~~~
//##88. vect assignment more 
[[1, 4, 9, 16] ; [1, 4, 27, 256] ; [true, true, true, false] ; [true, false, true, false] ; [2, 8, 24, 64] ; [3, 3, 2]]

~~~~~
//##89. vect assignment eq
[[10, 20, 30, 40] true]

~~~~~
//##90. vect assignment scalar
[[99, 99, 99, 99] true]

~~~~~
//##91. explicit call op overload
[mc: 2, mc: 3, mc: 4, mc: 5]

~~~~~
//##92. op overload self
[mc: 2, mc: 3, mc: 4, mc: 5]

~~~~~
//##93. op overload vectorized assignment
[mc: 16, mc: 36, mc: 64, mc: 100]

~~~~~
//##94. mixed lists and arrays
[2 4 6 8]

~~~~~
//##95. mixed lists and arrays 2
[[[[2, 3] ; [4, 5]], [[2 3], [4 5]]] 
 [2, 3] ; [4, 5] [[2 3], [4 5]] 
 [2, 3] ; [4, 5] 
 [2, 3] ; [4, 5]]