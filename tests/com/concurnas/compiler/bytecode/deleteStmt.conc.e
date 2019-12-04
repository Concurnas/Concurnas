//##1. DeleteOnUnusedReturn simple case
1

~~~~~
//##2. DeleteOnUnusedReturn simple case subclass
1

~~~~~
//##3. DeleteOnUnusedReturn simple case subclass
1

~~~~~
//##4. DeleteOnUnusedReturn predefined 
1

~~~~~
//##5. DeleteOnUnusedReturn predefined subclass 
1

~~~~~
//##6. DeleteOnUnusedReturn no call if null
1

~~~~~
//##7. DeleteOnUnusedReturn on ref when unused
2

~~~~~
//##8. DeleteOnUnusedReturn on ref when unused if expr
1

~~~~~
//##9. DeleteOnUnusedReturn on ref when unused - via arraydef
1

~~~~~
//##10. DeleteOnUnusedReturn on ref when unused - plus minus stmt
2

~~~~~
//##11. DeleteOnUnusedReturn on ref when unused - in funcinvokes
2

~~~~~
//##12. DeleteOnUnusedReturn on ref when unused - calls extracting from ref
2

~~~~~
//##13. custom refs should not share their internal state
[true, [CustRef: contents: [hi]], [CustRef: contents: [hi]]]

~~~~~
//##14. custom refs should not share their internal state - use fast copier if available
[true, [CustRef: contents: [hi]], [CustRef: contents: [hi]]]

~~~~~
//##15. custom refs should not share their internal state - onchange every etc
[true, [hi], [hi]]

~~~~~
//##16. custom refs should not share their internal state - primative types
[100 99]

~~~~~
//##17. bugfix used to be unable to combine custom refs in arrays
[CustRef: contents: [hi], CustRef: contents: [hi]]

~~~~~
//##18. bugfix used to not copy objects shared in trees correctly 
true, true:

~~~~~
//##19. bugfix used to not copy objects shared in trees correctly - ensure that implicit globals are caputred correctly 
[900, 900]

~~~~~
//##20. delete should be called on refs twice here as called via dot operator
2

~~~~~
//##21. delete should be called on refs twice here
2