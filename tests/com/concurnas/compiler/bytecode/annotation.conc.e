//##1. basic annotation auto imported
[interface java.lang.Deprecated, interface com.concurnas.lang.internal.NullStatus]

~~~~~
//##2. basic annotation auto imported use def methods
@Deprecated(forRemoval = false, since = ), @NullStatus(nullable = [])

~~~~~
//##3. basic annotation with para
@SimpleAnnotation, @NullStatus(nullable = [])

~~~~~
//##4. smore more advanted syntax
@SimpleAnnotation, @SimpleAnnotation2, @SimpleAnnotation3, @NullStatus(nullable = [])

~~~~~
//##5. single arg annots
[@AnnotOneArg(name = wongo), @NullStatus(nullable = []), @AnnotTwoArgOneDefault(name = wongozzz, name2 = hi), @NullStatus(nullable = [])]

~~~~~
//##6. constant expressions
[$ConcurnasMetaVersion$:, copy:, copy:, defaultFieldInit$:, equals:@NullStatus(nullable = [true]), fromBinary:@Uninterruptible, getGlobalDependancies$:, hashCode:@NullStatus(nullable = []), m1:@AnnotOneArg(name = 32.3), @NullStatus(nullable = []), m2:@AnnotOneArg(name = no), @NullStatus(nullable = []), m3:@AnnotOneArg(name = one), @NullStatus(nullable = []), m4:@AnnotOneArg(name = false), @NullStatus(nullable = []), m5:@AnnotOneArg(name = true), @NullStatus(nullable = []), m6:@AnnotOneArg(name = true), @NullStatus(nullable = []), m7:@AnnotOneArg(name = 243.0), @NullStatus(nullable = []), m8:@AnnotOneArg(name = true), @NullStatus(nullable = []), m9:@AnnotOneArg(name = 8), @NullStatus(nullable = []), metaBinary:@Uninterruptible, n1:@AnnotOneArg(name = -3), @NullStatus(nullable = []), n2:@AnnotOneArg(name = 3), @NullStatus(nullable = []), n3:@AnnotOneArg(name = 9), @NullStatus(nullable = []), n4:@AnnotOneArg(name = 9.0), @NullStatus(nullable = []), n5:@AnnotOneArg(name = class Ljava/lang/String;), @NullStatus(nullable = []), n6:@AnnotOneArg(name = int), @NullStatus(nullable = []), toBinary:@Uninterruptible]

~~~~~
//##7. constant expressions string subexpressions
[$ConcurnasMetaVersion$:, copy:, copy:, defaultFieldInit$:, equals:@NullStatus(nullable = [true]), fromBinary:@Uninterruptible, getGlobalDependancies$:, hashCode:@NullStatus(nullable = []), m1:@AnnotOneArg(name = 7hi33:int), @NullStatus(nullable = []), metaBinary:@Uninterruptible, toBinary:@Uninterruptible]

~~~~~
//##8. constant folding example
[hithere, himate, 7]

~~~~~
//##9. constant folding optimization in code in genneral
[7, true, false, true, hi, no, true, class [I, double, -100, 10, 42, 64, 1, 9]

~~~~~
//##10. constant folding pow operator
k 64, 64.0

~~~~~
//##11. annotation array argument
[$ConcurnasMetaVersion$:, copy:, copy:, defaultFieldInit$:, equals:@NullStatus(nullable = [true]), fromBinary:@Uninterruptible, getGlobalDependancies$:, hashCode:@NullStatus(nullable = []), m1:@AnotTakesIntArray(theArg = [1 2 3]), @NullStatus(nullable = []), metaBinary:@Uninterruptible, toBinary:@Uninterruptible]

~~~~~
//##12. annotation array argument empty array
[$ConcurnasMetaVersion$:, copy:, copy:, defaultFieldInit$:, equals:@NullStatus(nullable = [true]), fromBinary:@Uninterruptible, getGlobalDependancies$:, hashCode:@NullStatus(nullable = []), m1:@AnotTakesIntArray(theArg = []), @NullStatus(nullable = []), metaBinary:@Uninterruptible, toBinary:@Uninterruptible]

~~~~~
//##13. annotation key value
[$ConcurnasMetaVersion$:, copy:, copy:, defaultFieldInit$:, equals:@NullStatus(nullable = [true]), fromBinary:@Uninterruptible, getGlobalDependancies$:, hashCode:@NullStatus(nullable = []), m1:@AnnotTwoArgOneDefault(name = firstname, name2 = 999), @NullStatus(nullable = []), m2:@AnnotTwoArgOneDefault(name = firstname, name2 = hi), @NullStatus(nullable = []), m3:@AnnotTwoArgAllDefault(name = hi1, name2 = hi2), @NullStatus(nullable = []), metaBinary:@Uninterruptible, toBinary:@Uninterruptible]

~~~~~
//##14. annotation key value more than one
[$ConcurnasMetaVersion$:, copy:, copy:, defaultFieldInit$:, equals:@NullStatus(nullable = [true]), fromBinary:@Uninterruptible, getGlobalDependancies$:, hashCode:@NullStatus(nullable = []), m1:@AnnotTwoArgOneDefault(name = firstname, name2 = 999), @AnnotTwoArgAllDefault(name = hi1, name2 = hi2), @NullStatus(nullable = []), metaBinary:@Uninterruptible, toBinary:@Uninterruptible]

~~~~~
//##15. annotation key value more than one ararays
[$ConcurnasMetaVersion$:, copy:, copy:, defaultFieldInit$:, equals:@NullStatus(nullable = [true]), fromBinary:@Uninterruptible, getGlobalDependancies$:, hashCode:@NullStatus(nullable = []), m1:@AnnotTwoArgOneDefault(name = firstname, name2 = 999), @AnnotTwoArgAllDefault(name = hi1, name2 = hi2), @AnotTakesIntArrayx2(theArg = [1 2 3], theArg2 = [2 4 6]), @NullStatus(nullable = []), metaBinary:@Uninterruptible, toBinary:@Uninterruptible]

~~~~~
//##16. annotation class argument
[$ConcurnasMetaVersion$:, copy:, copy:, defaultFieldInit$:, equals:@NullStatus(nullable = [true]), fromBinary:@Uninterruptible, getGlobalDependancies$:, hashCode:@NullStatus(nullable = []), m1:@TakesClass(theArg = class java.lang.String), @NullStatus(nullable = []), m2:@TakesClass(theArg = int), @NullStatus(nullable = []), metaBinary:@Uninterruptible, toBinary:@Uninterruptible]

~~~~~
//##17. annotation enum argument
[$ConcurnasMetaVersion$:, copy:, copy:, defaultFieldInit$:, equals:@NullStatus(nullable = [true]), fromBinary:@Uninterruptible, getGlobalDependancies$:, hashCode:@NullStatus(nullable = []), m1:@TakesEnum(theArg = ONE), @NullStatus(nullable = []), metaBinary:@Uninterruptible, toBinary:@Uninterruptible]

~~~~~
//##18. annotation nested annotation argument
[$ConcurnasMetaVersion$:, copy:, copy:, defaultFieldInit$:, equals:@NullStatus(nullable = [true]), fromBinary:@Uninterruptible, getGlobalDependancies$:, hashCode:@NullStatus(nullable = []), m0:@TakesAnnotation(theArg = @TakesEnum(theArg = ONE)), @NullStatus(nullable = []), m1:@TakesAnnotation(theArg = @TakesEnum(theArg = ONE)), @NullStatus(nullable = []), metaBinary:@Uninterruptible, toBinary:@Uninterruptible]

~~~~~
//##19. annotation applied to constructors
[MyClass:[@AnnotOneArg(name = Billy) @NullStatus(nullable = [])]~[], MyClass:[@AnnotOneArg(name = Bzaby) @NullStatus(nullable = [])]~[int]]

~~~~~
//##20. annotation was having some reflection bother
[copy:[]~[ ;  ; ], copy:[]~[ ;  ; ], fromBinary:[@Uninterruptible]~[ ; ], init:[]~[ ;  ;  ;  ; ], leFunxc:[]~[@AnnotOneArg(name = hi) ; ], metaBinary:[@Uninterruptible]~[], toBinary:[@Uninterruptible]~[ ; ]]

~~~~~
//##21. annotation applied to method parameters
[$ConcurnasMetaVersion$:[]~[], copy:[]~[ ;  ; ], copy:[]~[ ;  ; ], defaultFieldInit$:[]~[ ;  ; ], equals:[@NullStatus(nullable = [true])]~[@ParamName(hasDefaultValue = false, isVararg = false, name = other) ; ], fromBinary:[@Uninterruptible]~[ ; ], getGlobalDependancies$:[]~[], hashCode:[@NullStatus(nullable = [])]~[], leFunc:[@NullStatus(nullable = [])]~[@AnnotOneArg(name = Bzaby) @ParamName(hasDefaultValue = false, isVararg = false, name = a) ; @TakesAnnotation(theArg = @TakesEnum(theArg = ONE)) @ParamName(hasDefaultValue = false, isVararg = false, name = b) ; ], metaBinary:[@Uninterruptible]~[], toBinary:[@Uninterruptible]~[ ; ]]

~~~~~
//##22. annotation at class level inc immutable one
[[@AnnotOneArg(name = Bzaby) @NullStatus(nullable = []) @ConcImmutable], [@AnnotOneArg(name = Bzaby) @NullStatus(nullable = [])]]

~~~~~
//##23. annotation at actor level inc immutable one
[@AnnotOneArg(name = Bzaby) @NullStatus(nullable = []) @ConcImmutable]

~~~~~
//##24. annotation at enum level inc immutable one
[@AnnotOneArg(name = Bzaby) @NullStatus(nullable = []) @ConcImmutable]

~~~~~
//##25. annotation of fields
[a: [@AnnotOneArg(name = ok)], b: [@AnnotOneArg(name = ok2)]]

~~~~~
//##26. annotation of enum items
[ENUM$VALUES: [], ONE: [@NullStatus(nullable = [])], TWO: [@AnnotOneArg(name = Bzaby) @NullStatus(nullable = [])]]

~~~~~
//##27. annotation locations simple
[a: [], b: [@AnnotOneArg(name = ok)], c: [@AnnotOneArg(name = ok)]]

~~~~~
//##28. annotation locations for fields
fields: [a: [], b: [@AnnotOneArg(name = ok)], c: [], d: [], e: []]
methods:[$ConcurnasMetaVersion$: [], clone: [], copy: [], copy: [], defaultFieldInit$: [], delete: [], equals: [@NullStatus(nullable = [])], fromBinary: [@Uninterruptible], getA: [@NullStatus(nullable = [])], getB: [@NullStatus(nullable = [])], getC: [@AnnotOneArg(name = ok) @NullStatus(nullable = [])], getD: [@AnnotOneArg(name = ok) @NullStatus(nullable = [])], getE: [@AnnotOneArg(name = ok-get) @NullStatus(nullable = [])], getGlobalDependancies$: [], hashCode: [@NullStatus(nullable = [])], init: [], metaBinary: [@Uninterruptible], setA: [@AnnotOneArg(name = ok) @NullStatus(nullable = [])], setB: [@NullStatus(nullable = [])], setC: [@NullStatus(nullable = [])], setD: [@AnnotOneArg(name = ok) @NullStatus(nullable = [])], setE: [@AnnotTwoArgAllDefault(name = ok-set, name2 = hi2) @NullStatus(nullable = [])], toBinary: [@Uninterruptible], toBoolean: [], toString: []]

~~~~~
//##29. annotation locations for classdefargs
fields: [a: [], b: [], c: [], d: [], e: [@AnnotOneArg(name = ok)]]
methods:[$ConcurnasMetaVersion$: [], clone: [], copy: [], copy: [], defaultFieldInit$: [], delete: [], equals: [@NullStatus(nullable = [])], fromBinary: [@Uninterruptible], getA: [@NullStatus(nullable = [])], getB: [@NullStatus(nullable = [])], getC: [@NullStatus(nullable = [])], getD: [@AnnotOneArg(name = ok) @NullStatus(nullable = [])], getE: [@NullStatus(nullable = [])], getGlobalDependancies$: [], hashCode: [@NullStatus(nullable = [])], init: [@NullStatus(nullable = [])], init: [], metaBinary: [@Uninterruptible], setA: [@NullStatus(nullable = [])], setB: [@NullStatus(nullable = [])], setC: [@AnnotOneArg(name = ok) @NullStatus(nullable = [])], setD: [@NullStatus(nullable = [])], setE: [@NullStatus(nullable = [])], toBinary: [@Uninterruptible], toBoolean: [], toString: []]
cons:[[@AnnotOneArg(name = ok) @ParamName(hasDefaultValue = false, isVararg = false, name = a) ; @AnnotOneArg(name = ok) @ParamName(hasDefaultValue = false, isVararg = false, name = b) ; @ParamName(hasDefaultValue = false, isVararg = false, name = c) ; @ParamName(hasDefaultValue = false, isVararg = false, name = d) ; @ParamName(hasDefaultValue = false, isVararg = false, name = e)]]

~~~~~
//##30. annotation of default constructor
fields: [a: []]
methods:[$ConcurnasMetaVersion$: [], clone: [], copy: [], copy: [], defaultFieldInit$: [], delete: [], equals: [@NullStatus(nullable = [])], fromBinary: [@Uninterruptible], getA: [@NullStatus(nullable = [])], getGlobalDependancies$: [], hashCode: [@NullStatus(nullable = [])], init: [@AnnotOneArg(name = ok) @NullStatus(nullable = [])], init: [], metaBinary: [@Uninterruptible], setA: [@NullStatus(nullable = [])], toBinary: [@Uninterruptible], toBoolean: [], toString: []]
cons: [[@AnnotOneArg(name = ok) @NullStatus(nullable = [])]]
class: [@NullStatus(nullable = [])]

~~~~~
//##31. annotation of default constructor for enums plus bugfix on loc names
fields: [ENUM$VALUES: [], a: [], ONE: [@NullStatus(nullable = [])], TWO: [@NullStatus(nullable = [])]]
methods:[$ConcurnasMetaVersion$: [], clone: [], compareTo: [], compareTo: [], copy: [], copy: [], defaultFieldInit$: [], delete: [], describeConstable: [], equals: [], fromBinary: [], getA: [@NullStatus(nullable = [])], getDeclaringClass: [], getGlobalDependancies$: [], hashCode: [], init: [@AnnotOneArg(name = ok) @NullStatus(nullable = [])], init: [], init: [], lambda$describeConstable$0: [], metaBinary: [], name: [], ordinal: [], setA: [@NullStatus(nullable = [])], toBinary: [], toBoolean: [], toString: [], valueOf: [], valueOf: [], values: []]
cons: [[@AnnotOneArg(name = ok) @NullStatus(nullable = [])]]
class: [@AnnotOneArg(name = ok to the class) @NullStatus(nullable = []) @ConcImmutable]

~~~~~
//##32. annotation of enum elements
fields: [ENUM$VALUES: [], a: [], b: [], c: [], d: [], e: [@AnnotOneArg(name = ok)], ONE: [@NullStatus(nullable = [])], TWO: [@NullStatus(nullable = [])], zall: [@AnnotOneArg(name = ok)]]
methods:[$ConcurnasMetaVersion$: [], clone: [], compareTo: [], compareTo: [], copy: [], copy: [], defaultFieldInit$: [], delete: [], describeConstable: [], equals: [], fromBinary: [], getA: [@NullStatus(nullable = [])], getB: [@AnnotOneArg(name = ok) @NullStatus(nullable = [])], getC: [@NullStatus(nullable = [])], getD: [@NullStatus(nullable = [])], getDeclaringClass: [], getE: [@NullStatus(nullable = [])], getGlobalDependancies$: [], getZall: [@AnnotOneArg(name = ok) @NullStatus(nullable = [])], hashCode: [], init: [@NullStatus(nullable = [])], init: [], init: [], lambda$describeConstable$0: [], metaBinary: [], name: [], ordinal: [], setA: [@NullStatus(nullable = [])], setB: [@NullStatus(nullable = [])], setC: [@AnnotOneArg(name = ok) @NullStatus(nullable = [])], setD: [@NullStatus(nullable = [])], setE: [@NullStatus(nullable = [])], setZall: [@AnnotOneArg(name = ok) @NullStatus(nullable = [])], toBinary: [], toBoolean: [], toString: [], valueOf: [], valueOf: [], values: []]
cons: [[ ;  ;  ;  ;  ;  ;  ;  ; ], [ ;  ;  ; ], [ ;  ; ], [ ;  ; ], [ ; ], [ ; ], [@ParamName(hasDefaultValue = false, isVararg = false, name = enum$0) ; @ParamName(hasDefaultValue = false, isVararg = false, name = enum$1) ; @AnnotOneArg(name = ok) @ParamName(hasDefaultValue = false, isVararg = false, name = a) ; @ParamName(hasDefaultValue = false, isVararg = false, name = b) ; @ParamName(hasDefaultValue = false, isVararg = false, name = c) ; @AnnotOneArg(name = ok) @ParamName(hasDefaultValue = false, isVararg = false, name = d) ; @ParamName(hasDefaultValue = false, isVararg = false, name = e) ; @AnnotOneArg(name = ok) @ParamName(hasDefaultValue = false, isVararg = false, name = zall)], []]
class: [@NullStatus(nullable = []) @ConcImmutable]

~~~~~
//##33. untyped actor
fields: [ax: [@AnnotOneArg(name = ok)], processQueue: [], running: []]
methods:[$ConcurnasMetaVersion$$ActorSuperCall: [], $ConcurnasMetaVersion$: [], access$0: [], access$1: [], clone: [], copy: [], copy: [], copy: [], defaultFieldInit$: [], delete: [], equals$ActorSuperCall: [@NullStatus(nullable = [])], equals$ActorSuperCallObjM: [@NullStatus(nullable = [true])], equals: [@NullStatus(nullable = [])], fromBinary$ActorSuperCall: [], fromBinary: [@Uninterruptible], getAx$ActorSuperCall: [], getAx: [@AnnotOneArg(name = ok) @NullStatus(nullable = [])], getClass$ActorSuperCall: [], getGlobalDependancies$: [], getType$ActorSuperCall: [], getType: [], hashCode$ActorSuperCall: [@NullStatus(nullable = [])], hashCode$ActorSuperCallObjM: [@NullStatus(nullable = [])], hashCode: [@NullStatus(nullable = [])], init: [@AnnotOneArg(name = ok) @NullStatus(nullable = [])], init: [@NullStatus(nullable = [])], init: [@NullStatus(nullable = [])], init: [], init: [], isRunning$ActorSuperCall: [], isRunning: [], metaBinary: [@Uninterruptible], onFail$ActorSuperCall: [], onFail: [], onFailActor$ActorSuperCall: [], onFailActor: [], recieveActor: [], restart$ActorSuperCall: [], restart: [], setAx$ActorSuperCall: [], setAx: [@AnnotOneArg(name = ok) @NullStatus(nullable = [])], something$ActorSuperCall: [], something: [@NullStatus(nullable = [])], start$ActorSuperCall: [], start: [], stop$ActorSuperCall: [], stop: [], toBinary$ActorSuperCall: [], toBinary: [@Uninterruptible], toBoolean: [], toString$ActorSuperCall: [@NullStatus(nullable = [])], toString$ActorSuperCallObjM: [@NullStatus(nullable = [])], toString: [@NullStatus(nullable = [])]]
cons: [[ ;  ;  ; ], [ ;  ;  ; ], [ ;  ; ], [ ;  ; ], [ ;  ; ], [ ; ], [ ; ], [ ; ], [@ParamName(hasDefaultValue = false, isVararg = false, name = types$forActor) ; @AnnotOneArg(name = ok) @ParamName(hasDefaultValue = false, isVararg = false, name = ax)], [@ParamName(hasDefaultValue = false, isVararg = false, name = types$forActor) ; @ParamName(hasDefaultValue = false, isVararg = false, name = a) ; @ParamName(hasDefaultValue = false, isVararg = false, name = b)], [@ParamName(hasDefaultValue = false, isVararg = false, name = types$forActor)], []]
con itself: [[@AnnotOneArg(name = []) @NullStatus(nullable = [])], [@AnnotOneArg(name = ok) @NullStatus(nullable = [])], [@NullStatus(nullable = [])], [@NullStatus(nullable = [])], [@NullStatus(nullable = [])], [@NullStatus(nullable = [])], [], [], [], [], [], []]
class: [@NullStatus(nullable = [])]

~~~~~
//##34. typed actor
fields: [a: [@AnnotOneArg(name = ok)], b: [], processQueue: [], running: []]
methods:[$ConcurnasMetaVersion$$ActorSuperCall: [], $ConcurnasMetaVersion$: [], access$0: [], access$1: [], bindCall$ActorSuperCall: [], bindCall: [], clone: [], copy: [], copy: [], copy: [], copy: [], defaultFieldInit$: [], delete: [], equals$ActorCall: [@NullStatus(nullable = [])], equals$ActorSuperCall: [@NullStatus(nullable = [])], equals$ActorSuperCallObjM: [@NullStatus(nullable = [true])], equals: [@NullStatus(nullable = [])], fromBinary$ActorSuperCall: [], fromBinary: [@Uninterruptible], getA$ActorSuperCall: [], getA: [@AnnotOneArg(name = ok) @NullStatus(nullable = [])], getActeeClone$ActorSuperCall: [], getActeeClone: [], getB$ActorCall: [@NullStatus(nullable = [])], getB$ActorSuperCall: [], getB: [@NullStatus(nullable = [])], getClass$ActorCall: [@NullStatus(nullable = [])], getClass$ActorSuperCall: [], getGlobalDependancies$: [], getType$ActorSuperCall: [], getType: [], getXa$ActorCall: [@NullStatus(nullable = [])], hashCode$ActorCall: [@NullStatus(nullable = [])], hashCode$ActorSuperCall: [@NullStatus(nullable = [])], hashCode$ActorSuperCallObjM: [@NullStatus(nullable = [])], hashCode: [@NullStatus(nullable = [])], init: [@AnnotOneArg(name = on the default constructor) @NullStatus(nullable = [])], init: [], init: [], init: [], init: [], init: [], isRunning$ActorSuperCall: [], isRunning: [], metaBinary: [@Uninterruptible], notify$ActorCall: [], notifyAll$ActorCall: [], onFail$ActorSuperCall: [], onFail: [], onFailActor$ActorSuperCall: [], onFailActor: [], recieveActor: [], restart$ActorSuperCall: [], restart: [], setA$ActorSuperCall: [], setA: [@AnnotOneArg(name = ok) @NullStatus(nullable = [])], setB$ActorCall: [@NullStatus(nullable = [])], setB$ActorSuperCall: [], setB: [@NullStatus(nullable = [])], setXa$ActorCall: [@NullStatus(nullable = [])], start$ActorSuperCall: [], start: [], startOperation$ActorSuperCall: [], startOperation: [], stop$ActorSuperCall: [], stop: [], stopOperation$ActorSuperCall: [], stopOperation: [], toBinary$ActorSuperCall: [], toBinary: [@Uninterruptible], toBoolean$ActorSuperCall: [], toBoolean: [@NullStatus(nullable = [])], toString$ActorCall: [@NullStatus(nullable = [])], toString$ActorSuperCall: [], toString$ActorSuperCallObjM: [], toString: [@NullStatus(nullable = [])], wait$ActorCall: [], wait$ActorCall: [], wait$ActorCall: []]
cons: [[ ;  ;  ; ], [ ;  ;  ; ], [ ;  ; ], [ ;  ; ], [ ; ], [ ; ], [@ParamName(hasDefaultValue = false, isVararg = false, name = types$forActor) ; @AnnotOneArg(name = ok) @ParamName(hasDefaultValue = false, isVararg = false, name = a) ; @ParamName(hasDefaultValue = false, isVararg = false, name = b)], []]
con itself: [[@AnnotOneArg(name = []) @NullStatus(nullable = [])], [@AnnotOneArg(name = on the default constructor) @NullStatus(nullable = [])], [], [], [], [], [], []]
class: [@AnnotOneArg(name = ok cls) @NullStatus(nullable = [])]

~~~~~
//##35. Inherited annotion works without effort
fields: []
methods:[$ConcurnasMetaVersion$: [], clone: [], copy: [], copy: [], copy: [], defaultFieldInit$: [], delete: [], equals: [@NullStatus(nullable = [true])], fromBinary: [@Uninterruptible], getGlobalDependancies$: [], hashCode: [@NullStatus(nullable = [])], init: [], init: [], metaBinary: [@Uninterruptible], toBinary: [@Uninterruptible], toBoolean: [], toString: []]
cons: [[], [], [ ; ], [ ;  ; ], [ ; ], [], [ ;  ;  ; ], [ ;  ; ]]
con itself: [[@NullStatus(nullable = [])], [@NullStatus(nullable = [])], [], [], [], [], [], []]
class: [@InheritedOne(value = hi) @NullStatus(nullable = [])]

~~~~~
//##36. custom annotations
fields: []
methods:[$ConcurnasMetaVersion$: [], clone: [], copy: [], copy: [], defaultFieldInit$: [], delete: [], equals: [@NullStatus(nullable = [])], fromBinary: [@Uninterruptible], getGlobalDependancies$: [], hashCode: [@NullStatus(nullable = [])], metaBinary: [@Uninterruptible], toBinary: [@Uninterruptible], toBoolean: [], toString: []]
cons: [[], [], [ ; ], [], [ ; ], [ ;  ; ]]
con itself: [[], [@NullStatus(nullable = [])], [], [@NullStatus(nullable = [])], [], []]
class: [@MYAnnotation($ConcurnasMetaVersion$ = Concurnas, a = hi, b = 8, c = ok, ff = [1 2 3], getGlobalDependancies$ = []) @NullStatus(nullable = []) @ConcImmutable]

~~~~~
//##37. custom annotations - enum
[@MYAnnotation($ConcurnasMetaVersion$ = Concurnas, ee = ONE, getGlobalDependancies$ = []) @NullStatus(nullable = []) @ConcImmutable]

~~~~~
//##38. custom annotations - class
[@MYAnnotation($ConcurnasMetaVersion$ = Concurnas, ee = interface x38customannotationsclass$MYAnnotation, ee2 = class java.lang.String, getGlobalDependancies$ = []) @NullStatus(nullable = []) @ConcImmutable]

~~~~~
//##39. custom with non primative arrays
[@MYAnnotation($ConcurnasMetaVersion$ = Concurnas, check = [one two], check2 = [hi there], getGlobalDependancies$ = []) @NullStatus(nullable = []) @ConcImmutable]

~~~~~
//##40.custom with array of enums
[@MYAnnotation($ConcurnasMetaVersion$ = Concurnas, check = [ONE TWO], check2 = [THREE FOUR], getGlobalDependancies$ = []) @NullStatus(nullable = []) @ConcImmutable]

~~~~~
//##41.simple w retention
[@MYAnnotation($ConcurnasMetaVersion$ = Concurnas, check2 = MyClass.class, getGlobalDependancies$ = []) @NullStatus(nullable = []) @ConcImmutable]

~~~~~
//##42. array of class
[@MYAnnotation($ConcurnasMetaVersion$ = Concurnas, check = [class java.lang.String class java.lang.String], check2 = [class x42arrayofclass$MyClass], getGlobalDependancies$ = []) @NullStatus(nullable = []) @ConcImmutable]

~~~~~
//##43. custom annotations having annotations as arguments lol
[@MYAnnotation($ConcurnasMetaVersion$ = Concurnas, check = @TakesAnnotation(theArg = @TakesEnum(theArg = ONE)), check2 = @TakesAnnotation(theArg = @TakesEnum(theArg = ONE)), checkuphere = @TakesAnnotation(theArg = @TakesEnum(theArg = ONE)), checkxxx = @TakesAnnotation(theArg = @TakesEnum(theArg = ONE)), getGlobalDependancies$ = []) @NullStatus(nullable = []) @ConcImmutable]

~~~~~
//##44. custom annotations arrays of annotations
[@MYAnnotation($ConcurnasMetaVersion$ = Concurnas, check = [@TakesAnnotation(theArg = @TakesEnum(theArg = ONE))], check2 = [@TakesAnnotation(theArg = @TakesEnum(theArg = ONE))], checkuphere = [@TakesAnnotation(theArg = @TakesEnum(theArg = ONE))], checkxxx = [@TakesAnnotation(theArg = @TakesEnum(theArg = ONE))], getGlobalDependancies$ = []) @NullStatus(nullable = []) @ConcImmutable]

~~~~~
//##45. custom annotations retension
[@MYAnnotation3($ConcurnasMetaVersion$ = Concurnas, b = 1, getGlobalDependancies$ = []) @NullStatus(nullable = []) @ConcImmutable]

~~~~~
//##46. auto type convert to an array
[@MYAnnotation1($ConcurnasMetaVersion$ = Concurnas, b = 1, c = [1], getGlobalDependancies$ = []) @NullStatus(nullable = [])]

~~~~~
//##47. ANNOTATION_TYPE element type
[@MYAnnotation1($ConcurnasMetaVersion$ = Concurnas, b = 1, c = [1], getGlobalDependancies$ = []) @NullStatus(nullable = [])]

~~~~~
//##48. annotations can be applied to annotation fields
methods: [$ConcurnasMetaVersion$: [], annotationType: [], b: [@AnotForAnot($ConcurnasMetaVersion$ = Concurnas, cool = yup, getGlobalDependancies$ = [])], c: [@AnotForAnot($ConcurnasMetaVersion$ = Concurnas, cool = yup, getGlobalDependancies$ = [])], equals: [], getGlobalDependancies$: [], hashCode: [], toString: []]

~~~~~
//##49. annotations can have nested enum
[@MYAnnotation1($ConcurnasMetaVersion$ = Concurnas, c = ONE, d = ONE, getGlobalDependancies$ = []) @NullStatus(nullable = []) @ConcImmutable]

~~~~~
//##50. annotations can have nested enum correct default type
[@MYAnnotation1($ConcurnasMetaVersion$ = Concurnas, c = ONE, d = ONE, getGlobalDependancies$ = []) @NullStatus(nullable = []) @ConcImmutable]

~~~~~
//##51. reference to nested annotation
[@MYAnnotation1($ConcurnasMetaVersion$ = Concurnas, c = @MYAnnotationNeste($ConcurnasMetaVersion$ = Concurnas, getGlobalDependancies$ = []), getGlobalDependancies$ = []) @NullStatus(nullable = []) @ConcImmutable]

~~~~~
//##52. reference to nested annotation as default
[@MYAnnotation1($ConcurnasMetaVersion$ = Concurnas, c = @MYAnnotationNeste($ConcurnasMetaVersion$ = Concurnas, getGlobalDependancies$ = []), getGlobalDependancies$ = []) @NullStatus(nullable = []) @ConcImmutable]

~~~~~
//##53. annotation nested in class
[[@NestedAnnot($ConcurnasMetaVersion$ = [], getGlobalDependancies$ = []) @NullStatus(nullable = []) @ConcImmutable], [@NestedAnnot($ConcurnasMetaVersion$ = [], getGlobalDependancies$ = []) @NullStatus(nullable = [])]]

~~~~~
//##54. annotation nested in enum
[@NestedAnnot($ConcurnasMetaVersion$ = [], getGlobalDependancies$ = []) @NullStatus(nullable = []) @ConcImmutable]

~~~~~
//##55. Deprecated works at runtime tagged ok
[@Deprecated(forRemoval = false, since = ) @NullStatus(nullable = []) @ConcImmutable]

~~~~~
//##56. aside, one need not qualify generics when class is used for class reference
[[@NullStatus(nullable = []) @ConcImmutable], [@NullStatus(nullable = []) @ConcImmutable]]

~~~~~
//##57. WOW compiler warnings can be suppressed
cool

~~~~~
//##58. check annotation can be read from existing compiled classes
[[@Deprecated(forRemoval = false, since = )], [@Deprecated(forRemoval = false, since = ) ; ]]

~~~~~
//##59. annotations are exposable at module level
[@MyAnnot($ConcurnasMetaVersion$ = Concurnas, getGlobalDependancies$ = []) @Typedefs(typedefs = [@Typedef(args = [X], name = mylistQ, type = Ljava/util/ArrayList<Ljava/lang/String;>;)])]

~~~~~
//##60. trust me override is annotatted as such
[@NullStatus(nullable = [])]

~~~~~
//##61. bug fix on constant folding
106751991167300

~~~~~
//##61. bug fix on constant folding enum items
ONE

~~~~~
//##62. constant folding classDef
class [Z

~~~~~
//##63. many target annotation
[@MYAnnotation1($ConcurnasMetaVersion$ = Concurnas, b = 1, c = [1], getGlobalDependancies$ = []) @NullStatus(nullable = [])]

~~~~~
//##64. fiber is a visible parameter 
[[@AnnotOneArg(name = hi) ;  ; ], [ ;  ;  ;  ; ], [ ; ], [ ; ], [], [ ;  ; ], [ ;  ; ]]