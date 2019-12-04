//##1. basic single ref case static and class
[hi [32, 2], hi [32, 2, 2]]

~~~~~
//##2. changed variable
hi [1] 8::8:, 99::99:, 911::911:, -1::-1:, 10::10:, 

~~~~~
//##3. calling of local function within initalizer - no extra arguments to bind
hi [1] 8::8:, 99::99:, 911::911:, -1::-1:, 10::10:, 

~~~~~
//##4. calling of local function within initalizer - extra arguments to bind
hi [1, 1] 8::8:, 99::99:, 911::911:, -1::-1:, 10::10:, 

~~~~~
//##5. calling of local function within initalizer - extra arguments to bind - via funcref so more args
hi [1, 1] 8::8:, 99::99:, 911::911:, -1::-1:, 10::10:, 

~~~~~
//##6. chain of onchanges - with ref creation
hi [1, 20, 40]32, 396, 3644, -4, 40, 

~~~~~
//##7. chain of onchanges - with ref pre creation 
hi [1, 20, 40]32, 396, 3644, -4, 40, 

~~~~~
//##8. locally bound variables get overwritten 
hi [1, 1, 99, 10]

~~~~~
//##9. inner function var bindings wihtin the onchange
hi [1, 40]

~~~~~
//##10. onchange multiple refs
hi [1111, 69, 20][2, 2]

~~~~~
//##11. big random test - many iso
[[hi a, hi b], [hi a, hi b]]true

~~~~~
//##11.b big random test - many iso - with notificaiton
[[hi [2]:a, hi [2]:b], [hi [2]:a, hi [2]:b]]

~~~~~
//##11.c big random test - simple case to validate non pause of iso
hi [1, 118]w: 118

~~~~~
//##12. ensure that binding operates on localvar and not all refnames
200

~~~~~
//##13.a nested lambda def
hi true

~~~~~
//##13.b nested lambda def with funcref
hi true

~~~~~
//##13.c nested lambda def and a funcref complex case...
hi [1111, 69, 20][2, 2]

~~~~~
//##13.d nested lambda def and a funcref EVEN MORE complex case...
hi [1111, 69, 20][2, 2]

~~~~~
//##14. close operates as expected - single
>>>>>>>>>>>>result: [true, 2]

~~~~~
//##14.b close operates as expected - double
>>>>>>>>>>>>result: [true, true]

~~~~~
//##15. ensure await working
[[hi a, hi b], [hi a, hi b]][true: true: true: true:]

~~~~~
//##16. await - ensure local changes inside await are persisted globally
[2, hi, hi, [10, 2], [10, 2], [[1, 100], 69], [[10, 2], 12], 99, 12]

~~~~~
//##17. ref async block await consistancy
[2, set in await, 100, 88, true]

~~~~~
//##18. ensure exceptions passed up the stack on await
[true, java.lang.RuntimeException: uh oh]

~~~~~
//##19. exceptions not caught and passed up stack on onchange
[false, fail]

~~~~~
//##20. multiawait
[1100, 1101]

~~~~~
//##21. assign ref to locally bound var
[7119, 7119, 7119, 7119]

~~~~~
//##22. ensure onchange exceptions not boiled up to initiator
[true, no msg as expected]

~~~~~
//##23. more than one onchange variable subscription
[3000, 7119, 7119, 7119]

~~~~~
//##24. double notification registration
[1000, 3]

~~~~~
//##25. ensure can write to classfield correctly
[7000, 8]

~~~~~
//##26. nested onchange
[99]

~~~~~
//##27. nested await
[2, true]

~~~~~
//##28. nested onchange - 3 levels
[99]

~~~~~
//##29. every
[3, 123]

~~~~~
//##30. catch errors on init
[ok, ok, ok]

~~~~~
//##31. this and super
one1[9, 99, 80]

~~~~~
//##32. break onchange
[99:, true:, true:, 1:]

~~~~~
//##33. break continue onchange
[111:, true:, true:, 1:]

~~~~~
//##34. break continue return onchange
[44:, true:, true:, 0:]

~~~~~
//##35. no return on break is required
[9, 24 no break ret 96 go into break]

~~~~~
//##36. no return on continue is required
[9, 24 got ret 96 go into continue got ret 9]

~~~~~
//##37. no return on return is required
[9, 24 got ret 96 go into so ret]

~~~~~
//##38. always returns
[69, 99]

~~~~~
//##39. break and ret
[69, 99]

~~~~~
//##40.a end cont
[69, 99]

~~~~~
//##40.b end cont
[69, 99]

~~~~~
//##41. break cont on own
[8, 8]

~~~~~
//##42. not sure why this is hear but broke before probably so include as test
[8, 6 go into so ret24 got ret 9]

~~~~~
//##43. dun expect a npe here
[10, 24 got ret 96 go into so ret]

~~~~~
//##44. break in await etc
6

~~~~~
//##44.b break in await etc
6

~~~~~
//##45. self writer
100

~~~~~
//##46. fix bug in default calling of hashcode from tostring on object level
true

~~~~~
//##47. this test may not belong here
[444, 444]

~~~~~
//##49. onchange on arrays of refs
[100: 100: 100: 100:]

~~~~~
//##50. list of refs
[100: 100: 100: 100:]

~~~~~
//##51. set of refs
[100: 100: 100: 100:]

~~~~~
//##52. hashset of refs
[100: 100: 100: 100:]

~~~~~
//##53. basic refset
26

~~~~~
//##54. refset await and every
[[26, true, true, true], [26, true, true, true], true]

~~~~~
//##54.b refset await and every - array version
[[26 true true true], [26 true true true], true]

~~~~~
//##55. refset add and remove post creation
[3, 9:=> 50:=> 99:=> ]

~~~~~
//##z56 async 1 simple
k done: 2

~~~~~
//##56.b close bug
k done: 2

~~~~~
//##z57 async 2 simple
k done: 1

~~~~~
//##z58 async pre called
k done: [1, true]

~~~~~
//##z59 async pre can be used in main block
k done: [1, 2]

~~~~~
//##z60 async do every for all just latter for onchange
[k done: [2, 1, 99], k done: [2, 1, 99], k done: [2, 1, 99], k done: [2, 1, 99], k done: [2, 1, 99]]

~~~~~
//##z61 async ensure registration performed once only
k done: [1, 1, 99]

~~~~~
//##z62 ensure that expr args declared for every onchange are visible
k done: [1, 99, enter 1 : 99]

~~~~~
//##z63 ensure that expr args declared for every onchange are visible more than one
complete: [1, 1, enter 1 : 2, enter 2 : 101]

~~~~~
//##z64 stateObj var refernces on subsequent local var defs get remapped
complete: [1, 1, enter 1 : 2, enter 2 : 101]

~~~~~
//##z65 infer return type
complete: [2, hi, enter 1 : 1, enter 1 : 2, ]

~~~~~
//##z66 infer return type many invokations and writes to return
complete: [4, hi, enter 1 : 1, enter 1 : 1, enter 1 : 2, enter 1 : 2, ]

~~~~~
//##z67 break support within async
complete: [2, hi, enter 1 : 1, enter 1 : 2, , ok]

~~~~~
//##z68 continue support within async
complete: [4, hi, enter 1 : 1, enter 1 : 2, , ok]

~~~~~
//##z69 return support within async
complete: [1, 1, hi, enter 1 : 1, , ok, true, leave]

~~~~~
//##z70 async edxplicit return type infer type
complete: [100, 3, hi3, enter 1 : 100, ]

~~~~~
//##z71 async edxplicit return type return type defined
complete: [100, 3, hi3, enter 1 : 100, ]

~~~~~
//##z72 async edxplicit return type return type defined ref of ref
complete: [100, 3, hi3, enter 1 : 100, ]

~~~~~
//##z72b async edxplicit return type return type defined ref of ref
complete: [100, 3, hi3, enter 1 : 100, ]

~~~~~
//##z73 return is permitted inside onchange async etc
complete: [200, hi: 100, hi: 100]

~~~~~
//##z74 continue is permitted
complete: [200, hi: 200, hi: 200]

~~~~~
//##z75 return var can be used within onchange args
complete: [100, hi2]

~~~~~
//##z76 break can be used in onchange at end
complete: [100, hi ]

~~~~~
//##z77 avoid splice confusion
complete: [100, hi [10, 44]]

~~~~~
//##z78 spawn async instances
[complete: [100, hi ]:, complete: [100, hi ]:, complete: [100, hi ]:, complete: [100, hi ]:, complete: [100, hi ]:]

~~~~~
//##z79 exceptions still count as counts
complete: [1, 101]

~~~~~
//##z80 ensure exceptions thrown ok in isocore
was ok: [1, 2]

~~~~~
//##z81 close triggers event to close async
was ok: [1, yup]

~~~~~
//##z82 exception triggers close triggers event to close async
was ok: [yup]

~~~~~
//##z83 exceptions still count as counts in async
complete: [1, 101, closed, ok java.lang.Exception: excep]

~~~~~
//##z84 check i didnt break copy
[true]

~~~~~
//##z85 check copy on ints etc works ok
[true]

~~~~~
//##z86 close ref written to on nothing more to do
it works

~~~~~
//##z87 onchange can be within an async block
Done

~~~~~
//##z88 a complex nested case of await setting things correctly
[7, set in await 7[9, 7]]

~~~~~
//##z89 ensure nested await is ok
cool

~~~~~
//##z90 minor tweaking of await to support no ret required
cool

~~~~~
//##z91 another minor await tweak
just fine

~~~~~
//##z92 catches exceptions as expected
was ok: [100, 200]

~~~~~
//##z93 creations in pre block correctly mapped 
[7, [ok, also ok]]

~~~~~
//##z94 try catch inside async block
was ok: 2

~~~~~
//##z95 check references to other nested functions work ok
1

~~~~~
//##z96 ref to other nested more complex
1

~~~~~
//##z97 ref to other nested more complex2
12

~~~~~
//##z98 ref to other nested more complex2 in neste
20

~~~~~
//##z99 big test to ensuer doing nesting correctly
[1, 20, 12, hi [20, 2, 2]]

~~~~~
//##z100 another nester thing to test
hi [32, 2, 2]

~~~~~
//##z101 even more complex
hi [130, 2, 2]

~~~~~
//##z102 nesting on two levels
[[88], [88]]

~~~~~
//##z103 ensure correct type passed to state object
6208.0

~~~~~
//##z104 correct preblock types inclusion mapping etc
k done: [1, true, 106]

~~~~~
//##z105 correct preblock types inclusion mapping etc with a ret
k done: [1, true, 6]

~~~~~
//##z106 vaar decl in pre works ok for every
k done: [1, 99, enter 1 : 99]

~~~~~
//##z107 vars in pre
k done: [1, 99, enter 1 : [100, 75]]

~~~~~
//##z108 nested funcs in pre
k done: [1, 99, enter 1 : [99, 99]]

~~~~~
//##z109 nested lambda outside of pre
k done: [1, 99, enter 1 : [99, 99]]

~~~~~
//##z110 nested lambde defined in pre
k done: [1, 99, enter 1 : [99]]

~~~~~
//##z111 nested lambde defined in pre used in every
k done: [1, 99, enter 1 : [99:, 999]]

~~~~~
//##z112 nested lambde defined in pre used in every trigger
k done: [1, 99, enter 1 : [99:, 999]]

~~~~~
//##z113 fix trigger vars
k done: [1, 99, enter 1 : [99, 9]]

~~~~~
//##z114 takes args correctly
k done: [1, 99, enter 1 : [4:, 999]]

~~~~~
//##z115 nested asyncs
k done: enter 1 : [99, 99]

~~~~~
//##z115 can return and use stuff from pre
was ok: [[88, 100], 0.0]

~~~~~
//##z116 seems not impacted by the ret bug
[88, orig, 999, 3]

~~~~~
//##z117 used to not correctly pass in the types and ret type
[[hi:, :, :, :], hi]

~~~~~
//##z118 double two space
[1.0 2.0 3.0]

~~~~~
//##z119 as above
was ok: [[88.0, 99.0, 100.0], 0.0]

~~~~~
//##z120 check nesting 
[88888]