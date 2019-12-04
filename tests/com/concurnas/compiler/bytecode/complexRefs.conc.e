//##1. is as basic tests
[55 0 0 0 0 0 0 0 0 0] - [55 0 0 0 0 0 0 0 0 0] T= [true true]

~~~~~
//##2. set and get - direct
[55 0 12 11 11 11 1 1 0 99 0 1 12 11]

~~~~~
//##3. set and get - indirect but resolves to ref type not held type
[55 0 12 11 11 11 1 1 0 99 0 1 12 11]

~~~~~
//##4. ref methods obtained from ref itself if not on type referenced
[55 0 0 100 0 0 0 0 0 0 100]

~~~~~
//##5. ref type constructor invokation
[99 null null 99 null null]

~~~~~
//##6. ref type constructor invokation - simple
9

~~~~~
//##7. small bugfix array ref conversion from generic to localarray
[[1: 2:] [3: 4:] has: [3: 4:]]

~~~~~
//##8. small bugfix array ref conversion from generic to localarray - via feilds directly
[[1: 2:] [3: 4:] has: [3: 4:]]

~~~~~
//##9. works, lovely
[[9: null] [9: null]]

~~~~~
//##10. works, lovely some more
[[2: 2:] has: [2: 2:]]

~~~~~
//##11. implicit construction 
[99 null]

~~~~~
//##12. implicit construction - more complex
[99: null]

~~~~~
//##13. explicit construction
[99 null]

~~~~~
//##14. do set on ref array
[99: null]

~~~~~
//##15. do set on ref array more advanced
[[12: null] [12: null] 10]

~~~~~
//##16. multiimport
[[12: null] [12: null] 10]

~~~~~
//##17. set and wakeup on stuff in list change
[[9 null] [9 null]]

~~~~~
//##18. more of above
[[9 89] [9 null], [9 89],  [9 89]]

~~~~~
//##19. simple transactions
[[9 null] [9 null]]

~~~~~
//##20. ensure transactional order preserved inside refarray
[[1 1], [10 1], [10 50], [90 55]]

~~~~~
//##21. single writes have transactional properties
[99, 111, , 111, 99, ]

~~~~~
//##22. sets on arrayref work outside of transactions
[100,]

~~~~~
//##23. ensure transactions outside of trans are correctly recorded
[[10 null null],[10 50 null],[90 55 null],[90 55 100],, false]

~~~~~
//##24. list thing modified
[[0],[1],[1, 0],[2], []]

~~~~~
//##25. doesnt belong here but i thought it was cool
[hi, hi, hi, out]

~~~~~
//##26. ensure transactions correctly captured when set to same value as before
[9 9]

~~~~~
//##x27. if no changes in transaction for referenced ref then that no longer blows up
[9 0]

~~~~~
//##x28 check all things set
[9 9 9]

~~~~~
//##x29 parfor ensure set a
[11:, 12:, 13:, 14:]

~~~~~
//##x30 parfor ensure set b
[11, 12, 13, 14]

~~~~~
//##x31 parforsync
[11, 12, 13, 14]

~~~~~
//##x32 parfor on old and new for loops
[[11, 12, 13, 14] [11, 12, 13, 14]]

~~~~~
//##x33 parfor returns something other than int
[a1, a2, a3, a4]

~~~~~
//##x34 ref arrays of arras used to be a problem
[333 null 222 99 ; null]

~~~~~
//##x35 ref arrays of arras used to be a problem as above but locked
[222 99 ; null]

~~~~~
//##x36 parfor stuff as arrays
[[1 2 1], [1 2 2], [1 2 3], [1 2 4]]

~~~~~
//##x37 nested parfor
[[11:, 12:, 13:, 14:]:, [11:, 12:, 13:, 14:]:, [11:, 12:, 13:, 14:]:, [11:, 12:, 13:, 14:]:]

~~~~~
//##x38 nested parfor simple types
[[11:, 12:, 13:, 14:]:, [11:, 12:, 13:, 14:]:, [11:, 12:, 13:, 14:]:, [11:, 12:, 13:, 14:]:]

~~~~~
//##x39 i thought it was neat
[[ok, ok, ok, ok] 1 2 3 4]

~~~~~
//##x40 no return at all from par for
[[1 2 3 4]]

~~~~~
//##x41 i thought this was cool
[11, 12, 13, 14]

~~~~~
//##x42 ref arrays getting values out of
[[11, 12, 13, 14] [11, 12, 13, 14] [11, 12, 13, 14] [[11, 12, 13, 14]] [11, 12, 13, 14]]

~~~~~
//##x43. ensure refArray operates on copy
[12 null]

~~~~~
//##x44. ensure refArray directly settable 
[1 2 3]

~~~~~
//##x45. set on RefArray 
[[1 2 3] [10 20 30]]

~~~~~
//##x46. refarray double level 
[1: null]

~~~~~
//##x47. refarray double level empty 
[]

~~~~~
//##x48. nothing 
[]

~~~~~
//##x49. nice this works
[1: 2: 3:]

~~~~~
//##x50. refarray double level initial assign
[1: 2: 3:]

~~~~~
//##x51. refarray double level later assign
[1: 2: 3:]

~~~~~
//##x52. bug in assignment where rhs is ref but not locked as such
[2: 2: 2:]

~~~~~
//##x53. was a bug before
[12, 12, 12, 12, 12, 12, 12, 12, 12, 12]

~~~~~
//##x54. not a bug
2

~~~~~
//##x55. parfor was playing up
[55, 10]

~~~~~
//##x56. used to be a problem
[12 2]

~~~~~
//##x57. looks cool
[13 true]

~~~~~
//##x58. ensure iface impl of DirectlyAssignable
ok
