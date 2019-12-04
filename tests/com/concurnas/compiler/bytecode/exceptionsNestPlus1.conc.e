//##0.0.0.1 triple reboot - 1 simple a
[10, 2, 10, 2, 2, 0, 2] :: 4

~~~~~
//##0.0.0.2 triple reboot - 1 simple b 
[10, 1, 10, 2, 2, 0, 2] :: 4

~~~~~
//##99.1 - nice and basic fin call
[[0 -8], [0 888], [2 0], [0 888], [3 0], [0 888], 6, 3] :: 6

~~~~~
//##99.2 - nice and basic fin call 2
[[0 456], [0 456], [0 7], [0 7], [0 7], [0 7], 6, 0] :: 6

~~~~~
//##99.2 - nice and basic fin call 3
[[0 -8], [0 100], [2 100], [0 100], [3 100], [0 100], 6, 99] :: 6

~~~~~
//##99.2 - nice and basic fin call 4
[[0 -8], [0 888], [2 100], [0 888], [3 100], [0 888], 6, 99] :: 6

~~~~~
//##99.2 - nice and basic fin call 5
[[0 456], [0 456], [0 7], [0 7], [0 7], [0 7], 6, 99] :: 6

~~~~~
//##0.0.1 double reboot - 1 
[10, 3, 10, 3, 2, 4, 2] :: 4

~~~~~
//##0.0.1 double reboot - 1b
[80, 80, 80, 80, 2, 4, 2] :: 4

~~~~~
//##0.0.1 double reboot - 1b complex fin block needs copy
[[0 -8], [0 7], [0 -8], [0 7], [3 100], [0 7], 6, 99] :: 6

~~~~~
//##0.0.1 double reboot - 1c no fin
[[0 -8], [0 7], [0 -8], [0 7], [3 0], [0 7], 0, 0] :: 6

~~~~~
//##0.0.1 double reboot - 1 locals in fin a
[2, 2, 2, 2, 2, 4, 2] :: 4

~~~~~
//##0.0.1 double reboot - 1 locals in fin b
[2, 1, 2, 2, 2, 4, 2] :: 4

~~~~~
//##0.0.1 double reboot - 2 nothing in fin
[2, 2, 2, 2, 2, 0, 2] :: 4

~~~~~
//##0.0.1 double reboot - 2 
[80, 80, 80, 80, 2, 4, 2] :: 4

~~~~~
//##0.0.1 double reboot - 3 
[80, 80, 80, 80, 2, 4, 2] :: 4

~~~~~
//##0.0.1 double reboot - 4 
[80, 80, 80, 80, 2, 4, 2] :: 4

~~~~~
//##0.0.1 double reboot - 5
[80, 80, 80, 80, 2, 4, 2] :: 4

~~~~~
//##0.0.1 double reboot - 2b 
[80, 80, 80, 80, 2, 4, 2] :: 4

~~~~~
//##0.0.1 double reboot - 3b
[80, 80, 80, 80, 2, 4, 2] :: 4

~~~~~
//##0.0.1 double reboot - 4b 
[80, 80, 80, 80, 2, 4, 2] :: 4

~~~~~
//##0.0.1 double reboot - 5b
[80, 80, 80, 80, 2, 4, 2] :: 4

~~~~~
//##0.0.2 double reboot
[80, 80, 80, 80, 2, 4, 2] :: 4

~~~~~
//##0.0.3 more the merry
[10, 2, 10, 2, 2, 0, 2] :: 4

~~~~~
//##0.1 reboot - simple 
[[1 100], [0 100], [2 0], [0 100], [3 100], [0 100], 6] :: 6

~~~~~
//##0.1 reboot - simple 2
[[1 100], [0 100], [2 100], [0 100], [3 100], [0 100], 6] :: 6

~~~~~
//##0.1 reboot - simple 3
[[1 100], [0 888], [2 100], [0 888], [3 100], [0 888], 6] :: 6

~~~~~
//##0.1 reboot - simple 4
[[1 100], [0 888], [2 100], [0 888], [3 100], [0 888]]
[[0 100], [0 100], [0 100], [0 100], [0 100], [0 100]]
12 :: 12

~~~~~
//##0.1 reboot - simple 5
[[1 1], [0 888], [2 2], [0 888], [3 3], [0 888]]
[[0 100], [0 100], [0 100], [0 100], [0 100], [0 100]]
12 :: 12

~~~~~
//##0.1 reboot - simple 6 - skip over exceptions
[[1 100], [0 888], [2 -8], [0 888], [3 -8], [0 888]]
[[1 100], [0 888], [2 -8], [0 888], [3 -8], [0 888]]
12 :: 12

~~~~~
//##0.1 reboot - simple 7 - all throw
[[1 -8], [0 888], [2 -8], [0 888], [3 -8], [0 888]]
[[1 -8], [0 888], [2 -8], [0 888], [3 -8], [0 888]]
12 :: 12

~~~~~
//##0.1 reboot - simple 8 - most complex exception case
[[1 -8], [0 888], [2 100], [0 888], [3 -8], [0 888]]
[[1 -8], [0 888], [2 100], [0 888], [3 -8], [0 888]]
12 :: 12

~~~~~
//##0.1 reboot - simple 9 - most complex exception case with early ret
[[1 -8], [0 888], [2 100], [0 888], [3 100], [0 888]]
[[1 -8], [0 888], [2 100], [0 888], [3 100], [0 888]]
12 :: 12

~~~~~
//##0.1 reboot - simple 10 - most complex exception case with early ret branch in try
[[1 -8], [0 888], [2 100], [0 888], [3 100], [0 888]]
[[0 100], [0 100], [0 100], [0 100], [0 100], [0 100]]
12 :: 12

~~~~~
//##0.2 reboot - split inside catch block 1
[[1 100 100], [0 888 100], [2 999 100], [0 888 100], [3 100 100], [0 888 100]]
[[1 100 100], [0 888 100], [2 -8 100], [0 888 100], [3 100 100], [0 888 100]]
12 :: 12

~~~~~
//##0.2 reboot - split inside catch block 2
[[1 100 100], [0 888 100], [2 999 100], [0 888 100], [3 100 100], [0 888 100]]
[[1 100 100], [0 888 100], [2 -8 100], [0 888 100], [3 100 100], [0 888 100]]
12 :: 12

~~~~~
//##0.2 reboot - catchs end in exception and fin block rets
[[1 0 -2], [0 1 -2], [0 0 9]]
[[1 0 66], [0 1 66], [0 0 9]]
[[0 0 9], [0 0 9], [0 0 9]]
[[0 0 9], [0 0 9], [0 0 9]]
[0, 0, 12] :: 12

~~~~~
//##0.2 reboot - catchs end in exception and fin block non def rets
[[1 0 -2], [0 1 -2], [0 0 9]]
[[1 0 66], [0 1 66], [0 0 9]]
[[0 0 9], [0 0 9], [0 0 9]]
[[0 0 9], [0 0 9], [0 0 9]]
[0, 0, 12] :: 12

~~~~~
//##0.2 reboot - split inside catch block 3
[[1 100 100], [0 888 100], [2 -8 100], [0 888 100], [3 100 100], [0 888 100]]
[[1 100 100], [0 888 100], [2 100 100], [0 888 100], [3 100 100], [0 888 100]]
12 :: 12

~~~~~
//##0.2 reboot - split inside catch block 1 - ret fin
[[1 100 100], [0 888 100], [2 999 100], [0 888 100], [3 100 100], [0 888 100]]
[[1 100 100], [0 888 100], [2 -8 100], [0 888 100], [3 100 100], [0 888 100]]
12 :: 12

~~~~~
//##0.2 reboot - split inside catch block 2 - ret fin
[[1 100 100], [0 888 100], [2 999 100], [0 888 100], [3 100 100], [0 888 100]]
[[1 100 100], [0 888 100], [2 -8 100], [0 888 100], [3 100 100], [0 888 100]]
12 :: 12

~~~~~
//##0.2 reboot - split inside catch block 3 - ret fin
[[1 100 100], [0 888 100], [2 -8 100], [0 888 100], [3 100 100], [0 888 100]]
[[1 100 100], [0 888 100], [2 100 100], [0 888 100], [3 100 100], [0 888 100]]
12 :: 12

~~~~~
//##0.3 reboot - supress final goto in catch block if fin visited instruction even in branch is a throws
[[1 2], [0 9], [2 -8], [0 9], [3 2], [0 9]]
[[1 2], [0 9], [2 2], [0 9], [3 2], [0 9]]
5 :: 12

~~~~~
//##1. test one excep - def ret
[1, 1] :: 1

~~~~~
//##2. test one excep - non def ret
[77, 1, 2, 2] :: 2

~~~~~
//##3. test one excep - def ret thingy
[1, 1, 2, 2, 3, 3, 78, 3] :: 4

~~~~~
//##4. test one excep - def ret thingy 3 non nesting
[1, 1, 2, 2, 3, 3, 78, 3, 100, 3, 100, 3, 4, 4, 78, 4] :: 8

~~~~~
//##5. test one excep - def ret thingy NESTING
[1, 1, 2, 2, 3, 3, 78, 3, 100, 3, 100, 3, 4, 4, 78, 4] :: 8

~~~~~
//##6. tag if statement carryOn - tag all relevant
[40, 40, 10, 40, 20, 40, 10, 40, 40, 40, 10, 40, 30, 40, 10, 40] :: 16

~~~~~
//##7. tag if statement carryOn - supress
[40, 40, 11, 40, 21, 40, 11, 40, 40, 40, 11, 40, 31, 40, 11, 40] :: 16

~~~~~
//##8. elifs - break
[90, 40, 11, 40, 90, 40, 11, 40, 90, 40, 11, 40, 90, 40, 11, 40]
[40, 40, 11, 40, 21, 40, 11, 40, 40, 40, 11, 40, 31, 40, 11, 40] :: 32

~~~~~
//##9. elifs - no break just have alloc
[91, 40, 11, 40, 91, 40, 11, 40, 91, 40, 11, 40, 91, 40, 11, 40]
[40, 40, 11, 40, 21, 40, 11, 40, 40, 40, 11, 40, 31, 40, 11, 40] :: 32

~~~~~
//##10. elifs - more than one break early
[90, 40, 11, 40, 90, 40, 11, 40, 90, 40, 11, 40, 90, 40, 11, 40]
[40, 40, 11, 40, 21, 40, 11, 40, 40, 40, 11, 40, 30, 40, 11, 40] :: 32

~~~~~
//##11. misc empty line
[1, 40, 11, 40, 1, 40, 11, 40, 1, 40, 11, 40, 1, 40, 11, 40]
[40, 40, 11, 40, 21, 40, 11, 40, 40, 40, 11, 40, 30, 40, 11, 40] :: 32

~~~~~
//##12. elif non defo returns - final thing is a ret - a 
[90, 40, 30, 40, 0, 40, 30, 40] :: 8

~~~~~
//##13. elif non defo returns - final thing is a ret - b 
[90, 40, 30, 40, 0, 40, 30, 40] :: 8

~~~~~
//##14. elif non defo returns - final thing is a ret with a twist- c
[90, 40, 30, 40, 1, 40, 30, 40] :: 8

~~~~~
//##15. nested if - a
[90, 40, 30, 40, 0, 40, 30, 40] :: 8

~~~~~
//##16. nested if - b - stuff after
[90, 40, 30, 40, 1, 40, 30, 40] :: 8

~~~~~
//##17. nested if - c - stuff after - another variant
[96, 40, 30, 40, 1, 40, 30, 40] :: 8

~~~~~
//##18. nested if - c - stuff after - another variant2
[96, 40, 30, 40, 3, 40, 30, 40] :: 8

~~~~~
//##19. the throwing of exceptions
[22, 99]
[22, 33]
22 :: 5

~~~~~
//##2-manyE-1. test one excep - def ret
[1, 1] :: 1

~~~~~
//##2-manyE-2. test one excep - non def ret
[77, 1, 2, 2] :: 2

~~~~~
//##2-manyE-3. test one excep - def ret thingy
[1, 1, 2, 2, 3, 3, 78, 3] :: 4

~~~~~
//##2-manyE-4. test one excep - def ret thingy 3 non nesting
[1, 1, 2, 2, 3, 3, 78, 3, 100, 3, 100, 3, 4, 4, 78, 4] :: 8

~~~~~
//##2-manyE-5. test one excep - def ret thingy NESTING
[1, 1, 2, 2, 3, 3, 78, 3, 100, 3, 100, 3, 4, 4, 78, 4] :: 8

~~~~~
//##2-manyE-6. tag if statement carryOn - tag all relevant
[40, 40, 10, 40, 20, 40, 10, 40, 40, 40, 10, 40, 30, 40, 10, 40] :: 16

~~~~~
//##2-manyE-7. tag if statement carryOn - supress
[40, 40, 11, 40, 21, 40, 11, 40, 40, 40, 11, 40, 31, 40, 11, 40] :: 16

~~~~~
//##2-manyE-8. elifs - break
[90, 40, 11, 40, 90, 40, 11, 40, 90, 40, 11, 40, 90, 40, 11, 40]
[40, 40, 11, 40, 21, 40, 11, 40, 40, 40, 11, 40, 31, 40, 11, 40] :: 32

~~~~~
//##2-manyE-9. elifs - no break just have alloc
[91, 40, 11, 40, 91, 40, 11, 40, 91, 40, 11, 40, 91, 40, 11, 40]
[40, 40, 11, 40, 21, 40, 11, 40, 40, 40, 11, 40, 31, 40, 11, 40] :: 32

~~~~~
//##2-manyE-10. elifs - more than one break early
[90, 40, 11, 40, 90, 40, 11, 40, 90, 40, 11, 40, 90, 40, 11, 40]
[40, 40, 11, 40, 21, 40, 11, 40, 40, 40, 11, 40, 30, 40, 11, 40] :: 32

~~~~~
//##2-manyE-11. misc empty line
[1, 40, 11, 40, 1, 40, 11, 40, 1, 40, 11, 40, 1, 40, 11, 40]
[40, 40, 11, 40, 21, 40, 11, 40, 40, 40, 11, 40, 30, 40, 11, 40] :: 32

~~~~~
//##2-manyE-12. elif non defo returns - final thing is a ret - a 
[90, 40, 30, 40, 0, 40, 30, 40] :: 8

~~~~~
//##2-manyE-13. elif non defo returns - final thing is a ret - b 
[90, 40, 30, 40, 0, 40, 30, 40] :: 8

~~~~~
//##2-manyE-14. elif non defo returns - final thing is a ret with a twist- c
[90, 40, 30, 40, 1, 40, 30, 40] :: 8

~~~~~
//##2-manyE-15. nested if - a
[90, 40, 30, 40, 0, 40, 30, 40] :: 8

~~~~~
//##2-manyE-16. nested if - b - stuff after
[90, 40, 30, 40, 1, 40, 30, 40] :: 8

~~~~~
//##2-manyE-17. nested if - c - stuff after - another variant
[96, 40, 30, 40, 1, 40, 30, 40] :: 8

~~~~~
//##2-manyE-18. nested if - c - stuff after - another variant2
[96, 40, 30, 40, 3, 40, 30, 40] :: 8

~~~~~
//##2-manyE-19. the throwing of exceptions
[22, 99]
[22, 33]
22 :: 5

~~~~~
//##30.1 finally - basic
[120, 100, 1] :: 2

~~~~~
//##30.1.b finally - basic - 2 catch
[120, 100, 1] :: 2

~~~~~
//##30.2 finally - no catch
[100, 401] :: 2

~~~~~
//##30.3 finally - early exception
[10, 1, 1] :: 2

~~~~~
//##30.4 finally - many cath blocks
[10, 11, 1] :: 2

~~~~~
//##30.5 finally - many cath blocks defo return
[10, 1, 1] :: 2

~~~~~
//##30.6 finally - no catch with early break return
[1, 1, 2] :: 2

~~~~~
//##30.7 finally - no early break
[1, 1, 2] :: 2

~~~~~
//##30.8 finally - return in finally block
[9, 10, 1, 2] :: 2

~~~~~
//##30.9 finally - return in finally block also in main block
[99, 1, 1, 2] :: 2

~~~~~
//##30.10 finally - conditional inside the finally block
[90, 1, 90, 2, 2, 4] :: 4

~~~~~
//##30.11 finally - no early return here
[90, 10, 90, 80, 2, 4] :: 4

~~~~~
//##30.12 finally - branch inside the tryblock
[99, 99, 99, 99, 90, 10, 90, 80, 2, 8] :: 8

~~~~~
//##30.13 finally - branch inside the tryblock defo fin return
[99, 99, 99, 99, 90, 10, 90, 10, 2, 8] :: 8

~~~~~
//##30.14 MISC finally - ensure correct type poped when ignoring thing put on stack code defo ret overrides it
[52.0, 52.0, 52.0, 52.0, 0.0, 4.0] :: 4

~~~~~
//##30.15.a finally - many catch, with ret in finally, ret in main blcok
[[1 10], [100 99], [2 10], [100 99], [3 10], [100 99], 6] :: 6

~~~~~
//##30.15.b finally - many catch, with ret in finally, ret in main blcok
[[1 90], [100 99], [2 10], [100 99], [3 10], [100 99], 6] :: 6

~~~~~
//##30.15.c finally - many catch, with ret in finally, ret in main blcok
[[1 10], [100 99], [2 10], [100 99], [3 90], [100 99], 6] :: 6

~~~~~
//##30.15.d finally - many catch, with ret in finally, ret in main blcok
[[1 90], [100 99], [2 90], [100 99], [3 90], [100 99], 6] :: 6

~~~~~
//##30.15.e finally - many catch, with ret in finally, ret in main blcok
[[1 10], [100 90], [2 10], [100 90], [3 10], [100 90], 6] :: 6

~~~~~
//##30.15.f finally - many catch, with ret in finally, ret in main blcok
[[1 10], [100 99], [2 10], [100 99], [3 90], [100 99], 6] :: 6

~~~~~
//##30.15.ff finally - many catch, with ret in finally, ret in main blcok
[[1 90], [100 99], [2 10], [100 99], [3 90], [100 99], 6] :: 6

~~~~~
//##30.15.g finally - many catch, with ret in finally, ret in main blcok
[[1 90], [100 90], [2 10], [100 90], [3 10], [100 90], 6] :: 6

~~~~~
//##30.15.h finally - many catch, with ret in finally, ret in main blcok
[[1 90], [100 90], [2 90], [100 90], [3 90], [100 90], 6] :: 6

~~~~~
//##30.15.2.a finally - many catch, with no ret in finally, ret in main blcok
[[1 1], [0 888], [2 2], [0 888], [3 3], [0 888], 6] :: 6

~~~~~
//##30.15.2.b finally - many catch, with no ret in finally, ret in main blcok
[[1 1], [0 888], [2 2], [0 888], [3 3], [0 888], 6] :: 6

~~~~~
//##30.15.2.c finally - many catch, with no ret in finally, ret in main blcok
[[1 1], [0 888], [2 2], [0 888], [3 3], [0 888], 6] :: 6

~~~~~
//##30.15.2.d finally - many catch, with no ret in finally, ret in main blcok
[[1 1], [0 888], [2 32], [0 888], [3 32], [0 888], 6] :: 6

~~~~~
//##30.15.2.e finally - many catch, with no ret in finally, ret in main blcok
[[1 1], [0 888], [2 2], [0 888], [3 3], [0 888], 6] :: 6

~~~~~
//##30.15.2.f finally - many catch, with no ret in finally, ret in main blcok
[[1 1], [0 888], [2 2], [0 888], [3 3], [0 888], 6] :: 6

~~~~~
//##30.15.2.ff finally - many catch, with no ret in finally, ret in main blcok
[[1 31], [0 888], [2 2], [0 888], [3 31], [0 888], 6] :: 6

~~~~~
//##30.15.2.g finally - many catch, with no ret in finally, ret in main blcok
[[1 8881], [0 8881], [2 2], [0 8881], [3 3], [0 8881], 6] :: 6

~~~~~
//##30.15.2.h finally - many catch, with no ret in finally, ret in main blcok
[[1 888], [0 888], [2 888], [0 888], [3 888], [0 888], 6] :: 6

~~~~~
//##30.15.3.a finally - many catch, throws in catch, with no ret in finally, ret in main blcok
[[1 -8], [0 888], [2 -8], [0 888], [3 -8], [0 888], 6] :: 6

~~~~~
//##30.15.3.b finally - many catch, throws in catch, with no ret in finally, ret in main blcok
[[1 1], [0 888], [2 -8], [0 888], [3 -8], [0 888], 6] :: 6

~~~~~
//##30.15.3.c finally - many catch, throws in catch, with no ret in finally, ret in main blcok
[[1 -8], [0 888], [2 -8], [0 888], [3 1], [0 888], 6] :: 6

~~~~~
//##30.15.3.d finally - many catch, throws in catch, with no ret in finally, ret in main blcok
[[1 -8], [0 888], [2 1], [0 888], [3 1], [0 888], 6] :: 6

~~~~~
//##30.15.3.e finally - many catch, throws in catch, with no ret in finally, ret in main blcok
[[1 -8], [0 1], [2 -8], [0 1], [3 -8], [0 1], 6] :: 6

~~~~~
//##30.15.3.f finally - many catch, throws in catch, with no ret in finally, ret in main blcok
[[1 -8], [0 888], [2 -8], [0 888], [3 1], [0 888], 6] :: 6

~~~~~
//##30.15.3.ff finally - many catch, throws in catch, with no ret in finally, ret in main blcok
[[1 1], [0 888], [2 -8], [0 888], [3 1], [0 888], 6] :: 6

~~~~~
//##30.15.3.g finally - many catch, throws in catch, with no ret in finally, ret in main blcok
[[1 1], [0 1], [2 -8], [0 1], [3 -8], [0 1], 6] :: 6

~~~~~
//##30.15.3.h finally - many catch, throws in catch, with no ret in finally, ret in main blcok
[[1 1], [0 1], [2 1], [0 1], [3 1], [0 1], 6] :: 6

~~~~~
//##30.15.4.s.a finally - branching inside the try, no fin
[[1 10], [0 99], [2 10], [0 99], [3 10], [0 99]]
[[0 90], [0 90], [0 90], [0 90], [0 90], [0 90]]
[[0 90], [0 90], [0 90], [0 90], [0 90], [0 90]]
18 :: 18

~~~~~
//##30.16 finally - many catch, with ret in finally, NO ret in main blcok
[10, 90, 1, 2, 1] :: 2

~~~~~
//##30.17 finally - return with nothing ret in the catch blcok
[90, 90, 1, 2, 1] :: 2

~~~~~
//##30.18 finally - return in fin with optional ret in catch
[90, 90, 20, 90, 2, 4, 1] :: 4

~~~~~
//##30.19 finally - throws exception in finally - no ret in try - catches
[80, 80, 1, 2, 1] :: 2

~~~~~
//##30.20 finally - throws exception in finally - ret in try - catches
[80, 1, 1, 2, 1] :: 2

~~~~~
//##30.21 finally - throws exception in finally - ret in try with brach - catches
[80, 1, 40, 40, 1, 4, 1] :: 4

~~~~~
//##30.22 finally - throws exception in finally - no ret in try
[80, 80, 1, 2, 0] :: 2

~~~~~
//##30.23 finally - throws exception in finally - ret in try
[80, 80, 1, 2, 0] :: 2

~~~~~
//##30.24 finally - throws exception in finally - ret in try with brach
[80, 80, 80, 80, 1, 4, 0] :: 4

~~~~~
//##30.25 finally - throws exception in finally - ret in try with brach - ret in catch
[80, 80, 80, 80, 1, 4, 1] :: 4

~~~~~
//##30.26 finally - throws exception in catch - no finally
[80, 1, 40, 40, 1, 1] :: 4

~~~~~
//##30.27 finally - throws exception in catch - with finally
[80, 1, 40, 40, 1, 4, 1] :: 4

~~~~~
//##30.28 finally - both catch and finally throw exception with certainty
[80, 1, 40, 40, 1, 4, 1] :: 4

~~~~~
//##30.28b finally - both catch and finally throw exception with certainty no catch
[90, 90, 90, 90, 1, 4, 0] :: 4

~~~~~
//##31.1 nest - no early ret
[2, 2, 2, 2, 0, 0] :: 4

~~~~~
//##31.1 nest - no early ret b - excep
[2, 2, 2, 2, 0, 4] :: 4

~~~~~
//##31.23 nest - haz early break for nesting
[23, 1, 23, 2, 2, 2, 4] :: 4

~~~~~
//##31.23 nest - haz early break for nesting - double nest
[23, 1, 23, 2, 2, 2, 4] :: 4

~~~~~
//##31.23 nest - haz early break for nesting - triple nest no fin
[23, 1, 23, 2, 2, 2, 0] :: 4

~~~~~
//##31.1 nest - no early ret c - ret
[1, 1, 1, 1, 0, 0] :: 4

~~~~~
//##31.2 nest - no early ret, throws in finally
[2, 2, 2, 2, 0, 4] :: 4

~~~~~
//##31.3 fin non defo return ensure that all the stuff is wrapped properly 1
[10, 20, 10, 60, 2, 4, 2] :: 4

~~~~~
//##31.3 fin non defo return ensure that all the stuff is wrapped properly 2 all elifs ret complex
[10, 90, 10, 80, 10, 90, 10, 60, 4, 8, 4] :: 8

~~~~~
//##32.0 fin with no exception or ret - simple catch
[10, 60, 10, 60, 3, 4, 2] :: 4

~~~~~
//##32.1 fin with no exception or ret - complex catch
[80, 60, 80, 60, 3, 4, 3] :: 4

~~~~~
//##32.2 fin with no exception or ret - no catches at all
[99, 60, 99, 60, 3, 4, 0] :: 4

~~~~~
//##33. nest simple
[78, 78, 78, 78, 2, 3] :: 4

~~~~~
//##33.1 nest simple - early ret in catch
[10, 80, 10, 80, 10, 80, 10, 80, 4, 2, 4, 2] :: 8

~~~~~
//##33.2 nest simple - early ret in catch, catch ends in exception
[10, 80, 10, 80, 10, 80, 10, 80, 4, 0, 4, 2] :: 8

~~~~~
//##33.3 nest simple - early ret in catch, catch ends in exception - many with also
[2, 9, 2, 9, 2, 1, 2, 0, 1] :: 4

~~~~~
//##33.4 nest simple - early ret in catch, catch ends in exception - many NO also
[2, 9, 2, 9, 2, 0, 2, 0, 1] :: 4

~~~~~
//##33.5 nest simple - with fins 1
[[1 1 2], [1 0 2], [0 0 9], [0 0 9]]
[0, 4] :: 4

~~~~~
//##33.5 nest simple - with fins 2a
[[1 1 2], [1 0 2], [0 0 9], [0 0 9]]
[4, 4] :: 4

~~~~~
//##33.5 nest simple - with fins 2b
[[1 1 2], [1 0 2], [0 0 9], [0 0 9]]
[4, 0] :: 4

~~~~~
//##33.5 nest simple - with fins 2c
[[1 1 2], [1 0 2], [0 0 9], [0 0 9]]
[0, 4] :: 4

~~~~~
//##0.3 reboot - something rather complex with double nesting
[[1 0 1 2], [1 0 1 2], [1 0 0 6], [1 1 0 2], [0 0 0 9], [0 0 0 9], [0 0 0 9], [0 0 0 9]]
[[1 0 1 2], [1 0 1 2], [1 0 0 6], [1 1 0 2], [0 0 0 9], [0 0 0 9], [0 0 0 9], [0 0 0 9]]
[[1 0 1 2], [1 0 1 2], [1 0 0 6], [1 1 0 2], [0 0 0 9], [0 0 0 9], [0 0 0 9], [0 0 0 9]]
6 :: 24

~~~~~
//##0.4 reboot - simple nesting
[[0 1 0 2], [0 1 0 2], [0 0 0 9], [0 0 0 9]]
[0, 0] :: 4

~~~~~
//##34.1 nest - fin comb- throws 1
[[1 0 1 0 -8], [0 1 0 1 -8], [0 0 0 0 9]]
[[1 0 0 0 -8], [0 1 0 0 -8], [0 0 0 0 9]]
[[0 0 0 0 9], [0 0 0 0 9], [0 0 0 0 9]]
[[0 0 0 0 9], [0 0 0 0 9], [0 0 0 0 9]]
[12, 12] :: 12

~~~~~
//##34.1 nest - fin comb- throws 2 - exception in both
[[1 0 1 0 -1], [0 1 0 1 -1], [0 0 0 0 9]]
[[1 0 0 0 -2], [0 1 0 0 -2], [0 0 0 0 9]]
[[0 0 0 0 9], [0 0 0 0 9], [0 0 0 0 9]]
[[0 0 0 0 9], [0 0 0 0 9], [0 0 0 0 9]]
[12, 12] :: 12

~~~~~
//##35.1 nest - fin comb- return 1 null 2
[[1 0 1 0 8], [0 1 0 1 8], [0 0 0 0 9]]
[[1 0 0 0 66], [0 1 0 0 66], [0 0 0 0 9]]
[[0 0 0 0 9], [0 0 0 0 9], [0 0 0 0 9]]
[[0 0 0 0 9], [0 0 0 0 9], [0 0 0 0 9]]
[12, 12] :: 12

~~~~~
//##35.2 nest - fin comb- return 1 return 2
[[1 0 1 0 77], [0 1 0 1 77], [0 0 0 0 9]]
[[1 0 0 0 66], [0 1 0 0 66], [0 0 0 0 9]]
[[0 0 0 0 9], [0 0 0 0 9], [0 0 0 0 9]]
[[0 0 0 0 9], [0 0 0 0 9], [0 0 0 0 9]]
[12, 12] :: 12

~~~~~
//##35.3 nest - fin comb- null 1 return 2
[[1 0 1 0 77], [0 1 0 1 77], [0 0 0 0 9]]
[[1 0 0 0 77], [0 1 0 0 77], [0 0 0 0 9]]
[[0 0 0 0 9], [0 0 0 0 9], [0 0 0 0 9]]
[[0 0 0 0 9], [0 0 0 0 9], [0 0 0 0 9]]
[12, 12] :: 12

~~~~~
//##36.1 nest - no catches
[[0 0 0 0 -2], [0 0 0 0 -2], [0 0 0 0 9]]
[[0 0 0 0 -2], [0 0 0 0 -2], [0 0 0 0 9]]
[[0 0 0 0 9], [0 0 0 0 9], [0 0 0 0 9]]
[[0 0 0 0 9], [0 0 0 0 9], [0 0 0 0 9]]
[[0 0 0 0 -2], [0 0 0 0 -2], [0 0 0 0 9]]
[[0 0 0 0 -2], [0 0 0 0 -2], [0 0 0 0 9]]
[[0 0 0 0 9], [0 0 0 0 9], [0 0 0 0 9]]
[[0 0 0 0 9], [0 0 0 0 9], [0 0 0 0 9]]
[24, 24] :: 24

~~~~~
//##36.2 nest - no catches, no ret on first
[[0 0 0 0 -2], [0 0 0 0 -2], [0 0 0 0 9]]
[[0 0 0 0 -2], [0 0 0 0 -2], [0 0 0 0 9]]
[[0 0 0 0 9], [0 0 0 0 9], [0 0 0 0 9]]
[[0 0 0 0 9], [0 0 0 0 9], [0 0 0 0 9]]
[[0 0 0 0 -2], [0 0 0 0 -2], [0 0 0 0 9]]
[[0 0 0 0 -2], [0 0 0 0 -2], [0 0 0 0 9]]
[[0 0 0 0 9], [0 0 0 0 9], [0 0 0 0 9]]
[[0 0 0 0 9], [0 0 0 0 9], [0 0 0 0 9]]
[24, 24] :: 24

~~~~~
//##36.3 nest - no catches, no ret on any
[[0 0 0 0 -2], [0 0 0 0 -2], [0 0 0 0 9]]
[[0 0 0 0 -2], [0 0 0 0 -2], [0 0 0 0 9]]
[[0 0 0 0 9], [0 0 0 0 9], [0 0 0 0 9]]
[[0 0 0 0 9], [0 0 0 0 9], [0 0 0 0 9]]
[[0 0 0 0 -2], [0 0 0 0 -2], [0 0 0 0 9]]
[[0 0 0 0 -2], [0 0 0 0 -2], [0 0 0 0 9]]
[[0 0 0 0 9], [0 0 0 0 9], [0 0 0 0 9]]
[[0 0 0 0 9], [0 0 0 0 9], [0 0 0 0 9]]
[24, 24] :: 24

~~~~~
//##36.4 nest - odd case
[[0 0 0 0 -2], [0 0 0 0 -2], [0 0 0 0 9]]
[[0 0 0 0 -2], [0 0 0 0 -2], [0 0 0 0 9]]
[[0 0 0 0 9], [0 0 0 0 9], [0 0 0 0 9]]
[[0 0 0 0 9], [0 0 0 0 9], [0 0 0 0 9]]
[[0 0 0 0 -2], [0 0 0 0 -2], [0 0 0 0 9]]
[[0 0 0 0 -2], [0 0 0 0 -2], [0 0 0 0 9]]
[[0 0 0 0 9], [0 0 0 0 9], [0 0 0 0 9]]
[[0 0 0 0 9], [0 0 0 0 9], [0 0 0 0 9]]
[24, 24] :: 24

~~~~~
//##37. ensure that visit ranges are correctly gennerated to be the end of the final return statement
[[0 0 0 0 -2], [0 1 0 0 8], [0 0 0 0 9]]
[[0 0 0 0 -2], [0 1 0 0 8], [0 0 0 0 9]]
[[0 0 0 0 9], [0 0 0 0 9], [0 0 0 0 9]]
[[0 0 0 0 9], [0 0 0 0 9], [0 0 0 0 9]]
[[0 0 0 0 -2], [0 1 0 0 8], [0 0 0 0 9]]
[[0 0 0 0 -2], [0 1 0 0 8], [0 0 0 0 9]]
[[0 0 0 0 9], [0 0 0 0 9], [0 0 0 0 9]]
[[0 0 0 0 9], [0 0 0 0 9], [0 0 0 0 9]]
[24, 0] :: 24

~~~~~
//##38. captured nested thingy watch out in the catch
[[0 0 0 0 7], [0 0 0 0 7], [0 0 0 0 9]]
[[0 0 0 0 7], [0 0 0 0 7], [0 0 0 0 9]]
[[0 0 0 0 9], [0 0 0 0 9], [0 0 0 0 9]]
[[0 0 0 0 9], [0 0 0 0 9], [0 0 0 0 9]]
[[0 0 0 0 7], [0 0 0 0 7], [0 0 0 0 9]]
[[0 0 0 0 7], [0 0 0 0 7], [0 0 0 0 9]]
[[0 0 0 0 9], [0 0 0 0 9], [0 0 0 0 9]]
[[0 0 0 0 9], [0 0 0 0 9], [0 0 0 0 9]]
[24, 24] :: 24

~~~~~
//##39. captured nested thingy watch out in the catch no fin first
[[0 0 0 0 7], [0 0 0 0 7], [0 0 0 0 9]]
[[0 0 0 0 7], [0 0 0 0 7], [0 0 0 0 9]]
[[0 0 0 0 9], [0 0 0 0 9], [0 0 0 0 9]]
[[0 0 0 0 9], [0 0 0 0 9], [0 0 0 0 9]]
[[0 0 0 0 7], [0 0 0 0 7], [0 0 0 0 9]]
[[0 0 0 0 7], [0 0 0 0 7], [0 0 0 0 9]]
[[0 0 0 0 9], [0 0 0 0 9], [0 0 0 0 9]]
[[0 0 0 0 9], [0 0 0 0 9], [0 0 0 0 9]]
[0, 24] :: 24

~~~~~
//##40. dunno abot this but works ok
[[0 0 0 0 8], [0 0 0 0 8], [0 0 0 0 9]]
[[0 0 0 0 8], [0 0 0 0 8], [0 0 0 0 9]]
[[0 0 0 0 9], [0 0 0 0 9], [0 0 0 0 9]]
[[0 0 0 0 9], [0 0 0 0 9], [0 0 0 0 9]]
[[0 0 0 0 8], [0 0 0 0 8], [0 0 0 0 9]]
[[0 0 0 0 8], [0 0 0 0 8], [0 0 0 0 9]]
[[0 0 0 0 9], [0 0 0 0 9], [0 0 0 0 9]]
[[0 0 0 0 9], [0 0 0 0 9], [0 0 0 0 9]]
[24, 24] :: 24

~~~~~
//##41. nest- first fin defo excep
[[0 0 0 0 7], [0 0 0 0 7], [0 0 0 0 9]]
[[0 0 0 0 7], [0 0 0 0 7], [0 0 0 0 9]]
[[0 0 0 0 9], [0 0 0 0 9], [0 0 0 0 9]]
[[0 0 0 0 9], [0 0 0 0 9], [0 0 0 0 9]]
[[0 0 0 0 7], [0 0 0 0 7], [0 0 0 0 9]]
[[0 0 0 0 7], [0 0 0 0 7], [0 0 0 0 9]]
[[0 0 0 0 9], [0 0 0 0 9], [0 0 0 0 9]]
[[0 0 0 0 9], [0 0 0 0 9], [0 0 0 0 9]]
[24, 24] :: 24

~~~~~
//##42. nest - branch oddness
[[0 0 0 0 7], [0 0 0 0 7], [0 0 0 0 9]]
[[0 0 0 0 7], [0 0 0 0 7], [0 0 0 0 9]]
[[0 0 0 0 9], [0 0 0 0 9], [0 0 0 0 9]]
[[0 0 0 0 9], [0 0 0 0 9], [0 0 0 0 9]]
[[0 0 0 0 7], [0 0 0 0 7], [0 0 0 0 9]]
[[0 0 0 0 7], [0 0 0 0 7], [0 0 0 0 9]]
[[0 0 0 0 9], [0 0 0 0 9], [0 0 0 0 9]]
[[0 0 0 0 9], [0 0 0 0 9], [0 0 0 0 9]]
[24, 24] :: 24

~~~~~
//##43. nest - more branch oddness
[[0 0 0 0 7], [0 0 0 0 7], [0 0 0 0 9]]
[[0 0 0 0 7], [0 0 0 0 7], [0 0 0 0 9]]
[[0 0 0 0 9], [0 0 0 0 9], [0 0 0 0 9]]
[[0 0 0 0 9], [0 0 0 0 9], [0 0 0 0 9]]
[[0 0 0 0 7], [0 0 0 0 7], [0 0 0 0 9]]
[[0 0 0 0 7], [0 0 0 0 7], [0 0 0 0 9]]
[[0 0 0 0 9], [0 0 0 0 9], [0 0 0 0 9]]
[[0 0 0 0 9], [0 0 0 0 9], [0 0 0 0 9]]
[24, 24] :: 24

~~~~~
//##43. nest - more branch oddness branch in fin, non def ret
[[0 0 0 0 7], [0 0 0 0 7], [0 0 0 0 9]]
[[0 0 0 0 7], [0 0 0 0 7], [0 0 0 0 9]]
[[0 0 0 0 9], [0 0 0 0 9], [0 0 0 0 9]]
[[0 0 0 0 9], [0 0 0 0 9], [0 0 0 0 9]]
[[0 0 0 0 7], [0 0 0 0 7], [0 0 0 0 9]]
[[0 0 0 0 7], [0 0 0 0 7], [0 0 0 0 9]]
[[0 0 0 0 9], [0 0 0 0 9], [0 0 0 0 9]]
[[0 0 0 0 9], [0 0 0 0 9], [0 0 0 0 9]]
[24, 24] :: 24

~~~~~
//##44. nest - more branch oddness branch in main block
[[0 0 0 0 7], [0 0 0 0 7], [0 0 0 0 9]]
[[0 0 0 0 7], [0 0 0 0 7], [0 0 0 0 9]]
[[0 0 0 0 9], [0 0 0 0 9], [0 0 0 0 9]]
[[0 0 0 0 9], [0 0 0 0 9], [0 0 0 0 9]]
[[0 0 0 0 7], [0 0 0 0 7], [0 0 0 0 9]]
[[0 0 0 0 7], [0 0 0 0 7], [0 0 0 0 9]]
[[0 0 0 0 9], [0 0 0 0 9], [0 0 0 0 9]]
[[0 0 0 0 9], [0 0 0 0 9], [0 0 0 0 9]]
[24, 24] :: 24

~~~~~
//##45. nest - simple cases with fin -1
[[0 0 0 0 8], [0 0 0 0 8], [0 0 0 0 8]]
[[0 0 0 0 8], [0 0 0 0 8], [0 0 0 0 8]]
[[0 0 0 0 8], [0 0 0 0 8], [0 0 0 0 8]]
[[0 0 0 0 8], [0 0 0 0 8], [0 0 0 0 8]]
[[0 0 0 0 8], [0 0 0 0 8], [0 0 0 0 8]]
[[0 0 0 0 8], [0 0 0 0 8], [0 0 0 0 8]]
[[0 0 0 0 8], [0 0 0 0 8], [0 0 0 0 8]]
[[0 0 0 0 8], [0 0 0 0 8], [0 0 0 0 8]]
[24, 24] :: 24

~~~~~
//##45. nest - simple cases with fin -2
[[0 0 0 0 8], [0 0 0 0 8], [0 0 0 0 8]]
[[0 0 0 0 8], [0 0 0 0 8], [0 0 0 0 8]]
[[0 0 0 0 8], [0 0 0 0 8], [0 0 0 0 8]]
[[0 0 0 0 8], [0 0 0 0 8], [0 0 0 0 8]]
[[0 0 0 0 8], [0 0 0 0 8], [0 0 0 0 8]]
[[0 0 0 0 8], [0 0 0 0 8], [0 0 0 0 8]]
[[0 0 0 0 8], [0 0 0 0 8], [0 0 0 0 8]]
[[0 0 0 0 8], [0 0 0 0 8], [0 0 0 0 8]]
[24, 0] :: 24

~~~~~
//##45. nest - simple cases with fin -3
[[0 0 0 0 8], [0 0 0 0 8], [0 0 0 0 8]]
[[0 0 0 0 8], [0 0 0 0 8], [0 0 0 0 8]]
[[0 0 0 0 8], [0 0 0 0 8], [0 0 0 0 8]]
[[0 0 0 0 8], [0 0 0 0 8], [0 0 0 0 8]]
[[0 0 0 0 8], [0 0 0 0 8], [0 0 0 0 8]]
[[0 0 0 0 8], [0 0 0 0 8], [0 0 0 0 8]]
[[0 0 0 0 8], [0 0 0 0 8], [0 0 0 0 8]]
[[0 0 0 0 8], [0 0 0 0 8], [0 0 0 0 8]]
[0, 24] :: 24

~~~~~
//##46. nest - check all fins get correctly called - a
[[0 0 0 -2], [0 0 0 888]] :: [2, 2, 2] :: 2

~~~~~
//##46. nest - check all fins get correctly called - b
[[0 0 0 -2], [0 0 0 888]] :: [0, 2, 2] :: 2

~~~~~
//##46. nest - check all fins get correctly called - c
[[0 0 0 -2], [0 0 0 888]] :: [2, 2, 0] :: 2

~~~~~
//##46. nest - check all fins get correctly called - d
[[0 0 0 -2], [0 0 0 888]] :: [2, 0, 2] :: 2

~~~~~
//##46. nest - check all fins get correctly called - e
[[0 0 0 -2], [0 0 0 888]] :: [0, 0, 2] :: 2

~~~~~
//##50. extra - non defo excep but more than one
Exception thrownjava.lang.Exception: ughExcep:Throwable thrownjava.lang.Throwable: ughTrh: :: 3