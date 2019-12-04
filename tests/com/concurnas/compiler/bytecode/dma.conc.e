//##1 ensure private fields are read locally
[9 88]

~~~~~
//##2 ensure fields are read locally
[9, 88]

~~~~~
//##3 ensure unicode works correctly
π=3.141592653589793

~~~~~
//##4 bugfix used to pop on RefNamedType
ok

~~~~~
//##4 bugfix direct to setter or getter 
99

~~~~~
//##5 dma map test 
got: [12, 100] use: [{class x5dmamaptest$MyClass->1}, {}] cont: false

~~~~~
//##6 simple with object array 
got: [hi[null wassup]] after cleanup: {}

~~~~~
//##7 big complex test 
got: [mc true]name: mc
int: 12
long: 999
double: 345.234234
float: 12.32
boolean: true
short: 12
byte: 99
char: c
int ar: [1 66]
int ar null: null
long ar: [10 669]
double ar: [23.34 666.78]
double ar 3d: [23.34 666.78 ; 23.34 666.78]
double ar 3d nully: [23.34 666.78 ; null]
float ar: [10.0 669.0]
boolean ar: [true false true]
short ar: [3 9]
byte ar: [4 9]
char ar: [g h]
object array: [null mc2]
object array wloop: [mc mc2]
object array sneeky: [mc mc2 null]
object array sneeky 3d: [mc mc2 null ; mc mc2 null]
typed obj array: [mc2 mc]
parent int: 100
an enum: ONE
an enum ar: [ONE TWO ONE]
me: mc
looper: mc2
looper short: 69
looper looper: mc
-- Vid use: {class x7bigcomplextest$MyClass->9, class x7bigcomplextest$MyEnum->5}
-- Vid use after remove: {}
keys: []

~~~~~
//##8 null object store 
[expect null: null, -- Vid use: {}, -- Vid use after delete: {}]

~~~~~
//##9 prim array store 
[int[2] array: [1 2 3 ; 1 2 3], -- Vid use: {}, -- Vid use after delete: {}]

~~~~~
//##10 prim float array store
[int[2] array: [1.0 2.0 3.0 ; 1.0 2.0 3.0], -- Vid use: {}, -- Vid use after delete: {}]

~~~~~
//##11 enum store
[int[2] array: ONE, -- Vid use: {class x11enumstore$MyEnum->1}, -- Vid use after delete: {}]

~~~~~
//##12 enum store array
[int[2] array: [ONE TWO], -- Vid use: {class x12enumstorearray$MyEnum->3}, -- Vid use after delete: {}]

~~~~~
//##13 simple object persistance
[off heap disk got item: hi short: 69, off heap disk got item ashort: 69]

~~~~~
//##14 set cap
[off heap disk capacity now: 20 meg]

~~~~~
//##15 set cap ram
[off heap ram capacity now: 20 meg, off heap ram extract: 9]

~~~~~
//##16 bugfix
ok

~~~~~
//##17 bugfix prim cast
ok 1048576

~~~~~
//##18 capacity can be increased
all is good

~~~~~
//##19 kv pair map
map in/out 56.22 vs 56.22
expect null: null
map overwrite in/out 99.9 vs 99.9 old: 56.22
same has different value: 101.2 vs 693.2
removed 99.9 vs 99.9
put back in 99.9 vs 99.9
tricky removed: 693.2 == 693.2
tricky still here: 101.2 == 101.2
tricky removed: 101.2 == 101.2
tricky still here: 693.2 == 693.2
contains key: true
size now: 5
keys: [AaAaBB, hello, nino]
values: [null, hi myclass , hi myclass ]
size after clear zero: 0
count post putAll: 1
post putAll keys: [hi]
count post putAll: 1
post putAll keys: [hi]
defxault get: 453.2 vs 453.2
putIfAbsent ok
remove ok
replace ok
replace ok
fin count 5
end keys: [hi, missingno, replaceme, replifmatch]

~~~~~
//##20 kv pair map OffHeapMapRAM one
[ram one works ok: 1]

~~~~~
//##21 restoration from disk
got from disk: there
got from disk: there
got from disk no cap: there
got from disk no cap: there
got from disk no cap: there
got from disk no cap: there
file path: false
got from disk no cap: there
got from disk no cap: there
got from empty disk: null == null

~~~~~
//##22 misc space related things
free space: 92.03630% [initial]
free space: 70.53528%
free space restart: 70.53528%

~~~~~
//##23 defrags
size: 2
size: 2
post defrag integrity: the long brown fox, the long brown fox3
size: 3
size: 2
post defrag integrity: the long brown fox, the long brown fox3
post defrag integrity: the long brown fox, the long brown fox3
free space pre compact: 1.17715%
comaction ok
comaction needs defrag
defrag on demand ok due to oom ok

~~~~~
//##24 misc bits and bobs
ok

~~~~~
//##25 test meta stored in main
stored vids: {class x25testmetastoredinmain$Myclass->1}
stored vids post restart: {class x25testmetastoredinmain$Myclass->1}
{class x25testmetastoredinmain$Myclass->1}
post defrag integrity: 123

~~~~~
//##26 persistance of boxed primative arrays
stored vids: {}
stored vids post restart: {}
post defrag integrity: 12

~~~~~
//##27. sizeof
[27, 27]

~~~~~
//##28. fun with unicode
[20, 22]

~~~~~
//##29. test java serialization
[0, ok]

~~~~~
//##30. transient fields defualt on Copier
[12, d88, nice, thing, cstr]

~~~~~
//##31. not sure what this proves actually
wtrans: [true - its null, false - [hi there]]

~~~~~
//##32. dma defaulting of transient fields
[12, d88, nice, thing, cstr]

~~~~~
//##33. call serlializer if there is one defined
wtrans: [[false, serial: [hi there], offheap: [hi there]], [false, serial: [hi there], offheap: [hi there]]]

~~~~~
//##34. call externalizer if there is one defined
wtrans: [[true, serial: [hi there], offheap: [hi there]], [false, serial: [hi there], offheap: [hi there]]]

~~~~~
//##35. bugfix on placement of continue stmts
[[2, 4, 6, 8], [2, 4, 6, 8], [2, 4, 6, 8]]

~~~~~
//##36. finally label bugfix
[[hi, hi, hi], false]

~~~~~
//##37. finally label bugfix 2
[withtrans: true - [hi there], withtrans: false - [hi there]]

~~~~~
//##38. enum copy transient fields
[[6, 6, 6, 6], [6, 6, 6, 6]]

~~~~~
//##39. enum only ever one
[ONE: 103, ONE: 103, ONE: 103, true]

~~~~~
//##40. enum dma nocopy
[ONE: [6, 6], ONE: [6, 6], true, true, ONE: [6, 6]]

~~~~~
//##41. test removal of metaBinary
[-- Vid use before delete: {class x41testremovalofmetaBinary$AnotherClass->1, class x41testremovalofmetaBinary$MyClass->1, class x41testremovalofmetaBinary$MyEnum->1}, 
, -- Vid use after delete: {}]

~~~~~
//##42. class and java primordial types arraylist bigdecimal etc
[-- Vid use before delete: {class java.lang.Class->1, class java.math.BigDecimal->1, class java.util.ArrayList->1, class x42classandjavaprimordialtypesarraylistbigdecimaletc$AnotherClass->1, class x42classandjavaprimordialtypesarraylistbigdecimaletc$MyClass->1, class x42classandjavaprimordialtypesarraylistbigdecimaletc$MyEnum->1}, 
, -- Vid use after delete: {}, 
, [hi, there], 12]

~~~~~
//##43. delete local var
true

~~~~~
//##44. delete map
[{there->{hi->23, there->55}}, {there->55}]

~~~~~
//##45. delete list element
[[there], [[hi, there, wassup]]]

~~~~~
//##45. delete from non map list objects
[[there, wassup], true]

~~~~~
//##46. delete from map
{0->9->wow, 1->1->hi, 11->null}

~~~~~
//##47. delete from map in class operator overload
{0->9->wow, 1->1->hi, 11->null}

~~~~~
//##48. dma custom to from binary
withtrans: [[hi there], hi there]

~~~~~
//##49. dma custom to from binary error in decoder
[true]

~~~~~
//##50. nested inner class
[[ouuter, hi, 11], true]

~~~~~
//##51. static bug fix
default name

~~~~~
//##52. funcref on static things
hi

~~~~~
//##53. static bug fix - when local
default name

~~~~~
//##54. dma of static things like modules etc
[true, whatthere, 11, true]

~~~~~
//##55. dma of static things like modules etc even with custom enc dec
[true, whatthere, 11, true]

~~~~~
//##56. check arrays of classes as class is a thing
class [Z

~~~~~
//##57. Uninterruptible cannot be used with actor spawns
Fiber method: 'getScheduler' must be invoked via concurnas, missing fiber information, @Uninterruptible cannot be used

~~~~~
//##58. copy actor is a nop
[true]

~~~~~
//##59. no dma on actor
[a: 12, untyped actor: 12, Actors cannot be converted into binary format: class x59nodmaonactor$NIC$0 is an actor, Actors cannot be converted into binary format: class x59nodmaonactor$MyActor is an actor]

~~~~~
//##60. dma hashset
[[hi], true]

~~~~~
//##61. dma class array correctly
[true]

~~~~~
//##62. dma a ref
[[a: 12, , a: 12, false], [a: 12, , a: 13, false], a: 13]

~~~~~
//##63. no dma of typed actor
[a: 12, true]

~~~~~
//##64. was a bug but ok
[[seems ok yup], [seems ok yup]]

~~~~~
//##65. check field acess report for bool
[true, true, true]

~~~~~
//##66. minor array optimization, no set if thing eq default value
[false true true false true 1 0 45 2 0 hi null null something another thing 0.3 4.5 0.0 0.0]

~~~~~
//##67. simple externalizable example
[hi there]

~~~~~
//##68. simple Serializable example
[hi there]