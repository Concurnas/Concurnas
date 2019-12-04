//##1 local classes
got: 12

~~~~~
//##2 local classes nested
got: 12

~~~~~
//##3 local classes pre bindings
[got: 9, got: hi 9]

~~~~~
//##4 local classes local bindings
[got: 9 also: 99, got: hi 9 also: 100]

~~~~~
//##5 local classes without new keyword 
[got: 9 also: 99, got: hi 9 also: 100]

~~~~~
//##6 local classes module level dependancies
[got: 9 also: 100, got: hi 9 also: 100]

~~~~~
//##7 local classes class level dependancies
got: 9 also: [99, 101, 1012]

~~~~~
//##8 local class deps - module functions
[got: 9 also: 1004, got: hi 9 also: 1004]

~~~~~
//##9 local class deps - local functions
[got: 9 also: 9959, got: hi 9 also: 995100]

~~~~~
//##10 local class deps - ref to module functions
[got: 9 also: 10041004, got: hi 9 also: 10041004]

~~~~~
//##11 local class deps - ref to local functions
[got: 9 also: 10041004, got: hi 9 also: 10041004]

~~~~~
//##12 local class extends normal class
MiniClass: [as, 33]

~~~~~
//##13 checkcast ok
MiniClass: hi

~~~~~
//##14 local class extends another local class
MiniClass: [hi, 33]

~~~~~
//##15 local class extends another local class, with dependancies which need splicing in
MiniClass: [hi, 33, 100]

~~~~~
//##16. nested local classes 1
MiniClass: [hi, 100]

~~~~~
//##17. nested local classes 2 and extends parent nestor
MiniClass: [hi, 100, 1111]

~~~~~
//##18. nested local classes constructor ref
[MiniClass: hi1, MiniClass: hi2, MiniClass: hi3]

~~~~~
//##19. nested local classes constructor ref implicit generics
[MiniClass: hi1, MiniClass: hi2, MiniClass: hi3]

~~~~~
//##20. nested local classes constructor ref explicit generics
[MiniClass: hi1, MiniClass: hi2, MiniClass: hi3]

~~~~~
//##21. local class refs
[MiniClass: hi2, MiniClass: hi3, MiniClass: 99, MiniClass: 89]

~~~~~
//##22. local class refs with generics
[MiniClass: hi2, MiniClass: hi3, MiniClass: 99, MiniClass: 89]

~~~~~
//##23. local constructor refs with dependancies
MiniClass: [hi2, 99, 199]

~~~~~
//##24. local class refs with dependancies
MiniClass: [hi2, 99, 199]

~~~~~
//##25. pattern needed for local class defs
MiniClass: [hi2, 99]

~~~~~
//##26. local classdef
MiniClass: [hi2, 99, 199]

~~~~~
//##27. check we can define blocks at class level on rhs
[21, 3]

~~~~~
//##28.local classes can have use outer variables
MiniClass: [ok, 100]

~~~~~
//##29.local classes can have use outer variables from inside the rhs class ass block
MiniClass: [1, 100]

~~~~~
//##30. local class ok even with name clash
MiniClass: hi

~~~~~
//##31. local as lambda as feidl
MiniClass: hi

~~~~~
//##32. check no generic qualifaction needed
ONE

~~~~~
//##33. local class can call module level remapped nested method
MiniClass: [oksdf, 22]

~~~~~
//##34. local class refer to another local class
LocalTwo: LocalOne: hi there

~~~~~
//##35. local class use this methods refs etc
MyClass: [ok there, 99, [22, 99], ok9, ok9]

~~~~~
//##36. local explicit typed actor
MyClass: ok there

~~~~~
//##37. local explicit typed actor with deps 
[991, ok there]

~~~~~
//##38. unmtyped local actors 
[[there 99], [there 100]]

~~~~~
//##38.b unmtyped local actors- bug in actor def
[[there, 99], [there, 100]]

~~~~~
//##39. local typed actor
ok there

~~~~~
//##40. more local typed actor
999 ok there

~~~~~
//##41. typed actors of localized stuff
LocalTwo: LocalOne: hi there

~~~~~
//##42. default actor of local class
TheLocal: [hi there, 22]

~~~~~
//##43. default actor of local class with dependancies
TheLocal: [hi there, 31, there]

~~~~~
//##44. create iface chain even for non local returned things ie superclasses
super oneyes we can call it 2

~~~~~
//##45. create iface chain even for non local returned things ie superclasses x2
[super one yes we can call it 2, super one yes we can call it 2]

~~~~~
//##46. class refs via FuncRefInvoke
[super oneyes we can call it 2, super oneyes we can call it 2]

~~~~~
//##47. local classes callable via super type
[super one norm one 1, super one norm one 2, super one yes we can call it 2]

~~~~~
//##48. str expr stuck in infinite loop as replace expr list every call
 norm one  22222

~~~~~
//##49. mested local class
ok

~~~~~
//##50. mested local class deps from parent
ok [99, 100]

~~~~~
//##51. local contains nested enum and anntoations
[ONE, 999]

~~~~~
//##52. new oo - basic
ok

~~~~~
//##53. new oo - basic2
ok

~~~~~
//##54. new oo - basic3
hi [1 2]

~~~~~
//##55. new oo - create
[class x55newoocreate$MyProvider, MyClass [34, 25], MyClass [1, 2]]

~~~~~
//##56. new oo - requires cast
[class x56newoorequirescast$MyProvider, MyClass [34, 25], MyClass [1, 2], null]

~~~~~
//##57. new oo - provider spits out object nulls the thing we asked for 
null

~~~~~
//##58. new oo - provider spits out local class for unknown type
D.MyOtherClass -> x58newooproviderspitsoutlocalclassforunknowntype$MyProvider$NIC$0 [1 2 3]

~~~~~
//##59. new oo - provider spits out local class for unknown type v2
D.MyOtherClass -> x59newooproviderspitsoutlocalclassforunknowntypev2$MyProvider$NIC$0 [1 2 3]

~~~~~
//##60. new oo - different signature
one -> x60newoodifferentsignature$NIC$0 [[1, 2, 3]]

~~~~~
//##61. new oo - as before
D.MyOtherClass -> x61newooasbefore$MyProvider$NIC$0 [1 2 3]

~~~~~
//##62. new oo - generics
MyGenClass 77

~~~~~
//##63. new oo - ref to oo new
MyGenClass [77, lovely]

~~~~~
//##64. new oo - with map
MyGenClass [cool, 77]

~~~~~
//##65. new oo - ref with map
MyGenClass [lovely, 77]

~~~~~
//##66. transient classes
[true, true, true]

~~~~~
//##67. return local class from lambda
true

~~~~~
//##68. top level local class def
10

~~~~~
//##69. more complex def
60

~~~~~
//##70. anon class
10

~~~~~
//##71. anon class v2
[10, 10, 20]

~~~~~
//##72. anon actor of an anon class
10

~~~~~
//##73. anon class variants
[10, 10, 20, 10]

~~~~~
//##74. anon class variants generics
[10, 10, 20, 10]

~~~~~
//##75. anon class in place
20

~~~~~
//##76. anon class cool
[60, 17]

~~~~~
//##77. expr stmt ext function and a anon class
4

~~~~~
//##77. expr stmt ext function and a anon class v2
4

~~~~~
//##78. as above but easy
4