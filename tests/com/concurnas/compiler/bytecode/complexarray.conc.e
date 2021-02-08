//##1. simple array
[1 2 3 4]

~~~~~
//##2. simple matrix
[1 2 ; 3 4]

~~~~~
//##3. concat simple matrix with one element item
[1 2 3 4 ; 8]

~~~~~
//##4. append to vector
[1 2 3 4 8]

~~~~~
//##5. more typical case
[111 222 8]

~~~~~
//##6. append to matrix
[1 2 ; 3 4 ; 5 6]

~~~~~
//##7. inline
[1 2 5 6]

~~~~~
//##8. newline ok
[1 2 3 4 ; 5 6 7 8]

~~~~~
//##9. 1d 2d
[1 2 3 1 2 3]

~~~~~
//##10. dirty little bug
[111 ; 222 333]

~~~~~
//##11. newlines matter
[1 2 3 4 ; 5 6 7 8]

~~~~~
//##12. simple disambiguate
[9 1 7]

~~~~~
//##13. simple disambiguate as funcinvoke
[9 1 7]

~~~~~
//##14. simple disambiguate all variants
[9 1 7 9 1 7 9 1 7 9 1 7]

~~~~~
//##15. disambiguate harder
[1 2 3 9 9 1 2 3 4]

~~~~~
//##16. list comprehension
[[1, 2, 3, 4, 5, 6, 7, 8, 9, 10], [2, 4, 6, 8, 10]]

~~~~~
//##17. list comprehension nice
[1, 4, 9, 16, 25, 36, 49, 64, 81, 100]

~~~~~
//##18. list comp this was a bug
[101, 102, 103, 104, 105, 106, 107, 108, 109, 110]

~~~~~
//##19. nested list comp 
[[[1, 3], [1, 4]], [[2, 3], [2, 1], [2, 4]], [[3, 1], [3, 4]]]

~~~~~
//##20. do vertical append
[[[1 2], [3 4]] ; [[1 2], [3 4]]]

~~~~~
//##21. probbaly a vertical append
[1 2 ; 3 4 ; 5 6]

~~~~~
//##22. horizontal append
[1 2 1 2 ; 3 4 3 4]

~~~~~
//##23. horizontal append easy
[1 2 1 2]

~~~~~
//##24. horizontal append easy2
[1 2 11 22]

~~~~~
//##25. the old way
[1 2 ; 3 4 5 6]

~~~~~
//##26. 3 to horiz concat
[11 12 21 22 11 12 ; 13 14 23 24 13 14]

~~~~~
//##27. prev bug with dims
[11 12 77 21 22 55 ; 13 14 77 23 24 77]

~~~~~
//##28. horizontal concat fail on mixed stuff
[1 2 1.0 2.0 1 2 hi there]

~~~~~
//##29. vertical concat fail on mixed stuff
[1 2 1.0 2.0 1 2 hi there]

~~~~~
//##30. failure on mixed types
[[1 2 hi there], [1 2 hi there], [[1 2], [hi there]]]

~~~~~
//##31. skip null
[[1 2 null 1 2], [1 2 ; null ; 3 4], [[1 2], null, [3 4]]]

~~~~~
//##32. process null ok
[Cannot read the array length because "<local3>" is null, [1 2 ; null ; 3 4], [[1 2], null, [3 4]]]

~~~~~
//##33. process null ok 2d
[null, [1 2 ; null], [[1 2], null]]

~~~~~
//##34. alt int matrix def
ok [[0 0 0 0 ; 0 0 0 0], [0 0 0 0 ; 0 0 0 0]]

~~~~~
//##35. alt way to define arrays without last parts qualified
ok: [[null ; null ; null ; null ; null]
, [null ; null ; null ; null ; null ; null ; null ; null ; null ; null]
, [null ; null ; null ; null ; null]
, [null ; null ; null ; null ; null]
]

~~~~~
//##36. omit new keyword on array constructor
ok: 

~~~~~
//##37. omit new keyword on array constructor prims
ok: 

~~~~~
//##38. fail too many elements
Array element size mismatch

~~~~~
//##39. alt array syntax expr list fix
ok: [null null ; null null]

~~~~~
//##40. bugfix on partial array creation
ok

~~~~~
//##41. array def containg ambig asyncrefref
[1: 2:]

~~~~~
//##42. array def containg ambig asyncrefref complex
[[[1: 2:] 3 4], [[1: 2:] [3: 4:]]]

~~~~~
//##43. bugfix on local array length
2