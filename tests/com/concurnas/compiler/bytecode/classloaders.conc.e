//##1 basic classloading gubbins
[AppClassLoader, x1basicclassloadinggubbins$MyClassLoader, true, InMemoryClassLoader]

~~~~~
//##2 second stab at custom classloader
[class x2secondstabatcustomclassloader$MyClass, class x2secondstabatcustomclassloader$MyClass, false]

~~~~~
//##3 classloader works ok
[class x3classloaderworksok$MyClass, class x3classloaderworksok$MyClass, false]

~~~~~
//##4. classloader of super type
[class x4classloaderofsupertype$MyClass, class x4classloaderofsupertype$MyClass, false, true]

~~~~~
//##5. misc bugfix on generic widlcard with upper bounds
[class x5miscbugfixongenericwidlcardwithupperbounds$MyClass, class x5miscbugfixongenericwidlcardwithupperbounds$MyClass, false, true]

~~~~~
//##6. via classForName
[x6viaclassForName$MyClass, true]

~~~~~
//##7. useas classloader
[CoolClass, true]

~~~~~
//##8. classloaders all implement new operator overloaded with reflection by default
[one, two]

~~~~~
//##9. pass correct formatted name to new overloader opereator
MyClass [1, 2]

~~~~~
//##10. ref to something output from classloader
[one, two]

~~~~~
//##11. ref to something output from classloader curry in args
[one, ok]

~~~~~
//##12. new operator overloader applies even to classloaders
null

~~~~~
//##13. simple one check use with serializer
ok: 100