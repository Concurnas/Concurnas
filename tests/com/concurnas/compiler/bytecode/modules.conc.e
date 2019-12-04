//##1. basic module
basic module works ok

~~~~~
//##2. more complex imports
works ok

~~~~~
//##3. vars and fucns
[9, hi]

~~~~~
//##4. 2 levels of dependancy
9

~~~~~
//##5. import a typedef
9

~~~~~
//##6. import things that are protected
lovely

~~~~~
//##7. bugfix on module level variable assignment
ok[99]

~~~~~
//##8. bugfix on as imports
ok[ok, 9, 99]

~~~~~
//##9. lambda defined in module
ok

~~~~~
//##10. lambda defined in module name redirect
ok

~~~~~
//##11. import a lambda
ok almabda

~~~~~
//##12. import a lambda redirect name
ok almabda

~~~~~
//##13. bugfix relating to the above
ok almabda

~~~~~
//##14. can import all types normally as
ok

~~~~~
//##15. bug in lambda definition and annotation reference
ok almabda

~~~~~
//##16. protected access from module
okok

~~~~~
//##17. tpyedef protected ok and pkg
ok 9

~~~~~
//##18. package protected enum
[ONE, ONE, ONE]

~~~~~
//##19. protected ok
ok

~~~~~
//##20. package
ok

~~~~~
//##21. circular dependancies
ok

~~~~~
//##22. via dot class reflection
ok: hi

~~~~~
//##23. compile all modules not just those imported
ok: hi

~~~~~
//##24. was a bug now its ok
fine

~~~~~
//##25. on ext func
ok: 22

~~~~~
//##26. import extension function
ok: [22, 22]

~~~~~
//##27. bugfix on function import
asdasd15

~~~~~
//##28. chain of three dependants
hi