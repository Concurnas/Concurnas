//##49. weird
[MyClass1 MyClass2 ; MyClass3 MyClass4]

~~~~~
//##1. check for null in conditions
its fine

~~~~~
//##2. null unsafe
8

~~~~~
//##3. safe operator
null

~~~~~
//##4. null unsafe used as operator
npe: java.lang.NullPointerException 

~~~~~
//##5. elvis operator
ok[hey alternative2]

~~~~~
//##6. non conc type defaults to unknown status unless annotated
ok

~~~~~
//##7. non conc type defaults to unknown status unless annotated funcref
ok

~~~~~
//##8. was a bug now its ok
ok

~~~~~
//##9. null check on unknown type where known expected
ok[ok: java.lang.NullPointerException, ok: java.lang.NullPointerException, ok: java.lang.NullPointerException, ok: java.lang.NullPointerException, ok: no check needed]

~~~~~
//##10.null check on unknown type field
[ok, ok, ok]

~~~~~
//##11.method with local generics qualification skip null
fine

~~~~~
//##12. when local gen not infered use qualification provided
fine

~~~~~
//##13. when local gen not infered use qualification provided 2
fine

~~~~~
//##14. when local gen not infered use qualification provided 3 - boxed
fine

~~~~~
//##15. when local gen not infered use qualification provided 4 - arys
fine

~~~~~
//##16. buy on type override for class level vars
ok

~~~~~
//##17. bug on type override for module level vars
ok

~~~~~
//##18. if thing established as no null then we can proceed as follows
ok

~~~~~
//##19. infer type correctly given unkown input
okj

~~~~~
//##20. nulls on lists like arrays
ok

~~~~~
//##21. box elvis operator
ok

~~~~~
//##22. vectorized elvis operator
ok[1 3 2]

~~~~~
//##23. vectorized non null assertion operator
ok[[1, 4, 2] 1 4 2 java.lang.NullPointerException]

~~~~~
//##24. null safe calls for list elements
ok[11 CallThing(1) CallThing(1)]

~~~~~
//##25. vect list
ok[11, 14, 12]

~~~~~
//##26. safe call on matrix
ok[11 null 12]

~~~~~
//##27. lists and arrays with vectorized safe calls
ok[[11, 14, 12] 11 14 12 11 null 12 11 null 12 11 null 12 [11, null, 12]]

~~~~~
//##28. nullable generics or not
[aString, null res1, null res2, null res1, null res2]

~~~~~
//##29. infer generic parameters preserver nullability
[nice, ok]

~~~~~
//##30. nullable gens for local types
[null res1 null res2 null res1 null res2 null res1]

~~~~~
//##31. arrays correctly concatinate and nullable type respected
[1 2 ; 3 null]

~~~~~
//##32. bug with concat
[[1 2 ; null], [1 2]]

~~~~~
//##33. ensure correct nullable type infered for concat
ok [1 2 ; null]

~~~~~
//##34. ensure funcref just does null check on toBoolean
1

~~~~~
//##35. this just looked fun to do
ok[uh oh uh oh]

~~~~~
//##36. logic check progressive within if test
ok

~~~~~
//##37. if non null assertion on own no dup
ok

~~~~~
//##38. elvis operator any expr on rhs
8

~~~~~
//##39. null safe array operator
1

~~~~~
//##40. null safe array operator with null
null

~~~~~
//##41. not null assertion with array
1

~~~~~
//##42. chained safe calls
GREEN

~~~~~
//##43. broken now its ok
[1, [1 2 3 4 ; 1 2 3 4]]

~~~~~
//##44. bugfixes to safe dot op calls
res: java.util.NoSuchElementException

~~~~~
//##45. bugfixes to safe dot op calls pop last one
res: [hey there]

~~~~~
//##46. allow non null assertion to be used in chain
res: [hey there]

~~~~~
//##47. precompiled with default params
ok: Thing(19, null)

~~~~~
//##48. null ref
null

~~~~~
//##49. weird was blowing up due to bug in static var being set in compiler and not copy object
[MyClass1 MyClass2 ; MyClass3 MyClass4]

~~~~~
//##50. null check logic
ok[1 11 11 11 1 11 11 11 11 11 11 11 11 11]

~~~~~
//##51. null check logic pt 2
ok[11 11]

~~~~~
//##52. null check logic pt 3 - not
ok

~~~~~
//##53. null check logic pt 4 - while
[2]

~~~~~
//##54. ae to known nullable type shouldnt check npe
ok

~~~~~
//##55. bug when creating lambdas at top level
ok

~~~~~
//##56. type infer post assertion
ok2

~~~~~
//##57. type infer post isolated block
ok

~~~~~
//##58. type infer branch simple
ok

~~~~~
//##59. Nullable wrapper
ok

~~~~~
//##60. nullable arrays pre defined
ok2

~~~~~
//##61. most generic of nullable types
2

~~~~~
//##62. most generic of nullable types - list
ok1

~~~~~
//##63. transient classes may be checked for nullability within async block
result: uh oh

~~~~~
//##64. as above but with tricky if else condition
uh oh

~~~~~
//##65. transient class into isolate
result: uh oh

~~~~~
//##66. transient field is unknown nullability
oknullnull

~~~~~
//##67. transient class is unknown nullability
ok[hi ok]

~~~~~
//##68. unknown local generic is ok
ok Optional[ok]

~~~~~
//##69. unknown local generic is ok 2
ok (Optional[ok], 12)

~~~~~
//##70. check cast nullable type
OK[nope, ok]

~~~~~
//##71. tests on aysncrefrefs
ok

~~~~~
//##72. dont apply inference to lhs
ok