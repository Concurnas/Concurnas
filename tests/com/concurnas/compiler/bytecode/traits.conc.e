//##1. simple trait
ok

~~~~~
//##2. trait with nothing defined
12

~~~~~
//##3. trait with default impl 
44

~~~~~
//##4. just checking this works
ok

~~~~~
//##5. override things defined in trait
12

~~~~~
//##6. simple trait extension
[12, 12, 100]

~~~~~
//##7. simple trait extension overeride
(100, 100)

~~~~~
//##8. basic generics
22.0

~~~~~
//##9. basic generics 2
22.0

~~~~~
//##10. force trait to become abstract
44.0

~~~~~
//##11. no equals no hashcode requires static mapping
[ok, 1]

~~~~~
//##12. via iface static
ok: [12, 12]

~~~~~
//##13. no explicit calling on iface type
[ok, 1]

~~~~~
//##14. bug used to think this was not a trait
ok

~~~~~
//##15. ensure upper bounc correctly captured on classDefJava instances
[ok, 22]

~~~~~
//##16. super by default points to trait super not superclass plus qualified super
([trait, trait, super], trait)

~~~~~
//##17. super trait generic type is qualified with boxed type
([trait12, trait12, super12.0], trait2)

~~~~~
//##18. ensure eq and hashcode gennerated
(true, false)

~~~~~
//##19. redirect super calls to correct static method
([trait2 12, trait1 12, trait2 12, super 12.0, this 12, this 12], this 2)

~~~~~
//##20. qualified trait can be fully defined name
([trait2 12, trait1 12, trait2 12, super 12.0, this 12, this 12], [trait2 12, trait1 12, trait2 12, super 12.0, this 12, this 12], this 2)

~~~~~
//##21. was incorrectly gennerateing equals and hashcode
hitrait1 12

~~~~~
//##22. used to not work now its fine
hitrait1 12

~~~~~
//##23. works with funcrefs
[trait2 12, trait1 12, trait2 12, super 12.0, this 12, this 12]

~~~~~
//##24. unbound funcrefs
[trait1 12, trait2 12, super 12.0, this 12]

~~~~~
//##25. super abstract
([trait2 12, trait1 12, trait2 12, this 12, this 12], this 2)

~~~~~
//##26. disambiguation of two methods trait methods with same sig 
nice: log1: hi

~~~~~
//##27. disambiguation and correct routing A
log1: hi

~~~~~
//##28. disambiguation and correct routing B
log1: hi

~~~~~
//##29. heriarchically related instances to disambiguate
[log1: hi, log2: hi]

~~~~~
//##30. intermediate superclass abstract with conflict
hi there:hi

~~~~~
//##31. trait extend non trait class
(56, 56)

~~~~~
//##32. via module
(56, 56)

~~~~~
//##33. trait extends non trait class so does concrete class
(56, 56)

~~~~~
//##34. trait extends non trait class so does concrete class hierarhcy
(56, 56)

~~~~~
//##35. basic method chaining
R1 T2 T1 R3 R2 A 

~~~~~
//##36. simple hierarhcy super mapping
(157, 157)

~~~~~
//##37. ensure tostring equals only gennerated if all traits are missing definition
1

~~~~~
//##38. ref to trait super
(157, 157)

~~~~~
//##39. another stacked example
BA

~~~~~
//##40. with generics stacked
[BCA CBA]

~~~~~
//##41. with generics stacked disambig
[BCA MyClass2CBA]

~~~~~
//##42. qualified super
MyClassBA

~~~~~
//##43.trait protected methods
BA

~~~~~
//##44. stacked, not overriden inside B
A

~~~~~
//##45. first stab at trait vars
11

~~~~~
//##46. via abstract class
11

~~~~~
//##47. trait field type can be subtype
[ok ok]

~~~~~
//##48. trait field is generic
[10.0 10.0]

~~~~~
//##49. trait field is generic quali with prim non eq type exactly
[10.0 10.0]

~~~~~
//##50. more complex trait field for encoder decoder
1

~~~~~
//##51. var in superclass
[11, 12]

~~~~~
//##52. trait can share var name
(101, 103, 103)

~~~~~
//##53. trait inhnerirts from trait 
(101, 103, 103)

~~~~~
//##54. gennerate fun for each field type instance
([100 100], [100 100 100], 100)

~~~~~
//##55. bug. same field type multi define only def once
([100 100], [100 100 100], 100)

~~~~~
//##56. trait variable with assigned value
[101 101]

~~~~~
//##57. multiple traits to call
202

~~~~~
//##58. inherit or extend result is the same
[ok ok]

~~~~~
//##59. overwrite value of trait assigned value in concrete class
3002

~~~~~
//##60. refer to abstract class intermediate trait value
3002

~~~~~
//##61. override field
[10, 0, 10, 10]

~~~~~
//##62. override works with classdefargs
(101, 103, 103)

~~~~~
//##63. override works on fields used by traits defined in abstract classes
[101, 102]

~~~~~
//##64. val and var
(9, 10)

~~~~~
//##64.b val and var assign it something
(0, 1)

~~~~~
//##65. getters and setters 1 override
[1100, 88, 88]

~~~~~
//##66. getters and setters 1 override getter setter
[1100, 88, 88]

~~~~~
//##67. getters and setters 1 direct access trait variable
[199, 88, 88]

~~~~~
//##68. var defined in trait
expected

~~~~~
//##69. var defined in trait - one in between
expected

~~~~~
//##70. bugifx this used to not work correctly
9

~~~~~
//##71. direct trait variable access
hi

~~~~~
//##72. generic trait variable from compiled trait
hi

~~~~~
//##73. generic trait variable from compiled trait via intermediate class
hi

~~~~~
//##74. actor default call trait method
1

~~~~~
//##75. actor of class with trait
1

~~~~~
//##76. actor with a trait
1

~~~~~
//##77. trait is as 
ok: [true myTrait]

~~~~~
//##78. use in anon class
13.0

~~~~~
//##79. qualified super
[version AbstractFooClass, version A, version B]

~~~~~
//##80.stacked traits
[60, 17]

~~~~~
//##81.can call stacked trait without override etc onto abstract super
[60, 17]

~~~~~
//##82. butgix on instance vs trait method call
20

~~~~~
//##83. override var via abstract class
10

~~~~~
//##84. abstract class has traits concrete class correct init of variables
(0, 10)

~~~~~
//##85. correct overloading of variables
oops[FromAsbtract, MyclassFromTrait, AbstractClass, MyTrait]

~~~~~
//##86. more correct overloading
(0, 10)

~~~~~
//##87. correct number of func calls
(2, 4, 2, 4, -2, 6)

~~~~~
//##88. lets use a comparitor
hi[Node(1) Node(2) Node(2) Node(3) Node(4) Node(5)]

~~~~~
//##89. persistance of trait fields
got: [101, 201] use: [{class x89persistanceoftraitfields$MyClass->1}, {}] cont: false

~~~~~
//##90. copy a trait
its ok: 12
