//##1. basic enum
hi ONE

~~~~~
//##2. basic enum supporting methods
hi [ONE, class x2basicenumsupportingmethods$MyEnum, true, true, class java.lang.Enum, 16393, [ONE TWO], 2, [ONE TWO]]

~~~~~
//##3. check popping
[ONE, ONE]

~~~~~
//##4. check popping of fields
[99, 99]

~~~~~
//##5. methods of enum types instances themselves
[0, ONE]

~~~~~
//##6. methods inside enums
[hi [ONE, ONE], hi [TWO, TWO], hi [ONE, ONE], hi [TWO, TWO]]

~~~~~
//##7. private methods are ok
okok

~~~~~
//##8. equality 1
[true, true]

~~~~~
//##9. equality 2
[true, true]

~~~~~
//##10. enum fields
[ONE, 9, 10, 11, 10, 10]

~~~~~
//##10.b enum fields
[ONE, 9, 10, 11, 10, 10]

~~~~~
//##11. enum fields
[true, 99, true]

~~~~~
//##12. check immutability
[true, true]

~~~~~
//##13. field init correctly
99

~~~~~
//##14. class specific private fields
ONE

~~~~~
//##15. enum item specific methods
[[9, 0], [10, 0]]

~~~~~
//##16. enum final fields done ok
[ONE(1, 2.0), TWO(1, 2.0)]

~~~~~
//##17. enum constructors
[[ONE(1, 2.3), TWO(2, 5.65), THREE(8, 0.34), FOUR(99, 0.11)], [ONE(1, 2.3) TWO(2, 5.65) THREE(8, 0.34) FOUR(99, 0.11)]]

~~~~~
//##18. no weirdnes with psar anymore
[ONE(99, 90.0) : hi, TWO(22, 8.0)]

~~~~~
//##19. enum default and calling therof
[ONE(12, 13), TWO(22, 33), THREE(11, 12), 12, 13]

~~~~~
//##20. enums with fields from one line definition plus calling from constructor
[ONE(12, 13), TWO(22, 33), THREE(11, 12), 12, 13]

~~~~~
//##21. check enum default fields are created as protected by default
[ONE(12, 13), TWO(22, 33), THREE(11, 12), 12, 13]

~~~~~
//##22. simple nested enum
[ONE(12, 13), TWO(22, 33)]

~~~~~
//##23. nested enums
GREEN

~~~~~
//##24. nested class in enum accesing stuff
[ONE, 12 -> 12]

~~~~~
//##25. nested class in enum accesing stuff above with functions
[ONE, 12 -> 12lol]

~~~~~
//##26. enum sub element constructors fiddly stuff
[7 8 22 33]

~~~~~
//##27. enum sub element constructors fiddly stuff x2
[9 8 22 33]

~~~~~
//##28. a nice test to round things off
2.0

~~~~~
//##29. access enum
ONE

~~~~~
//##30. double chec nesting
ONE

~~~~~
//##31. call valueOf
ONE

~~~~~
//##32. cannot copy enums now proper singltons
true

~~~~~
//##33. was prevsoiuly only adding one constructor
ONE(12, 13)

~~~~~
//##34. as above so below 
[ONE(12, 13), TWO(22, 33), THREE(11, 12), 12, 13]

~~~~~
//##35. basic enum should be ok
[[global local constant private], [global local constant private]]