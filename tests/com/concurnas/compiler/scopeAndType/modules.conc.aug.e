//##Test: 0 - 1. declare simple
x1modulemissingthingsinimport.conc line 3:0 Imported name: com.myorg.code2.result cannot be resolved to a type
x1modulemissingthingsinimport.conc line 4:0 Imported name: com.myorg.code2.MYClass2 cannot be resolved to a type
x1modulemissingthingsinimport.conc line 3:0 Imported name: com.myorg.code2.result cannot be resolved to a type
x1modulemissingthingsinimport.conc line 4:0 Imported name: com.myorg.code2.MYClass2 cannot be resolved to a type
 
~~~~~
//##2. module respects public private etc
 
~~~~~
//##3. tpyedef pppp
x3tpyedefpppp.conc line 9:4 typedef has been marked private
x3tpyedefpppp.conc line 10:4 typedef has been marked protected
x3tpyedefpppp.conc line 11:4 typedef has been marked package
x3tpyedefpppp.conc line 9:4 typedef has been marked private
x3tpyedefpppp.conc line 10:4 typedef has been marked protected
x3tpyedefpppp.conc line 11:4 typedef has been marked package

~~~~~
//##4. class pppp
x4classpppp.conc line 9:9 thingPriv has been marked private
x4classpppp.conc line 11:11 thingProt has been marked protected
x4classpppp.conc line 12:12 thingPack has been marked package
x4classpppp.conc line 9:9 thingPriv has been marked private
x4classpppp.conc line 11:11 thingProt has been marked protected
x4classpppp.conc line 12:12 thingPack has been marked package

~~~~~
//##5. enum pppp
x5enumpppp.conc line 9:9 thingPriv has been marked private
x5enumpppp.conc line 11:11 thingProt has been marked protected
x5enumpppp.conc line 12:12 thingPack has been marked package
x5enumpppp.conc line 9:9 thingPriv has been marked private
x5enumpppp.conc line 11:11 thingProt has been marked protected
x5enumpppp.conc line 12:12 thingPack has been marked package

~~~~~
//##6. annotation pppp
x6annotationpppp.conc line 8:0 thingPriv has been marked private
x6annotationpppp.conc line 14:0 thingProt has been marked protected
x6annotationpppp.conc line 17:0 thingPack has been marked package
x6annotationpppp.conc line 8:0 thingPriv has been marked private
x6annotationpppp.conc line 14:0 thingProt has been marked protected
x6annotationpppp.conc line 17:0 thingPack has been marked package
 
~~~~~
//##7. respect for protected
x7respectforprotected.conc line 6:0 Annot has been marked protected
x7respectforprotected.conc line 10:3 The variable avar is not visible
x7respectforprotected.conc line 11:3 typedef has been marked protected
x7respectforprotected.conc line 12:8 The method myfunc is not visible
x7respectforprotected.conc line 13:13 MYEnum has been marked protected
x7respectforprotected.conc line 14:14 MyClass has been marked protected
x7respectforprotected.conc line 15:15 MyClass has been marked protected
x7respectforprotected.conc line 16:16 MyClass has been marked protected
x7respectforprotected.conc line 17:7 The method alambda is not visible
x7respectforprotected.conc line 6:0 Annot has been marked protected
x7respectforprotected.conc line 10:3 The variable avar is not visible
x7respectforprotected.conc line 11:3 typedef has been marked protected
x7respectforprotected.conc line 12:8 The method myfunc is not visible
x7respectforprotected.conc line 13:13 MYEnum has been marked protected
x7respectforprotected.conc line 14:14 MyClass has been marked protected
x7respectforprotected.conc line 15:15 MyClass has been marked protected
x7respectforprotected.conc line 16:16 MyClass has been marked protected
x7respectforprotected.conc line 17:7 The method alambda is not visible

~~~~~
//##8. respect for package
x8respectforpackage.conc line 6:0 Annot has been marked package
x8respectforpackage.conc line 10:3 The variable avar is not visible
x8respectforpackage.conc line 11:3 typedef has been marked package
x8respectforpackage.conc line 12:8 The method myfunc is not visible
x8respectforpackage.conc line 13:13 MYEnum has been marked package
x8respectforpackage.conc line 14:14 MyClass has been marked package
x8respectforpackage.conc line 15:15 MyClass has been marked package
x8respectforpackage.conc line 16:16 MyClass has been marked package
x8respectforpackage.conc line 17:7 The method alambda is not visible
x8respectforpackage.conc line 6:0 Annot has been marked package
x8respectforpackage.conc line 10:3 The variable avar is not visible
x8respectforpackage.conc line 11:3 typedef has been marked package
x8respectforpackage.conc line 12:8 The method myfunc is not visible
x8respectforpackage.conc line 13:13 MYEnum has been marked package
x8respectforpackage.conc line 14:14 MyClass has been marked package
x8respectforpackage.conc line 15:15 MyClass has been marked package
x8respectforpackage.conc line 16:16 MyClass has been marked package
x8respectforpackage.conc line 17:7 The method alambda is not visible
