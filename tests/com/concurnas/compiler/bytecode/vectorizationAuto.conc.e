//##1. simple additive
[11 12 ; 13 14]
[11 12 ; 13 14]
[2 3 ; 4 5]
[2 4 ; 6 8]
[102 104 ; 106 108]
[102 104 ; 106 108]

~~~~~
//##2. simple additive on list
[[11, 12], [13, 14]]
[[11, 12], [13, 14]]
[[2, 3], [4, 5]]
[[2, 4], [6, 8]]
[[102, 104], [106, 108]]
[[102, 104], [106, 108]]

~~~~~
//##3. simple additive avoid on op overload object
ok

~~~~~
//##4. simple multiplacitative
[10 20 ; 30 40]
[10 20 ; 30 40]
[2 4 ; 6 8]
[1 4 ; 9 16]
[100 400 ; 900 1600]
[10 40 ; 90 160]

~~~~~
//##5. simple pow 
[1 1024 ; 59049 1048576]
[10 100 ; 1000 10000]
[1 4 ; 9 16]
[1 4 ; 27 256]
[1 16 ; 19683 4294967296]
[1 16 ; 19683 4294967296]

~~~~~
//##6. relational
[false false false false false true true true true true]
[false false false false false true true true true true]
[false false true true true true true true true true]
[true true true true true true true true true true]
[false false false false false false false false false false]
[false false false false false false false false false false]

~~~~~
//##7. eq neq etc
[false false false false true false false false false false]
[false false false false true false false false false false]
[false true false false false false false false false false]
true
true
[true true true true true true true true true true]
[false, true]
[false, true]

~~~~~
//##8. supress auto vect for this case
[true, false]

~~~~~
//##9. but we do want auto vectorization here as operates on int type of A
[[false false false false false false false false true true], [false false false false false false false false false false]]

~~~~~
//##10. was broken before
[10 11 12 13 14 15 16 17 18 19]

~~~~~
//##11. vectorize object op override
[10 11 12 13 14 15 16 17 18 19]

~~~~~
//##12. auto vectorize object op override
[10 11 12 13 14 15 16 17 18 19]

~~~~~
//##13. dont auto vect op override
cool[1 2 3 4 5 6 7 8 9 10]9

~~~~~
//##14. list of thing haveing operator overloaded 
[11 12 13]

~~~~~
//##15. list of thing haveing operator overloaded return an array type BASIC
[11 21 31 ; 12 22 32 ; 13 23 33]

~~~~~
//##16. list of thing haveing operator overloaded return an array type
[11 21 31 ; 12 22 32 ; 13 23 33]

~~~~~
//##17. pl this works now
[[false false false false false false false false true true], [false false false false false false false false false false]]

~~~~~
//##18. bitshift norm and oo
[8 16 32 64 128 256 512 1024 2048 4096 ; 4 8 12 16 20 24 28 32 36 40]

~~~~~
//##19. is
[true false false true ; false true true false ; false true true false ; true false false true]

~~~~~
//##20. just works
fine

~~~~~
//##21. bitwise norm and oo
[0 2 2 ; 0 2 2]

~~~~~
//##22. thought id try this
[MyClas 11 MyClas 11 true]

~~~~~
//##23. prefix norm and oo
[2 3 4 5 6 2 3 4 5 6 MyClas 11 MyClas 7 MyClas 8]

~~~~~
//##24. postfix norm and oo
[2 3 4 5 6 1 2 3 4 5 MyClas 11 MyClas 7 MyClas 8]

~~~~~
//##25. prefix neg norm and oo
[1 2 3 4 5 -1 -2 -3 -4 -5 MyClas -10 MyClas -6 MyClas -7]

~~~~~
//##26. assign existing norm and oo
[11 12 13 14 15 MyClas 20 MyClas 16 MyClas 17]

~~~~~
//##27. with extension func defined
[MyClas 20 MyClas 16 MyClas 17 ; MyClas 110 MyClas 106 MyClas 107]

~~~~~
//##28. bugfix for missing this in ext fucntions on lhs of assignment
[MyClas 30 MyClas 26 MyClas 27]

~~~~~
//##29. bugfix for missing this in ext fucntions on lhs of assignment 2
[MyClas [30] MyClas [26] MyClas [27]]

~~~~~
//##30. bugfix on object array copy 
[MyClas 10 MyClas 6 MyClas 7 ; MyClas 10 MyClas 6 MyClas 7]

~~~~~
//##31. vect on thing with default arg
[11 12 13]

~~~~~
//##32. auto vect on thing with default arg
[11 12 13]

~~~~~
//##33. auto vect on thing with no default arg
[11 12 13]

~~~~~
//##34. basic funcref invoke
[11 12 13]

~~~~~
//##35. basic funcref invoke auto
[11 12 13]

~~~~~
//##36. bugfix on equiv stringrep
[true]

~~~~~
//##37. funcref invoke
[[101 102 103 104], [101, 102, 103, 104]]

~~~~~
//##38. out param used to breka this
[[101 102 103 104], [101, 102, 103, 104]]

~~~~~
//##39. auto vect new
[MC: 1 MC: 2 MC: 3 MC: 4 MC: 5 ; MC: 1 MC: 2 MC: 3 MC: 4 MC: 5]

~~~~~
//##40. auto vect funcref class and constructor
[MC: 1 MC: 2 MC: 3 ; MC: 1 MC: 2 MC: 3 ; MC: 1 MC: 2 MC: 3 ; MC: 1 MC: 2 MC: 3]

~~~~~
//##41. auto vect of funcrefs
[[MC: 1, MC: 2, MC: 3], [MC: 1, MC: 2, MC: 3]]

~~~~~
//##42. auto vect of actor calls
[[hi:[11 1] hi:[11 2] hi:[11 3]], [&hi:[11 1], &hi:[11 2], &hi:[11 3]]]

~~~~~
//##43. auto vect of new actor
[[MC: 1 MC: 2 MC: 3], [MC: 1, MC: 2, MC: 3]]

~~~~~
//##44. auto vect of new actor when actor itself 
[[MC: 1 MC: 2 MC: 3], [MC: 1, MC: 2, MC: 3], [MC: 1 MC: 2 MC: 3], [MC: 1, MC: 2, MC: 3]]

~~~~~
//##45. auto vect of calls on actor when actor itself 
[[hi:[11 1] hi:[11 2] hi:[11 3]], [&hi:[11 1], &hi:[11 2], &hi:[11 3]]]

~~~~~
//##46. auto vect expression lists
[101 102 103 104 105 ; 101 102 103 104 105]

~~~~~
//##47. auto vect expression lists class
[myCls 1 myCls 2 myCls 3 ; myCls 1 myCls 2 myCls 3]

~~~~~
//##48. auto vect arrayref operations
[4 5 ; 6 7 ; 7 8]

~~~~~
//##49. auto vect arrayref operations op overload
[cool [4 5] cool [6 7] cool [7 8]]

~~~~~
//##50. mkt tmp var when no nested vectees 
[[1 2 3 4], [2 3 4 5]]

~~~~~
//##51. mkt tmp var when no nested vectees AUTO 
[[1 2 3 4], [2 3 4 5]]

~~~~~
//##52. this thing works
[4 5]

~~~~~
//##53. this thing works AUTO
[4 5]

~~~~~
//##54. this thing works AUTO
[4 5]

~~~~~
//##55. vectorized in expression
[true false true]

~~~~~
//##56. vectorized in expression matr
[true false true ; true false true]

~~~~~
//##57. vectorized in expression op overload
[false true false true ; false true false true]

~~~~~
//##58. choose correct op overload bug
[hi: true hi: true]

~~~~~
//##59. bugfix on calling of vect funct
[hi: true hi: true]

~~~~~
//##60. vectorized in expression op overload when choices
[hi: true hi: true]

~~~~~
//##61. vectorized in expression op overload when choices EXT function
[false true false true ; false true false true]

~~~~~
//##62. vectorized in expression rhs is vect with oo
[[[true true true]], [[false false false]]]

~~~~~
//##63. vectorized in expression rhs is vect with oo as above but implicit
[[[true true true]], [[false false false]]]

~~~~~
//##64. vectorized in expression list of
[[[true true true]], [[false false false]]]

~~~~~
//##65. auto vectoriz in rhs
[false, true]

~~~~~
//##66. auto vectoriz in lhs
[false true false true ; false true false true]

~~~~~
//##67. field access hat
[12 13 14 15]

~~~~~
//##68. field access hat 2d and goes to getter
[hi: 12 hi: 13 hi: 14 hi: 15 ; hi: 1 hi: 2 hi: 3]

~~~~~
//##69. field access hat 2d and goes to getter via overriden getter
[hi: 12 hi: 13 hi: 14 hi: 15 ; hi: 1 hi: 2 hi: 3]

~~~~~
//##70. via getter but this didnt used to work very well
[hi: 12 hi: 13 hi: 14 hi: 15 ; hi: 1 hi: 2 hi: 3]

~~~~~
//##71. is field
[hi: true hi: true]

~~~~~
//##72. field and subfield
hi[1 2 ; 3 4]

~~~~~
//##73. assign to vectfield ref
[11 12 11 12 13 14 15 16 Myclass: 99 Myclass: 99 Myclass: 99 Myclass: 99 Myclass: 99 Myclass: 99 Myclass: 99 Myclass: 99]

~~~~~
//##74. bugfix on lhs and rhs assign
[11 12 13]

~~~~~
//##75. bugfix on lhs and rhs assign ext 2
[Myclass: 101 Myclass: 102 Myclass: 101 Myclass: 102 Myclass: 103 Myclass: 104 Myclass: 105 Myclass: 106]

~~~~~
//##76. field ref pre post
[Myclass: 2 Myclass: 3 Myclass: 4]

~~~~~
//##77. field ref in place
[Myclass: 101 Myclass: 102 Myclass: 103]

~~~~~
//##78. in place assigmnet
[Myclass: 101 Myclass: 102 Myclass: 103]

~~~~~
//##79. vect field ref long chain
lovely: [Myclass: FieldCls: 10 Myclass: FieldCls: 10 ; Myclass: FieldCls: 10 Myclass: FieldCls: 10 Myclass: FieldCls: 101 Myclass: FieldCls: 102 ; Myclass: FieldCls: 103 Myclass: FieldCls: 104 101 102 ; 103 104]

~~~~~
//##80. no arg funcref
[result!, result!]

~~~~~
//##81. vect called each time for assignment not just once
ok[[0 1 2 3], 4]

~~~~~
//##82. auto vect field access
[1 2 3 4 5]

~~~~~
//##83. auto vect field assignment
[Myclass: 69 Myclass: 69 Myclass: 69 Myclass: 69 Myclass: 69]

~~~~~
//##84. auto vect field inc
[Myclass: 2 Myclass: 3 Myclass: 4 Myclass: 5 Myclass: 6]

~~~~~
//##85. auto vect in place ops
[Myclass: 67 Myclass: 68 Myclass: 69 Myclass: 70 Myclass: 71 ; Myclass: 100 Myclass: 101 Myclass: 102 Myclass: 103 Myclass: 104]

~~~~~
//##86. auto vect manual in place ops
[Myclass: 67 Myclass: 68 Myclass: 69 Myclass: 70 Myclass: 71]

~~~~~
//##87. auto vect ops nested etc complex
lovely: [Myclass: FieldCls: 10 Myclass: FieldCls: 10 ; Myclass: FieldCls: 10 Myclass: FieldCls: 10 Myclass: FieldCls: 101 Myclass: FieldCls: 102 ; Myclass: FieldCls: 103 Myclass: FieldCls: 104 101 102 ; 103 104]

~~~~~
//##88. acuto vect func invoke
[hi: 1 hi: 2 hi: 3 hi: 4 hi: 5]

~~~~~
//##89. acuto vect func ref
[hi: 1, hi: 2, hi: 3, hi: 4, hi: 5]

~~~~~
//##90. inner class init
[SubClass: (1, 8) SubClass: (2, 8) SubClass: (3, 8) SubClass: (4, 8) SubClass: (5, 8)]

~~~~~
//##91. inner class init auto vect
[SubClass: (1, 8) SubClass: (2, 8) SubClass: (3, 8) SubClass: (4, 8) SubClass: (5, 8)]

~~~~~
//##92. cnt called for each iteration
[ok: 0 ok: 1 ok: 2]

~~~~~
//##93. new nested class
[[SubClass: 1, 0 SubClass: 2, 1 SubClass: 3, 2 SubClass: 4, 3 SubClass: 5, 4], 5]

~~~~~
//##94. new nested class implicit
[SubClass: 1, 8 SubClass: 2, 8 SubClass: 3, 8 SubClass: 4, 8 SubClass: 5, 8]

~~~~~
//##95. new nested class auto vect
[[SubClass: 1, 0 SubClass: 2, 1 SubClass: 3, 2 SubClass: 4, 3 SubClass: 5, 4], 5]

~~~~~
//##96. new nested class auto vect op overload
[new: HiThere 1 8 new: HiThere 2 8 new: HiThere 3 8 new: HiThere 4 8 new: HiThere 5 8]

~~~~~
//##97. ext function on ar type plus auto vect
[ok[Myclass: 1 Myclass: 2 Myclass: 3] ok[Myclass: 4 Myclass: 5]]

~~~~~
//##98. expression lists func invoke ref
[[hi: 1 hi: 2 hi: 3 hi: 4 hi: 5], [hi: 1, hi: 2, hi: 3, hi: 4, hi: 5]]

~~~~~
//##99. expression lists field assignment
[[Myclass: 100 Myclass: 100 Myclass: 100], [100 100 100]]

~~~~~
//##100. expression lists field assignment in place ops
[Myclass: 101 Myclass: 102 Myclass: 103]

~~~~~
//##101. vectorize dneste class new via expr list
[[SubClass: 1, 0 SubClass: 2, 1 SubClass: 3, 2 SubClass: 4, 3 SubClass: 5, 4], 5]

~~~~~
//##102. this thing works ok
[false, false]

~~~~~
//##103. in on lists directed to underlying correctly
[[true, true, true] [true, true, true] false]

~~~~~
//##104. in on lists directed to underlying correctly - advanced
[[false] [false]]

~~~~~
//##105. set and get of the array of list of int array
[[[[1]] [[1]] [[1]] [[1]]], [1, 1, 1, 1]]

~~~~~
//##106. array ref element vectorized
[[2 4 7], [2 3 ; 4 5 ; 7 8]]

~~~~~
//##107. array ref assign
[1 2 3 4 5 99 99 99 9 10]

~~~~~
//##108. array ref assign AUTO vect version
[1 2 3 4 5 99 99 99 9 10]

~~~~~
//##109. inc array ref and auto
[1 2 3 4 5 8 9 10 9 10]

~~~~~
//##110. cool
[106 107 108]

~~~~~
//##111. this is fine
[106 107 108]

~~~~~
//##112. assignment of ar
[1 2 3 4 5 206 207 208 9 10]

~~~~~
//##113. assignment of ar with arg vect
[5 3 4 5 ; 5 5 6 7 8 ; 5 8 9 10 11]

~~~~~
//##114. cool i guess
[2 4 7]

~~~~~
//##115. cool i guess AUTO
[2 4 7]

~~~~~
//##116. bugfix correctly operates on vect type now
[2 1 1 ; 2 1 1 ; 2 1 1]

~~~~~
//##117. ar vect postfix op
[[1 1 1], [2 1 1 ; 2 1 1 ; 2 1 1]]

~~~~~
//##118. ar vect assign
[10 1 1 ; 10 1 1 ; 10 1 1]

~~~~~
//##119. ar in place
[101 1 1 ; 101 1 1 ; 101 1 1]

~~~~~
//##120. great it works
[[[[1]] [[1]] [[1]] [[1]]], [1 1 1 1]]

~~~~~
//##121. lhs of vect ar invoke only once
[1, [2 1 2 ; 1 1 1 ; 1 1 1]]

~~~~~
//##122. lhs of vect ar arg vect invoke only once
[1, [2 1 1 ; 2 1 1 ; 2 1 1]]

~~~~~
//##123. ar ops on element and subs
[[1 1 1], [1 1 1 ; 1 1 1 ; 1 1 1], [999 1 1 ; 1 999 1 ; 1 1 999], [2 1 1 ; 1 2 1 ; 1 1 2], [101 1 1 ; 1 101 1 ; 1 1 101]]

~~~~~
//##124. bugfix return correct type on vect func ref with args also vect
[false true true]

~~~~~
//##125. bugfix on non eq assingment special method dup calls on lhs
[[101 1 1 ; 1 101 1 ; 1 1 101], 1, [101 1 1 ; 1 101 1 ; 1 1 101], 2]
