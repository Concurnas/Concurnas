//##1. simple anon lambda new
[2, 4, 6]

~~~~~
//##2. simple anon lambda new param
[2, 4, 6]

~~~~~
//##3. via constructor
[2, 4, 6]

~~~~~
//##4. funcref args
[2, 4, 6]

~~~~~
//##5. funcref args v2
[2, 4, 6]

~~~~~
//##6. assign existing, simple
[2, 4, 6]

~~~~~
//##7. assign existing, lhs non refname
[2, 4, 6][2, 4, 6]

~~~~~
//##8. assign existing, lhs array
[2, 4, 6]

~~~~~
//##9. strange rhs - block
[2, 4, 6]

~~~~~
//##10. strange rhs - ifstatement
[[2, 4, 6], [4, 8, 12]]

~~~~~
//##11. catch block
[[4, 8, 12], [2, 4, 6]]

~~~~~
//##12. array simple
[[2, 4, 6] [3, 6, 9] [4, 8, 12]]

~~~~~
//##13. array complex
[[2, 4, 6] ; [3, 6, 9] ; [4, 8, 12]]

~~~~~
//##14. lists
[[2, 4, 6], [3, 6, 9], [4, 8, 12]]

~~~~~
//##15. as arg block
[2, 4, 6]

~~~~~
//##16. as arg ifstmt
[[1, 4, 9], [2, 4, 6]]

~~~~~
//##17. as arg exception
[[2, 4, 6], [1, 4, 9]]

~~~~~
//##18. array def
[[1, 2, 3], [2, 4, 6], [3, 6, 9]]

~~~~~
//##19. complex matrix
[[[1, 2, 3]], [[2, 4, 6]], [[3, 6, 9]]]

~~~~~
//##20. arg on list
[[1, 2, 3], [2, 4, 6], [3, 6, 9]]

~~~~~
//##21. arg complex one
[[[1, 2, 3], [2, 4, 6], [3, 6, 9]], [[10, 20, 30], [2, 4, 6], [3, 6, 9]]]

~~~~~
//##22. fancy way to define functype
[[2, 4, 6], [2, 4, 6]]

~~~~~
//##23. ret type defined
[2, 4, 6]

~~~~~
//##24. shortcut rhs to lambadef
[2, 4, 6]

~~~~~
//##25. shortcut rhs also fine
[2, 4, 6]

~~~~~
//##26. some but not all types defined
[11, 12, 13]

~~~~~
//##27. simple SAM method impl
[1, 2, 3]

~~~~~
//##28. simple SAM method impl 2
[11, 12, 13, 14, 15]

~~~~~
//##29. get continuations to work with java 8 lambda style
108

~~~~~
//##30. get continuations to work with java 8 lambda style precompiled
[1, 2, 3, 4, 5]

~~~~~
//##31. get continuations to work with java 8 lambda style precompiled via static
108

~~~~~
//##32. get continuations to work with java 8 lambda style precompiled via static more complex
[11, 12, 13, 14]

~~~~~
//##33. full example
[11, 12, 13, 14, 15]

~~~~~
//##34. vars from outside the thing and also binding of SAM via if statement
[[101, 102, 103, 104, 105], [11, 12, 13, 14, 15]]

~~~~~
//##35. sam type on lhs imply
100

~~~~~
//##36. sam type rhs is a proper lambda
[100, 100]

~~~~~
//##37. ensure infered generic type rebound when returned from argument type
[11, 12, 13, 14, 15]

~~~~~
//##38. boxed input on SAM type
100

~~~~~
//##39. normal defined lambda within an argument which is itself a SAM type
[[11, 12, 13, 14, 15] [11, 12, 13, 14, 15] [11, 12, 13, 14, 15] [11, 12, 13, 14, 15]]

~~~~~
//##40. ensure collect is correctly gernercised
[1, 2, 3, 4, 5]

~~~~~
//##41. map to int
[2 3 4 5 6]

~~~~~
//##42. mor mapping more elaborate toString with sort here
[1, 2, 3, 4, 5]

~~~~~
//##43. lhs is argless lambda convert rhs
[0 5 1 5 2]

~~~~~
//##44. lhs is argless lambda convert rhs - void
[0 1 1 1 2]

~~~~~
//##45. lhs is argless lambda convert rhs - assign existing
[0 5 1 5 2]

~~~~~
//##46. lazy funcref
[0 5 1 5 2]

~~~~~
//##47. lazy funcref - auto unassign
[0 5 1 5 2]

~~~~~
//##48. lazy funcref - auto unassign - ae
[0 5 1 5 2]

~~~~~
//##49. lazy funcref rhs convert
[0 5 1 5 2]

~~~~~
//##50. lazy funcref rhs convert ae
[0 5 1 5 2]

~~~~~
//##51. lazy funcref from class
[0 5 1 5 2]

~~~~~
//##52. lazy funcref from class via getter
[0 5 1 5 2]

~~~~~
//##53. obtain lazy lambda field via getter
5

~~~~~
//##54. obtain lazy lambda field via getter 2 unassign
5

~~~~~
//##55. lazy labda unassign
5

~~~~~
//##56. use of lazy lambdas in class fields
[0 5 5 5 3]

~~~~~
//##57. lazy lambda funcref
[0 5 1 5 2]

~~~~~
//##58. convert arg to argless lambda
[0, 8, 1]

~~~~~
//##59. convert arg to argless lambda - lazy
[0, 8, 1]

~~~~~
//##60. convert arg to argless lambda - constructor
[0, 8, 1]

~~~~~
//##61. convert arg to argless lambda - funcref 1
[0, 8, 1]

~~~~~
//##62. convert arg to argless lambda - funcref 2
[0, 8, 1]

~~~~~
//##63. lambda as default arg
[0, 8, 1]

~~~~~
//##64. lambda as default arg implicit
[0, 8, 1]

~~~~~
//##65. lambda default arg implicit vararg
[0, [8, 8, 9], 2]

~~~~~
//##66. lambda default arg implicit vararg lazy
[0, [8, 8, 9], 2]

~~~~~
//##67. auto sam function convertion
[0 5 1 5 444 2]

~~~~~
//##68. auto sam function convertion - ae
[0 5 1 5 44 2]

~~~~~
//##69. auto sam function convertion - func invoke
[0 5 1 5 44 2]

~~~~~
//##70. auto sam function convertion - consturcotr
[0 5 1 5 44 2]

~~~~~
//##71. auto sam function convertion - funcref
[[0 5 1 5 44 2], [2 5 3 5 44 4]]

~~~~~
//##72. sam vararg
[0, [50, 5], 2]

~~~~~
//##73. auto sam function convertion - default param
[0, 5, 1]

~~~~~
//##74. SAM function with state
[0 5 1 5 6969 2]

~~~~~
//##75. SAM function with state undefined
[0 5 1 5 0 2]

~~~~~
//##76. SAM function fun example
[[0 5], [1 5], [2 5]]

~~~~~
//##77. neat little zero arg lambdas
[0, 1, 5, 25, 3]

~~~~~
//##78. expr list takes lambda
[4, [1 1 5 2 25 3 125 4], 4]

~~~~~
//##79. expr list takes lambda 2
[4, [1 1 5 2 25 3 125 4], 4]

~~~~~
//##80. expr list takes lambda to SAM function
[3, [0 1 1 5 2 25], 3]

~~~~~
//##81. expr list takes lambda to SAM function direct lambda match
[3, [0 1 1 5 2 25], 3]

~~~~~
//##82. expr list lazy param
[3, [0 1 1 5 2 25], 3]

~~~~~
//##83. expr list lazy param lambda def
[33 33 33]

~~~~~
//##84. expr list lazy param lambda def lazy block
[33 33 33]

~~~~~
//##85. expr list lazy param lambda def lazy block SAM
[33 33 33]

~~~~~
//##86. expr list lazy param lambda def SAM
[33 33 33]

~~~~~
//##87. expr list lazy vararg
[3, [[0 1], [1 5], [2 25]], 3]

~~~~~
//##88. expr list lazy vararg expr block for lambda
[3 [34, 45, 56, 5, 25] 3]

~~~~~
//##89. vectorized lazy lambda upgrades
[3 
[34]
[34, 34]
[34]
[34]
[34, 67]
[34, 45]
[34, 45]
[1, 2]
[3, 4, 5, 6, 49]
[34, 45, 56, 5, 25] 3]

~~~~~
//##90. upgraded anon void
okokok

~~~~~
//##91. upgraded anon void SAM tpye
okokok

~~~~~
//##92. bugfix on lambda def
[8 8 8 8]

~~~~~
//##93. imply and qualify returned lambda
ok12

~~~~~
//##94. imply and qualify returned lambda explicit return
ok12

~~~~~
//##95. imply and qualify returned lambda nested functions
ok[12 12]

~~~~~
//##96. imply and qualify returned lambda as lambda
ok[12 12]

~~~~~
//##97. imply and qualify returned lambda complex
ok[12 12]

~~~~~
//##98. imply and qualify returned lambda for SAM type
ok12