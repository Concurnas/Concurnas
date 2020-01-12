//##1 try catch stack status final throws
[80, 80, 80, 80, 2, 0, 0]

~~~~~
//##2 try catch stack status final throws
[80, 80, 80, 80, 2, 0, 0]

~~~~~
//##3 try catch stack status final throws applies to catch also
[80, 80, 80, 80, 2, 4, 2]

~~~~~
//##4 vararg generics
[1, 2, 3, 4, 5]

~~~~~
//##5 vararg generics type inference infer type
[1, 2, 3, 4, 5]

~~~~~
//##6 vararg simple needs boxing
[1, 2, 3, 4, 5]

~~~~~
//##7 vararg simple no boxing
[1, 2, 3, 4, 5]

~~~~~
//##8. when quailifying generic types used to not include source generic array level count
[12 13 ; 12]

~~~~~
//##9. qualify generic varag with 1:1 non vararg mathc 
[[hi, there]]

~~~~~
//##10. qualify generic vararg with boxed param
[[12, 12]]

~~~~~
//##11. qualify generic vararg with unboxed param
[1, 2, 3]

~~~~~
//##12. qualify generic vararg with boxed param
[1 1 1]

~~~~~
//##13. qualify generic vararg with boxed param box up
[1 2 3]

~~~~~
//##14. qualify generic implicit vararg with boxed param box up
[1 2 3]

~~~~~
//##15. qualify generic implicit vararg with free vars boxing
[1, 2, 3, 4, 5]

~~~~~
//##16. qualify generic implicit vararg with free vars already boxed 
[1, 2, 3, 4, 5]

~~~~~
//##17. qualify generic implicit vararg with free vars already boxed generic declared
[1, 2, 3, 4, 5]

~~~~~
//##18. generic vararg more
[1, 2, 3, 4, 5]

~~~~~
//##19. generic vararg more 2
[1, 2, 3, 4, 5]

~~~~~
//##20. generic vararg more 3
[1, 2, 3, 4, 5]

~~~~~
//##21. cast elements to char not int
[M W]

~~~~~
//##22. double check this works vararg is null
ok[]

~~~~~
//##23. static methods were being missing from the origonal class - now we redirect
true

~~~~~
//##24. Fiber used to be uncreatable and now on reflection we find fiberized version if approperiate
[[public static com.concurnas.bootstrap.runtime.cps.Fiber com.concurnas.lang.precompiled.ClassWithStaticClinit.getTheCurrentFiber(com.concurnas.bootstrap.runtime.cps.Fiber)], public static com.concurnas.bootstrap.runtime.cps.Fiber com.concurnas.lang.precompiled.ClassWithStaticClinit.getTheCurrentFiber(com.concurnas.bootstrap.runtime.cps.Fiber)]

~~~~~
//##25. reflect to remove fiber parameter from method
[[], 0]

~~~~~
//##26. reflect to splice in fiber to invokation
true

~~~~~
//##27. tricky problem with reflection and enums and fibers
[0, [OP NO], NO, 4, 2]

~~~~~
//##28. method does not exist exception thrown correctly
java.lang.NoSuchMethodException: com.concurnas.lang.precompiled.ClassWithStaticClinit.doesntexist(com.concurnas.bootstrap.runtime.cps.Fiber)

~~~~~
//##29. bug concerning local arrays
[[100: 200:], [100: 200:]]

~~~~~
//##30. can use js style setter when no getter defined
{-->1, one->101}

~~~~~
//##31.a broken before now ok
10000

~~~~~
//##31.b broken before now ok
10000

~~~~~
//##31.c broken before now ok
10000

~~~~~
//##32. ensure function invokation locks ref
[true, true]

~~~~~
//##33. used to incorrectly call generic version
[[1, 2], no set]

~~~~~
//##34. object is supertype of primatives
69

~~~~~
//##35. didnt used to work
MyClass [1, 2]

~~~~~
//##36. funcref arg being arrayref on non array
[102, 102]

~~~~~
//##37. labels here were a problme, now they are ok
[[false, false, true], [false, false, true]]

~~~~~
//##38. lists vs arrays
[1 2 3]
[1 2 3 ; 1 2 3]
[1, 2, 3, 4]
[[1, 2, 3], [1, 2, 3]]

~~~~~
//##39 broken before now ok array version
10000

~~~~~
//##40 tidy up on array decl
[[23.34 666.78 ; null], [23.34 666.78 ; 1.0], [23.34 666.78 []]]

~~~~~
//##41 tidy up on array decl - like above
[[23.34 666.78 ; null], [23.34 666.78 ; 1.0], [23.34 666.78 []], [null null], [23.34 666.78 ; null], [23.34 666.78 ; null null]]

~~~~~
//##42. problem with generic upper bounds for list iterator variables
360

~~~~~
//##43. problem as above
ok

~~~~~
//##44. list with no items
[[1], [1], [hi]]

~~~~~
//##45. default value for array creation bug advanced
[MyClass 6 MyClass 6 MyClass 6]

~~~~~
//##46. default value for array creation bug simple
[9 9]

~~~~~
//##47. default value for array creation bug v advanved
[[] [] [] []]

~~~~~
//##48. default value for array creation bug v advanved ciool
[66 ; 66 ; 66 ; 66]

~~~~~
//##49. bug on type resolution
5

~~~~~
//##50. looks cool
31009758835927976162028940951119564627122582128250440845346049355501038354121075789226812053363403343793703122280586671433190877580906564794266034581242189685674876870738536090955680902945695395622718356013689267234042424324792348677691532817483887147987087028779637930707327393375070418511352716458390860785921907558405475073723187785533766910717857917938382624831205590014948489575031784415866797361639926060397712472269851201166675573590406323779904519114135345668519837679633367007211937514341461373308373492541296816341048318084212875106627416230071912498234411201666005954625685164303067341539292838516687618642901629570460708734640717702908299343585187499207323782005143716078669658165965172110966319514731577896204515090132109591556274093826497421954498801603930559499935217966045503758590118738249423070770123765288822269947416047564163705338138866653174661890349405179971970926522932594213780312535546045155446943985309118198788623050398114421933260728912238701305780522958247948587751824286226381422011309168207063864967472074186787134040297536300249526765071817122531372948865815549899745222760388418193755678624725620008298201985677457493881089902661078912381318746148467037111985231507235952681971847738857729150825726853114390200189507201624574478820255819920174570970586423454692976584788464416225700102436370948177588246481912688792310513400746687461429801419943802013353874145354163211765076496866488242145023618676316631302782481307199294039458568872464125513326303399370870459677594788048497217053051348259811760492342082784973876251884426241906504880731090632529393860177497816127909369452333108126052075079528020196374265823333241986831233376353540740401471595138548713954222195302573226858706830116991985168263454459025838173294388563701829721568746469512778412656290181655506206960071919831376939029302920835936446741570379574083727447840452108078616238135749594720749142138641046116897845177110858303826298608075473401341811644182574184365246859201896633828096524397615858657624914191761843500014508729161120921463157896003148289466392860799373665859941277513048379434803286277194246994146507540109743094145260156461514752

~~~~~
//##51. globalization of static final fields
[[-30, -30], [-30, -30]]

~~~~~
//##52. imported local generic
ok

~~~~~
//##53. clean up stack on naught static call
fine[77, yaya, 102]

~~~~~
//##54. incorrect inference of return type bugfix
true

~~~~~
//##55. add bridge method for virtual call with differing return type signature
x55addbridgemethodforvirtualcallwithdifferingreturntypesignature$Inst

~~~~~
//##56. add bridge method for copier
x56addbridgemethodforcopier$Inst

~~~~~
//##57. ensure invoke special called for super invocation as part of asyncref  
sup

~~~~~
//##58. nasty fiber bug concerning System calls inside try catch blocks
true

~~~~~
//##59. list comprehensions on arrays 1
[11, 12, 13, 14, 15, 16]

~~~~~
//##60. list comprehensions on arrays 2
[[11], [12], [13], [14], [15], [16]]

~~~~~
//##61. parfor on array
12

~~~~~
//##62. magic hack concerning static fibers and native code opencl specificially 
hi[4, true]

~~~~~
//##63. a short array
[1 2 3]

~~~~~
//##64. lets fix chars
[c, X, astring, astring]

~~~~~
//##65. weird
12

~~~~~
//##66. subclass of Ref has self copier
ok

~~~~~
//##67. problem where we were cleaning up the stack and pop off value
2

~~~~~
//##68. problem where we were cleaning up the stack and pop off value - where value wanted
2

~~~~~
//##69. implicit ref creation needs zero arg constructor
[ok, java.lang.Object cannot be cast to x69implicitrefcreationneedszeroargconstructor$NoZeroARgOne[java.lang.Object], as no zero arg constructor defined for type: x69implicitrefcreationneedszeroargconstructor.NoZeroARgOne]

~~~~~
//##70. double check this works
int

~~~~~
//##71. fwd ref failure
ok

~~~~~
//##72. ignore generic when testing to see if we can do an equals on a type
ok true

~~~~~
//##73. indirect abstract classes bug
hi

~~~~~
//##74. define a stub function
ok

~~~~~
//##75. private class mapping when there is a choice
[[ClType1, ClType2] [ClTypeB1, ClTypeB2]]

~~~~~
//##76. private class can be used like this
ClType1

~~~~~
//##77. local class wrong package name format
okhi

~~~~~
//##78. bug when defining default map lambda
ok 2

~~~~~
//##79. wrong assign type being used on array default values
[[1.0 3.0 ; 4.0 5.0], [0.0 0.0 ; 0.0 0.0], 79]

~~~~~
//##80. fix generics on generic types upper bounded when multi chooice
ok[[int float class java.lang.String], [int boolean]]

~~~~~
//##81. fix generics above use case
ok[false, true]

~~~~~
//##82. bug with type convertion for primative type comparison operators
-33346560 [true, false, false, true]

~~~~~
//##83. bug concerning setting of wanted on stack or not on ast override for funcinvoke which is really a constructor
ok

~~~~~
//##84. was a bug with accessability
okhi

~~~~~
//##85. incorrectly filling in missing value for thing of array type
ok

~~~~~
//##86. null to ref with no zero arg constructor cannot do
fine java.lang.Object cannot be cast to x86nulltorefwithnozeroargconstructorcannotdo$GPURef[java.lang.Object], as no zero arg constructor defined for type: x86nulltorefwithnozeroargconstructorcannotdo.GPURef

~~~~~
//##87. del npe
ok

~~~~~
//##88. bridge method genneration
12

~~~~~
//##89. assertion string incorrectly processed
10 should be less than 3

~~~~~
//##90. this ref on prefix op
cool[44]

~~~~~
//##91. error on null type tagging
[[null null], [null null], [null null]]

~~~~~
//##92. problem with label alocation on if else and try catch
engine is null

~~~~~
//##93. neg in things
ok: [true, true, true]

~~~~~
//##94. neg for ints in exprlists
9

~~~~~
//##95. add getter setter if parent class has field accessable
23

~~~~~
//##96. imply generics of argument returning local generic when used in callsite
false

~~~~~
//##97. imply generics of argument returning local generic when used in callsite v2
12

~~~~~
//##98. bug fixed relating to generics
[[[100, 200]], [[100, 200]]]

~~~~~
//##99. bug since vectorized code was not being copied
[2, 3, 4, 5, 6, 7]

~~~~~
//##100. bug with defo assignment use within block
its false

~~~~~
//##101. idx fix
ok45

~~~~~
//##102. idx fix on while
ok55

~~~~~
//##103. idx fix on while with ret
ok[55, [9999, 9999, 9999, 9999, 9999, 9999, 9999, 9999, 9999, 9999]]

~~~~~
//##104. while ret
ok[0, [9999, 9999, 9999, 9999, 9999, 9999, 9999, 9999, 9999, 9999, 9999]]

~~~~~
//##105. while ret 2
ok[0, [9999, 9999, 9999, 9999, 9999, 9999, 9999, 9999, 9999, 9999, 9999]]

~~~~~
//##106. if used to nop
ok45

~~~~~
//##107. if used to nop 2
ok0

~~~~~
//##108. another one
ok[45, [1, 2, 3, 4, 5, 6, 7, 8, 9]]

~~~~~
//##109. while idx variable problem when defined at module level
[[1000, 1001], [1001, 1003], [1002, 1005], [1003, 1007], [1004, 1009], [1005, 1011], [1006, 1013], [1007, 1015], [1008, 1017], [1009, 1019]]

~~~~~
//##110. for idx variable problem when defined at module level
[1000 1001][1001 1002][1002 1004][1003 1005][1004 1007][1005 1008][1006 1010][1007 1011][1008 1013][1009 1014][1010 1020]

~~~~~
//##111. ast redirect of expr list needs should be preserved on stack call
676

~~~~~
//##112. ensure idx for for loop is captured correctly
[10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20]

~~~~~
//##113. ensure idx for while loop is captured correctly
[10, 11, 12, 13, 14, 15, 16, 17, 18, 19]

~~~~~
//##114. parfor list compre works
([ok:, ok:, ok:, ok:, ok:, ok:, ok:, ok:, ok:, ok:, ok:], [ok:, ok:, ok:, ok:, ok:, ok:, ok:, ok:, ok:, ok:, ok:])

~~~~~
//##115. used to incorrectly store state
888

~~~~~
//##116. and so this is fine now 
[11:, 12:, 13:, 14:]

~~~~~
//##117. try catch label allocator needs to ignore things inside async blocks
[fine:, fine:, fine:, fine:]

~~~~~
//##118. try catch label allocator needs to ignore things inside async blocks
[fine:, fine:, fine:, fine:]

~~~~~
//##119. bug with nested sync not setting should be kept on stack correctly
[[x, x, x, x]:, [x, x, x, x]:, [x, x, x, x]:, [x, x, x, x]:]

~~~~~
//##120. set nested ret type
[[11:, 12:, 13:, 14:]:, [11:, 12:, 13:, 14:]:, [11:, 12:, 13:, 14:]:, [11:, 12:, 13:, 14:]:]

~~~~~
//##121. nested for list comprehension with parforsync
[[(0, 0):, (0, 1):, (0, 2):, (0, 3):], [(1, 0):, (1, 1):, (1, 2):, (1, 3):]]

~~~~~
//##122. bug with imm loader
(56, 56)

~~~~~
//##123. type not being directed to asttype
true

~~~~~
//##124. function defined at module leve within a block
12

~~~~~
//##125. function defined at module level within block using external deps
112

~~~~~
//##126. function defined at module level within block using external deps - if stmts 
112

~~~~~
//##127. looks cool
true

~~~~~
//##128. problem with fiberization of globalized clinit
true

~~~~~
//##129. module level actors
ok: cool

~~~~~
//##130. more module level actors
10

~~~~~
//##131. try catch labels when wrapping loops etc
ok: [(0, 0) (0, 0) (0, 666) (0, 666)]

~~~~~
//##132. branch implicit and explicit returns
20

~~~~~
//##133. constructor ref on new op overload bug on vars
MyGenClass [77, lovely]

~~~~~
//##134. bridge method not needed for notify below
ok[hi]

~~~~~
//##135. field created by default creator when referenced in supercon
[88 222 88]

~~~~~
//##136. lets estimate pi
complete: 3

~~~~~
//##137. fix locked ref cast
ughfalse

~~~~~
//##138. fix locked ref cast 2
complete: true

~~~~~
//##139. can extend Function0
1

~~~~~
//##140. five ways to access a thing exending function0
5

~~~~~
//##141. generic type upper bound on rhs can be set to type of upper bound on lhs
hi

~~~~~
//##142. has line number for lambda
has line number for lambda true

~~~~~
//##143. local class may have references to external vars
ok100

~~~~~
//##144. nested inner var use default var
ok20

~~~~~
//##145. nested inner var use default var 2
ok20

~~~~~
//##146. bug with funcrefs with no args having captured vars
hi message

~~~~~
//##147. actor constructor with default args
ok

~~~~~
//##148. actor method with default args
ok10

~~~~~
//##149. local class as default param
42000

~~~~~
//##150. override trait method differing ret type
ok its ok

~~~~~
//##151. override super method differing ret type
ok its ok

~~~~~
//##152. override super method differing ret type - both
ok [its ok its ok]

~~~~~
//##153. catch blocks throwing bug
uh oh[12, 12, 12]

~~~~~
//##154. bc genneration for await was incorrect 
ok

~~~~~
//##155. bc genneration for classreferences was incorrect
ok

~~~~~
//##156. ret lambda ok
ok

~~~~~
//##157. do things with pre compiled variable the long way
ok23

~~~~~
//##158. functype needs to treat int params as Integer etc
[1 1]

~~~~~
//##159. from primative to other boxed type
[2.0 2.0 2.0 2.0]

~~~~~
//##160. from boxed to other boxed type
[2.0 2.0 2.0 2.0]

~~~~~
//##161. check protected
(1, 1)

~~~~~
//##162. injectable actor double create constructor err
ok

~~~~~
//##163. inf loop bug
ok

~~~~~
//##164. reset local var counter at end of isolated block with direct parent being module level
SUCCESS

~~~~~
//##165. can throw thing held in ref
java.lang.RuntimeException: hi

~~~~~
//##166. custom ref param convertion for primative array types 
ok

~~~~~
//##167. bugfix try catch label nested in if
ok

~~~~~
//##168. catch may return something
oknull

~~~~~
//##169. checkcast on array return type bugfix
ok[1 2]

~~~~~
//##170. bug on idx casting type
[0 0 0 0 0]

~~~~~
//##171. bug on position of created for loop variable with same name
ok

~~~~~
//##172. catch can return
ok[9, 12]

~~~~~
//##173. lca type of tuples defined as par below
ok

~~~~~
//##174. lambda gen signature corrected for arrays
[0, 0, [0 0], 0]

~~~~~
//##175. return from catch needs convertion to object type
oktrue

~~~~~
//##176. trans block missing isolated=true on main work block
java.lang.Exception: uh oh

~~~~~
//##177. allow dot over multiple lines
[[11, 12, 13, 14, 15] [11, 12, 13, 14, 15] [11, 12, 13, 14, 15] [11, 12, 13, 14, 15]]

~~~~~
//##178. transactions is closed
true

~~~~~
//##179. sub string expressions - nested
[hey: two:8 nice 6 ok, hey: one:9 nice 6 ok]

~~~~~
//##180. sub string expressions - none
[hey: , hey: ]

~~~~~
//##181. sub string expressions - starts with thing
[4, 4]

~~~~~
//##182. sub string expressions - escape char
[{2+2}, {2+2}]

~~~~~
//##183. sub string expressions - failed
[f {2+2 sdf, f {2+2 sdf]

~~~~~
//##184. neste neste
resultsomething

~~~~~
//##185. bugfix on op overload redirect
[0, 100, true]

~~~~~
//##186. bugfix on op overload redirect pt 2
[0, 100, true]

~~~~~
//##187. loop idx ddefault to int
ok: [(0, 0), (1, 4), (2, 8), (3, 12), (4, 16), (5, 20)]

~~~~~
//##188. loop idx take thing on righ
ok: [[(0, 0), (4, 1), (8, 2), (12, 3), (16, 4), (20, 5)], [(0, 0), (4, 1), (8, 2), (12, 3), (16, 4), (20, 5)], [(0, 0), (4, 1), (8, 2), (12, 3), (16, 4), (20, 5)], [(0, 0), (4, 1), (8, 2), (12, 3), (16, 4), (20, 5)]]

~~~~~
//##189. loop idx take thing on righ - while
ok: [[(1, 0), (2, 1), (3, 2), (4, 3)], [(1, 0), (2, 1), (3, 2), (4, 3)], [(1, 0), (2, 1), (3, 2), (4, 3)], [(1, 0), (2, 1), (3, 2), (4, 3)], [(1, 0), (2, 1), (3, 2), (4, 3)]]

~~~~~
//##190. loop idx take thing on righ - loop
ok: [[(1, 0), (2, 1), (3, 2), (4, 3), (5, 4)], [(1, 0), (2, 1), (3, 2), (4, 3), (5, 4)], [(1, 0), (2, 1), (3, 2), (4, 3), (5, 4)], [(1, 0), (2, 1), (3, 2), (4, 3), (5, 4)], [(1, 0), (2, 1), (3, 2), (4, 3), (5, 4)]]

~~~~~
//##191. example from book
ok: [[(2, 0), (3, 1), (4, 2), (5, 3), (2, 4), (1, 5), (3, 6), (4, 7), (2, 8), (2, 9), (1, 10)], [(2, 0), (3, 1), (4, 2), (5, 3), (2, 4), (1, 5), (3, 6), (4, 7), (2, 8), (2, 9), (1, 10)], [(2, 100), (3, 101), (4, 102), (5, 103), (2, 104), (1, 105), (3, 106), (4, 107), (2, 108), (2, 109), (1, 110)], [(2, 100), (3, 101), (4, 102), (5, 103), (2, 104), (1, 105), (3, 106), (4, 107), (2, 108), (2, 109), (1, 110)], [(2, 10), (3, 11), (4, 12), (5, 13), (2, 14), (1, 15), (3, 16), (4, 17), (2, 18), (2, 19), (1, 20)]]

~~~~~
//##192. example from book 2
ok: [[(1, 0), (2, 1), (3, 2), (4, 3), (5, 4), (6, 5), (7, 6), (8, 7), (9, 8), (10, 9)], [(1, 10), (2, 11), (3, 12), (4, 13), (5, 14), (6, 15), (7, 16), (8, 17), (9, 18), (10, 19), (11, 20)]]

~~~~~
//##193. null on funcref used to blow up
err

~~~~~
//##194. tests from manual
[Complex(5.0, 7.0), Complex(12.0, 3.0), Complex(5.0, 7.0), Complex(12.0, 3.0)]

~~~~~
//##195. used to blow up
ok1

~~~~~
//##196. bug on choice of items if one or more void
ok

~~~~~
//##197. similar bug to above unecisary cast
oh

~~~~~
//##198. nice little filter and lazy example
4068

~~~~~
//##199. bug concerning local classes in extension functions
ok

~~~~~
//##200. ext funcs lazy eval with anon class
4068

~~~~~
//##201. ext funcs lazy eval with anon class and local class
4068

~~~~~
//##202. bug now ok
ok

~~~~~
//##203. jumps bug
[small, small, said hi, ]

~~~~~
//##204. invokedynamic was not passing the fiber
ok

~~~~~
//##205. invokedynamic bugfix
some text

~~~~~
//##206. invokedynamic bugfix - alt method
some text

~~~~~
//##207. partially qualified lambda expr
[1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15]

~~~~~
//##208. nested inner func mapping with default params
fine

~~~~~
//##209. bug on tuplederef within for loop
[[4, 5, 6], [1, 2, 3]]

~~~~~
//##210. import star class
ok[]

~~~~~
//##211. import star static assets
ok[12 112]

~~~~~
//##212. onchange was not utilizing existing return value ref
ok3

~~~~~
//##213. top level every and await
ok102

~~~~~
//##214. top level del on func
bytecodeSandbox.conc line 2:4 thing resolves to something other than a local variable