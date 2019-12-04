//##1. qualified this via this obj
[outer method, outer variable, inner method, inner variable]

~~~~~
//##2. qualified this direct invocation
[outer method, outer variable, inner method, inner variable]

~~~~~
//##3. three levels
[outer method, outer variable, inner method, inner variable, inner2 method, inner2 variable]

~~~~~
//##4. simple funcext
hi there

~~~~~
//##5. ext function shouldnt hide the class method
in MyClass: there

~~~~~
//##6. ext func call implicit this
12

~~~~~
//##7. ext func call explicit this
12

~~~~~
//##8. implicit and explicit variable access
[12, 12]

~~~~~
//##9. references to ext func class methods
[12, 12, 12, 12]

~~~~~
//##10. calls in nested inner function
nest [12, 12]

~~~~~
//##11. bugfix ensure args called correctly, factored in instance type of ext function
12

~~~~~
//##12. can override methods 
in ext function

~~~~~
//##13. ext function in bytecode
hihihihi

~~~~~
//##14. ext method 
[12, 12]

~~~~~
//##15. ext method call vars 
[12, 12]

~~~~~
//##16.a ext method call nested indirect
[12]

~~~~~
//##16.b ext method call nested indirect
[12]

~~~~~
//##17. references inside extension methods
[12, 12]

~~~~~
//##18. qualified this
[extendee, extendee, extendor, extendee]

~~~~~
//##19. qualified this in nested function
[extendee, extendee, extendor, extendee]

~~~~~
//##20. qualified this refs
[extendee, extendee, extendor, extendee]

~~~~~
//##21. generic ext func
[12, 12]

~~~~~
//##22. generic ext func 2
[12, 12]

~~~~~
//##23. generic ext func as func
[12, 12]

~~~~~
//##24. unused local generic
[12, 12]

~~~~~
//##25. local generic infered
[12, 12]

~~~~~
//##26. local generic from args
[12, 12]

~~~~~
//##27. ext func as op overload
[12, 12]

~~~~~
//##28. ext func on primative type
12582912

~~~~~
//##29. ext func on primative type boexed up
12582912

~~~~~
//##30. ext func on primative type unboxed
12582912

~~~~~
//##31. ext func on primative type boxed sub
12582912

~~~~~
//##32. primative as qualified this
[33, MyClass, 33]

~~~~~
//##33. extension function with varargs
varag: [12, 6, [99 7]]

~~~~~
//##34. extension func super to avoid recursive call
5, Mi: 4, 4, Mi: 3, 3, Mi: 2, 2, Mi: 1, 1, Mi: 0, 0

~~~~~
//##35. ar syntax get and set on objects with ext function
i

~~~~~
//##36. ar syntax get and set on objects with ext function 2
[1, [1 99 3 4 5]]

~~~~~
//##37. ar syntax get+set on objects with ext function 2
ok[1 2 3 27 5]

~~~~~
//##38. broke this now fixed it
ok[1, 2, 3, 900, 5]

~~~~~
//##39. fixed list assign with op overload
ok[1, 2, 3, 13, 5]

~~~~~
//##40. doubles work phew
ok[1, 2, 3, 16, 5]

~~~~~
//##41. str concat version
ok[one, twohi, three22]

~~~~~
//##42. extension function when in class override get set
i

~~~~~
//##43. ext func many nesting
i

~~~~~
//##44. get and set normal
ok[1, [2 2 3 4 5]]

~~~~~
//##45. get and set via ext func
ok[1, [2 2 3 4 5]]

