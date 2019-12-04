//##1. simple - On OWN
x1simplelonerOnOWN.conc line 6:1 Expression cannot appear on its own line
  
~~~~~
//##2. simple loner - if - On OWN
x2simplelonerifOnOWN.conc line 7:10 Expression cannot appear on its own line

~~~~~
//##3. simple loner - if fail 2 - On OWN
x3simpleloneriffail2OnOWN.conc line 7:10 Expression cannot appear on its own line
  
~~~~~
//##4. map stuff - On OWN
x4mapstuffOnOWN.conc line 4:2 Expression cannot appear on its own line
x4mapstuffOnOWN.conc line 5:1 Expression cannot appear on its own line
 
~~~~~
//##5. ltr - ifs  
x5ltrifs.conc line 5:15 Expression cannot appear on its own line
 
~~~~~
//##5.b ltr - ifs
x5bltrifs.conc line 5:5 if statement must return something
 
~~~~~
//##6. ltr - blocks
x6ltrblocks.conc line 5:5 block must return something
 
~~~~~
//##7. ltr - while
x7ltrwhile.conc line 6:20 while loop must return something

~~~~~
//##8. ltr - nested 
x8ltrnested.conc line 6:20 while loop must return something
x8ltrnested.conc line 6:31 Expression cannot appear on its own line
 
~~~~~
//##9. ltr - for new 
x9ltrfornew.conc line 6:24 for loop must return something
 
~~~~~
//##10. ltr - for old 
x10ltrforold.conc line 6:24 for loop must return something
 
~~~~~
//##11. ltr - try catch
x11ltrtrycatch.conc line 5:5 try catch must return something
x11ltrtrycatch.conc line 5:14 catch block of try catch must return something
 
~~~~~
//##11.b ltr - try catch fin
x11bltrtrycatchfin.conc line 4:0 This method must return a result of type java.lang.String
 
~~~~~
//##12. fin overrides all
x12finoverridesall.conc line 8:2 Expression cannot appear on its own line
 
~~~~~
//##13. a more complex case
x13amorecomplexcase.conc line 20:4 Expression cannot appear on its own line
x13amorecomplexcase.conc line 27:3 Expression cannot appear on its own line
 
~~~~~
//##14. break and continue cannot return void
x14breakandcontinuecannotreturnvoid.conc line 7:12 Expression cannot appear on its own line
x14breakandcontinuecannotreturnvoid.conc line 9:35 for loop must return something
x14breakandcontinuecannotreturnvoid.conc line 10:5 unexpected type: void for if
x14breakandcontinuecannotreturnvoid.conc line 11:3 break statement must return something
x14breakandcontinuecannotreturnvoid.conc line 13:7 Unable to find method with matching name: ffxx
x14breakandcontinuecannotreturnvoid.conc line 14:3 Expression cannot appear on its own line
x14breakandcontinuecannotreturnvoid.conc line 17:3 continue statement must return something
 
~~~~~
//##15. last thing in function is ret

~~~~~
//##16. last thing in lambda is ret

~~~~~
//##17. cannot ret this
x17cannotretthis.conc line 5:1 int is not a subtype of java.lang.String
 
~~~~~
//##18. invalid
x18invalid.conc line 14:2 Fianlly blocks cannot contain return statements

~~~~~
//##19.a final invalid 
 
~~~~~
//##19.b final invalid - wrong type too 
 
~~~~~
//##19.c final - not permitted
x19cfinalnotpermitted.conc line 7:1 final block must return something since try catch blocks do not
 
~~~~~
//##19.d final - very ambigious so throwing lots of errors here seems reasonable
x19dfinalveryambigioussothrowinglotsoferrorshereseemsreasonable.conc line 13:28 Expression cannot appear on its own line
 
~~~~~
//##20. sync block should return something
x20syncblockshouldreturnsomething.conc line 5:7 sync block must return something
x20syncblockshouldreturnsomething.conc line 8:1 Return statement in method must return type of java.lang.String
 
~~~~~
//##21. perfect the try catch err
x21perfectthetrycatcherr.conc line 5:3 try catch must return something
x21perfectthetrycatcherr.conc line 6:2 catch block of try catch must return something
x21perfectthetrycatcherr.conc line 7:44 catch block of try catch must return something
x21perfectthetrycatcherr.conc line 12:3 try catch must return something
x21perfectthetrycatcherr.conc line 12:6 try block of try catch must return something
x21perfectthetrycatcherr.conc line 19:3 try catch must return something
 
~~~~~
//##22. no fin ret as expected plus deadcode
x22nofinretasexpectedplusdeadcode.conc line 10:2 Fianlly blocks cannot contain return statements
x22nofinretasexpectedplusdeadcode.conc line 12:1 Unreachable code after return statement
 
~~~~~
//##23. missing fin set plus slready
x23missingfinsetplusslready.conc line 6:1 These variables have been declared val but have not been assigned a value in this constructor: z
x23missingfinsetplusslready.conc line 18:1 These variables have been declared val and can only be set once in constructor call hierarchy: z
 
~~~~~
//##24. ret cases
x24retcases.conc line 6:1 Return statement in method must return type of int
x24retcases.conc line 13:1 Unreachable code after return statement
x24retcases.conc line 13:8 a1 cannot be resolved to a variable
x24retcases.conc line 18:1 Return statement in method must return type of int
x24retcases.conc line 18:1 Unreachable code after return statement
 
~~~~~
//##25. some more on own errs
x25somemoreonownerrs.conc line 6:2 Expression cannot appear on its own line
x25somemoreonownerrs.conc line 7:1 Expression cannot appear on its own line
x25somemoreonownerrs.conc line 8:4 Return type expected
x25somemoreonownerrs.conc line 8:4 block must return something
 
~~~~~
//##26. err as refmaker is not in the loop
x26errasrefmakerisnotintheloop.conc line 5:32 Type mismatch: cannot convert from void to java.lang.Integer:
x26errasrefmakerisnotintheloop.conc line 5:33 continue cannot be used outside of a loop or inside a parallel for loop
 
~~~~~
//##27. cont break not outside
x27contbreaknotoutside.conc line 10:3 continue cannot be used outside of a loop or inside a parallel for loop
x27contbreaknotoutside.conc line 15:3 break cannot be used outside of a loop, onchange, every, await or async block or inside a parallel for loop

~~~~~
//##28. implicit returns must be universal
x28implicitreturnsmustbeuniversal.conc line 11:2 Expression cannot appear on its own line
x28implicitreturnsmustbeuniversal.conc line 15:0 Expression cannot appear on its own line
x28implicitreturnsmustbeuniversal.conc line 19:2 Expression cannot appear on its own line
x28implicitreturnsmustbeuniversal.conc line 25:4 Invalid type void
 
~~~~~
//##29. one ret and one not implicit not permited
x29oneretandonenotimplicitnotpermited.conc line 8:1 if statement must return something
x29oneretandonenotimplicitnotpermited.conc line 10:6 Return type expected
 
~~~~~
//##30. this is ok

~~~~~
//##31. try block must return something
x31tryblockmustreturnsomething.conc line 19:1 try catch must return something
x31tryblockmustreturnsomething.conc line 21:2 catch block of try catch must return something
x31tryblockmustreturnsomething.conc line 52:4 Invalid type void
 
~~~~~
//##32. more implicit return cases
x32moreimplicitreturncases.conc line 21:2 Expression cannot appear on its own line
 
~~~~~
//##33. try restriction
x33tryrestriction.conc line 9:2 Expression cannot appear on its own line
 
~~~~~
//##34. try with inner with no implicit return
x34trywithinnerwithnoimplicitreturn.conc line 37:1 try catch must return something
x34trywithinnerwithnoimplicitreturn.conc line 38:17 while loop must return something
 
~~~~~
//##35. no implicit return in finally block
x35noimplicitreturninfinallyblock.conc line 8:2 Expression cannot appear on its own line