//##1. simple infer rhs
res: [item]

~~~~~
//##2. simple infer rhs no new
res: [item]

~~~~~
//##3. simple infer from typdef
res: [item]

~~~~~
//##4. simple infer from typdef no new
res: [item]

~~~~~
//##5. two typedefs can coexist
res: [[item] [item]]

~~~~~
//##6. bind to more than one thing needing qualification
res: [item]

~~~~~
//##7. bind to more than one thing needing qualification - array
res: [[item] []]

~~~~~
//##8. var later assigned
res: [ok]

~~~~~
//##9. var depends on another do it in one pass
res: ([], [ok])

~~~~~
//##10. var depends on another loops
res: ([], [ok])

~~~~~
//##11. var depends on another loops self ref
res: ([], [ok])

~~~~~
//##12. partial looping of dependants
res: ([], [ok])

~~~~~
//##13. partial looping of dependants via typedef
res: ([], [ok])

~~~~~
//##14. qualify a lhs gen type
res: ([], [ok])

~~~~~
//##15. complex inference
res: ([], [ok], [ok], [ok], [ok], [ok])

~~~~~
//##16. use qualification from usage in method
res: [hey there]

~~~~~
//##17. use qualification from usage in method with some local generics
res: [three]

~~~~~
//##18. use qualification from usage in constructor
res: [great]

~~~~~
//##19. use qualification from usage in funcref
res: [hey there]

~~~~~
//##20. return type qualification
res: [hey there]

~~~~~
//##21. qualification by setting field
res: [hey there]

~~~~~
//##22. via array usage
res: [[hey there] [hey there]]

~~~~~
//##23. complex inference
ok: nice: [nice]

~~~~~
//##24. infer list elements
res: [[hey there], [hey there]]

~~~~~
//##25. infer list elements via typedef
res: [[hey there], [hey there]]

~~~~~
//##26. infer list elements via typedef - just one
res: [[hey there]]

~~~~~
//##27. quliafy via funcinvoke method arg names
res: [hey there]

~~~~~
//##28. quliafy via funcinvoke consturctor arg names
res: [great]

~~~~~
//##29. quliafy via funcref
res: [hey there]

~~~~~
//##30. multi choice to infer generics
res: [12]

~~~~~
//##31. implicit return
res: [hey there]

~~~~~
//##32. implicit return w branching
res: [hey there]

~~~~~
//##33. partial qualification of generic types via usage on methods
res: MyGenericClass[x: hey, y: 18]

~~~~~
//##34. partial qualification of generic types via usage on methods one full
res: MyGenericClass[x: 12, y: null]

~~~~~
//##35. partial qualification of generic types via usage on methods multi quali
res: MyGenericClass[x: 12, y: 12]

~~~~~
//##36. set x and y at same time
res: MyGenericClass[x: hey, y: 12]

~~~~~
//##37. method call indirect gen type association
res: MyGenericClass[x: hey]

~~~~~
//##38. qulaified with a primative
res: MyGenericClass[x: 12]

~~~~~
//##39. watch out for generic bindings
res: MyGenericClass[x: 12]

~~~~~
//##40. bind generic field being set
res: MyGenericClass[x: 12]

~~~~~
//##41. bind when qualified via a funcref
res: MyGenericClass[x: ok]

~~~~~
//##42. bind when qualified via a funcref with type qualification
res: MyGenericClass[x: nice]

~~~~~
//##43. module level qualificaiton
res: [MyGenericClass[x: null] MyGenericClass[x: thing]]

~~~~~
//##44. inference of local class level fields
res: DoWork[x: [ok]]

~~~~~
//##45. inference of local class level fields prefix this
res: DoWork[x: [ok]]

~~~~~
//##46. qualify a method argument
res: [lovely]

~~~~~
//##47. subsequent assinment to something which is not new
res: [nice]

~~~~~
//##48. qualify a nested method argument
res: [lovely]

~~~~~
//##49. qualify a nested lambda
res: [lovely]

~~~~~
//##50. nested things ok
res: []

~~~~~
//##51. map short cuts
res: {12->ok}

~~~~~
//##52. list short cuts
res: [okok]

~~~~~
//##53. use pythonic - basic import and use of typedef
ok[ok]

~~~~~
//##54. map from list of tuples
ok{1->2, 3->4}

~~~~~
//##55. bugfix on default values and local generics qualification
1

~~~~~
//##56.  use pythonic - sorted no comparitors
[[1, 2, 2, 3, 3, 4, 4, 5], [1, 2, 2, 3, 3, 4, 4, 5], true, true]

~~~~~
//##57. bugfix on unknown generic return type
[5, 4, 4, 3, 3, 2, 2, 1]

~~~~~
//##58. bugfix on more than one default param with unbounded generics
[5, 4, 4, 3, 3, 2, 2, 1]

~~~~~
//##59. bugfix on comparitors SAM type
ok

~~~~~
//##60. explicit local generics comparitor
[5, 4, 4, 3, 3, 2, 2, 1]

~~~~~
//##61. infer sorted type from comparitor and other input
[5, 4, 4, 3, 3, 2, 2, 1]

~~~~~
//##62. infer comparitor
[5, 4, 4, 3, 3, 2, 2, 1]

~~~~~
//##63. comparitor and args
[5, 4, 4, 3, 3, 2, 2, 1]

~~~~~
//##64. datautils sorted list and set
[[5, 4, 4, 3, 3, 2, 2, 1], [5, 4, 4, 3, 3, 2, 2, 1], [23, 5, 4, 3, 2, 1], true, true]

~~~~~
//##65. sorted mix list etc
[1, 2, 3, 4]

~~~~~
//##66. reversed
[4, 3, 2, 1]

~~~~~
//##67. zip
[(1, 4), (2, 3), (3, 2), (4, 1)], [(1, 4, 4, 4), (2, 3, 3, 3), (3, 2, 2, 2), (4, 1, 1, 1)]

~~~~~
//##68. range map list
{1->5, 2->4, 3->3, 4->2, 5->1}

~~~~~
//##69. auto vect set add
[1, 2, 3, 4]

~~~~~
//##70. auto vect set add type infer
[1, 2, 3, 4]

~~~~~
//##71. import star
import ok

~~~~~
//##72. import star other way
import ok

~~~~~
//##73. import star from non compiled
import ok

~~~~~
//##74. set list etc auto imported
[4, 3, 2, 1]

~~~~~
//##75. partial inference from constructor and from method call
nice

~~~~~
//##76. partial inference from constructor and from method call more than one choice
nice

~~~~~
//##77. infer local generics
{0->val}

~~~~~
//##78. infer local generics partial
Holder(100)

~~~~~
//##79. infer local gens and map anon lambda as well 
{0->1}

~~~~~
//##80. infer local gens partial
{0->0}

~~~~~
//##81. data utils enumerate
[(0, 1), (1, 2), (2, 3), (3, 4), (4, 5)]

~~~~~
//##82. simple via superclass
res: [ok]

~~~~~
//##83. simple via superclass typedef generics
res: [ok]

~~~~~
//##84. simple via superclass typedef generics 2
res: ([], [ok])