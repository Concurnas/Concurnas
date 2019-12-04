//##1.1.a anf transform - constructor
2

~~~~~
//##1.1.c anf transform - constructor - remember doubles take two slots
[2.0, 2.0, 2.0, 2.0]

~~~~~
//##1.1.b anf transform - constructor - nested
+2+

~~~~~
//##1.2.a anf transform - constructor - in cls
2

~~~~~
//##1.2.d anf transform - constructor - nested - in cls
+2+

~~~~~
//##1.3.a anf transform - constructor - in cls
2

~~~~~
//##1.3.d anf transform - constructor - nested - in cls
+2+

~~~~~
//##2. method overriden from primordial class use normal version
works

~~~~~
//##2.b method overriden from primordial class use normal version
[99, 99]:2

~~~~~
//##3. anf transform - nested
Holder obj

~~~~~
//##4. remove the exceptions consoldation code :- trying to be too clever above
[ throw onCatch throw onFin,  throw onCatch throw onFin extra]

~~~~~
//##5. fiber operation performed even for non concable methods
[hi:, hi:]

~~~~~
//##6. nested innner class consturction
99

~~~~~
//##6.b nested innner class consturction - more complex
991

~~~~~
//##7. shift across boundaries
true:

~~~~~
//##7.b shift across boundaries -advanced
true true

~~~~~
//##7.c shift across boundaries -advanced
true true

~~~~~
//##7.d shift across boundaries -super complex case
[1, 2, 3, 4, 5, 6, 7, 8, 9, 10] [1, 2, 3, 4, 5, 6, 7, 8, 9, 10] true true

~~~~~
//##8. primarch methods arnt instumented
hi

~~~~~
//##9. ensure that fiber version called
Hello world

~~~~~
//##9.b ensure that fiber version called - on lambdas
Hello world

~~~~~
//##10. dunno looks complex stick it here
true

~~~~~
//##11. enable globals to be fiberized
[nice, hi there mate, hi there mate]

~~~~~
//##12. use of non public consturctor in nestee
ok

~~~~~
//##13. avoid inf loop on module level lambdas
ok

~~~~~
//##14.a fib of init - basic
hi

~~~~~
//##14.b fib of init - basic classes
10

~~~~~
//##14.c fib of init - basic classes more adv
cool

~~~~~
//##14.d fib of init - basic classes more adv 2
[10, cool]

~~~~~
//##14.e fib of init - ensure no dup set get methods created
Holder objMyClass obj

~~~~~
//##15. primordial superclasses - direct subclass
10

~~~~~
//##15.b primordial superclasses - direct subclass - via no arg subclass
5

~~~~~
//##15.c primordial superclasses - direct subclass - kids get infected too
5

~~~~~
//##15.d primordial superclasses - direct subclass - kids get infected too explicit
12

~~~~~
//##15.e primordial superclasses - direct subclass - kids get infected too explicit x2
13

~~~~~
//##15.f primordial superclasses - direct subclass - i dunno
0

~~~~~
//##15.g primordial superclasses - ensure called once only
1

~~~~~
//##15.h.1 primordial superclasses - self conc call
5, 9, hi

~~~~~
//##15.h.2 primordial superclasses - self conc call - nested
5

~~~~~
//##15.h.3 primordial superclasses - self conc call - nested x2 
5 : 5

~~~~~
//##16. oh i see you have a stringbuilder before the super call
MyClass obj - mate hi

~~~~~
//##16.b oh i see you have a stringbuilder before the super call - more complex case
ok true my b:  false my b: null

~~~~~
//##17 stackframemap - ensure that correct supertype is chosen!
[my: one, my: two]

~~~~~
//##18. globalizer not overriden
my: one: my: two

~~~~~
//##19. call a different init in an init
Hello world: [4hi]

~~~~~
//##20. antoher primordial sup which now works ok
5

~~~~~
//##21. dunno what this does but i thought it was a bug
[6, 6, 5, 6]

~~~~~
//##22. this error goes away when rt.jar is fiberized
wonderful
