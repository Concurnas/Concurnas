//##1. normal copy
(12, 14, hi there)
(12, 14, hi there)

~~~~~
//##2. override field
(12, 14, hi there)
(99, 14, hi there)

~~~~~
//##3. override more than one
(12, 14, hi there)
(92, 14, cool)

~~~~~
//##4. normal copy on fast type
[(12, 14, hi there)]
[(12, 14, hi there)]

~~~~~
//##5. copy with explicit include
(12, 14, hi there)
(12, 0, null)

~~~~~
//##6. copy with explicit exclude
(12, 14, hi there, friend)
(12, 0, hi there, friend)

~~~~~
//##7. exclude item
23 | (12, 14, hi there, friend)
23 | null

~~~~~
//##8. nested copier
23 | (12, 14, hi there, friend)
23 | (12, 0, hi there, null)

~~~~~
//##9. super copier
(1, 2, three, four) | (5, 6)
(1, 0, three, four) | (0, 0)

~~~~~
//##10. unchecked copier with super
(1, 2, three, four) | (5, 6)
(1, 0, three, four) | (0, 6)

~~~~~
//##11. unchecked copier without super
(1, 2, three, four) | (5, 6)
(1, 2, three, four) | (0, 6)

~~~~~
//##12. copy none
(12, 14, hi there)
(0, 0, null)

~~~~~
//##13. copy none field
(12, 14, hi there, friend) | (12, 14, hi there, friend)
(0, 0, null, null) | (12, 14, hi there, friend)

~~~~~
//##14. copy none super
(1, 2, three, four) | (5, 6)
(0, 0, null, null) | (0, 0)

~~~~~
//##15. default values
(12, 14, hi there, 9)
(24, 25, default, 9)

~~~~~
//##16. defaults on super
(12, 14, hi there, 9)
(24, 25, default, 99)

~~~~~
//##17. defaults on super via copier unchecked
(12, 14, hi there, 9)
(24, 25, default, 99)

~~~~~
//##18.no defaults
(12, 14, hi there, 9)
(0, 0, null, 9)

~~~~~
//##19. no defaults via unchecked
(12, 14, hi there, 9)
(0, 0, null, 9)