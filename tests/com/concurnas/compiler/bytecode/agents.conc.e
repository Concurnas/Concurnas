//##1 copy on actors
[true]

~~~~~
//##2 priority queue
nice 2

~~~~~
//##3 basic default actor implementation
[12, 20, false]

~~~~~
//##4 some more smart actor stuff
[12, err as expected on second start java.lang.RuntimeException: Actor is already running, err as expected java.lang.Exception: Actor is not running, err as expected on second stop java.lang.RuntimeException: Actor is not running, 20, 28]

~~~~~
//##5 misc bugfix
[[12, 8], 20]

~~~~~
//##6 creating these constructors was being a problem previously
[hi, hi]

~~~~~
//##7 actor calls return refs
[20, 22]

~~~~~
//##8. doing an async block on an agent
999

~~~~~
//##9. broke before now ok
18

~~~~~
//##10. was busted, now ok
18

~~~~~
//##11. was busted, now ok2
18

~~~~~
//##12. was busted, now ok3
18

~~~~~
//##13. unlock on asyncrefref
[18, 18, 18, 18]

~~~~~
//##14. two actors - same stuff
[18, 18]

~~~~~
//##15. two actors - same class two refs
[18, 18]

~~~~~
//##16. actor at module level - still an issue
end 18

~~~~~
//##17. just works no issues
12

~~~~~
//##18. used to wait forever
[20, 22]

~~~~~
//##19. ensure special methods called correctly
[toStringCalled, onover hit]

~~~~~
//##20. call default actor manager methods - easy ones
[toString on myclass hit, 234]

~~~~~
//##21. ensure correct toStirng variant called
[true, 234]

~~~~~
//##22actor is the gen type for all
234

~~~~~
//##23. as above but more elaborate
234

~~~~~
//##24. probably a problem before
234

~~~~~
//##25. probably a problem before 2
true

~~~~~
//##26. stop when closed already both variants
[java.lang.RuntimeException: java.lang.Exception: Actor is not running, err]

~~~~~
//##27. bring that all together
[12, ok]

~~~~~
//##28. equals
true

~~~~~
//##29. some more stuff
[24, 36:, true, false]

~~~~~
//##30 field getters and setters
[12, 9]

~~~~~
//##31 field getters and setters pre postfix ops
[12, 14, 14, 16]

~~~~~
//##32 sort out toString
[hi [true, true], hi [true, true], hi [true, true]]

~~~~~
//##33 sort out toString x2 alt syntax
[hi [true, true], hi [true, true], hi [true, true]]

~~~~~
//##34 implicit to string
ok -> hiok -> hi

~~~~~
//##35 correct routing with 4 different creation methods
[ok -> hi, ok -> hi, ok -> hi, ok -> hi]:[ok -> hi, ok -> hi, ok -> hi, ok -> hi][true, true, true, true][true, true, true, true]

~~~~~
//##36. ensure iface gennerates for actor correct methods
[[true], hi there]

~~~~~
//##37. rather complex but operational
[hi [true, true], hi [true, true], hi [true, true], hi [true, true]]

~~~~~
//##38. simple little case
hi there

~~~~~
//##39. hashcode and equals
[true, true, true, true][12, true]

~~~~~
//##39. generic actors simple
ok[stuff, ooh stuff]

~~~~~
//##40. generic actors more than one
ok[stuff, ooh stuff, 100]

~~~~~
//##41. instanceof simple
[true, true]

~~~~~
//##42. instanceof simple - with generic params
[true, true]

~~~~~
//##43. infer type from generic parameter passed to actor
[[class java.lang.String class java.lang.Double class java.lang.Integer], true]

~~~~~
//##44. cast types must match exactly
[12, ok, ok]

~~~~~
//##45. this actor can be created ok
[class java.lang.String class java.lang.Integer]

~~~~~
//##46. is actor
[true]

~~~~~
//##47. as actor
[true, true]

~~~~~
//##48. references to actor constructors
[12, 12]

~~~~~
//##49. references to actor constructors correct types
[true, true]

~~~~~
//##50. bugfix, used to fail on sueprclass generics
12

~~~~~
//##x51 actors on nested inner classes
hi

~~~~~
//##x52 actors on nested inner classes with correct instantiation of parent
[from outer12.0, from outer12.0]

~~~~~
//##53 actors on nested inner classes with correct instantiation of parent - 3x
[from outer14.012.0, from outer14.012.0]

~~~~~
//##54 references to actor functions
[12, 12]

~~~~~
//##55 references to actor functions non generic
[12, 12]

~~~~~
//##56. more ref generic stuff
[hi, hi][true, true][false, false]

~~~~~
//##57 double check on above
[false, false]

~~~~~
//##58. instanceof and cast on actors 
[true, true, hey, hey]

~~~~~
//##59. default methods for custom actors
[hi 12, [true, true], [100, true], [class x59defaultmethodsforcustomactors$MyClass, class x59defaultmethodsforcustomactors$MyActor], [true, true], [true, false]]

~~~~~
//##60. actor has its own state
[hi 12, [true, true], [100, true], [class x60actorhasitsownstate$MyClass, class x60actorhasitsownstate$MyActor], [true, true], [true, false]]

~~~~~
//##61. actor and actee calls
[actee stuff call: 12, actor call: 9, 12, actor call: 9, actor call: 9, actee stuff call: 12, actee mything, actee mything, got Caugt]

~~~~~
//##62. implicit call refs
[actee mything, got Caugt]

~~~~~
//##63. actee constructors
[12, 20]

~~~~~
//##64. actor constructors
12

~~~~~
//##65. custom actors, calling themselves
[12, 20]

~~~~~
//##66. some more custom consturctors
[12, 99]

~~~~~
//##67. check field access to actor
[9, 13, 13, 99, 12]

~~~~~
//##68. more field setting etc between actor and actee
[[[99, 100], [99, 100], [99, 100]], |, [[555, 100], [555, 100], [555, 100]]]

~~~~~
//##69. ok no arg constructor
[null, 12]

~~~~~
//##70. we get it, these work now
[24, 12]

~~~~~
//##71. haha cool
[200, 12]

~~~~~
//##72. dont have to be arguments to the actor
[200]

~~~~~
//##73. typed abstract actor of something yet to be defined
happy

~~~~~
//##74. simple inheritance of abstract typed actor
[100, hi there]

~~~~~
//##75. chain of actors
[100, hi there works ok]

~~~~~
//##76. abstract actor default constructor simple
[100, hi there]

~~~~~
//##77. abstract actor default constructor stuff to super
[100, hi there [3, 12]]

~~~~~
//##78. abstract actor default constructor stuff to super chain
[100, hi there hi there 1313]

~~~~~
//##79. abstract actor overrides stuff cool
[100, hi there, [call on a lambda: true]]

~~~~~
//##80. actors can be created like this too...
[100]

~~~~~
//##81. actors can inherit from other actors
[100, 100]

~~~~~
//##82. lovely actors can inheirt and pass args up chain
[100, 13]

~~~~~
//##83. generic actors part 1 simple
[99, true, true, true, true, 99]

~~~~~
//##84. wow bugfix on generic supertype class definition
com.concurnas.lang.TypedActor<x84wowbugfixongenericsupertypeclassdefinition$MyClass<Axx>>

~~~~~
//##85. thing is not an actor of an actor
true

~~~~~
//##86. use of abstract actor of xxx syntax for is and as
[true, hi]

~~~~~
//##87. generic typed actors
[99, true, true, true, true, 99, true, true, hi mum]

~~~~~
//##89. generic typed actors 2
[99, true, true, true, true, 99, true, true, hi mum, hi, there]

~~~~~
//##90. generic typed actors 3
[99, true, true, true, true, 99, true, true, hi mum, hi, there]

~~~~~
//##91. generic typed actors 4
[99, true, true, true, true, 99, true, true, hi mum, hi, there]

~~~~~
//##92. generic typed actors 5 cool example i thought up first
[12, 12]

~~~~~
//##100. typed actors on nested types
[hi there, hi there 70]

~~~~~
//##101. typed actors on nested types inc generics
[hi there, hi there 70]

~~~~~
//##102. custom actor of normal stuff
hi

~~~~~
//##103. make sure simple actors works again
ok: 12

~~~~~
//##104. untyped actors
[hi, hi 29, 29]

~~~~~
//##105. untyped actors as simple
[true, true, true, hi]

~~~~~
//##106. typed actor custom stuff
[88, 9, 99]

~~~~~
//##107. untyped actors implicit super called as approperiate
[88, 9, 99]

~~~~~
//##108. auto create defualt constructor
[88, 9, 99]

~~~~~
//##109. manual create of default constructor
[88, 9, 99]

~~~~~
//##110. manual create of default constructor typed
[88, 9, 99]

~~~~~
//##111. manual create of default constructor typed 2
[88, 9, 99]

~~~~~
//##112. untyped actor constructors correct
[hi12, hi12, hi0]

~~~~~
//##113. correct instance types for untyped actors
[true, true, true, hi12, 12]

~~~~~
//##114. can correctly infer types now
[true, true, true, hi12, 12]

~~~~~
//##115. custom hashcode eq and toString etc
stuff[false, false, false, 1, 1, asd, asd]

~~~~~
//##116. untyped actors can inherit
hi hijj

~~~~~
//##117. actor copy in and out
true

~~~~~
//##118. copy an actor returns self
true

~~~~~
//##119. untyped ref
ok [true, true, true, 100]

~~~~~
//##120. instanceof actor bugfix
true

~~~~~
//##121. another instanceof actor bugfix
[true, true, true]

~~~~~
//##122. double check this works
got: 33

~~~~~
//##123. agents on private classes
hi

~~~~~
//##124. bugfix you can create instances of nested static classes now
khi

~~~~~
//##125. actor of static nested class lovely
khi

~~~~~
//##126. can create actors of private classes cool
k[hi, hi]

~~~~~
//##127. seems ok
true

~~~~~
//##128. actor of generic type unbound
k: [hi, hi2]

~~~~~
//##129. actor of generic type unbound
k: [hi, hi2]

~~~~~
//##130. actor of generic type unbound and bound
k: [[hi, there], [hi, there]]

~~~~~
//##131. pre qualified generic actor args need not be qualificed at genneration time to comply with iface rep
k: [[ok], ok]

~~~~~
//##132. nice now these work ok
k: [[ok, hi, there] [ok, hi, there]]

~~~~~
//##133. enmsure error propgated back to caller
k: java.lang.RuntimeException: java.lang.IndexOutOfBoundsException: Index 0 out of bounds for length 0

~~~~~
//##134. actors implicit interfaces
[10 10]

~~~~~
//##135. actors implicit interfaces with generics
[10 10]

~~~~~
//##136. used to fail oninit on actor due to psar genneration
(1, k)

~~~~~
//##137. this was a bug before
ok100

~~~~~
//##138. actor constructor having default params
okport: 42315

~~~~~
//##139. actor can return refs
12

~~~~~
//##140. ensure actor call within async gets routed to actor 
true

~~~~~
//##141. ensure actor call within async gets routed to actor funcref 
true

~~~~~
//##142. ensure external funcref goes via actor
[true, true]

~~~~~
//##143. func invokation of actor with default param copied into final block 
okok

~~~~~
//##144. actor return ref of array
344

~~~~~
//##145. actor return ref of array
344

~~~~~
//##146. actor return ref of array
344

~~~~~
//##147. actor return ref of array
344

~~~~~
//##148. actor return not locked as ref
344

~~~~~
//##149. actor being called with shared variable argumnet
[true, true]