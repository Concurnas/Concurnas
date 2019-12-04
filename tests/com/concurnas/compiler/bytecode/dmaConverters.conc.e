//##1 basic boiler plate and baic convertion test
MyClass2 x1basicboilerplateandbaicconvertiontest$MyClass [hi, null]

~~~~~
//##2 as above, more compact
MyClass2 x2asabovemorecompact$MyClass [hi, null]

~~~~~
//##3 bugfix on maps with fields
hi

~~~~~
//##4 remove object field
[MyClass [a, b, c, d, e, f], MyClass [a, b, c, d, e, f], MyClass2 [a, d, f]]

~~~~~
//##5 remove object field different types
[MyClass [a, 12, 99.0, d, e, 100], MyClass [a, 12, 99.0, d, e, 100], MyClass2 [a, d, 100]]

~~~~~
//##6 add field ensure defualt used
[MyClass [a, 12, 998.1], MyClass [a, 12, 998.1], MyClass2 [a, 12, new fella, null, 0, 998.1]]

~~~~~
//##7 remove first bug
[MyClass [a, 12, 998.1], MyClass [a, 12, 998.1], MyClass2 [, 12, 998.1]]

~~~~~
//##8 added and removed 1
[MyClass [first, 12, 998.1], MyClass [first, 12, 998.1], MyClass2 [12, new fella, null, 0, 998.1]]

~~~~~
//##9 added and removed 2
[MyClass [first, 12, 123.0, 998.1], MyClass [first, 12, 123.0, 998.1], MyClass2 [12, new fella, null, 0, 998.1]]

~~~~~
//##10 arrays 1
[MyClass [first, 12, 123.0, 998.1], MyClass2 [12, new fella, null, 0, 998.1]]

~~~~~
//##11 held class
[Holder: MyClass [first, 12, 123.0, 998.1], Holder: MyClass2 [12, new fella, null, 0, 998.1]]

~~~~~
//##12 arrays 2
[Holder: [MyClass [first, 12, 123.0, 998.1]], Holder: [MyClass2 [12, new fella, null, 0, 998.1]]]

~~~~~
//##13 convert primatives and boxed variants of
[MyClass [-, 101, 89, 45, 8, 843, 945, 5656.22], MyClass2 [-, 101.0, 89.0, 45, 8, 843, 945.0, 5656.22]]

~~~~~
//##14 convert anything to boolean
[MyClass [-, [], [23]], MyClass2 [-, false, true]]

~~~~~
//##15 convert anything to Boolean - boxed
[MyClass [-, [], [23]], MyClass2 [-, false, true]]

~~~~~
//##16 boxed to boolean is null
[MyClass [-, null], MyClass2 [-, false]]

~~~~~
//##17 nulls
[MyClass [-, null, null, null, null, null, null, null, null], MyClass2 [-, false, false, null, false, 0, 0.0, false, false]]

~~~~~
//##18 anything toString
[MyClass [-, 567, 456456, Parent toString, null, null], MyClass2 [-, 567, 456456, Parent toString, null, null]]

~~~~~
//##19 subclass to superclass or superinterface
[MyClass [-, P or subclass of P: C, P or subclass of P: C, P or subclass of P: P], MyClass2 [-, P or subclass of P: C, P or subclass of P: C, null]]

~~~~~
//##20 simple array casts
[MyClass [-, [123 235 456546 ; 1 2 3]], MyClass2 [-, [123.0 235.0 456546.0 ; 1.0 2.0 3.0]]]

~~~~~
//##21 ar convert - Integer 1 -> float 1 
[MyClass [-, [123 235 456546]], MyClass2 [-, [123.0 235.0 456546.0]]]

~~~~~
//##22 ar convert - Integer 2 -> float 2 
[MyClass [-, [123 235 456546 ; 1 2 3]], MyClass2 [-, [123.0 235.0 456546.0 ; 1.0 2.0 3.0]]]

~~~~~
//##23 ar convert - Integer 1 -> Float 1 
[MyClass [-, [1 2 3]], MyClass2 [-, [1.0 2.0 3.0]]]

~~~~~
//##24 ar convert - int 1 -> Double 1
[MyClass [-, [1 2 3]], MyClass2 [-, [1.0 2.0 3.0]]]

~~~~~
//##25 ar convert - float 2 -> Integer 2
[MyClass [-, [1.99 2.0 3.0 ; 12.0 24.0 53.0]], MyClass2 [-, [1 2 3 ; 12 24 53]]]

~~~~~
//##26 ar convert - Double 2 -> Integer 2 
[MyClass [-, [1 2 3 ; 12 24 53]], MyClass2 [-, [1.0 2.0 3.0 ; 12.0 24.0 53.0]]]

~~~~~
//##27 ar convert - arrays to String arrays 
[MyClass [-, [99 100], [1 2 3 ; 5 6 7], [99 56 46], [199 156 146 ; 1299 1256 1246]], MyClass2 [-, [99 100], [1 2 3 ; 5 6 7], [99 56 46], [199 156 146 ; 1299 1256 1246]]]

~~~~~
//##28 encoder store new version - missing fields 
[MyClass [a, b, c, d, e, f], MyClass2 [a, d, f]]

~~~~~
//##29 encoder store new version - missing fields vid count
[MyClass [a, b, c, d, e, f], MyClass2 [a, d, f], [1]]

~~~~~
//##30 encoder more than one of same type encoded
[[MyClass [a1, b1, c1, d1, e1, f1], MyClass [a2, b2, c2, d2, e2, f2]], [MyClass [a1, b1, c1, d1, e1, f1] MyClass2 [a1, d1, f1] MyClass2 [a2, d2, f2]], [2]]

~~~~~
//##31 decoder self type reference as converted field
[MyClass [MyClass [null, kid b1, kid c1], b1, c1], MyClass2 [MyClass2 [null, kid c1], c1]]

~~~~~
//##32 encoder, nested reference to same type plus correct vids
[1.0 MyClass [2.0 MyClass [null, kid b2, kid c2], parent b1, parent c1], |||
, 1.0 MyClass2 [2.0 MyClass2 [null, kid c2], parent c1], [2]]

~~~~~
//##32 parent class has convertion changes
[1.0 MyClass [main b1, main c1], 1.0 MyClass2 [main c1], [1]]

~~~~~
//##33 parent class has convertion changes - more complex
[1.0 MyClass [main b1, main c1, Middle: 23.0], 1.0 MyClass2 [main c1, Middle2: [23.0, default str]], [1]]

~~~~~
//##34. loops self reference
MyClass [B, true, Bb, Bc] -> [MyClass2 [B, true, Bc], MyClass2 [A, true, Ac], MyClass2 [B, true, Bc], MyClass2 [A, true, Ac], [2]]

~~~~~
//##35. more than one version of class persisted
MyClass [B, true, Bb, Bc] -> [MyClass2 [B, true, Bc], MyClass2 [A, true, Ac], MyClass2 [B, true, Bc], MyClass2 [A, true, Ac], [2][4]]

~~~~~
//##36. change classloader ensure this is picked up
MyClass [B, true, Bb, Bc] -> 
[MyClass2 [B, true, Bc], MyClass2 [A, true, Ac], MyClass2 [B, true, Bc], MyClass2 [A, true, Ac], [2][4]]
[MyClass2 [B, true, Bc], MyClass2 [A, true, Ac], MyClass2 [B, true, Bc], MyClass2 [A, true, Ac], [2][4]]

~~~~~
//##37. classloader missing defs
[java.lang.RuntimeException: Class definition is missing in classloader: uh oh, MyClass2 [B, true, Bc]]

~~~~~
//##38. bugfix on class ? vs class ? lt object
fine

~~~~~
//##39. enums are not affected by all this
MyEnum[one, 1] -> [MyEnum2[1], [1]]

~~~~~
//##40. fieldless class
MyClass -> [MyClass2, [1][1]]

~~~~~
//##41. basic map change value
MyClass2 x41basicmapchangevalue$MyClass [hi, def]

~~~~~
//##42. basic map change key
[3398][3398][MyClass2 x42basicmapchangekey$MyClass [hi, 69]]one

~~~~~
//##43. ensure that change to classloader is picked up
MyClass2 x43ensurethatchangetoclassloaderispickedup$MyClass [hi, def]

~~~~~
//##44. if you define your own tobinary frombinary your on your own
MyClass x44ifyoudefineyourowntobinaryfrombinaryyouronyourown$MyClass [hi, there]MyClass2 x44ifyoudefineyourowntobinaryfrombinaryyouronyourown$MyClass [there, hi]MyClass3 x44ifyoudefineyourowntobinaryfrombinaryyouronyourown$MyClass [hi, there]

~~~~~
//##45. off heap cannot be shared across isos
null engine is null