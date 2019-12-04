//##1. simple match
[?, 1 or 2, 1 or 2, ?, ?, five, ?, ?, gt 7, gt 7]

~~~~~
//##2. as above but defo returns
[?, 1 or 2, 1 or 2, ?, ?, five, ?, ?, gt 7, gt 7]

~~~~~
//##3. more complex
[or case, ten, explicit 9, gt three not ten, gt 5, gt to 11, something else-5]

~~~~~
//##4. in for arrays
[true, false, false, true, true, true]

~~~~~
//##5. spot bugfix
[ small ,  small ,  big 30]

~~~~~
//##6. match on in
[small, small, big30]

~~~~~
//##7. untyped expr
[small, big3, big30]

~~~~~
//##7.b untyped expr 2
[small, big3, less small, big30]

~~~~~
//##8. typed refed expr
[small, else: 3, said hi, else: []]

~~~~~
//##9. typed refed without an expression
[an int2, an int3, said hi, else: []]

~~~~~
//##10. use thing inside block
[other int: 2, got 3: 3, said hi: hello, else: []]

~~~~~
//##11. only gennerate used vars
[other int: 2, got 3: 3, said hi: , else: []]

~~~~~
//##12. use thing in case block
[small: 2, big3, big30]

~~~~~
//##13. just typed
[small: 2, another int: 103, a str: 5, sth else: 2.0]

~~~~~
//##14. var typed expressionless
[an int: 102, an int: 103, sth else: hello, sth else: 2.0]

~~~~~
//##15. catch all
[an int: [2, 2], an int: [3, 3], catch all: [hello, hello], catch all: [2.0, 2.0]]

~~~~~
//##16. catch all => else
[an int: [2, 2], an int: [3, 3], catch all: [hello, hello], catch all: [2.0, 2.0]]

~~~~~
//##16. catch all with TypedCaseExpression
[an int: [2, 2], an int: [3, 3], catch all: hello, catch all: 2.0]

~~~~~
//##17. bugfix on number to primative convetion
[9, 9]

~~~~~
//##18. supertype of thing being matched will always match
[an int: [2, 2], an int: [3, 3], catch all: 2.0]

~~~~~
//##19. handles null ok
[an int: [2, 2], an int: [3, 3], else: 2.0, a null]

~~~~~
//##20. else not needed if not certain to ret
[an int: [2, 2], an int: [3, 3], something else, a null]

~~~~~
//##21. cater for captured case expression pres correctly
[case1, case other]

~~~~~
//##22. case on enums
[case1, case2, other case: CASE3]

~~~~~
//##23. case on enums in short form
[case1, case2, other case: CASE3]

~~~~~
//##24. refname in or caseExpression
[case2 or 3, case2 or 3, fail]

~~~~~
//##25. for enums can use ors
[case1, case2 or 3, case2 or 3]

~~~~~
//##26. we cover all the enum cases so no else needed
[case1, case2 or 3, case2 or 3]

~~~~~
//##27. non exhaustive enum match warning can be supressed
[case1, case2, dunno]

~~~~~
//##28. catch this or that
en exception

~~~~~
//##29. instanceof works with or
true

~~~~~
//##30. this or that CaseExpressionAssign
[child: Child1, child: Child2, not sure what this isdunno]

~~~~~
//##31. this or that CaseExpressionAssign with expr
[fancy child: Child1, child: Child2, not sure what this isdunno]

~~~~~
//##32. this or that CaseExpressionAssign with NO expr
[child: Child1Child1, child: Child2Child2, not sure what this isdunno]

~~~~~
//##33. this or that TypedCaseExpression with NO expr
[child: Child1, child: Child2, not sure what this isdunno]

~~~~~
//##34. this or that TypedCaseExpression with expr
[child: Child1, child: Child2, not sure what this isdunno]

~~~~~
//##35. match on dot op external stuff
[another int1, little int, little int]

~~~~~
//##36. non refname assignemnt in the match
[another int101, another int109, little int]

~~~~~
//##37. non refname assignemnt in the match assign existing
[another int1, another int9, little int]

~~~~~
//##38. non refname assignemnt in the match assign new
[another int1, another int9, little int]

~~~~~
//##39. use compact case def style
[another int1, another int9, little int]

~~~~~
//##40. operates on object ref
[another int1, another int9, little int]

~~~~~
//##41. operates on object ref some more
[an int, another thing, an int ten]

~~~~~
//##42. operates on object ref some more seems ok
[an int, another thing9:true, an int ten, another thing10:false]

~~~~~
//##43. bugfix full use of vars inside thingy
[child: Child1 , child: Child2 , not sure what this isdunno]

~~~~~
//##44. ops used on post basis
[oh, an int gt 20, not sure what this is2]

~~~~~
//##45. bug with lt
[not sure what this is200, oh, an int lt 20]

~~~~~
//##46. bugfix match on expression element resolution
MyClass [1, 2]

~~~~~
//##47. also on CaseExpressionWrapper
MyClass [1, 2]

~~~~~
//##48. also on TypedCaseExpression
[[false, big2, big3, big4, big30], [true, small 2, small 3 , another small, big30]]

~~~~~
//##49. alos on pre post or 
[[false, big2, big3, big4, big5, big30], [true, small 2, small 3 , small 4 or 5 , small 4 or 5 , big30]]

~~~~~
//##50. bugfix on compound stmt first element of string concat
[is a Palindrome, is not a Palindrome]

~~~~~
//##51. nice littl test
[1, 2, 3, 4][5, 6, 7][8, 9, 10]

~~~~~
//##52. more concise syntax
[first case, first case, other case]

~~~~~
//##53. more compact form
is a Palindrome

~~~~~
//##54. bugfix on label tagging when match used in else block
ok

~~~~~
//##55. this was a bug now its ok
its ok

~~~~~
//##56. match tuple 
3: 3, 4, 5
gt 4
lt 3
4: ok
4: ok with double
5: 1, 2, 3, 4, spanner
fail fails

~~~~~
//##57. match tuple case variant
3: 3, 4, 5
gt 4
lt 3
4: ok
4: ok with double
5: 1, 2, 3, 4, spanner
fail fails

~~~~~
//##58. tuple content match
[(1, 1), (2, 2), Fizz, (1, 4), Buzz, Fizz, (1, 2), (2, 3), Fizz, Buzz, (2, 1), Fizz, (1, 3), (2, 4), FizzBuzz]

~~~~~
//##59. tuple content match vs var
[(1, 1), (2, 2), Fizz, (1, 4), Buzz, Fizz, (1, 2), (2, 3), Fizz, Buzz, (2, 1), Fizz, (1, 3), (2, 4), FizzBuzz]

~~~~~
//##60. tuple partial compoent test
[(1, 1, 1), (2, 2, 0), (0, 3, 1), got 0, (2, 0, 1), (0, 1, 0), (1, 2, 1), (2, 3, 0), (0, 4, 1), got 0, (2, 1, 1), (0, 2, 0), got 1, (2, 4, 0), (0, 0, 1)]

~~~~~
//##61. tuple match with extra test
[got (1, 1), got (2, 2), got (0, 3), got (1, 4), got (2, 0), got (0, 1), got (1, 2), got (2, 3), got (0, 4), got (1, 0), got (2, 1), got (0, 2), got (1, 3), got (2, 4), got (0, 0)]

~~~~~
//##62. impossible to match
[n, n, n, n, n, n, n, n, n, n, n, n, n, n, n]

~~~~~
//##63. match on tuple
(1, 1):: [eq2 a:1 :: extract: 1, 1]
(2, 2):: [any: (2, 2) :: eq a:2]
(0, 3):: [zN :: extract: 0, 3]
(1, 4):: [eq2 a:1 :: extract: 1, 4]
(2, 0):: [Nz :: eq a:2]
(0, 1):: [zN :: extract: 0, 1]
(1, 2):: [eq2 a:1 :: extract: 1, 2]
(2, 3):: [any: (2, 3) :: eq a:2]
(0, 4):: [zN :: extract: 0, 4]
(1, 0):: [Nz :: extract: 1, 0]
(2, 1):: [any: (2, 1) :: eq a:2]
(0, 2):: [z2 :: extract: 0, 2]
(1, 3):: [eq2 a:1 :: extract: 1, 3]
(2, 4):: [any: (2, 4) :: eq a:2]
(0, 0):: [zN :: extract: 0, 0]

~~~~~
//##64. tuple match with extra check using values
[n, n, n, n, n, n, n, n, n, n, n, n, n, n, got (0, 0)]

~~~~~
//##65. this match is fine
[catch all: [2, 2], catch all: [3, 3], catch all: [hello, hello], catch all: [2.0, 2.0]]

~~~~~
//##66. was broken now fine
weird (1, 2)

~~~~~
//##67. tuple catch all
weird (1, 2)

~~~~~
//##68. fizzbuzz example
[(1, 1), (2, 2), Fizz, (1, 4), Buzz, Fizz, (1, 2), (2, 3), Fizz, Buzz, (2, 1), Fizz, (1, 3), (2, 4), FizzBuzz]

~~~~~
//##69.match on object contents
record: 2

~~~~~
//##70.match on object contents case
record: 2

~~~~~
//##71.match on object contents case long dot name version
[record: 2 record M2: 3]

~~~~~
//##72. match on object contents nested version
[record: (2, Record: 2, Dave, [PassDetails: one, 21]), ignore]

~~~~~
//##73. match on object with var assignment
[Person. Born: 1945, Person. Born: 1962, unknown input]

~~~~~
//##74. another way to write match
[or case, ten, explicit 9, gt three not ten, gt 5, gt to 11, something else-5]

~~~~~
//##75. another way to write match 2
[or case, ten, explicit 9, gt three not ten, gt 5, gt to 11, something else-5]

~~~~~
//##76. another alternative
[small, else: 3, said hi, else: []]

