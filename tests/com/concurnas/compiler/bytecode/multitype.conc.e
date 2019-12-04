//##1. simple example of multitype
[20.0, 20]

~~~~~
//##2. multitype constructors
[MyClass: 1, MyClass: 1.0]

~~~~~
//##3. multitype ext funcs
[102.01, 102, 2100]

~~~~~
//##4. multitype typedef
[102.01, 102]

~~~~~
//##5. nested multitypes
[102.01, 102, aa100]

~~~~~
//##6. imported typedef
2

~~~~~
//##7. imported typedef precompiled
3.0

~~~~~
//##8. imported typedef plus string
2

~~~~~
//##9. imported typedef precompiled plus string
[3.0, as1]

~~~~~
//##10. bug earlier
[1 2 ; 3 4]

~~~~~
//##11. bug earlier 2
1

~~~~~
//##12. bug earlier 3
[2 3 ; 4 5]

~~~~~
//##13. simple matrix operation
[2 3 ; 4 5]

~~~~~
//##14. simple matrix operation via typedef
[[2 3 ; 4 5], [2.0 3.0 ; 4.0 5.0]]

~~~~~
//##15. simple matrix operation via imported typedef
[[2 3 ; 4 5], [2.0 3.0 ; 4.0 5.0]]

~~~~~
//##16. multitype new
[hi, myclass: hi]

~~~~~
//##17. multitype new bit more complex
[[hi, hi2], [myclass: hi, myclass: hi2]]

~~~~~
//##18. multitype matrix multiplication
[58 64 ; 139 154]

~~~~~
//##19. array element wise initializer
[99 99 99 ; 99 99 99]

~~~~~
//##20. array element wise initializer object types
[[MyClass: 22 MyClass: 22 MyClass: 22 ; MyClass: 22 MyClass: 22 MyClass: 22], true]

~~~~~
//##21. array element wise initializer with multitype
[4 4 4 ; 4 4 4]

~~~~~
//##22. multittype as bugfix
[25 28 ; 73 82]

~~~~~
//##23. multittype as bugfix boxed variant
[25 28 ; 73 82]

~~~~~
//##24. ensure is works for multitypes
[false true]

~~~~~
//##25. typdef used to convert prims to boxed variant
[25 28 ; 73 82]

~~~~~
//##26. matrix mult on all numerical types
[25 28 ; 73 82 25 28 ; 73 82]

~~~~~
//##27. looks cool
cd

~~~~~
//##28. was a bug now its ok
[25 28 ; 73 82]