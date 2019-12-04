//##1. params get annotations
[[$ConcurnasMetaVersion$[], clone[], copy[ ;  ; ], copy[ ;  ; ], defaultFieldInit$[ ;  ; ], delete[], equals[@ParamName(hasDefaultValue = false, isVararg = false, name = a) ; ], fromBinary[ ; ], getGlobalDependancies$[], hashCode[], init[ ;  ;  ;  ; ], init[@ParamName(hasDefaultValue = false, isVararg = false, name = a) ;  ; ], metaBinary[], thefunction[@ParamName(hasDefaultValue = false, isVararg = false, name = a) ; @ParamName(hasDefaultValue = false, isVararg = false, name = b) ; ], toBinary[ ; ], toBoolean[], toString[]], [x1paramsgetannotations$Myclass[@ParamName(hasDefaultValue = false, isVararg = false, name = a)]]]

~~~~~
//##2. simple one
34

~~~~~
//##3. w refs inside
34

~~~~~
//##4. locall defined non bc extracted
[-11, -11]

~~~~~
//##5. locall looked cool
[2, 2, 2]

~~~~~
//##6. method generics
[hi, hi]

~~~~~
//##7. class generics
[hi, hi]

~~~~~
//##8. constructors
22-12

~~~~~
//##9. constructors no new keyword
22-12

~~~~~
//##10. constructors with generic params
[12-22, 12-22, hi, hi]

~~~~~
//##11. this constructor ref
1-99

~~~~~
//##12. super constructor ref
1-99

~~~~~
//##13. overloaded ok
ok 2

~~~~~
//##14. overloaded ok specific path
ok str

~~~~~
//##15. can be used on precompiled constructor
[its23, its23, its23, its23]

~~~~~
//##16. operator overloaded invoke function
25

~~~~~
//##17. nested functions
25

~~~~~
//##18. funcrefs
[34, 34]

~~~~~
//##19. funcrefs local function
[34, 34, 34]

~~~~~
//##20. funcrefs constructors
[34, 34, 34]

~~~~~
//##21. funcrefs constructors with new
[34, 34, 34]

~~~~~
//##22. funcrefs constructors with generics
[12-22, 12-22, hi, hi]

~~~~~
//##22. funcrefs local gens
[hi, hi, hi]

~~~~~
//##23. funcrefs class gens
[hi, hi]

~~~~~
//##24. calling iface function
[[1 2, 2 1], [1 2, 2 1]]

~~~~~
//##25. actor funcref with named param constructor
[34, 34, 34]

~~~~~
//##26. actor creation with named params normal way
[34, 34, 34]

~~~~~
//##27. normal actor methods
[34, 34, 34, 34]

~~~~~
//##28. actor method norm and via funcref
[34, 34, 34, 34]

~~~~~
//##29. thing was reffed great
34

~~~~~
//##30. default arguments
[20, 20, 20, 20, 20, 20, 77, 10, 10, 71]

~~~~~
//##31. default arguments more complex
[[100, 12, 200, 8], [100, 8, 200, 12], [100, 8, 200, 12], [100, 8, 200, 12], [1, 2, 3, 4], [100, 1, 200, 2], [100, 1, 9, 2], [1, 2, 200, 3]]

~~~~~
//##32. func invoke on local funcdef w default params
[12, 8, 200, 6]

~~~~~
//##33. constrcutor invoke on local funcdef w default params
[[100, 12, 200, 8], [100, 8, 200, 12], [100, 8, 200, 12], [100, 8, 200, 12], [1, 2, 3, 4], [100, 1, 200, 2], [100, 1, 9, 2], [1, 2, 200, 3]]

~~~~~
//##34. constrcutor invoke via this
[100, 99, 200, 88]

~~~~~
//##35. constrcutor invoke via super
[100, 99, 200, 88]

~~~~~
//##36. constrcutor nested class with defaults
[100, 99, 200, 88]

~~~~~
//##37. default constructors support default args
[[100, 12, 200, 13], [12, 13, 99, 88], [100, 99, 200, 88]]

~~~~~
//##38. calls on default actors
[[100, 12, 200, 13], [100, 99, 200, 88], 101, 101]

~~~~~
//##39. calls on non default actors
[[100, 12, 200, 13], [100, 45, 200, 466], 101, 101, [1, 10, 100]]

~~~~~
//##40. funcref method
[[100, 12, 200, 8], [100, 8, 200, 12], [100, 8, 200, 12], [100, 8, 200, 12], [1, 2, 3, 4], [100, 1, 200, 2], [100, 1, 9, 2], [1, 2, 200, 3]]

~~~~~
//##41. funcref local method
[[100, 12, 200, 8], [100, 8, 200, 12], [100, 8, 200, 12], [100, 8, 200, 12], [1, 2, 3, 4], [100, 1, 200, 2], [100, 1, 9, 2], [1, 2, 200, 3]]

~~~~~
//##42. funcref constructors
[[100, 12, 200, 8], [100, 8, 200, 12], [100, 8, 200, 12], [100, 8, 200, 12], [1, 2, 3, 4], [100, 1, 200, 2], [100, 1, 9, 2], [1, 2, 200, 3]]

~~~~~
//##43. funcref constructors classdef
[[100, 12, 200, 8], [100, 8, 200, 12], [100, 8, 200, 12], [100, 8, 200, 12], [1, 2, 3, 4], [100, 1, 200, 2], [100, 1, 9, 2], [1, 2, 200, 3]]

~~~~~
//##44. funcref constructors actors
[[100, 12, 200, 8], [100, 8, 200, 12], [100, 8, 200, 12], [100, 8, 200, 12], [1, 2, 3, 4], [100, 1, 200, 2], [100, 1, 9, 2], [1, 2, 200, 3]]

~~~~~
//##45. funcref methods actors
[[100, 12, 200, 8], [100, 8, 200, 12], [100, 8, 200, 12], [100, 8, 200, 12], [1, 2, 3, 4], [100, 1, 200, 2], [100, 1, 9, 2], [1, 2, 200, 3]]

~~~~~
//##46. funcref methods typed actor
[[100, 12, 200, 8], [100, 8, 200, 12], [100, 8, 200, 12], [100, 8, 200, 12], [1, 2, 3, 4], [100, 1, 200, 2], [100, 1, 9, 2], [1, 2, 200, 3]]

~~~~~
//##47. bugfix
k

~~~~~
//##48. local generics ok
lovely: 5

~~~~~
//##49. class level generics
lovely: 5

~~~~~
//##50. null on rhs
lovely: Myclass: 2

~~~~~
//##51. null on rhs pt2
lovely: 5

~~~~~
//##52. spot check this is ok
lovely: Myclass: ok

~~~~~
//##53. null on rhs classdef
lovely: Myclass: ok

~~~~~
//##54. type inf to object when rhs varnull
lovely: hinull

~~~~~
//##55. basic vararg as int[]
[123, 124, 125]

~~~~~
//##56. basic vararg passed in
[101, 102, 103]

~~~~~
//##57. basic vararg passed in with extra arg after
[111, 112, 113]

~~~~~
//##58. empty array if vararg missing
[]:23

~~~~~
//##59. single arg to vararg
[123]

~~~~~
//##60. last arg is always considered vararg if its an array
[101, 102, 103]

~~~~~
//##61. can call as direct array
[[111, 112, 113], [101, 102, 103]]

~~~~~
//##62. could be ambigious but isnt
[[111, 112, 113], [101, 102, 103]]

~~~~~
//##63. could be ambigious but isnt
[1, 2, 3, 4, 5]

~~~~~
//##64. bugfix vararg ar object etc
[[1:100, 2:100, 3:100], [1:100, 2:100, 3:100]]

~~~~~
//##65. existing java code
[ok: hithere, 1 2]

~~~~~
//##66. with default value of different type
[2100.1, 3100.1, 4100.1]

~~~~~
//##67. with default value of different type no intermediate item
[1xxx, 2xxx, 3xxx, 4xxx, 5xxx]

~~~~~
//##68. with default value of different type no intermediate item at end
[1xxx, 2xxx, 3xxx, 4xxx, 5xxx]

~~~~~
//##69. direct call with default present
[101, 102, 103, 104, 105]

~~~~~
//##70. direct call is ok
[1, 2, 3, 4, 5]

~~~~~
//##71. vararg immediatly follows default param
[101, 102, 103, 104, 105]

~~~~~
//##72. defaultp normal then vararg
[101str, 102str, 103str, 104str, 105str]

~~~~~
//##73. can use named parameter on the vararg
[[k1, k2, k3, k4, k5] [k1, k2, k3, k4, k5]]

~~~~~
//##74. used to blow up now ok
3.0

~~~~~
//##75. bug but ok now
[1, 2, 3, 4, 5]

~~~~~
//##76. constructors
[[k1, k2, k3, k4, k5], [k1, k2, k3, k4, k5]]

~~~~~
//##77. this constructor
[ok1nicecat, ok2nicecat, ok3nicecat]

~~~~~
//##78. super constructor
[ok1nicecat, ok2nicecat, ok3nicecat]

~~~~~
//##79. agent constructor
[[haha1, haha2, haha3, haha4, haha5, haha6], [lovely1, lovely2, lovely3]]

~~~~~
//##80. agent constructor more complex
[ok1hi, ok2hi, ok3hi, ok4hi, ok5hi, ok6hi]

~~~~~
//##81. agent method calls
[lovely[1 2 3 4]-1, lovely[1 2 3 4]-2, lovely[1 2 3 4]-3, lovely[1 2 3 4]-4]

~~~~~
//##82. agent method calls plus
[[ok1hi, ok2hi, ok3hi, ok4hi, ok5hi, ok6hi], [ok1hi, ok2hi, ok3hi, ok4hi, ok5hi, ok6hi], [lovely4, lovely5, lovely6, lovely99], [lovely4, lovely5, lovely6, lovely99]]

~~~~~
//##83. funcref of vararg
[[101, 102, 103], [123]]

~~~~~
//##84. funcref of vararg constructor
[[101, 102, 103], [123]]

~~~~~
//##85. funcref of actors
[[ok1hi, ok2hi, ok3hi, ok4hi, ok5hi, ok6hi], [ok1hi, ok2hi, ok3hi, ok4hi, ok5hi, ok6hi], [lovely4, lovely5, lovely6, lovely99], [lovely4, lovely5, lovely6, lovely99]]

~~~~~
//##86. bugfix on varargs
[[77 87], [M N]]

~~~~~
//##86.b bugfix on varargs
[[M W], [M N]]

~~~~~
//##87. bugfix 2 on varargs
[77, [77 87 87]]

~~~~~
//##87. bugfix 3 on varargs
12

~~~~~
//##88. no arg funcref
[athena, lovely: hi]

~~~~~
//##89. no arg funcref cons
[gots: 12, gots: 12]

~~~~~
//##90. no arg funcref cons actor
i have a 12

~~~~~
//##91. no arg funcref cons actor method
145

~~~~~
//##92. generic binding on funcref
[hi, there]

~~~~~
//##93. generic binding on constructors
got: hi

~~~~~
//##94. more generic of functypes bugfix
"ok"

~~~~~
//##95. basic ConstructorRef
[got: hi, got: 12, got: hi, got: 12]

~~~~~
//##96. ConstructorRef generics
[got: hi, got: 12, got: hi, got: 12]

~~~~~
//##97. ConstructorRef inner class generics bound
[got: [hi, 99], got: [12, 99], got: [hi, 99], got: [12, 99]]

~~~~~
//##97.b ConstructorRef inner class generics bound - non generical
[got: [hi, 99], got: [12, 99], got: [hi, 99], got: [12, 99]]

~~~~~
//##98. ConstructorRef inner class generics unbounded
[got: [hi, 99], got: [12, 99], got: [hi, 99], got: [12, 99]]

~~~~~
//##99. ConstructorRefbugfix 
gots: 12

~~~~~
//##100. ConstructorRef on actor
[got: hi, got: 12, got: hi, got: 12]

~~~~~
//##101. in out generics in
all ok

~~~~~
//##102. in out generics out
all ok

~~~~~
//##103. double check int to Object Number works ok
all ok[12, 12]

~~~~~
//##104. in out imported typed correclty
all ok[88, 99]

~~~~~
//##105. in out out correctly bound bugfix
hi

~~~~~
//##106. as above bug fields
hi

~~~~~
//##107. bugfix generic in out type subtype
lovely

~~~~~
//##108. problem as above resolved for explicit anctor refs
got: ok

~~~~~
//##109. constructor refs for actors
[hi, hi][[got: ok1, x109constructorrefsforactors$NIC$0, x109constructorrefsforactors$MyClass, true, true, true], [got: ok2, x109constructorrefsforactors$NIC$0, x109constructorrefsforactors$MyClass, true, true, true]]

~~~~~
//##110. class ref and constructor refs for actors
[hi, hi][[got: ok1, x110classrefandconstructorrefsforactors$NIC$0, x110classrefandconstructorrefsforactors$MyClass, true, true, true], [got: 66, x110classrefandconstructorrefsforactors$NIC$0, x110classrefandconstructorrefsforactors$MyClass, true, true, true]]

~~~~~
//##111. class ref and constructor refs for untyped actors
[hi, hi][[got: ok1, x111classrefandconstructorrefsforuntypedactors$MyClass, true, true, true], [got: 66, x111classrefandconstructorrefsforuntypedactors$MyClass, true, true, true]]

~~~~~
//##112. class ref and constructor refs for untyped actors bugfix
[hi, hi][[got: ok1, x112classrefandconstructorrefsforuntypedactorsbugfix$MyClass, true, true, true], [got: 66, x112classrefandconstructorrefsforuntypedactorsbugfix$MyClass, true, true, true]]

~~~~~
//##113. not sure what this proves but works ok
[got: hi, got: 12, got: hi, got: 12]

~~~~~
//##114. improved funcrefs 2 auto create from refname
ok [hi, hi]

~~~~~
//##115.b improved funcrefs 2 auto create from refname
ok [hi, hi]

~~~~~
//##116. new class constructor
got: 33

~~~~~
//##117. introduce ClassRefIface to deal with casting etc
[got: 12, got: cool]

~~~~~
//##118. ClassRefIface on FuncRefInvoke
got: 12

~~~~~
//##119. ClassRefIface on normal java classes
13

~~~~~
//##120. pass lambda to java and back again
it: 13

~~~~~
//##121. ClassRef pass through normal code and from java
[12, 13]

~~~~~
//##122. ref to ClassRef 
[got: 12, got: 15, got: j88]

~~~~~
//##123. more ref to ClassRef
got: 12

~~~~~
//##124. more ref to ClassRef cool
[got: 12, got: 12]

~~~~~
//##125. total use of classrefs
got: 12, got: 12

~~~~~
//##126. actors of classrefs
[got: 33, got: 33]

~~~~~
//##127. field and module level classref
hi2 hisad

~~~~~
//##128. field and module level classref 2
hi2

~~~~~
//##129. field and module level classref 3
hi2

~~~~~
//##130. field level block
991

~~~~~
//##131. field level block 2
hi2

~~~~~
//##132. check block def works ok
MiniClass: hi

~~~~~
//##133. bugfix on vararg ambiguity
[cool, cool[1 2 3]]

~~~~~
//##134. underscore is optional on funcrefs
[ok, 12, 23]

~~~~~
//##135. underscore is optional works on inner nested classes now
[null, 12, 23]

~~~~~
//##136. this didnt use to work
[ok, 12, 99]

~~~~~
//##137. map function invokation to funcref if approperiate
[ok, 12, 33]

~~~~~
//##138. map func invokation to funcref automatically if primative or lambda passed in
[ok, 12, 44]

~~~~~
//##139. no underscore needed on name map either
[ok, 12, 45]

~~~~~
//##140. no underscore on name map for primatives
[ok, 69, 12]

~~~~~
//##141. no underscore on name map and auto convert to funcref
[ok, 12, 45]

~~~~~
//##142. no underscore on name map and auto convert to funcref for primatives
[ok, 69, 12]

~~~~~
//##143. bugfix cast from Object to int etc
[12, 12]

~~~~~
//##144. Object to int array Integer array
res1: as expected
res2: [12 12 34 5]
res3: [12 12 34 5]
res4: as expected


~~~~~
//##145. bugfix used to end up in inf loop
100

~~~~~
//##146. convert from new to constructor ref when types provided in place of varnames
MyGenClass [easy enough, 77]

~~~~~
//##146.b convert from new to constructor ref when types provided in place of varnames
[MyGenClass [easy enough, 77], MyGenClass [easy enough, 77]]

~~~~~
//##147. funcref on varargs
[1, 2, [hi there], 3, 4]

~~~~~
//##148. var args for funcrefinvoke
gu[1, 2, [8 9]]

~~~~~
//##149. named params for funcrefinvoke
gu[1, 2, [8 9]]

~~~~~
//##150. double check this works ok
null

~~~~~
//##151. its fine its how we do it
[1],[1],hi

~~~~~
//##152. nice it works
[1], [1], hi

~~~~~
//##153. varargs which are ref types were failing
ok

~~~~~
//##154. varargs which are ref types were failing pt 2
[ok 2 ok 2 ok 2 ok 2]

~~~~~
//##155. vararg then default
[[hi], 12]
[[hi there], 12]
[[hi], 7]
[[hi there], 7]

~~~~~
//##156. vararg after some defaults
[hi, [1 2], null, [hi there]]

~~~~~
//##157. import precompiled
exe: a 12 null 0

~~~~~
//##158. mixed default args and varargs
[a, [1 2], [3 4], []]
[a, [1 2], null, []]

~~~~~
//##159. another mixed variant
[hi, [1 2], [999 888], [hi there]]

~~~~~
//##160. mixed default args and varargs more
[a, [1 2], [3 4], [ok]]

~~~~~
//##161. mixed default args and varargs more complete 
[a, [1 2], [3 4], []]
[a, [1 2], [3 4], [ok then]]
[a, [1 2], null, [ok then]]
[a, [1 2], [3 4], [ok]]

~~~~~
//##162. bugfix on default params missing from extension function
null 0

~~~~~
//##163. more calls on non typed actors with default constructors
42000

~~~~~
//##164. default param can also be vararg
ok

~~~~~
//##165. bridge methods for public methods of non public classes whic are used as superclasses
[12, 12, 12]

~~~~~
//##166. bridge methods for as above maps for compiled classes correctly now
[true]

~~~~~
//##167. in param
[1]

