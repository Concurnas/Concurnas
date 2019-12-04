//##0. zero capture case
28

~~~~~
//##1. simple localvar ref
42

~~~~~
//##2. simple localvar ref 2 levels
169

~~~~~
//##3. simple localvar ref via class
51

~~~~~
//##4. simple localvar ref via class - 2 levels
178

~~~~~
//##5. simple localvar ref - funcref
42

~~~~~
//##6. simple localvar ref 2 levels- funcref
169

~~~~~
//##7. simple localvar ref via class- funcref
51

~~~~~
//##8. simple localvar ref via class - 2 levels- funcref
178

~~~~~
//##9.a - funcref of funcref nested inner
169

~~~~~
//##9.b - funcref of funcref nested inner
178

~~~~~
//##10 - funcref of funcref nested inner
0, 1, 23, 46, 91, 159, 272, 453, 747, 1222, 1991, 

~~~~~
//##11 - nested class def -1
150

~~~~~
//##11 - nested class def -2
167

~~~~~
//##12 - nested function inside rhs of field assignment
91

~~~~~
//##13 - nested function inside rhs of field assignment with deps
frank289

~~~~~
//##14. lambda gen bug on more than one nested function
MyClass: [ok 911, [22 12]]

~~~~~
//##15. was a bug now its fine
cool
