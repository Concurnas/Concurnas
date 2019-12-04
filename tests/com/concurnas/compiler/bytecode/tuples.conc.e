//##1. simple tuple creation
(friday, 13)

~~~~~
//##2. simple tuple create no ret 
(friday, 13)

~~~~~
//##3. tuples as auto include type
(12, 13)

~~~~~
//##4. tuples in compact form
(12, 13)

~~~~~
//##5. decl tuple para
(12, 13)

~~~~~
//##6. tuple arg
(12, 13)

~~~~~
//##7. a normal declaration
(12, 13)

~~~~~
//##8. is
[true, false, true]

~~~~~
//##9. as
[(12, 13), com.concurnas.lang.tuples$Tuple2[java.lang.String, java.lang.String] cannot be cast to com.concurnas.lang.tuples$Tuple2[java.lang.Integer, java.lang.Integer]]

~~~~~
//##10. is alt type def 
[true, false, true]

~~~~~
//##11. as alt type def 
[(12, 13), com.concurnas.lang.tuples$Tuple2[java.lang.String, java.lang.String] cannot be cast to com.concurnas.lang.tuples$Tuple2[java.lang.Integer, java.lang.Integer]]

~~~~~
//##12. iteration
[el: 12, el: 13]

~~~~~
//##13. pull out items
[12 13]

~~~~~
//##14. simple decomposition
[12 13]

~~~~~
//##15. simple decomposition rhs ref
[12 13]

~~~~~
//##16. simple decomposition rhs ref contained
[12 13]

~~~~~
//##17. decomposition some new
[12 13]

~~~~~
//##18. decomposition assign existing
[112 113]

~~~~~
//##19. decomposition multi assign
[1 2 1 2]

~~~~~
//##20. decomposition skips
[1 3]

~~~~~
//##21. equal check
true

~~~~~
//##22. code gennerator
true

~~~~~
//##23. long ones
(1, 2, 3, 4, 5, 6)

~~~~~
//##24. this was broken before
(12, hi)

~~~~~
//##25. tuple in for loop
[3, 7, 11]

~~~~~
//##26. tuple in for loop with types
[3, 7, 11]

~~~~~
//##27. tuple in for loop missing items
[2, 6, 10]

~~~~~
//##28. tuple in for loop if thing is an array
[3, 7, 11]

~~~~~
//##29. tuple in list comprehension
[3, 7, 11]

~~~~~
//##30. tuples is refied type
ok

~~~~~
//##31. tuple FizzBuzz
[1, 2, Fizz, 4, Buzz, Fizz, 7, 8, Fizz, Buzz, 11, Fizz, 13, 14, FizzBuzz]

~~~~~
//##32. bugfix defined in str
ok (1, 2)

~~~~~
//##33. tuples of arrays
ok([1 2 3], [4 5 6])

~~~~~
//##34. shared trait makes implementors also shared
ok10

~~~~~
//##35. color in type for null where expected
ok(1, null)

~~~~~
//##36. tuples with choice resolving to null
ok(1, null)

~~~~~
//##37. tuples with choice resolving to null on return
ok[(12, null) (12, null)]

~~~~~
//##38. tuple equality
true

~~~~~
//##39. tuple creation when assigned to a ref
ok