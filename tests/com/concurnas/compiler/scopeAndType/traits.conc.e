//##1. traits cannot be closed
x1traitscannotbeclosed.conc line 2:0 traits cannot be closed
 
~~~~~
//##2. check correct error on override here
x2checkcorrecterroronoverridehere.conc line 10:1 In order for the method 'def thing() int' of class: 'x2checkcorrecterroronoverridehere.Concrete' to be overriden it must be defined in the superclass: x2checkcorrecterroronoverridehere.PassThrough - its been declared abstract
 
~~~~~
//##3. should be overriden
x3shouldbeoverriden.conc line 8:1 Method: 'def ifaceMethod() int' of class: 'java.lang.Object or traits: x3shouldbeoverriden.MyTrait' should be explicitly overriden in subclass: x3shouldbeoverriden.MyClass. Use the override keyword
 
~~~~~
//##4. overriden thing which doesnt exist
x4overridenthingwhichdoesntexist.conc line 8:1 In order for the method 'def ifaceMethodno() int' of class: 'x4overridenthingwhichdoesntexist.MyClass' to be overriden it must be defined in superclass: java.lang.Object or traits: x4overridenthingwhichdoesntexist.MyTrait
 
~~~~~
//##5. missing impliment
x5missingimplimentation.conc line 8:0 Class 'x5missingimplimentation.MyClass' is missing implementations of abstract method definitions inherited: x5missingimplimentation.MyTrait:{def foo() int}
x5missingimplimentation.conc line 10:1 In order for the method 'def something() int' of class: 'x5missingimplimentation.MyClass' to be overriden it must be defined in superclass: java.lang.Object or traits: x5missingimplimentation.MyTrait
 
~~~~~
//##6. cannot define as not defined in MyTrait
x6cannotdefineasnotdefinedinMyTrait.conc line 9:1 In order for the method 'def foo() int' of class: 'x6cannotdefineasnotdefinedinMyTrait.MyClass' to be overriden it must be defined in the superclass: java.lang.Object or traits: x6cannotdefineasnotdefinedinMyTrait.MyTrait - its been declared abstract
 
~~~~~
//##7.Traits cannot be instantiated
x7Traitscannotbeinstantiated.conc line 9:10 x7Traitscannotbeinstantiated.MyTrait is a trait. Traits cannot be instantiated
 
~~~~~
//##8. Trait requires qualifcation type to meet specs of trait
x8Traitrequiresqualifcationtypetomeetspecsoftrait.conc line 8:0 Generic type refernce at index: 0 is not compatible with trait definiton. T Number is not equal to or a subtype of java.lang.String

~~~~~
//##9. cannot be qualified more than once with different arguments in the heirarchy
x9cannotbequalifiedmorethanoncewithdifferentargumentsintheheirarchy.conc line 10:0 The generic trait x9cannotbequalifiedmorethanoncewithdifferentargumentsintheheirarchy.MyTrait cannot be qualified more than once with different arguments in the hierarchy for class: x9cannotbequalifiedmorethanoncewithdifferentargumentsintheheirarchy.ConcClass. Incompatible definitions: x9cannotbequalifiedmorethanoncewithdifferentargumentsintheheirarchy.MyTrait<java.lang.Double>, x9cannotbequalifiedmorethanoncewithdifferentargumentsintheheirarchy.MyTrait<java.lang.Integer>
 
~~~~~
//##10. Circular inheritance reference detected
x10Circularinheritancereferencedetected.conc line 3:0 Circular inheritance reference detected for: x10Circularinheritancereferencedetected.MyTrait
 
~~~~~
//##11. cannot be qualified more than once with different arguments in the heirarchy precompiled
x11cannotbequalifiedmorethanoncewithdifferentargumentsintheheirarchyprecompiled.conc line 7:0 The generic trait com.concurnas.lang.precompiled.TraitHelper$MyTrait cannot be qualified more than once with different arguments in the hierarchy for class: x11cannotbequalifiedmorethanoncewithdifferentargumentsintheheirarchyprecompiled.ConcClass. Incompatible definitions: com.concurnas.lang.precompiled.TraitHelper$MyTrait<java.lang.Double>, com.concurnas.lang.precompiled.TraitHelper$MyTrait<java.lang.Integer>
 
~~~~~
//##12. force thing to be abstract
x12forcethingtobeabstract.conc line 12:0 Class 'x12forcethingtobeabstract.ConcClass' is missing implementations of abstract method definitions inherited: x12forcethingtobeabstract.AnotherThing:{def converter(java.lang.Double) double}

~~~~~
//##13. can only use with on traits
x13canonlyusewithontraits.conc line 4:0 class MyClass cannot be composed with: TraitHelper as it is not a trait
x13canonlyusewithontraits.conc line 8:0 class MyClass2 cannot be composed with: MyClass, TraitHelper as they are not traits
 
~~~~~
//##14. super qualifier restrictions on use
x14superqualifierrestrictionsonuse.conc line 7:1 super references can only be made within a class or extension function not gpu functions or kernels
x14superqualifierrestrictionsonuse.conc line 12:1 qualifier for super cannot be defined inside extension function
x14superqualifierrestrictionsonuse.conc line 17:6 qualifier for super does not exist: MyTraitx
x14superqualifierrestrictionsonuse.conc line 18:6 qualifier for super must be either the super class: x14superqualifierrestrictionsonuse.ConccClass or any traits: x14superqualifierrestrictionsonuse.MyTrait
x14superqualifierrestrictionsonuse.conc line 24:6 qualifier for super must be either the super class: x14superqualifierrestrictionsonuse.ConccClass2 or any traits the current class is composed of
 
~~~~~
//##15. only traits can qualify a super
x15onlytraitscanqualifyasuper.conc line 12:2 Only Concurnas traits may be explicity referenced: com.concurnas.lang.precompiled.TraitHelper$MyTraitx is not a trait, it's an interface
x15onlytraitscanqualifyasuper.conc line 13:2 Only Concurnas traits may be explicity referenced: com.concurnas.lang.precompiled.TraitHelper$MyTrait2x is not a trait, it's an interface

~~~~~
//##16. Cannot directly invoke the abstract method
x16Cannotdirectlyinvoketheabstractmethod.conc line 17:41 Cannot directly invoke the abstract method: thing

~~~~~
//##17. ambiguous methods from traits
x17ambiguousmethodsfromtraits.conc line 13:0 class x17ambiguousmethodsfromtraits.ConccClass has ambiguous methods from traits. log of type: def (java.lang.String) java.lang.String is from: x17ambiguousmethodsfromtraits.Logger1, x17ambiguousmethodsfromtraits.Logger2. It must be added to: x17ambiguousmethodsfromtraits.ConccClass (either as an abstract or concrete method) or x17ambiguousmethodsfromtraits.ConccClass declared abstract

~~~~~
//##18. used to blow up
x18usedtoblowup.conc line 7:1 Itr cannot resolve reference to trait: Iterator
x18usedtoblowup.conc line 18:16 Unable to resolve type corresponding to name: Iterator
x18usedtoblowup.conc line 19:6 Unable to determine type of: Iterator<java.lang.Object>

~~~~~
//##19. intermediate superclass abstract with conflict
x19intermediatesuperclassabstractwithconflict.conc line 16:0 Class 'x19intermediatesuperclassabstractwithconflict.ConccClass' is missing implementations of abstract method definitions inherited: x19intermediatesuperclassabstractwithconflict.SupClass:{def log(java.lang.String) java.lang.String}
 
~~~~~
//##20. Trait implicity extend incompatable
x20Traitimplicityextendincompatable.conc line 6:0 Traits: com.myorg.code2.MyTrait implicity extend: com.myorg.code2.AbstractClass which is not compatable with defined supertype of: x20Traitimplicityextendincompatable.AnotherClass
x20Traitimplicityextendincompatable.conc line 6:0 Traits: com.myorg.code2.MyTrait implicity extend: com.myorg.code2.AbstractClass which is not compatable with defined supertype of: x20Traitimplicityextendincompatable.AnotherClass
 
~~~~~
//##21. Incompatible trait extends non trait class heirarchy
x21Incompatabletraitextendsnontraitclassheirarchy.conc line 4:0 Traits: com.myorg.code2.MyTrait, com.myorg.code2.MyTrait2 extend differing non related non trait supertypes: com.myorg.code2.AbstractClass, com.myorg.code2.AbstractClass2 - they cannot be used together in concrete classes
x21Incompatabletraitextendsnontraitclassheirarchy.conc line 4:0 Traits: com.myorg.code2.MyTrait, com.myorg.code2.MyTrait2 extend differing non related non trait supertypes: com.myorg.code2.AbstractClass, com.myorg.code2.AbstractClass2 - they cannot be used together in concrete classes
 
~~~~~
//##22. private things not visible
x22privatethingsnotvisible.conc line 14:37 The method thing is not visible
 
~~~~~
//##23. variables need implmenting for traits
x23variablesneedimplmentingfortraits.conc line 14:0 Class 'x23variablesneedimplmentingfortraits.Fail1' is missing implementations of trait fields: count of type int from x23variablesneedimplmentingfortraits.MyTrait
x23variablesneedimplmentingfortraits.conc line 18:0 Class 'x23variablesneedimplmentingfortraits.Fail2' is missing implementations of trait fields: count of type int from x23variablesneedimplmentingfortraits.MyTrait
x23variablesneedimplmentingfortraits.conc line 28:0 Class 'x23variablesneedimplmentingfortraits.Fail3' is missing implementations of trait fields: count of type int from x23variablesneedimplmentingfortraits.MyTrait
 
~~~~~
//##24. annotate traits with vars
x24annotatetraitswithvars.conc line 6:0 Class 'x24annotatetraitswithvars.MyClass' is missing implementations of trait fields: tfield of type int from com.concurnas.lang.precompiled.TraitHelper$TraitWithField
 
~~~~~
//##25. contain duplicate trait references
x25containduplicatetraitreferences.conc line 5:0 class AbstrClass contains duplicate trait references: com.myorg.MyTrait
x25containduplicatetraitreferences.conc line 5:0 class AbstrClass contains duplicate trait references: com.myorg.MyTrait

~~~~~
//##26. missing many trait fields
x26missingmanytraitfields.conc line 22:0 Class 'x26missingmanytraitfields.MyClass' is missing implementations of trait fields: count of type int from x26missingmanytraitfields.MyTrait1, count2 of type float from x26missingmanytraitfields.MyTrait1, count3 of type float from x26missingmanytraitfields.MyTrait1, x26missingmanytraitfields.MyTrait2

~~~~~
//##27. no constructors
x27noconstructors.conc line 5:1 Constructors cannot be defined within traits
 
~~~~~
//##28. trait cannot extend same thing as well
x28traitcannotextendsamethingaswell.conc line 8:0 trait Another contains duplicate trait and supertype refernce: MyTrait
 
~~~~~
//##29. override variables in trait and normal classes
x29overridevariablesintraitandnormalclasses.conc line 5:1 Field: a can only be overriden if they exist and are accessible in a superclass or composing trait of class: x29overridevariablesintraitandnormalclasses.Myclass
x29overridevariablesintraitandnormalclasses.conc line 6:1 Field: b can only be overriden if they exist and are accessible in a superclass or composing trait of class: x29overridevariablesintraitandnormalclasses.Myclass
x29overridevariablesintraitandnormalclasses.conc line 9:2 The override keyword can only be used with fields at class level
x29overridevariablesintraitandnormalclasses.conc line 10:2 The override keyword can only be used with fields at class level
x29overridevariablesintraitandnormalclasses.conc line 11:2 The override keyword can only be used with fields at class level
x29overridevariablesintraitandnormalclasses.conc line 26:1 Field: a can only be overriden if they exist and are accessible in a superclass or composing trait of class: x29overridevariablesintraitandnormalclasses.Myclass2
x29overridevariablesintraitandnormalclasses.conc line 27:1 Field: b can only be overriden if they exist and are accessible in a superclass or composing trait of class: x29overridevariablesintraitandnormalclasses.Myclass2
x29overridevariablesintraitandnormalclasses.conc line 29:1 Overriden field: d must be of type: int. It is of type: java.lang.String
x29overridevariablesintraitandnormalclasses.conc line 41:1 Overriden trait field: count2 must be explicitly overriden with the 'override' keyword in trait: x29overridevariablesintraitandnormalclasses.Child as it overrides a trait field
 
~~~~~
//##30. bugfix tuple cannot contain void
x30bugfixtuplecannotcontainvoid.conc line 23:24 tuples cannot contain void
 
~~~~~
//##31. override only if accessable in superclass
x31overrideonlyifaccessableinsuperclass.conc line 11:1 Field: anotherFieldx can only be overriden if they exist and are accessible in a superclass or composing trait of class: x31overrideonlyifaccessableinsuperclass.Child
 
~~~~~
//##32. gen type args expected
x32gentypeargsexpected.conc line 20:0 trait x32gentypeargsexpected.MyTrait does not expect generic type arguments
 
~~~~~
//##33. no actors on traits
x33noactorsontraits.conc line 8:0 Unable to resolve type corresponding to name: MyActor

~~~~~
//##34. no init block int trait
x34noinitblockinttrait.conc line 7:1 init blocks may not be defined within traits
 
~~~~~
//##35. no trait nesting
x35notraitnesting.conc line 5:1 traits cannot be nested
x35notraitnesting.conc line 8:2 traits cannot nest classes, actors or traits

~~~~~
//##36. trait must have non abstract parent method impl
x36traitmusthavenonabstractparentmethodimpl.conc line 13:0 Unable to resolve non abstract super reference for method: operate of trait: x36traitmusthavenonabstractparentmethodimpl.DivTwo

~~~~~
//##37. typed actor of trait must match on generic types
x37typedactoroftraitmustmatchongenerictypes.conc line 9:0 The generic trait x37typedactoroftraitmustmatchongenerictypes.ID cannot be qualified more than once with different arguments in the hierarchy for class: x37typedactoroftraitmustmatchongenerictypes.MyActor. Incompatible definitions: x37typedactoroftraitmustmatchongenerictypes.ID<java.lang.Integer>, x37typedactoroftraitmustmatchongenerictypes.ID<java.lang.String>
 
~~~~~
//##38. missing zero arg constructor err on anon class
x38missingzeroargconstructorerronanonclass.conc line 10:6 Implicit super constructor for superclass x38missingzeroargconstructorerronanonclass.Myclass of Anonymous Class is undefined. Must explicitly invoke another constructor or invoke superconstructor
 
~~~~~
//##39. overriden trait vars must have matching types
x39overridentraitvarsmusthavematchingtypes.conc line 9:1 Overriden field: count must be of type: long. It is of type: int
x39overridentraitvarsmusthavematchingtypes.conc line 13:1 Overriden trait field: count must be explicitly overriden with the 'override' keyword in trait: x39overridentraitvarsmusthavematchingtypes.MyTrait3 as it overrides a trait field
x39overridentraitvarsmusthavematchingtypes.conc line 17:1 Overriden field: count must be of type: long. It is of type: int

~~~~~
//##40. anon class can only have zero arg constructors defined
x40anonclasscanonlyhavezeroargconstructorsdefined.conc line 11:1 Anonymous classes may only define zero arg constructors (if any)

~~~~~
//##41. override thing ret type must equal or be subtype
x41overridethingrettypemustequalorbesubtype.conc line 12:1 The return type of method 'def echo1() int' in class x41overridethingrettypemustequalorbesubtype.MyClass cannot be matched with method: 'def echo1() x41overridethingrettypemustequalorbesubtype.MyTrait' in superclass: x41overridethingrettypemustequalorbesubtype.SupClass or traits: x41overridethingrettypemustequalorbesubtype.MyTrait
x41overridethingrettypemustequalorbesubtype.conc line 13:1 The return type of method 'def echo2() int' in class x41overridethingrettypemustequalorbesubtype.MyClass cannot be matched with method: 'def echo2() x41overridethingrettypemustequalorbesubtype.SupClass' in superclass: x41overridethingrettypemustequalorbesubtype.SupClass or traits: x41overridethingrettypemustequalorbesubtype.MyTrait
 