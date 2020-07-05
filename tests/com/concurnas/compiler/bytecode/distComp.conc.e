//##00. run first to check for mem leak
got: 7

~~~~~
//##0. load class remotely
[1235, false]

~~~~~
//##1. easy executor
ans: 3

~~~~~
//##2. exe more complex
ans: [3 22]

~~~~~
//##3. exe more advanced version
ans: [3 30]

~~~~~
//##4. test retry
ans: 2

~~~~~
//##5. test retry 2
ans: 2

~~~~~
//##6. result is available for exception assignment etc
ans: java.lang.Exception: fail as expected

~~~~~
//##7. stateful dist comp
[ans: 2, ans: java.lang.Exception: fail as expected]

~~~~~
//##8. check shared vars can be overriden
ok

~~~~~
//##9. connect and disconnect
ok

~~~~~
//##10. override global static variable
oktrue

~~~~~
//##11. connect and disconnect
[true, true, true, true]

~~~~~
//##12. basic encode decode with string
MyClass:(hi)

~~~~~
//##13. serizliaze null refs
MyClass:(one, null, three)

~~~~~
//##14. serizliaze graph cycle
MyClass:(one, true, one, true, true)

~~~~~
//##15. serizliaze primatives
MyClass:(one, 12, 2, 200, 23.0, 243.545, true, false, 3, c)

~~~~~
//##16. serizliaze boxed primatives
MyClass:(one, 12, 2, 200, 23.0, 243.545, true, false, 3, c)

~~~~~
//##17. serizliaze primative int array
MyClass:(12.0, [3 4 5])

~~~~~
//##18. serizliaze primative int array with nulls
MyClass:(12.0, [1 2 3], null, [])

~~~~~
//##19. serizliaze multi deimentional arrays w null
MyClass:(12.0, [1 2 3 ; 4 5 6 ; null ; 7 8 9])

~~~~~
//##20. serizliaze other primative arrays
MyClass:(12.0, [1.0 2.0 3.0 ; 4.0 5.0 6.0 ; null ; 7.0 8.0 9.0])

~~~~~
//##21. serizliaze boxed obj array
MyClass:(12.0, [2 3 4])

~~~~~
//##22. serizliaze more than one
MyClass:(12.0, [2 3 4], [9 6 7])

~~~~~
//##23. serizliaze obj array dups
MyClass:(12.0, [2 3 4], [2 3 4])

~~~~~
//##24. serizliaze obj array with nulls
[MyClass:(12.0, [2 3 4 ; null ; null ; 5 6 9]) MyClass:(12.0, [2 3 4 ; null ; null ; 5 6 9])]

~~~~~
//##25. serizliaze obj int array ref dup
MyClass:(12.0, [1 2 3], [1 2 3], true)

~~~~~
//##26. serizliaze obj double array ref dup
MyClass:(12.0, [1.0 2.0 3.0], [1.0 2.0 3.0], true)

~~~~~
//##27. serizliaze special encoding for class
MyClass:(12.0, class java.lang.String)

~~~~~
//##28. serizliaze enum
MyClass:(12.0, TWO)

~~~~~
//##29. serizliaze primative array masked as object
MyClass:(12.0, [1 2 3])

~~~~~
//##30. serizliaze other primative array masked as object
MyClass:([false true], 12.0, [1.0 2.0 3.0])

~~~~~
//##31. serizliaze check boxed object type dupe
MyClass:(12.0, [2 3 4], [2 3 4], true)

~~~~~
//##32. serizliaze boxed array type masked as object
MyClass:(12.0, [1 2 3], 56.8)

~~~~~
//##33. serizliaze list of arrays
MyClass:(12.0, [[1 2 3], [5.6 5.6], [hi]], 56.8)

~~~~~
//##34. serizliaze array multitype
[MyClass:(12.0, [1 2 3 hi], 56.8) hi]

~~~~~
//##35. serizliaze with default params
[MyClass:(12.0, 56.8, 99), MyClass:(12.0, 56.8, 100)]

~~~~~
//##36. serizliaze object type masks single boxed thing
MyClass:(12.0, 454.4, 56.8)

~~~~~
//##37. test PausableLinkedQueue
done

~~~~~
//##38. quick spawn connect disconnect etc
ok

~~~~~
//##39. connect do work disconnect
ok: 1235

~~~~~
//##40. BlockingLocalRef
class java.lang.String

~~~~~
//##41. DedicatedThreadWorkerPool
got: 7

~~~~~
//##42. serialize an exception stack 1
[[FourThings(java.lang.Throwable, init, Throwable.java, 273) FourThings(java.lang.Throwable, init, Throwable.java, 274) FourThings(java.lang.Throwable, init, Throwable.java, 275) null null], [FourThings(java.lang.Throwable, init, Throwable.java, 273) FourThings(java.lang.Throwable, init, Throwable.java, 274) FourThings(java.lang.Throwable, init, Throwable.java, 275) null null]]

~~~~~
//##43. serialize an exception stack 2
true

~~~~~
//##44. request more dependancies
[27, false, false]

~~~~~
//##45. basic dependency analysis
deps are: [x45basicdependencyanalysis$MyClass, x45basicdependencyanalysis$SuperClass, x45basicdependencyanalysis$ATrait, x45basicdependencyanalysis$Annot, x45basicdependencyanalysis$AnotherClass, x45basicdependencyanalysis$TopLevelClass, x45basicdependencyanalysis]

~~~~~
//##46. dependency analysis - generic param
deps are: [x46dependencyanalysisgenericparam$MyClass, x46dependencyanalysisgenericparam$Anotherclass]

~~~~~
//##47. dependency analysis - generic and in seperate module
deps are: [x47dependencyanalysisgenericandinseperatemodule$MyClass, com/myorg/code$Anotherclass]

~~~~~
//##48. dependency analysis - submit deps upfront
[27, true, false, false]

~~~~~
//##49. dont send deps client already has
[27, 28, true, true, true]

~~~~~
//##50. deps sent with unknown dependencies loaded via reflection
[24, true, true, true, true]

~~~~~
//##51. task name is submit
done: [112, true]

~~~~~
//##52. no copy security manager
ok[true]

~~~~~
//##53. custom security manager all permissions
done: [112, true]

~~~~~
//##54. security manager no permissions!
excep

~~~~~
//##55. startup and restart server and client
ok20

~~~~~
//##56. connect when already connected fail
ok[ok err: java.lang.Exception: RemoteServer has already been started, ok err: java.lang.Exception: Connection to: localhost:42000 already established]

~~~~~
//##57. close connection before already connected
ok[ok err: java.lang.Exception: RemoteServer has not been started, ok err: java.lang.Exception: Attempted disconnection but not currently connected]

~~~~~
//##58. auto close and explicit connect
ok20

~~~~~
//##59. cancel reamining on disconnect
as expected