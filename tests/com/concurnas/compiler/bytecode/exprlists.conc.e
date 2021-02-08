//##1. expression list
16

~~~~~
//##2. expression list valid expression consumed first
16

~~~~~
//##3. expression list more complex example
[16, 24]

~~~~~
//##4. expression list calling an extension function operating on a primative 
26

~~~~~
//##5. expr lists on things returning functions
[gu[1, 2, 3], fu[1, 2, 3], fu2[1, 2, 3]]

~~~~~
//##6. expr lists of funcrefs
[16, 24]

~~~~~
//##7. expr lists constructor refs 
[lovely: 4, lovely: 4]

~~~~~
//##8. constrcutor invoke
lovely: 4

~~~~~
//##9. abigiuity overriden by normal logic rules
15

~~~~~
//##10. two on own
12

~~~~~
//##11. three itesms
13

~~~~~
//##12. four itesms
16

~~~~~
//##13. more four
25

~~~~~
//##14. assign new to expr list
12

~~~~~
//##15. map to dot dot
[ok, [10, 99]]

~~~~~
//##16. direct dot and double dot
[10, 99]

~~~~~
//##17. array length
3

~~~~~
//##18. vararg lambda
gu[1 2 3 5]

~~~~~
//##19. dot and double dot optimization
x is: 1

~~~~~
//##20. operator overload of invoke
33

~~~~~
//##21. vararg call
varag: [[33 23 24], 99]

~~~~~
//##22. extension function with varargs
varag: [12, 6, [99 7]]

~~~~~
//##23. funcrefs with varargs to get correctly mapped
[gu[1, 2, [8 9]], gu[1, 2, [8 9]]]

~~~~~
//##24. bugfix on zero arg version
14

~~~~~
//##25. bugfix expr lists can be on lhs of assignment
[Myclass: 99 Myclass: 2 Myclass: 3 Myclass: 4 Myclass: 5]

~~~~~
//##26. expr lists on module like things
true

~~~~~
//##27. expr lists on module like things also fine
true

~~~~~
//##28. expr lists on module like things longer def
true

~~~~~
//##29. expr lists on module with auto import variant
true

~~~~~
//##30. expr lists on module with auto import variant v2
true

~~~~~
//##31. vars and things in call set
fine

~~~~~
//##32. vars and things in call set more complex 1
fine

~~~~~
//##33. partial dot definition in expression list
ok: hi

~~~~~
//##34. partial dot definition in expression list done
fine

~~~~~
//##35. check this works goto getter or setter
hi

~~~~~
//##36. generic param binding works ok now
hi

~~~~~
//##37. generic param binding works ok now cool more clever version
2

~~~~~
//##38. expr takes lambda
ok

~~~~~
//##39. this is fine
ok[12 12]

~~~~~
//##40. expr list takes a labda
ok

~~~~~
//##41. can route to auto imported functions
ok

~~~~~
//##42. looks cool
20

~~~~~
//##43. expr list map takes anon lambda
ok

~~~~~
//##44. expr list takes nice map example
{1->3, 2->6, 3->4, 4->3}

~~~~~
//##45. nice example
Buy 1000000 GBP at when

~~~~~
//##46. map local func nif redirect
12582912

~~~~~
//##47. bugix on double dot pop
Buy 1 GBP at when

~~~~~
//##48. tweak last element to return something
Buy 1000000 GBP at when

~~~~~
//##49. expr list of nested extension function and nested function
[10, 11, 12, 13, 14, 116, 117, 118, 119, 120, 121, 122, 123, 124, 125, 126]

~~~~~
//##50. involve imported function
ok-0.5328330203333975

~~~~~
//##51. involve imported module
ok-0.5328330203333975