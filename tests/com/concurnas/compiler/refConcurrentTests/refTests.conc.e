//##1.1.a basic - assign existing ref
done cool [15, 15]

~~~~~
//##1.1.a basic - assign existing ref x2 more complex
12

~~~~~
//##1.1.a basic - assign existing ref x2
done cool [15, 15, 15]

~~~~~
//##1.1.b basic - create new ref
done cool 15

~~~~~
//##1.1.c basic - create new ref 2nd varient
done cool 15

~~~~~
//##1.2.a assign to passed in ref
done cool 15

~~~~~
//##1.2.a assign to passed in ref x2
done cool [15, 12]

~~~~~
//##1.3.a in class - create new ref 
done cool 14

~~~~~
//##1.3.b in class - create new ref - with args
done cool 14

~~~~~
//##1.3.c in class - all class locals access variants
done cool [[15, 15, 15], [26, 26, 26]]

~~~~~
//##1.3.d in class - all class fields access variants
done cool [[15, 15], [26, 26]]

~~~~~
//##1.3.e in class - all class fields get set access variants
done cool [[15, 15], [26, 26]]

~~~~~
//##1.4.a external to class - fields
done cool [[15, 15], [26, 26]]

~~~~~
//##1.4.b external to class - fields - getter
done cool [[15, 15], [26, 26]]

~~~~~
//##1.4.c external to class - fields - setter
done cool [[15, 15], [26, 26]]

~~~~~
//##1.5. ref creation from any expr
done cool [1, 1]

~~~~~
//##1.5.b ref creation from any expr - 2
done cool hi

~~~~~
//##1.5.c refs can be null
done cool 23

~~~~~
//##1.5.d double check not set to anything on create
done cool [false, 26]

~~~~~
//##1.5.e null ar checks
done cool [false, 8]

~~~~~
//##1.6.a simple array set
done cool [8, 8]

~~~~~
//##1.6.b simple array set x2
done cool [16, 16]

~~~~~
//##1.6.c array fields
done cool [16, 16]

~~~~~
//##1.6.d array fields external acc
done cool [16, 16]

~~~~~
//##1.6.d array fields external acc getter setter
done cool [16, 16]

~~~~~
//##1.7. misc module level creation
[12, 22]

~~~~~
//##1.8. nested refs
i got: 12

~~~~~
//##1.8.b nested refs - smaller form
i got: 12

~~~~~
//##1.8.c nested refs - extreme
i got: 12

~~~~~
//##1.9. ensure lambdas are not broken
[22, 22]

~~~~~
//##1.10. brackets implicit
[6, 6] no ref overwrite: true

~~~~~
//##1.10. brackets implicit - b with function
[6, 6] no ref overwrite: true

~~~~~
//##1.11. ref double level
done cool 15

~~~~~
//##1.11.b ref double level -x2
done cool 15

~~~~~
//##1.11.c ref doubles yeah looks alright
done cool [15, 22, 21]

~~~~~
//##1.12 ref in lambda in ref
works 47

~~~~~
//##1.13. lambda in ref ref in lambda
works [47, 11]

~~~~~
//##1.14. catch exceptions - ret something
got exception: oops: java.lang.RuntimeException: java.lang.Exception: uh oh

~~~~~
//##1.15. catch exceptions - no ret something
got exception: works 2

~~~~~
//##1.16.a some elaborate stuff which didnt work before but is ok now
done cool 16

~~~~~
//##1.16.b some elaborate stuff which didnt work before but is ok now
done cool [15, 15]

~~~~~
//##1.16.c some elaborate stuff which didnt work before but is ok now - v1
done cool 61

~~~~~
//##1.16.c some elaborate stuff which didnt work before but is ok now - v2
done cool 61

~~~~~
//##1.16.c some elaborate stuff which didnt work before but is ok now - v3
done cool 61

~~~~~
//##1.17. ensure async block implicitly created correctly 
done cool [10, 10]

~~~~~
//##1.18. async does return something for tracking
it's done: [9, true]

~~~~~
//##1.18.b async does return something for tracking
it's done: [9]

~~~~~
//##1.18.c async does return something for tracking double void
it's done: [true, 9]

~~~~~
//##1.18.d async does return something for tracking double void
it's done: [9]

~~~~~
//##1.19. ref to lambda
it's done: 50

~~~~~
//##1.20. ref to lambda x2
it's done: 51

~~~~~
//##1.21. ref to lambda x2 - compact
it's done: 51

~~~~~
//##1.22. more elaborate
it's done: 51

~~~~~
//##1.22.b more elaborate alt variant
it's done: 51

~~~~~
//##1.23. return stmt blows up weird
it's done: 1

~~~~~
//##1.24. also normal
it's done: 1

~~~~~
//##1.25. copy - locals
it's done: [[], [hi]]

~~~~~
//##1.25.b copy - stuff in own class
it's done: [[], [hi]]

~~~~~
//##1.25.c copy - module level
it's done: [[added at module, one expect in one passed to iso], [added at module, one expect in one passed to iso, hi]]

~~~~~
//##1.25.d copy - kids only
it's done: [[firstItem, hi], [firstItem]]

~~~~~
//##2.1. check that eq code gets called with fiber
[true, true]

~~~~~
//##2.2. check that fibers call getClass from object correctly wihtout fiberization
hi[class java.lang.String, class java.lang.String]

~~~~~
//##2.3. ensure that finalize invoked via super is done so via invokespecial not invokevirtual
hi yes done

~~~~~
//##2.4. some ref stuff which looks alright
~[ho, mate, false]

~~~~~
//##2.4. correct equals gets called
[true, true, true]