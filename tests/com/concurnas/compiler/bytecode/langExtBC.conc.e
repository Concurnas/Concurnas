//##1. locations
ok: [hello world: (3, 7, TOPLEVEL, (+ 1 2 3 )), hello world: (6, 9, CLASS, (+ 1 2 3 )), hello world: (10, 8, EXPRESSION, (+ 1 2 3 ))]

~~~~~
//##2. simple lang ext
0khello world

~~~~~
//##3. simple lang ext with escape
0khello world

~~~~~
//##4. escape character
ok: hello world

~~~~~
//##5. simple lisp
ok: 12

~~~~~
//##6. use ext vars
ok: 6

~~~~~
//##7. use ext methods
ok: 8

~~~~~
//##8. get info about classes etc
ok: MyClass: [const, const], [(a, I), (b, I)], [equals, getClass, hashCode, something, toString], [Nested]

~~~~~
//##9. info about class long import 
ok: MyClass: [const], [(a, I), (b, I)], [equals, getClass, hashCode, something, toString], [Nested]

~~~~~
//##10. nested location exploration
ok:  hey nestfunc myFunc MyClass 

~~~~~
//##11. load via module
ok:  hey nestfunc myFunc MyClass 

~~~~~
//##12. load via module v2
ok:  hey nestfunc myFunc MyClass 

~~~~~
//##13. can create variables
ok: 12

~~~~~
//##14. expanded locations
ok: [ funcInMethod func:nestfunc method:myFunc class:MyClass ,  funcInExtMethod func:nestfunc[Ext Func of:I] method:myFunc2 class:MyClass ,  regFunc func:inFunc ,  extfunc func:inExtFunc[Ext Func of:I] ,  inConst const: class:MyClass ]

~~~~~
//##15. smart cast bugs
ok:  hey nestfunc myFunc MyClass 

~~~~~
//##16. bug on label allocation
ok:

~~~~~
//##17. bug on label allocation
ok:  hey nestfunc myFunc MyClass 

~~~~~
//##18. name matching
ok:  hey nestfunc myFunc MyClass 

~~~~~
//##19. map import to overriding naem correctly
ok:  hey nestfunc myFunc MyClzass 

