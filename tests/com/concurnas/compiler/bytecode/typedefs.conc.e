//##1. simple typedef
[hi]

~~~~~
//##2. simple typedef args
[[hi], [hi]]

~~~~~
//##3. simple used in functions
[hi]

~~~~~
//##4. use in other places
[2, true]

~~~~~
//##5. without new keyword
[12]

~~~~~
//##6. without new keyword 2
[12]

~~~~~
//##7. typedef to typdef
[12]

~~~~~
//##8. typedef ref to typedef and differing args
[12]

~~~~~
//##9. typedef ref to typedef and differing args rev order fwd ref
[12]

~~~~~
//##10. typedef of non named type simple
[3, 12]

~~~~~
//##11. resolves to nt and ignore error extract from lhs td
55

~~~~~
//##12. functype with name mapping
[3]

~~~~~
//##13. typedef passed in as argument and used within typedef
{1->[]}

~~~~~
//##14. typedef type inference
12

~~~~~
//##15. box primatives and refer to thing on rhs
[12, 13]

~~~~~
//##16. was a bug now its ok
{12->ok}

~~~~~
//##17. gens correctly
{12->ok}

~~~~~
//##18. nested class 
null

~~~~~
//##19. typedef inside function 
null

~~~~~
//##20. typdef defined in compiled class
[[hi], [hi]]

~~~~~
//##21. typdef defined in compiled class takes args
[hi]

~~~~~
//##22. more complex involving object arrays
{12->[1 2 3]}

~~~~~
//##23. ret a primative and bugfix
[1 2 3]

~~~~~
//##24. functypes
3

~~~~~
//##25. use typedef the long unimported way
[[hi], [hi]]

~~~~~
//##26. generics dont require qualifciation
[hi]

~~~~~
//##27. as above but for generics of namedtype too
[[hi]]

~~~~~
//##28. annotations at module level
[@Typedefs(typedefs = [@Typedef(args = [x], name = mylistQ, type = Ljava/util/ArrayList<Lx;>;) @Typedef(args = [], name = myfuncdef2, type = Lcom/concurnas/bootstrap/lang/Lambda$Function2<Ljava/lang/Integer;Ljava/lang/Integer;+Ljava/lang/Integer;>;)]) @ConcImmutable]

~~~~~
//##29. annotations at module level to disable warnings
nice

~~~~~
//##30. annotations at module level to disable warnings 2
nice

~~~~~
//##31. annotations at module level to disable warnings 3
nice

~~~~~
//##32. typedef arrays
[null null null]

~~~~~
//##33. typedef complex used to break
wtrans: [true - its null, false - [hi there]]

~~~~~
//##35. typedef complex used to break 2d arr
wtrans: [true - its null, false - [hi ; there]]

~~~~~
//##36. typedef recusrive 3d
ok [[hi, there]]

~~~~~
//##37. typedef maps to type and assigment will cast to it
9

~~~~~
//##38. import from bc - more than one typedef w same name
[12]

~~~~~
//##39. tuple typedef use x args
[3, 7, 11]