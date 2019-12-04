//##1. block simple
9

~~~~~
//##1. block complex
9

~~~~~
//##1b. block func
9

~~~~~
//##1c. block func var ret
9

~~~~~
//##1d. block in an if stmt
ok

~~~~~
//##2.a if else
[2, 1]

~~~~~
//##2.b if elif else
[1, 15, 16]

~~~~~
//##2.c early termination
[1, 15, 7]

~~~~~
//##2.d why not
[8, 8, 8]

~~~~~
//##2.e check casts
[1.0, 15.0, 7.0]

~~~~~
//##3.a try catch - basic
6

~~~~~
//##3.b try catch - with final
12

~~~~~
//##3.c try catch - detailed
[7, 6]

~~~~~
//##4.a try catch - funcRepoint
a6

~~~~~
//##4.b try catch - funcRepoint with final
a32999a

~~~~~
//##4.c try catch - repoint var capture
[a13, a12]

~~~~~
//##4.d try catch - mul it
144

~~~~~
//##4.e try catch - reuse same code
[168, 168, 192, 2147483647]

~~~~~
//##4.f try catch - multi nested
[2209, 1209, 2209, 1108, 2108, 1209, 2108, 1108]

~~~~~
//##4.g try catch - no translation required
[13, 12]

~~~~~
//##4.h try catch - no translation required - as class
[13, 12]

~~~~~
//##5. for loop simple cases
[[2, 4, 6, 8, 10, 12, 14, 16, 18, 20], [2, 4, 6, 8, 10, 12, 14], [4, 8, 12, 16, 20, 24, 28, 32, 36, 40]], [2, 4, 6, 8, 10, 12, 14, 16, 18, 20]

~~~~~
//##5.b for loop simple cases - nested for loops
[[[1, 2, 3, 4, 5, 6, 7, 8, 9, 10], [2, 4, 6, 8, 10, 12, 14, 16, 18, 20], [3, 6, 9, 12, 15, 18, 21, 24, 27, 30], [4, 8, 12, 16, 20, 24, 28, 32, 36, 40], [5, 10, 15, 20, 25, 30, 35, 40, 45, 50], [6, 12, 18, 24, 30, 36, 42, 48, 54, 60], [7, 14, 21, 28, 35, 42, 49, 56, 63, 70], [8, 16, 24, 32, 40, 48, 56, 64, 72, 80], [9, 18, 27, 36, 45, 54, 63, 72, 81, 90], [10, 20, 30, 40, 50, 60, 70, 80, 90, 100]]
, [[1, 2, 3], [2, 4, 6], [3, 6, 9]]
, [[1, 2, 3, 4, 5, 6, 7, 8, 9, 10], [2, 4, 6, 8, 10, 12, 14, 16, 18, 20], [3, 6, 9, 12, 15, 18, 21, 24, 27, 30], [4, 8, 12, 16, 20, 24, 28, 32, 36, 40], [5, 10, 15, 20, 25, 30, 35, 40, 45, 50], [6, 12, 18, 24, 30, 36, 42, 48, 54, 60], [7, 14, 21, 28, 35, 42, 49, 56, 63, 70], [8, 16, 24, 32, 40, 48, 56, 64, 72, 80], [9, 18, 27, 36, 45, 54, 63, 72, 81, 90], [10, 20, 30, 40, 50, 60, 70, 80, 90, 100]]
, [[1, 2, 3], [2, 4, 6], [3, 6, 9]]
]

~~~~~
//##5.b for loop simple cases - nested for loops - array version
[[[1, 2, 3, 4, 5, 6, 7, 8, 9, 10], [2, 4, 6, 8, 10, 12, 14, 16, 18, 20], [3, 6, 9, 12, 15, 18, 21, 24, 27, 30], [4, 8, 12, 16, 20, 24, 28, 32, 36, 40], [5, 10, 15, 20, 25, 30, 35, 40, 45, 50], [6, 12, 18, 24, 30, 36, 42, 48, 54, 60], [7, 14, 21, 28, 35, 42, 49, 56, 63, 70], [8, 16, 24, 32, 40, 48, 56, 64, 72, 80], [9, 18, 27, 36, 45, 54, 63, 72, 81, 90], [10, 20, 30, 40, 50, 60, 70, 80, 90, 100]]
, [[1, 2, 3], [2, 4, 6], [3, 6, 9]]
, [[1, 2, 3, 4, 5, 6, 7, 8, 9, 10], [2, 4, 6, 8, 10, 12, 14, 16, 18, 20], [3, 6, 9, 12, 15, 18, 21, 24, 27, 30], [4, 8, 12, 16, 20, 24, 28, 32, 36, 40], [5, 10, 15, 20, 25, 30, 35, 40, 45, 50], [6, 12, 18, 24, 30, 36, 42, 48, 54, 60], [7, 14, 21, 28, 35, 42, 49, 56, 63, 70], [8, 16, 24, 32, 40, 48, 56, 64, 72, 80], [9, 18, 27, 36, 45, 54, 63, 72, 81, 90], [10, 20, 30, 40, 50, 60, 70, 80, 90, 100]]
, [[1, 2, 3], [2, 4, 6], [3, 6, 9]]
]

~~~~~
//##6.a - simple while
[2, 4, 6, 8, 10]

~~~~~
//##6.b - simple while - complex
[[[4, 12, 24, 40, 60], [16, 36, 60, 88, 120]], 
[[4, 12, 24, 40, 60], [16, 36, 60, 88, 120]], 
[[4, 12, 24, 40, 60], [16, 36, 60, 88, 120]], 
[[4, 12, 24, 40, 60], [16, 36, 60, 88, 120]], 
[[4, 12, 24, 40, 60], [16, 36, 60, 88, 120]]]

~~~~~
//##7.a break continue returns - simple while
[[1, 2, 3, 4, 5, 6, 7, 8, 9, 10], [1]]

~~~~~
//##7.b break continue returns - complex while fors
[[0, 1, 2, 3, 4, 5, 6, 7, 8, 9], [0]]
[[0, 1, 2, 3, 4, 5, 6, 7, 8, 9], [0]]
[[0, 1, 2, 3, 4, 5, 6, 7, 8, 9], [0], [0, 1, 2, 3, 4, 5, 6, 7, 8, 9], [0]]

~~~~~
//##7.c break continue returns - complex while fors -  inc early cont
[[0, 2, 4, 6, 8], [1, 3, 5, 7, 9], [1]]
[[1, 3, 5, 7, 9], [1]]
[[1, 3, 5, 7, 9], [1], [1, 3, 5, 7, 9], [1]]

~~~~~
//##7.d break continue returns - complex while fors -  inc early cont p90
[[0, 90, 2, 90, 4, 90, 6, 90, 8, 90], [90, 1, 90, 3, 90, 5, 90, 7, 90, 9], [90, 1]]
[[90, 1, 90, 3, 90, 5, 90, 7, 90, 9], [90, 1]]
[[90, 1, 90, 3, 90, 5, 90, 7, 90, 9], [90, 1], [90, 1, 90, 3, 90, 5, 90, 7, 90, 9], [90, 1]]

~~~~~
//##7.e break continue returns - complex while fors -  inc early break p90
[[0, 90], [90], [90]]
[[90], [90]]
[[90], [90], [90], [90]]

~~~~~
//##7.f break continue returns - complex while fors -  inc early break
[[0], [], [0]]
[[0], [0]]
[[0], [0], [0], [0]]

~~~~~
//##8. a real example
[[1:_, 2:2, 3:3, 4:2, 5:_, 6:2, 7:_, 8:2, 9:3, 10:2], 
, [1:_, 2:2, 3:3, 4:2, 5:_, 6:2, 7:_, 8:2, 9:3, 10:2], 
, [0:2, 1:_, 2:2, 3:3, 4:2, 5:_, 6:2, 7:_, 8:2, 9:3], 
, [2, _, 2, 3, 2, _, 2, _, 2, 3]]

~~~~~
//##8.b a real example - 2
[[true, true, true], [true, true, true], [true, true, true]]

~~~~~
//##8.c a real example - 3 - as aboe but simpler
[[true, true, true], [true, true, true], [true, true, true]];;
[true, true, true]
[true, true, true]
[true, true, true]

~~~~~
//##9.a exceptions - simple
[12, 99, 12, 99, 12, 99, 12, 99, 12]::[99, 99, 99, 99, 99, 99, 99, 99, 99]

~~~~~
//##9.b exceptions - adv
[12, 99, 99, 99, 12, 99, 12, 99, 99]

~~~~~
//##9.c exceptions - adv 2
[93, 22, 33, 22, 93, 11, 93, 22, 33]

~~~~~
//##9.cc exceptions - adv 3
[33, 22, 93, 22, 33, 11, 33, 22, 93]

~~~~~
//##9.d exceptions - adv 2 - for block old
[93, 22, 33, 22, 93, 11, 93, 22, 33]

~~~~~
//##10. more elaborate while
[[y1, l2, y3, l4, y5, p6, y7, l8, y9, l10], [l2, l4, p6, l8, l10, p12, l14, l16, p18, l20], [y3, p6, y9, p12, y15, p18, y21, p24, y27, p30], [l4, l8, p12, l16, l20, p24, l28, l32, p36, l40], [y5, l10, y15, l20, y25, p30, y35, l40, y45, l50], [p6, p12, p18, p24, p30, p36, p42, p48, p54, p60], [y7, l14, y21, l28, y35, p42, y49, l56, y63, l70], [l8, l16, p24, l32, l40, p48, l56, l64, p72, l80], [y9, p18, y27, p36, y45, p54, y63, p72, y81, p90], [l10, l20, p30, l40, l50, p60, l70, l80, p90, l100]]

~~~~~
//##11. handly little mini test
9

~~~~~
//##12. complex exception cases
[93, 22, 33, 22, 93, 22, 93, 22, 33]

~~~~~
//##12.b complex exception cases
[1000, 1000, 1000, 1000, 1000, 1000, 1000, 1000, 1000]

~~~~~
//##12.c complex exception cases - no stuff after
[555, 555, 555, 555, 555, 555, 555, 555, 555]

~~~~~
//##12.d complex exception cases - no stuff after getting better
[93, 22, 33, 22, 93, 22, 93, 22, 33]

~~~~~
//##12.e complex exception cases - dunno
[99, 22, 33, 22, 99, 22, 99, 22, 33]

~~~~~
//##12.f complex exception cases - dunno 2
[33, 22, 33, 22, 33, 22, 33, 22, 33]

~~~~~
//##12.g complex exception cases - dunno 3
[33, 22, 33, 22, 33, 22, 33, 22, 33]

~~~~~
//##12.h complex exception cases - dunno 4
[33, 22, 33, 22, 33, 22, 33, 22, 33]

~~~~~
//##13. minor point
[999, 999, 999]

~~~~~
//##14. nest try catch with fin
[88, 88, 88, 88, 88, 88, 88, 88, 88]

~~~~~
//##15. the fin does the return op
[33, 33, 33, 33, 33, 33, 33, 33, 33]

~~~~~
//##16. last thing in function is ret
ok

~~~~~
//##17. last thing in lambda is ret
12

~~~~~
//##18. ensure old ways still work
fine

~~~~~
//##19.a def with final
19

~~~~~
//##19.b def with final
12

~~~~~
//##19.c def with final - as above but void
12

~~~~~
//##19.d.a def with final - finally void
12

~~~~~
//##19.d.b def with final - finally void 2
12>1

~~~~~
//##19.d.b def with final - finally void 3
12

~~~~~
//##19.e dunno why this is here
22

~~~~~
//##20. one line func def
[great, work]

~~~~~
//##20.b one line func def - lambda
[great, work]

~~~~~
//##20.c one line func def - constru
10

~~~~~
//##21. minor bug i found by chance
12

~~~~~
//##21.b minor bug i found by chance - phew least this is ok
12

~~~~~
//##22.a Type inference
[hi, hi, hi, hi]

~~~~~
//##22.b Type inference - on lambda
15

~~~~~
//##23. another edge case, block can have immediate use
false

~~~~~
//##24. this was playing up, now its ok
8

~~~~~
//##25. pop the thing constructed or array if not required
ok

~~~~~
//##26. last thing ret iso blocks and blocks
[12, 12, [12:, 12], 12]

~~~~~
//##27. check for block old
fine

~~~~~
//##28. sync block
hi

~~~~~
//##29. incomplete statements if tcf last thing ret bevahour
2

~~~~~
//##30. misc - the ret is not used
[10, 10, 10, 10, 10, 10, 10, 10]

~~~~~
//##31. misc - ltr 
[99, 22, 11, 22, 99, 11, 99, 22, 11]

~~~~~
//##31.b misc - ltr 2
[99, 22, 11, 22, 99, 11, 99, 22, 11]

~~~~~
//##31.c misc - ltr 3
[33, 22, 11, 22, 33, 11, 33, 22, 11]

~~~~~
//##32. misc - easy while x2
[[2, 2, 2, 2, 2, 2, 2, 2, 2, 2], [2, 2, 2, 2, 2, 2, 2, 2, 2, 2]]

~~~~~
//##33. misc - infer return type
hi

~~~~~
//##34. for loop eh why not
[7, 7]

~~~~~
//##35. for loop variants
[[22, 22, 22, 11, 11, 11, 11, 11, 11, 11, 11], 
, [22, 22, 22, 11, 11, 11, 11, 11, 11, 11, 11], 
, [22, 22, 22, 11, 11, 11, 11, 11, 11, 11, 11]]

~~~~~
//##35.a for loop variants - via array
[[22, 22, 22, 11, 11, 11, 11, 11, 11, 11, 11], 
, [22, 22, 22, 11, 11, 11, 11, 11, 11, 11, 11], 
, [22, 22, 22, 11, 11, 11, 11, 11, 11, 11, 11]]

~~~~~
//##36. easy while
[22, 22, 11, 11, 11, 11, 11, 11, 11, 11, 11]

~~~~~
//##37. nest while
[[22, 22, 22, 22, 22], [22, 22, 22, 22, 22], [11, 11, 11, 11, 11], [11, 11, 11, 11, 11], [11, 11, 11, 11, 11]]

~~~~~
//##38. while and tcf
[12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12]

~~~~~
//##39. simple for
[22, 22, 22, 11, 11, 11, 11, 11, 11, 11]

~~~~~
//##39.b simple for - new
[22, 22, 11, 11]

~~~~~
//##39.c simple for - new but old
[22, 22, 11]

~~~~~
//##40. another one
[0, 1, 2, 3, 4, 5, 6, 7, 8, 9]

~~~~~
//##41. for and while no need cont - for map old
[99, 99, 99]

~~~~~
//##41.b for and while no need cont - for old
[99, 99, 99]

~~~~~
//##42.c for and while no need cont - for new
[99, 99, 99]

~~~~~
//##42.d break and continue - for old
[[0, 1, 2, 3, 4, 99, 6, 7, 8, 9], [0, 1, 2, 3, 4, 5, 6, 7, 8, 9], [0, 1, 2, 3, 4, 99], [0, 1, 2, 3, 4, 99, 6, 7, 8, 9]]

~~~~~
//##42.e break and continue - for new maps to old
[[1, 2, 3, 4, 99, 6, 7, 8, 9, 10], [1, 2, 3, 4, 5, 6, 7, 8, 9, 10], [1, 2, 3, 4, 99], [1, 2, 3, 4, 99, 6, 7, 8, 9, 10]]

~~~~~
//##42.f break and continue - new for
[[1, 2, 3, 4, 99, 6, 7, 8, 9, 10], [1, 2, 3, 4, 5, 6, 7, 8, 9, 10], [1, 2, 3, 4, 99], [1, 2, 3, 4, 99, 6, 7, 8, 9, 10]]

~~~~~
//##42.f break and continue - on its own
[1, 2, 3, 4, 99]

~~~~~
//##42.f break and continue - while
[[1, 2, 3, 4, 99, 6, 7, 8, 9, 10, 11], [1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11], [1, 2, 3, 4, 99], [1, 2, 3, 4, 99, 6, 7, 8, 9, 10, 11]]

~~~~~
//##43. the else is optional if the if is in a loop
[[99, 99, 99, 99, 99], [99, 99, 99, 99, 99, 99], [99, 99, 99, 99, 99], [99, 99, 99, 99, 99]]

~~~~~
//##44. nice and concise
[3, 4, 5, 6, 7]