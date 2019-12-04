//##1. simple
ok

~~~~~
//##2. two instances
ok

~~~~~
//##3. zero arg constructor for injectable fields
ok

~~~~~
//##4. three ways to inject a zero arg constructor
ok

~~~~~
//##5. basic field injection
ok[msg prefix: real message]

~~~~~
//##6. method injection
ok[msg prefix: real message]

~~~~~
//##7. constructor injection
ok[msg prefix: real message]

~~~~~
//##8. qualify superfields
ok[msg prefix: real message]

~~~~~
//##9. qualify super methods needing inject
ok[msg prefix: real message]

~~~~~
//##10. ensure no arg construtor added only once
okits fine

~~~~~
//##11. transient dependancy
ok[msg prefix: real message]

~~~~~
//##12. transient dependancy - direct
ok[msg prefix: real message]

~~~~~
//##13. provide can also be a dependancy
ok[[PrefixHolder: real message], PrefixHolder: ]

~~~~~
//##14. provide has expr on rhs
ok[[prefix: real message], prefix: ]

~~~~~
//##15. provide method name
failmsg prefix: 

~~~~~
//##16. compiled classes tagged with Inject can be injected
cool: part1 part2

~~~~~
//##17. object provider can depend on other object providers
ok[prefix: real message]

~~~~~
//##18. basic generics
ok[hi]

~~~~~
//##19. fully qualified generics
ok[hi]

~~~~~
//##20. object provider defined before classes it provides
okhi

~~~~~
//##21. provider expression generic
nice: class java.util.ArrayList

~~~~~
//##22. generic dependancies
nice: ok

~~~~~
//##23. private provider
nice: ok

~~~~~
//##24. this just works
nice: ok12

~~~~~
//##25. trait fields
nice: [ok12 ok one hundred ok]

~~~~~
//##26. provide specific dependancies
nice: [(12, ok), mighty]

~~~~~
//##27. provide an actor
nice: (12, ok)

~~~~~
//##28. dependancy can be an actor
nice: (12, ok)

~~~~~
//##29. provide a typed actor
nice: (12, ok)100

~~~~~
//##30. actor as dependancy 1
nice: (12, ok)

~~~~~
//##31. actor as dependancy 2
nice: (12, ok)

~~~~~
//##32. untyped actor
nice: (ok, 12)

~~~~~
//##33. untyped actor with pass though
nice: (ok, 12, ok)

~~~~~
//##34. untyped actor as implicit dependancy
nice: (ok, 12, ok)

~~~~~
//##35. untyped actor as explicit dependancy
nice: (ok, 12)

~~~~~
//##36. default actor with injectable methods
nice: (ok, 12, ok)

~~~~~
//##37. use provide nested elements
nice: (ok, 12, thing)

~~~~~
//##38. provide ref
nice: (ok, 12)

~~~~~
//##39. provide ref 2 levels
nice: (ok, 12)

~~~~~
//##40. custom refs
nice: (ok, 12)

~~~~~
//##41. custom refs - no inject
nice: (ok, 12)

~~~~~
//##42. custom refs - injected
nice: [(ok, 12), nice works]

~~~~~
//##43. ref as pass though dependancy
nice: (ok, 12)

~~~~~
//##44. ref as normal dependancy
nice: (ok, 12)

~~~~~
//##45. zero arg constructor
nice: (ok, 11)

~~~~~
//##46. primative dep ok
nice: (ok, 11)

~~~~~
//##47. primative dep ok provide specific
nice: (ok, 11)

~~~~~
//##48. primative ref dep
nice: (ingf, 23:)

~~~~~
//##49. another generic instance
good

~~~~~
//##50. provider unique single instances
nice: [true, true, true, true, true]

~~~~~
//##51. single provide expression
nice: true

~~~~~
//##52. provider nested dep
nice: [true true]

~~~~~
//##53. zero arg constructor is ok
nice: ok

~~~~~
//##54. lazy val new 1
200

~~~~~
//##55. lazy val new 2
200

~~~~~
//##56. lazy val ae new
200

~~~~~
//##57. easy lazy example
[300 56 400]

~~~~~
//##58. lazy assign existing 1
400

~~~~~
//##59. lazy assign existing 2
400

~~~~~
//##60. lazy assign existing - field
[200 400]

~~~~~
//##61. new lazy arg with no rhs
[null 400]

~~~~~
//##62. lazy ref
200

~~~~~
//##63. lazy ref 2
200

~~~~~
//##64. lazy ref 3 ae
200

~~~~~
//##65. lazy ref 4 ae and assignment
[200 444]

~~~~~
//##66. lazy tuple
(12, 23)

~~~~~
//##67. pass around lazy vals
[88 200 99]

~~~~~
//##68. lazy val in function arg
[88 200 99]

~~~~~
//##69. lazy val in function arg dont call
[88 200 99]

~~~~~
//##70. go for non lazy one
[88 200 99]

~~~~~
//##71. lazy funcref bound
[88 200 99]

~~~~~
//##72. lazy funcref unbound no arg
[88 200 99]

~~~~~
//##73. lazy funcref bound no arg
[88 200 99]

~~~~~
//##74. lazy default value as arg
[88 200 99]

~~~~~
//##75. lazy default value in params
[88 200 99]

~~~~~
//##76. lazy default value short version no arg arg
[88 200 11 11 200 55]

~~~~~
//##77. lazy classdefarg
[88 22 100]

~~~~~
//##78. lazy classdefarg via super
[88 22 100]

~~~~~
//##79. lazy vararg
[88 200 300 99]

~~~~~
//##80. lazy vararg funcred
[88 200 300 99]

~~~~~
//##81. lazy vararg normal plus varafuncref
[88 23 57 99 88 88 23 57 99 88]

~~~~~
//##82. lazy vararg no arg
[88 88 88 88]

~~~~~
//##83. basic lazy lhs def
nice: [88 ok 99]

~~~~~
//##84. basic lasy dependancy provide
nice: [88 ok 99]

~~~~~
//##85. basic lasy dependancy provide method
nice: [88 ok 99]

~~~~~
//##86. basic lasy dependancy provide constructor
nice: [88 ok 99]

~~~~~
//##87. provide lazy
nice: lazy [88 ok 88]

~~~~~
//##88. provide lazy expr
nice: lazy [88 ok 88]

~~~~~
//##89. normal dep to qualify lazy
nice: [88 ok ok ok 91]

~~~~~
//##90. normal dep to qualify lazy local dep
nice: [88 ok ok ok 91]

~~~~~
//##91. provide expr as dependancy qualifcation for lazy variable
nice: [88 ok ok ok 91]

~~~~~
//##92. single provide expr as dependancy qualifcation for lazy variable
nice: [88 ok ok ok 89]

~~~~~
//##93. single dependancy for lazy
nice: [88 ok ok ok 89]

~~~~~
//##94. single lazy dependancy
nice: [88 ok ok ok 89]

~~~~~
//##95. basic provider
nice: [0 ok ok ok 3]

~~~~~
//##96. provider use normal dep
nice: [0 ok ok ok 3]

~~~~~
//##97. provider use normal dep single
nice: [0 ok ok ok 1]

~~~~~
//##98. provider with expr
nice: [0 ok ok ok 3]

~~~~~
//##99. provider with expr single
nice: [0 ok ok ok 1]

~~~~~
//##100. provide transient instance
nice: [0 ok ok ok 3]

~~~~~
//##101. dep transient instance for provide
nice: [0 ok ok ok 3]

~~~~~
//##102. optional single
nice: [1 ok ok ok 1]

~~~~~
//##103. optional 
nice: [3 ok ok ok 3]

~~~~~
//##104. optional not included
nice: [0 false false false 0]

~~~~~
//##105. optional is null
nice: [0 false false false 0]

~~~~~
//##106. named - field by param name
nice: [String2 normal String]

~~~~~
//##107. named - field by param name - precompiled
nice: String2 normal String

~~~~~
//##108. named - field by Named
nice: [String2 String3]

~~~~~
//##109. named - field by Named - precompiled
nice: String2 String3

~~~~~
//##110. named - method param
nice: [String2 normal String]

~~~~~
//##111. named - method param - precompiled
nice: String2 String3

~~~~~
//##112. named - method param - named
nice: [String2 normal String]

~~~~~
//##113. named - method param - named - precompiled
nice: String2 String3

~~~~~
//##114. named - constrcutor param - arg
nice: [String2 normal String]

~~~~~
//##115. named - constrcutor param - arg - precompiled
nice: String2 String3

~~~~~
//##116. named - constrcutor param - named
nice: [first String normal String]

~~~~~
//##117. named - constrcutor param - named precompiled
nice: String2 String3

~~~~~
//##118. named - use special provides
nice: [String2 normal String]

~~~~~
//##119. arrays
nice: [String another 33]

~~~~~
//##120. arrays 2 with prim
nice: [String another 33 3434]

~~~~~
//##121. oh you can have int
ok

~~~~~
//##122. bug with lazy varargs
[88 [] 88 88 [] 88]

~~~~~
//##123. type only dep quali
theMessage

~~~~~
//##124. type only dep quali - field name quali
theMessage othermsg2

~~~~~
//##125. type only dep quali - another one
oksent: proc: a message

~~~~~
//##126. nested deps
oksent: proc: a message

~~~~~
//##127. nested deps in provider
oksent: proc: a message

~~~~~
//##128. partial type qualification bug
[0 sent: proc: a message: ok 1]

~~~~~
//##129. partial type for provider
[0 sent: proc: a message: ok 1]

~~~~~
//##130. partial type for optional
[[1 sent: proc: a message: ok 1], java.util.NoSuchElementException: No value present]

~~~~~
//##131. always use provider to satify dependancies if there is one
callCount:1 madeCount:1, ss:1

~~~~~
//##132. single dependancy no elaboration
[true, true]

~~~~~
//##133. shared dependancy no elaboration
[true, false]

~~~~~
//##134. shared dependancy no elaboration no rhs
[true, false]

~~~~~
//##135. shared provide
[true, false]

~~~~~
//##136. named single dep use
[true, true]

~~~~~
//##137. named shared dep use
[true, false]

~~~~~
//##138. named shared dep use no rhs
[true, false]

~~~~~
//##139. nested shared no rhs
[true, false]

~~~~~
//##140. nested shared rhs
[true, false]

~~~~~
//##141. nested shared rhs ensure override correctly
[true, true]

~~~~~
//##142. ensure nested used dep is marked as being used
ok

~~~~~
//##143. inject private etc
[Myclass(dave) Myclass2(dave)]

~~~~~
//##144. shared qualification
[true true]
