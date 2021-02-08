//##1. shared local variable
[99, 2, 3, 4, 5, 6, 7]

~~~~~
//##2. shared class
[99, 2, 3, 4, 5, 6, 7]

~~~~~
//##3. shared func param
[99, 2, 3, 4, 5, 6, 7]

~~~~~
//##4. shared class as param
[99, 2, 3, 4, 5, 6, 7]

~~~~~
//##5. shared module level
[99, 2, 3, 4, 5, 6, 7]

~~~~~
//##6. shared module level class type is shared
[99, 2, 3, 4, 5, 6, 7]

~~~~~
//##7. shared at class level
[99, 2, 3, 4, 5, 6, 7]

~~~~~
//##8. module level class shared variable
[99, 2, 3, 4, 5, 6, 7]

~~~~~
//##9. module level shared class
[99, 2, 3, 4, 5, 6, 7]

~~~~~
//##10. module level variable can share
[99, 2, 3, 4, 5, 6, 7]

~~~~~
//##11. class level params
[99, 2, 3, 4, 5, 6, 7]

~~~~~
//##12. class level params shared class
[99, 2, 3, 4, 5, 6, 7]

~~~~~
//##13. shared class applies to child class too
[99, 2, 3, 4, 5, 6, 7]

~~~~~
//##14. shared class variable done properly
[99, 2, 3, 4, 5, 6, 7]

~~~~~
//##15. shared module level variables
ok[199 [23: 23:]]

~~~~~
//##16. shared module level var always set
ok[111 [23: 23:]]

~~~~~
//##17. shared gotcha
ok[500: 500:]

~~~~~
//##18. shared class
ok[100: 23: 23: 23:]

~~~~~
//##19. shared class ae
ok[100: 23: 23: 23:]

~~~~~
//##20. shared class gotcha
[new Value: new Value:]

~~~~~
//##21. shared multi assignment
ok[ok: ok: ok:]

~~~~~
//##22. shared ref pointless
ok[ok: ok:]

~~~~~
//##23. shared class def level variable
true

~~~~~
//##24. shared fields are volatile
true