//##1. circular dependancy
x1circulardependancy.conc line 6:1 Class to provide: x1circulardependancy.Client has circular dependency in chain: [constructor arg of type: 'worker' x1circulardependancy.ClientWorker -> constructor arg of type: 'circle' x1circulardependancy.Client]
x1circulardependancy.conc line 10:10 Unable to resolve type corresponding to name: ClientProvider
x1circulardependancy.conc line 11:16 Unable to find method with matching name: Client
 
~~~~~
//##2. provider must have at least one thing it provides
x2providermusthaveatleastonethingitprovides.conc line 3:0 Provider ClientProvider must have at least one provide definition

~~~~~
//##3. dep rhs must be subtype
x3deprhsmustbesubtype.conc line 8:1 Expression for provide dependency qualification must be equal to or a subtype of: java.lang.String. int is not
 
~~~~~
//##4. dep rhs must be subtype from class arg
x4deprhsmustbesubtypefromclassarg.conc line 10:1 Expression for provide dependency qualification must be equal to or a subtype of: java.lang.String. int is not
 
~~~~~
//##5. provide rhs must be subtype
x5providerhsmustbesubtype.conc line 7:1 Expression on right hand side of provide must be equal to or a subtype of: java.lang.String. int is not

~~~~~
//##6. unused provide specific dependancy
x6unusedprovidespecificdependancy.conc line 29:2 Provide specific declared dependency: java.lang.Float is not used
x6unusedprovidespecificdependancy.conc line 37:11 Unable to resolve type corresponding to name: UCProvider
x6unusedprovidespecificdependancy.conc line 38:12 Unable to find method with matching name: Userclass
x6unusedprovidespecificdependancy.conc line 39:12 Unable to find method with matching name: Userclass2
x6unusedprovidespecificdependancy.conc line 40:24 Expected Iterable object or array for list comprehension not: void
 
~~~~~
//##7. unused provide specific dependancy 2
x7unusedprovidespecificdependancy2.conc line 23:2 Provide specific declared dependency: java.lang.Float is not used
x7unusedprovidespecificdependancy2.conc line 30:11 Unable to resolve type corresponding to name: UCProvider
x7unusedprovidespecificdependancy2.conc line 31:12 Unable to find method with matching name: MyActor
 
~~~~~
//##8. private provide
x8privateprovide.conc line 31:15 The method ThingHolder is not visible
 
~~~~~
//##9. no non object provide
x9nononobjectprovide.conc line 12:1 Provide defintions can be expressed only for non array object types
x9nononobjectprovide.conc line 13:1 Provide defintions can be expressed only for non array object types

~~~~~
//##10. no provide primative ref
x10noprovideprimativeref.conc line 5:1 Class to provide: java.lang.Integer: has no public fields, methods or constructors marked inject
 
~~~~~
//##11. injector constructors must be marked public
x11injectorconstructorsmustbemarkedpublic.conc line 7:1 Class to provide: com.concurnas.lang.precompiled.ToInjectHelpers$Illegal has no public fields, methods or constructors marked inject
 
~~~~~
//##12. no lazy mutiassign
x12nolazymutiassign.conc line 4:0 multi assign variables may not be lazy
 
~~~~~
//##13. used to blow up

~~~~~
//##14. used to blow up - used to match on lazy
x14usedtoblowupusedtomatchonlazy.conc line 9:6 Unable to find method with matching name: afunc and arguments (int)
x14usedtoblowupusedtomatchonlazy.conc line 10:6 Unable to find method with matching number of arguments with name: xx
 
~~~~~
//##15. no vectroized lazy ref
x15novectroizedlazyref.conc line 8:5 Unable to find method with matching name: afunc and arguments (int[]^)

~~~~~
//##16. missing injectable constructor
x16missinginjectableconstructor.conc line 11:1 Class to provide: x16missinginjectableconstructor.MyClass has nested dependant class having injectable fields or methods without an injectable constructor: [constructor arg of type: 'an' com.concurnas.lang.types$Lazy<java.lang.String>]
x16missinginjectableconstructor.conc line 16:8 Unable to find method with matching name: MCPRovider
x16missinginjectableconstructor.conc line 17:14 Unable to find method with matching name: MyClass
 
~~~~~
//##17. cannot inject as method param names unknown
x17cannotinjectasmethodparamnamesunknown.conc line 7:1 Fields an2, an3 of com.concurnas.lang.precompiled.Injectables$UnNamedMethod are marked as being injectable but are not public
x17cannotinjectasmethodparamnamesunknown.conc line 8:1 Declared dependency: 'Second Field' java.lang.String is not used
x17cannotinjectasmethodparamnamesunknown.conc line 9:1 Declared dependency: 'Third Field' java.lang.String is not used
x17cannotinjectasmethodparamnamesunknown.conc line 14:14 Unable to find method with matching name: NamedFields
 
~~~~~
//##18. non public fields
x18nonpublicfields.conc line 7:1 Fields an2, an3 of com.concurnas.lang.precompiled.Injectables$UnNamedMethodPName are marked as being injectable but are not public
x18nonpublicfields.conc line 13:8 Unable to find method with matching name: MCPRovider
x18nonpublicfields.conc line 14:14 Unable to find method with matching name: UnNamedMethodPName
 
~~~~~
//##19. non public method
x19nonpublicmethod.conc line 8:1 Declared dependency: 'an2' java.lang.String is not used
x19nonpublicmethod.conc line 9:1 Declared dependency: java.lang.String is not used
x19nonpublicmethod.conc line 13:8 Unable to find method with matching name: MCPRovider
x19nonpublicmethod.conc line 14:14 Unable to find method with matching name: NamedWPrivateMethod
 
~~~~~
//##20. non public constructor
x20nonpublicconstructor.conc line 7:1 Class to provide: com.concurnas.lang.precompiled.Injectables$NamedWConstrcutorNamedIllegal has no public fields, methods or constructors marked inject
x20nonpublicconstructor.conc line 8:1 Declared dependency: 'first arg' java.lang.String is not used
x20nonpublicconstructor.conc line 9:1 Declared dependency: java.lang.String is not used
x20nonpublicconstructor.conc line 13:8 Unable to find method with matching name: MCPRovider
x20nonpublicconstructor.conc line 14:14 Unable to find method with matching name: NamedWConstrcutorNamedIllegal
 
~~~~~
//##21. Provide defintions must be unique
x21Providedefintionsmustbeunique.conc line 11:1 Provide defintions must be unique. A provide with name MyClass has already been defined
x21Providedefintionsmustbeunique.conc line 13:1 Provide defintions must be unique. A provide with name podl has already been defined
 
~~~~~
//##22. only one constructor may be injected
x22onlyoneconstructormaybeinjected.conc line 4:0 Class: MyClass may have only one injectable constructor, it has: 2
x22onlyoneconstructormaybeinjected.conc line 13:1 Ambiguous constructor injection definition in class to provide, more than one constructor for: x22onlyoneconstructormaybeinjected.MyClass has been marked inject
x22onlyoneconstructormaybeinjected.conc line 14:1 Declared dependency: java.lang.String[] is not used
 
~~~~~
//##23. class level inject only
x23classlevelinjectonly.conc line 4:0 Only class level fields may be injected
x23classlevelinjectonly.conc line 5:0 Only class level methods may be injected
 
~~~~~
//##24. no inject consturctor uh oh
x24noinjectconsturctoruhoh.conc line 3:0 Class: MyClass cannot have injectable fields or methods without an injectable constructor
x24noinjectconsturctoruhoh.conc line 11:1 Class to provide: x24noinjectconsturctoruhoh.MyClass cannot have injectable fields or methods without an injectable constructor
x24noinjectconsturctoruhoh.conc line 12:1 Declared dependency: java.lang.String[] is not used
x24noinjectconsturctoruhoh.conc line 13:1 Declared dependency: int[] is not used
 
~~~~~
//##25. anon and local dont get injectables
x25anonandlocaldontgetinjectables.conc line 7:2 Anonymous and local classes may not have injectable fields
 
~~~~~
//##26. Provide dependancy qualifications must be unique
x26Providedependancyqualificationsmustbeunique.conc line 11:1 Class to provide: x26Providedependancyqualificationsmustbeunique.MyClass has nested dependant class having injectable fields or methods without an injectable constructor: [constructor arg of type: 'an2' java.lang.String[]]
x26Providedependancyqualificationsmustbeunique.conc line 12:1 Declared dependency: 'x' java.lang.String[] is not used
x26Providedependancyqualificationsmustbeunique.conc line 13:1 Provide dependency qualifications must be unique. A provide dependency qualifications with name 'x' java.lang.String[] has already been defined
 
~~~~~
//##27. Provide dependancy qualifications must be unique 2
x27Providedependancyqualificationsmustbeunique2.conc line 10:1 Class to provide: x27Providedependancyqualificationsmustbeunique2.MyClass has nested dependant class having injectable fields or methods without an injectable constructor: [constructor arg of type: 'an2' java.lang.String[]]
x27Providedependancyqualificationsmustbeunique2.conc line 11:2 Provide specific declared dependency: 'x' java.lang.String[] is not used
x27Providedependancyqualificationsmustbeunique2.conc line 12:2 Provide dependency qualifications must be unique. A provide dependency qualifications with name 'x' java.lang.String[] has already been defined
 
~~~~~
//##28. Expression for provide dependancy qualifications
x28Expressionforprovidedependancyqualifications.conc line 12:1 Expression for provide dependency qualification must be equal to or a subtype of: java.lang.String. int is not
 
~~~~~
//##29. nothing at all with inject
x29nothingatallwithinject.conc line 10:1 Class to provide: x29nothingatallwithinject.MyClass has no public fields, methods or constructors marked inject
 
~~~~~
//##30. has circular dependancy in chain
x30hascirculardependancyinchain.conc line 11:1 Class to provide: x30hascirculardependancyinchain.MyClass has circular dependency in chain: [field anInt of type: x30hascirculardependancyinchain.MyClass?]
 
~~~~~
//##31. Declared dependancy is not used
x31Declareddependancyisnotused.conc line 13:1 Declared dependency: int is not used
 
~~~~~
//##32. Generic parameters for provide definitions
x32Genericparametersforprovidedefinitions.conc line 11:1 Generic parameters for provide definitions must have a right hand side expression
 
~~~~~
//##33. named dependancy unused
x33nameddependancyunused.conc line 12:1 Declared dependency: 'aan' java.lang.String is not used
 
~~~~~
//##34. transient missing dependancy
x34transientmissingdependancy.conc line 15:1 Class to provide: x34transientmissingdependancy.MyClass is missing dependency of type: 'a' int to provide for [constructor arg of type: 'an2' x34transientmissingdependancy.Another -> constructor arg of type: 'a' int]

~~~~~
//##35. ensure partial type defs used
x35ensurepartialtypedefsused.conc line 31:3 Provide specific declared dependency: java.lang.Object is not used
x35ensurepartialtypedefsused.conc line 33:2 Provide specific declared dependency: java.lang.Object is not used
x35ensurepartialtypedefsused.conc line 35:1 Declared dependency: java.lang.Object is not used

~~~~~
//##36. partial type must be approperiate for lhs def
x36partialtypemustbeapproperiateforlhsdef.conc line 27:1 Class to provide: x36partialtypemustbeapproperiateforlhsdef.MessageProcessor has nested dependant class having injectable fields or methods without an injectable constructor: [constructor arg of type: 'obtainer' x36partialtypemustbeapproperiateforlhsdef.MessageGetter]
x36partialtypemustbeapproperiateforlhsdef.conc line 29:2 Type qualification for type only qualifier must be equal to or a subtype of: x36partialtypemustbeapproperiateforlhsdef.MessageGetter. int is not

~~~~~
//##37. Cannot specify dependancy qualification on its own
x37Cannotspecifydependancyqualificationonitsown.conc line 12:1 Invalid provide dependency qualification: x37Cannotspecifydependancyqualificationonitsown.Bean on its own

~~~~~
//##38. right hand side of dependency must be specified for primative type
x38righthandsideofdependencymustbespecifiedforprimativetype.conc line 10:0 Right hand side of dependency must be specified for: int
 
~~~~~
//##39. single dependancy no elaboration no injectable thing
x39singledependancynoelaborationnoinjectablething.conc line 12:1 Class to provide: x39singledependancynoelaborationnoinjectablething.Bean has nested dependant class having injectable fields or methods without an injectable constructor: [constructor for: x39singledependancynoelaborationnoinjectablething.Bean]
