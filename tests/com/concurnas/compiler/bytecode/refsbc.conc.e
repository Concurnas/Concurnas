//##1. basic declarations
[true, true]

~~~~~
//##1b. basic declarations
[true]

~~~~~
//##1.c double check
[true]:9.0:9.0

~~~~~
//##2. basic declarations - no assignment
[true]

~~~~~
//##2b. basic declarations - no assignment
[true]

~~~~~
//##3. basic assignment
[true]

~~~~~
//##3b. basic assignment
[true]

~~~~~
//##4.1.a preable
[true, true, true]

~~~~~
//##4.1.b preable
[true, true]

~~~~~
//##4.1.c preable
[true, true]

~~~~~
//##4.1.d preable more
[true, true, false, false]

~~~~~
//##4.1.e preamble not variant
[true, true, true, true]

~~~~~
//##4.1.f final preamble
[[true, true, true, true], [true, false, false, true]]

~~~~~
//##4. implicit type declaration
[true]

~~~~~
//##4.b implicit type declaration
[true, true, true, true, true, true]

~~~~~
//##4.c implicit type declaration
[true, true, true]

~~~~~
//##4.d implicit type declaration
[true, true]

~~~~~
//##5. basic function calls
[true, true, true, true, true, true]

~~~~~
//##5.b basic function calls
k

~~~~~
//##6.a basic gets
9

~~~~~
//##6.b gets
[true, true] [9, 9, 9, 81, 18, 18, 10, 10]

~~~~~
//##7. str ops
[true, true, true, true]

~~~~~
//##8. array casting str
[4:, 9:]:[400, 9]:[9, 9]

~~~~~
//##8.b array casting str
[400.0, 9.0]~~[4:, 9.0:]~~[9.0, 9.0]

~~~~~
//##11.a instanceof check
[true, true, true]

~~~~~
//##11.b instanceof check
[true, true]

~~~~~
//##11.c instanceof check - simpler
[true, true]

~~~~~
//##12. arrays of refs
[[9: 8:], [8: 8:]][8, 9]@[true]

~~~~~
//##13. comparison operations
[true, true, true, true]

~~~~~
//##14. refs from external locations
[true, true, true]:[62, 62, 62]

~~~~~
//##14.b refs from external locations
[true, true, true]:[62, 62, 62]

~~~~~
//##15. null ref handle ok
[true, [true, true]]

~~~~~
//##16. implicit and explicit cast
[true, true, true, true]
[true, true, true, true]
[true, true, true, true]

~~~~~
//##17. another cast check
[true, true]

~~~~~
//##18. more cast check
[true, true]

~~~~~
//##19. print out ok
9,9,9:

~~~~~
//##19.a print out ok
9

~~~~~
//##19.b print out ok
10:,10

~~~~~
//##20. cast exception on ref to int even if otherwise ok
okexcep

~~~~~
//##21. check cast from obj etc
77:]][7, 7]:~:[true, true, true, true]
onlockedExpect: its ok

~~~~~
//##22. check cast from obj etc 2 - more complex cases, ref to raw, raw to ref via cast
[true, true, true, true, true, true, 7:]

~~~~~
//##23. check cast unref etc
[true, true]

~~~~~
//##24. another set of casts
[true, true, true, true, true, true, true]

~~~~~
//##25. simple array casts:
[7: 8:]@8:true[7: 8:]

~~~~~
//##25.b simple array casts: - advanced
[true, true, true, true, [44:], true]

~~~~~
//##26.a refs of arrays - get
1

~~~~~
//##26.b refs of arrays - set
[[23 2], [23, 2]]

~~~~~
//##26.c refs of arrays - explicit type dec probably other usese
1

~~~~~
//##26.d refs of arrays - simple print, ref all
[[1 2]: [1 2]:]===[1 2]:>[[1 2], 1, 1, 1]

~~~~~
//##26.e refs of arrays - ok check u can make an array of these ref arrays
[[9: 2:], [9: 2:]]

~~~~~
//##26.f refs of arrays - ok check u can make an array of these ref arrays more complex
[[[1 2]: [1 2]:], [[1 2]: [1 2]:]]

~~~~~
//##26.g.1 getting silly now
[[1:, 2:]:, [3:, 4:]:]

~~~~~
//##26.g.2 getting silly now 2
[[1:, 2:]:, [3:, 4:]:]:

~~~~~
//##26.g.3 getting silly now
[1 2 ; 1 2]==[1 2 ; 1 2]
[[1 2]: [1 2]:]
[[1 2]: [1 2]:]:
[1 2]
[1 2]:

~~~~~
//##26.g.4 getting silly now
[[1 2]: [1 2]:]==[12: 5: 6:]

~~~~~
//##27.a arr assignment 2d cases
[[9 81]: [10 20]: [9 81]:]

~~~~~
//##27.b arr assignment 3d cases
[[[3: 4:]: [3: 4:]:]: [[30: 40:]: [1: 2:]: [1: 2:]:]: [[300: 27:]:]:]

~~~~~
//##28. i thought this was a bug but clearly i was wrong
true->9:

~~~~~
//##29.1 revisit as logic
{}java.lang.ClassCastException: java.util.HashMap[_, _] cannot be cast to java.lang.Integer:

~~~~~
//##29.2 revisit as logic
[{}:, {}:, {}, {}][true, true, true, true]

~~~~~
//##29.3 revisit as logic
[{}:, {}:, {}:, {}:]
java.lang.ClassCastException: java.util.HashMap[java.lang.String, java.lang.Integer]: cannot be cast to java.lang.Integer:
java.lang.ClassCastException: java.util.HashMap[_, _] cannot be cast to java.lang.Integer:
[true, true, true, true]

~~~~~
//##29.4 revisit as logic - ensure autobox up
5:

~~~~~
//##29.5 some other tests
[true, true, true, true, true, true]

~~~~~
//##30. module level fields
Hello world[5:, 5:]

~~~~~
//##31. clsas level fields
Hello world[5:, 5:, 5:]

~~~~~
//##32.a class fields
Hello world[12:, 13:]

~~~~~
//##32.b class fields more
[12, mate, 12, mate]
[12, mate, 12, mate]
[mate]~[12:, mate:, 12:, mate:]
true
[12:, mate:, 12:, mate:]
[mate:]

~~~~~
//##33.a module field correct behavour - simple case
[newValue, newValue2]
[initial, initial2]
[newValue:, initial2]

~~~~~
//##33.1.a module field correct behavour
[initial, newValue:, newValue]
[initial, initial:, newValue]
[initial, initial:, newValue]
[initial, newValue:, newValue]
[initial, initial:, more stuff, more stuff]

~~~~~
//##33.1.b module field correct behavour - with int
[1, 2:, 2]
[1, 1:, 2]
[1, 1:, 2]
[1, 2:, 2]
[1, 1:, 3, 3]

~~~~~
//##33.1 module field correct behavour - as mod
[initial, newValue:, newValue]
[initial, initial:, newValue]
[initial, initial:, newValue]
[initial, newValue:, newValue]
[initial, initial:, more stuff, more stuff]

~~~~~
//##33.2 module field correct behavour - as mod int
[1, 2:, 2]
[1, 1:, 2]
[1, 1:, 2]
[1, 2:, 2]
[1, 1:, 3, 3]

~~~~~
//##34. sneak in double check
newValue: newValue:

~~~~~
//##33.1 as module fields in clinit
[initial, newValue:, newValue]
[initial, initial:, newValue]
[initial, initial:, newValue]
[initial, newValue:, newValue]
[initial, initial:, more stuff, more stuff]

~~~~~
//##33.2 as module fields in clinit - as int
[1, 2:, 2]
[1, 1:, 2]
[1, 1:, 2]
[1, 2:, 2]
[1, 1:, 3, 3]

~~~~~
//##33.2 as class fields direct access - ext
[initial, newValue:, newValue:]
[initial, initial:, newValue:]
[initial, initial:, newValue:]
[initial, newValue:, newValue:]
[initial, initial:, more stuff:, more stuff]

~~~~~
//##33.2.b as class fields direct access - ext as int
[1, 2:, 2:]
[1, 1:, 2:]
[1, 1:, 2:]
[1, 2:, 2:]
[1, 1:, 3:, 3]

~~~~~
//##33.3 as class fields direct access - internal
[initial, newValue:, newValue:]
[initial, initial:, newValue:]
[initial, initial:, newValue:]
[initial, newValue:, newValue:]
[initial, initial:, more stuff:, more stuff]

~~~~~
//##33.3.b as class fields direct access - internal - int
[1, 2:, 2:]
[1, 1:, 2:]
[1, 1:, 2:]
[1, 2:, 2:]
[1, 1:, 3:, 3]

~~~~~
//##34 the class is a ref
[initial, newValue:, newValue:]
[initial, initial:, newValue:]
[initial, initial:, newValue:]
[initial, newValue:, newValue:]
[initial, initial:, more stuff:, more stuff]

~~~~~
//##34.b the class is a ref - int
[1, 2:, 2:]
[1, 1:, 2:]
[1, 1:, 2:]
[1, 2:, 2:]
[1, 1:, 3:, 3]

~~~~~
//##35. class via getter setter
[initial, newValue:, newValue:]
[initial, initial:, newValue:]
[initial, initial:, newValue:]
[initial, newValue:, newValue:]
[initial, initial:, more stuff:, more stuff]

~~~~~
//##35. class via getter setter - int
[1, 2:, 2:]
[1, 1:, 2:]
[1, 1:, 2:]
[1, 2:, 2:]
[1, 1:, 3:, 3]

~~~~~
//##36. ensure correct function called when many choices
[normal, normal, objstr, ref]
[normal, normal, objstr:]

~~~~~
//##37. esnure correct setter called
[getX, setX:, setX:, setX, getX]

~~~~~
//##37. esnure correct setter called - missing direct
[getX, setX:, setX:, setX:, getX]

~~~~~
//##38. esnure correct setter called part 2 
[true, true][orig:, newRef:]

~~~~~
//##38. esnure correct setter called part 3
[true, true, true]newRef2:

~~~~~
//##38. esnure correct setter called part 4
[true, true, true]newRef2

~~~~~
//##39. ref of array
[[initial], [newValue]:, [newValue]]
[[initial], [initial]:, [newValue]]
[[initial], [initial]:, [newValue]]
[[initial], [newValue]:, [newValue]]
[[initial], [initial]:, [more stuff], [more stuff]]

~~~~~
//##39. ref of array - int
[[1], [2]:, [2]]
[[1], [1]:, [2]]
[[1], [1]:, [2]]
[[1], [2]:, [2]]
[[1], [1]:, [3], [3]]

~~~~~
//##39. ref of array - module
[[initial], [newValue]:, [newValue]]
[[initial], [initial]:, [newValue]]
[[initial], [initial]:, [newValue]]
[[newValue], [newValue]:, [newValue]]
[[initial], [initial]:, [more stuff], [more stuff]]

~~~~~
//##39. ref of array - module - int
[[1], [2]:, [2]]
[[1], [1]:, [2]]
[[1], [1]:, [2]]
[[2], [2]:, [2]]
[[1], [1]:, [3], [3]]

~~~~~
//##39. ref of array - class - internal
[[initial], [newValue]:, [newValue]:]
[[initial], [initial]:, [newValue]:]
[[initial], [initial]:, [newValue]:]
[[initial], [newValue]:, [newValue]:]
[[initial], [initial]:, [more stuff]:, [more stuff]]

~~~~~
//##39. ref of array - class - internal - int
[[1], [2]:, [2]:]
[[1], [1]:, [2]:]
[[1], [1]:, [2]:]
[[1], [2]:, [2]:]
[[1], [1]:, [3]:, [3]]

~~~~~
//##39. ref of array - class - external
[[initial], [newValue]:, [newValue]:]
[[initial], [initial]:, [newValue]:]
[[initial], [initial]:, [newValue]:]
[[initial], [newValue]:, [newValue]:]
[[initial], [initial]:, [more stuff]:, [more stuff]]

~~~~~
//##39. ref of array - class - external - int
[[1], [2]:, [2]:]
[[1], [1]:, [2]:]
[[1], [1]:, [2]:]
[[1], [2]:, [2]:]
[[1], [1]:, [3]:, [3]]

~~~~~
//##39. ref of array - class - external - getter setter
[[initial], [newValue]:, [newValue]:]
[[initial], [initial]:, [newValue]:]
[[initial], [initial]:, [newValue]:]
[[initial], [initial]:, [more stuff]:, [more stuff]]

~~~~~
//##39. ref of array - class - external - int - getter setter
[[1], [2]:, [2]:]
[[1], [1]:, [2]:]
[[1], [1]:, [2]:]
[[1], [1]:, [3]:, [3]]

~~~~~
//##40. array of ref - local vars
[[initial:], [initial:], [newValue:]]
[[initial:], [initial:]:, [newValue:]]
[[initial:], [initial:]:, [newValue:]]
[[initial:], [initial:]:, [newValue:], [more stuff:]]

~~~~~
//##40. array of ref - local vars - int 
[[1:], [1:], [2:]]
[[1:], [1:]:, [2:]]
[[1:], [1:]:, [2:]]
[[1:], [1:]:, [2:], [3:]]

~~~~~
//##40. array of ref - module vars
[[initial:], [initial:], [newValue:]]
[[initial:], [initial:]:, [newValue:]]
[[initial:], [initial:]:, [newValue:]]
[[initial:], [initial:]:, [newValue:], [more stuff:]]

~~~~~
//##40. array of ref - module vars - int 
[[1:], [1:], [2:]]
[[1:], [1:]:, [2:]]
[[1:], [1:]:, [2:]]
[[1:], [1:]:, [2:], [3:]]

~~~~~
//##40. array of ref - class internal
[[initial:], [initial:], [newValue:]]
[[initial:], [initial:], [newValue:]]
[[initial:], [initial:], [newValue:]]
[[initial:], [initial:], [newValue:], [more stuff:]]

~~~~~
//##40. array of ref - class internal - int 
[[1:], [1:], [2:]]
[[1:], [1:], [2:]]
[[1:], [1:], [2:]]
[[1:], [1:], [2:], [3:]]

~~~~~
//##40. array of ref - external class
[[initial:], [initial:], [newValue:]]
[[initial:], [initial:], [newValue:]]
[[initial:], [initial:], [newValue:]]
[[initial:], [initial:], [newValue:], [more stuff:]]

~~~~~
//##40. array of ref - external class - int
[[1:], [1:], [2:]]
[[1:], [1:], [2:]]
[[1:], [1:], [2:]]
[[1:], [1:], [2:], [3:]]

~~~~~
//##40. array of ref - external class -gs
[[initial:], [initial:], [newValue:]]
[[initial:], [initial:], [newValue:]]
[[initial:], [initial:], [newValue:]]
[[initial:], [initial:], [newValue:], [more stuff:]]

~~~~~
//##40. array of ref - external class  -gs - int
[[1:], [1:], [2:]]
[[1:], [1:], [2:]]
[[1:], [1:], [2:]]
[[1:], [1:], [2:], [3:]]

~~~~~
//##40. array of ref - minor bug
[66, [2:]]

~~~~~
//##41. ref of array index operations
[1, [1 2 44 4 5 6 7 8 9 10]]
[1, [1 2 44 4 5 6 7 8 9 10]]
[1, [1 2 44 4 5 6 7 8 9 10]]
[1, [1 2 44 4 5 6 7 8 9 10]]
[1, [1 2 44 4 5 6 7 8 9 10], [1 2 44 4 5 6 7 8 9 10]:]


~~~~~
//##42. array of ref index operations
[1, [1: 2: 44: 4: 5: 6: 7: 8: 9: 10:]]
[1, [1: 2: 44: 4: 5: 6: 7: 8: 9: 10:]]
[1, [1: 2: 44: 4: 5: 6: 7: 8: 9: 10:]]
[1, [1: 2: 44: 4: 5: 6: 7: 8: 9: 10:]]
[1, [1: 2: 44: 48: 5: 6: 7: 8: 9: 10:], [1: 2: 44: 48: 5: 6: 7: 8: 9: 10:]]


~~~~~
//##43. ref of array of ref 
[1, [1: 2: 44: 4: 5: 6: 7: 8: 9: 10:]]
[1, [1: 2: 44: 4: 5: 6: 7: 8: 9: 10:]]
[1, [1: 2: 44: 4: 5: 6: 7: 8: 9: 10:]]
[1, [1: 2: 44: 4: 5: 6: 7: 8: 9: 10:]]
[1, [1: 2: 44: 48: 5: 6: 7: 8: 9: 10:], [1: 2: 44: 48: 5: 6: 7: 8: 9: 10:]:]


~~~~~
//##44. array of ref - some op fail
[[1:, 2:, 3:, 4:, 5:, 6:, 7:, 8:, 9:, 10:], [1:, 2:, 3:, 4:, 5:, 6:, 7:, 8:, 9:, 10:]]

~~~~~
//##44.b array of ref - some op fail more
[1: 2:]=>true

~~~~~
//##44.c array of ref - the special case catered for
[[1: 2:], [1: 2:], [1: 2:], [1: 2:], dude called]

~~~~~
//##44.d array of ref - so now these are ok too
[[1, 2, 3]:, [1, 2, 3]:]

~~~~~
//##45. simple inc
[6:, 6:, 4:, 4:]

~~~~~
//##45.b simple inc - with assignment
[6:, 6:, 4:, 4:][[5, 6, 5, 4]

~~~~~
//##45.c simple inc - with assignment - copy refs correctly
[6:, 6:, 4:, 4:][][6:, 6:, 4:, 4:][true, true, true, true]

~~~~~
//##45.d simple inc - in place operations
[6:, 4:, 10:]

~~~~~
//##45.d simple inc - string concatenation
5hi

~~~~~
//##46. inc operations - module level
[6:, 6:, 4:, 4:]
[6:, 6:, 4:, 4:][[5, 6, 5, 4]
[6:, 6:, 4:, 4:][][6:, 6:, 4:, 4:][true, true, true, true]
[6:, 4:, 10:]
5hi


~~~~~
//##47. inc operations - class inner level
[6:, 6:, 4:, 4:]
[6:, 6:, 4:, 4:][[5, 6, 5, 4]
[6:, 6:, 4:, 4:][][6:, 6:, 4:, 4:][true, true, true, true]
[6:, 4:, 10:]
5hi
[6:, 6:, 4:, 4:]


~~~~~
//##48. inc operations - class field
[6:, 6:, 4:, 4:]
[6:, 6:, 4:, 4:][[5, 6, 5, 4]
[6:, 6:, 4:, 4:][][6:, 6:, 4:, 4:][true, true, true, true]
[6:, 4:, 10:]
5hi


~~~~~
//##49. inc operations - class field get set
[6:, 6:, 4:, 4:]
[6:, 6:, 4:, 4:][[5, 6, 5, 4]
[6:, 6:, 4:, 4:][][6:, 6:, 4:, 4:][true, true, true, true]
[6:, 4:, 10:]
5hi


~~~~~
//##50. inc operations - class field get only
[6:, 6:, 4:, 4:]
[6:, 6:, 4:, 4:][[5, 6, 5, 4]
[6:, 6:, 4:, 4:][][6:, 6:, 4:, 4:][true, true, true, true]
[6:, 4:, 10:]
5hi


~~~~~
//##51. inc operations - class field set only
[6:, 6:, 4:, 4:]
[6:, 6:, 4:, 4:][[5, 6, 5, 4]
[6:, 6:, 4:, 4:][][6:, 6:, 4:, 4:][true, true, true, true]
[6:, 4:, 10:]
5hi


~~~~~
//##52.a inc operations - ref of array - local
[[6 5 5 5]:, [6 5 5 5]:, [4 5 5 5]:, [4 5 5 5]:]
[[6 5 5 5]:, [6 5 5 5]:, [4 5 5 5]:, [4 5 5 5]:][[5, 6, 5, 4]
[[6 5 5 5]:, [6 5 5 5]:, [4 5 5 5]:, [4 5 5 5]:][][5:, 6:, 5:, 4:]
[[6 5 5 5]:, [4 5 5 5]:, [10 5 5 5]:]
[5hi 5 5 5]


~~~~~
//##52.b inc operations - ref of array - module
[[6 5 5 5]:, [6 5 5 5]:, [4 5 5 5]:, [4 5 5 5]:]
[[6 5 5 5]:, [6 5 5 5]:, [4 5 5 5]:, [4 5 5 5]:][[5, 6, 5, 4]
[[6 5 5 5]:, [6 5 5 5]:, [4 5 5 5]:, [4 5 5 5]:][][5:, 6:, 5:, 4:]
[[6 5 5 5]:, [4 5 5 5]:, [10 5 5 5]:]
[5hi 5 5 5]


~~~~~
//##52.c inc operations - ref of array - class inner
[[6 5 5 5]:, [6 5 5 5]:, [4 5 5 5]:, [4 5 5 5]:]
[[6 5 5 5]:, [6 5 5 5]:, [4 5 5 5]:, [4 5 5 5]:][[5, 6, 5, 4]
[[6 5 5 5]:, [6 5 5 5]:, [4 5 5 5]:, [4 5 5 5]:][][5:, 6:, 5:, 4:]
[[6 5 5 5]:, [4 5 5 5]:, [10 5 5 5]:]
[5hi 5 5 5]


~~~~~
//##52.d inc operations - ref of array - class field
[[6 5 5 5]:, [6 5 5 5]:, [4 5 5 5]:, [4 5 5 5]:]
[[6 5 5 5]:, [6 5 5 5]:, [4 5 5 5]:, [4 5 5 5]:][[5, 6, 5, 4]
[[6 5 5 5]:, [6 5 5 5]:, [4 5 5 5]:, [4 5 5 5]:][][5:, 6:, 5:, 4:]
[[6 5 5 5]:, [4 5 5 5]:, [10 5 5 5]:]
[5hi 5 5 5]


~~~~~
//##52.e inc operations - ref of array - class getter and setter
[[6 5 5 5]:, [6 5 5 5]:, [4 5 5 5]:, [4 5 5 5]:]
[[6 5 5 5]:, [6 5 5 5]:, [4 5 5 5]:, [4 5 5 5]:][[5, 6, 5, 4]
[[6 5 5 5]:, [6 5 5 5]:, [4 5 5 5]:, [4 5 5 5]:][][5:, 6:, 5:, 4:]
[[6 5 5 5]:, [4 5 5 5]:, [10 5 5 5]:]
[5hi 5 5 5]


~~~~~
//##555 intermission - respect pre post ops
[6, 5]

~~~~~
//##555.2 intermission - auto extract ref thing
[[5], [5:]]

~~~~~
//##555.3 intermission - ensure that keys and values in maps are cast approperiatly in bytecode
5

~~~~~
//##555.4 intermission - another one
5

~~~~~
//##555.5 intermission - array index use ref as arg unref the int man
5

~~~~~
//##555.6 intermission - use of refs inside pow op
[3125, 759375, 3125][5, 5, 5, 5]

~~~~~
//##555.7 intermission - ensure that postfix operator returns thing being refered to - a
[5 5 5 5 6 6 5 5 5 5 5 5 5 5 5 5 5 5 5 5 5 5 5 5 true]

~~~~~
//##555.7 intermission - ensure that postfix operator returns thing being refered to - b
[5, 5, 5, true, true, true, 5, 5, 5, 5, 5, 5, 6, 5]

~~~~~
//##555.8 intermission - private static field which happens to be a ref
[5, 19]

~~~~~
//##53.a inc operations - array of ref - local
[[6: 5: 5: 5:]:, [6: 5: 5: 5:]:, [4: 5: 5: 5:]:, [4: 5: 5: 5:]:]
[[6: 5: 5: 5:]:, [6: 5: 5: 5:]:, [4: 5: 5: 5:]:, [4: 5: 5: 5:]:][[5, 6, 5, 4]
[[6: 5: 5: 5:]:, [6: 5: 5: 5:]:, [4: 5: 5: 5:]:, [4: 5: 5: 5:]:][][6:, 6:, 4:, 4:]
[[6: 5: 5: 5:]:, [4: 5: 5: 5:]:, [10: 5: 5: 5:]:]
[5hi: 5: 5: 5:]


~~~~~
//##53.b inc operations - array of ref - module
[[6: 5: 5: 5:]:, [6: 5: 5: 5:]:, [4: 5: 5: 5:]:, [4: 5: 5: 5:]:]
[[6: 5: 5: 5:]:, [6: 5: 5: 5:]:, [4: 5: 5: 5:]:, [4: 5: 5: 5:]:][[5, 6, 5, 4]
[[6: 5: 5: 5:]:, [6: 5: 5: 5:]:, [4: 5: 5: 5:]:, [4: 5: 5: 5:]:][][6:, 6:, 4:, 4:]
[[6: 5: 5: 5:]:, [4: 5: 5: 5:]:, [10: 5: 5: 5:]:]
[5hi: 5: 5: 5:]


~~~~~
//##53.c inc operations - array of ref - class local
[[6: 5: 5: 5:]:, [6: 5: 5: 5:]:, [4: 5: 5: 5:]:, [4: 5: 5: 5:]:]
[[6: 5: 5: 5:]:, [6: 5: 5: 5:]:, [4: 5: 5: 5:]:, [4: 5: 5: 5:]:][[5, 6, 5, 4]
[[6: 5: 5: 5:]:, [6: 5: 5: 5:]:, [4: 5: 5: 5:]:, [4: 5: 5: 5:]:][][6:, 6:, 4:, 4:]
[[6: 5: 5: 5:]:, [4: 5: 5: 5:]:, [10: 5: 5: 5:]:]
[5hi: 5: 5: 5:]


~~~~~
//##53.d inc operations - array of ref - class field
[[6: 5: 5: 5:]:, [6: 5: 5: 5:]:, [4: 5: 5: 5:]:, [4: 5: 5: 5:]:]
[[6: 5: 5: 5:]:, [6: 5: 5: 5:]:, [4: 5: 5: 5:]:, [4: 5: 5: 5:]:][[5, 6, 5, 4]
[[6: 5: 5: 5:]:, [6: 5: 5: 5:]:, [4: 5: 5: 5:]:, [4: 5: 5: 5:]:][][6:, 6:, 4:, 4:]
[[6: 5: 5: 5:]:, [4: 5: 5: 5:]:, [10: 5: 5: 5:]:]
[5hi: 5: 5: 5:]


~~~~~
//##53.e inc operations - array of ref - class getter setter
[[6: 5: 5: 5:]:, [6: 5: 5: 5:]:, [4: 5: 5: 5:]:, [4: 5: 5: 5:]:]
[[6: 5: 5: 5:]:, [6: 5: 5: 5:]:, [4: 5: 5: 5:]:, [4: 5: 5: 5:]:][[5, 6, 5, 4]
[[6: 5: 5: 5:]:, [6: 5: 5: 5:]:, [4: 5: 5: 5:]:, [4: 5: 5: 5:]:][][6:, 6:, 4:, 4:]
[[6: 5: 5: 5:]:, [4: 5: 5: 5:]:, [10: 5: 5: 5:]:]
[5hi: 5: 5: 5:]


~~~~~
//##53.f inc operations - array of ref - class getter only
[[6: 5: 5: 5:]:, [6: 5: 5: 5:]:, [4: 5: 5: 5:]:, [4: 5: 5: 5:]:]
[[6: 5: 5: 5:]:, [6: 5: 5: 5:]:, [4: 5: 5: 5:]:, [4: 5: 5: 5:]:][[5, 6, 5, 4]
[[6: 5: 5: 5:]:, [6: 5: 5: 5:]:, [4: 5: 5: 5:]:, [4: 5: 5: 5:]:][][6:, 6:, 4:, 4:]
[[6: 5: 5: 5:]:, [4: 5: 5: 5:]:, [10: 5: 5: 5:]:]
[5hi: 5: 5: 5:]


~~~~~
//##53.g inc operations - array of ref - class setter only
[[6: 5: 5: 5:]:, [6: 5: 5: 5:]:, [4: 5: 5: 5:]:, [4: 5: 5: 5:]:]
[[6: 5: 5: 5:]:, [6: 5: 5: 5:]:, [4: 5: 5: 5:]:, [4: 5: 5: 5:]:][[5, 6, 5, 4]
[[6: 5: 5: 5:]:, [6: 5: 5: 5:]:, [4: 5: 5: 5:]:, [4: 5: 5: 5:]:][][6:, 6:, 4:, 4:]
[[6: 5: 5: 5:]:, [4: 5: 5: 5:]:, [10: 5: 5: 5:]:]
[5hi: 5: 5: 5:]


~~~~~
//##54.a inc operations - ref of array of ref - local
[[6: 5: 5: 5:]:, [6: 5: 5: 5:]:, [4: 5: 5: 5:]:, [4: 5: 5: 5:]:]
[[6: 5: 5: 5:]:, [6: 5: 5: 5:]:, [4: 5: 5: 5:]:, [4: 5: 5: 5:]:][[5, 6, 5, 4]
[[6: 5: 5: 5:]:, [6: 5: 5: 5:]:, [4: 5: 5: 5:]:, [4: 5: 5: 5:]:][][6:, 6:, 4:, 4:]
[[6: 5: 5: 5:]:, [4: 5: 5: 5:]:, [10: 5: 5: 5:]:]
[5hi: 5: 5: 5:]


~~~~~
//##54.b inc operations - ref of array of ref - module
[[6: 5: 5: 5:]:, [6: 5: 5: 5:]:, [4: 5: 5: 5:]:, [4: 5: 5: 5:]:]
[[6: 5: 5: 5:]:, [6: 5: 5: 5:]:, [4: 5: 5: 5:]:, [4: 5: 5: 5:]:][[5, 6, 5, 4]
[[6: 5: 5: 5:]:, [6: 5: 5: 5:]:, [4: 5: 5: 5:]:, [4: 5: 5: 5:]:][][6:, 6:, 4:, 4:]
[[6: 5: 5: 5:]:, [4: 5: 5: 5:]:, [10: 5: 5: 5:]:]
[5hi: 5: 5: 5:]


~~~~~
//##54.c inc operations - ref of array of ref - class local
[[6: 5: 5: 5:]:, [6: 5: 5: 5:]:, [4: 5: 5: 5:]:, [4: 5: 5: 5:]:]
[[6: 5: 5: 5:]:, [6: 5: 5: 5:]:, [4: 5: 5: 5:]:, [4: 5: 5: 5:]:][[5, 6, 5, 4]
[[6: 5: 5: 5:]:, [6: 5: 5: 5:]:, [4: 5: 5: 5:]:, [4: 5: 5: 5:]:][][6:, 6:, 4:, 4:]
[[6: 5: 5: 5:]:, [4: 5: 5: 5:]:, [10: 5: 5: 5:]:]
[5hi: 5: 5: 5:]


~~~~~
//##54.d inc operations - ref of array of ref - class field
[[6: 5: 5: 5:]:, [6: 5: 5: 5:]:, [4: 5: 5: 5:]:, [4: 5: 5: 5:]:]
[[6: 5: 5: 5:]:, [6: 5: 5: 5:]:, [4: 5: 5: 5:]:, [4: 5: 5: 5:]:][[5, 6, 5, 4]
[[6: 5: 5: 5:]:, [6: 5: 5: 5:]:, [4: 5: 5: 5:]:, [4: 5: 5: 5:]:][][6:, 6:, 4:, 4:]
[[6: 5: 5: 5:]:, [4: 5: 5: 5:]:, [10: 5: 5: 5:]:]
[5hi: 5: 5: 5:]


~~~~~
//##54.e inc operations - ref of array of ref - class getter setter
[[6: 5: 5: 5:]:, [6: 5: 5: 5:]:, [4: 5: 5: 5:]:, [4: 5: 5: 5:]:]
[[6: 5: 5: 5:]:, [6: 5: 5: 5:]:, [4: 5: 5: 5:]:, [4: 5: 5: 5:]:][[5, 6, 5, 4]
[[6: 5: 5: 5:]:, [6: 5: 5: 5:]:, [4: 5: 5: 5:]:, [4: 5: 5: 5:]:][][6:, 6:, 4:, 4:]
[[6: 5: 5: 5:]:, [4: 5: 5: 5:]:, [10: 5: 5: 5:]:]
[5hi: 5: 5: 5:]


~~~~~
//##54.f inc operations - ref of array of ref - class getter only
[[6: 5: 5: 5:]:, [6: 5: 5: 5:]:, [4: 5: 5: 5:]:, [4: 5: 5: 5:]:]
[[6: 5: 5: 5:]:, [6: 5: 5: 5:]:, [4: 5: 5: 5:]:, [4: 5: 5: 5:]:][[5, 6, 5, 4]
[[6: 5: 5: 5:]:, [6: 5: 5: 5:]:, [4: 5: 5: 5:]:, [4: 5: 5: 5:]:][][6:, 6:, 4:, 4:]
[[6: 5: 5: 5:]:, [4: 5: 5: 5:]:, [10: 5: 5: 5:]:]
[5hi: 5: 5: 5:]


~~~~~
//##54.g inc operations - ref of array of ref - class setter only
[[6: 5: 5: 5:]:, [6: 5: 5: 5:]:, [4: 5: 5: 5:]:, [4: 5: 5: 5:]:]
[[6: 5: 5: 5:]:, [6: 5: 5: 5:]:, [4: 5: 5: 5:]:, [4: 5: 5: 5:]:][[5, 6, 5, 4]
[[6: 5: 5: 5:]:, [6: 5: 5: 5:]:, [4: 5: 5: 5:]:, [4: 5: 5: 5:]:][][6:, 6:, 4:, 4:]
[[6: 5: 5: 5:]:, [4: 5: 5: 5:]:, [10: 5: 5: 5:]:]
[5hi: 5: 5: 5:]


~~~~~
//##55 intermission, funcs lock types now
454$454$454$12

~~~~~
//##56.1. nested refs
[8, 2, 7:[5, 7]]

~~~~~
//##56.2. nested refs - double check module level
[8, 2, 7:[5, 7]]

~~~~~
//##56.3. nested refs x2 - three levels 
[10, [10:, 8:], [10, 8], [10:, 8:]]

~~~~~
//##57. refs to lambdas
[[hi3500, hi3500, hi3500, hi3507], [new one, new one, new one, new one, new one==new one, true, hi3500==hi3507, false]]

~~~~~
//##57.b refs to lambdas - as funrefs
[hi3500, hi3500, hi3500, hi3500]

~~~~~
//##58. double refs
null, null

~~~~~
//##59. module level ref init correctly
8

~~~~~
//##60. basic double ref set
[8, 8]

~~~~~
//##60.a basic double initial
[8, 8]

~~~~~
//##60.b basic double initial - already ref on rhs so dont wipe it out
8

~~~~~
//##61. two level init
[8, 8]

~~~~~
//##61.b two level init -keep ref
[8::, 8::]

~~~~~
//##61.c two level init -keep ref new
[8::, 8::]

~~~~~
//##61.d two level init -keep ref new
[8, 8]

~~~~~
//##61.e two level init -keep ref new
[8::, 8::]

~~~~~
//##61.f reassign
9::

~~~~~
//##62. explicit cast
8::

~~~~~
//##63. up ref
[8::, 8::, 8::, 8::]

~~~~~
//##64. down ref
[8:, 8:, 8:, 8:]

~~~~~
//##64.a down ref - simple 1
8:

~~~~~
//##64.a down ref - simple 2
8:

~~~~~
//##64.a down ref - note 3
[8:, 8:]

~~~~~
//##64.a down ref - simple 3
8:

~~~~~
//##64.a down ref - note 2
8:true,true

~~~~~
//##65. explicit cast
[8::, 8:, 8::]~[true, true, true]

~~~~~
//##66. implicit cast
[true, true, true, true]
[8::, 8:, 8::, 8::]

~~~~~
//##66.b implicit cast
[int:, int:]~[int::, int::]
[int:, int:]~[int::, int::]

~~~~~
//##66.c implicit cast
8:

~~~~~
//##67. minor details
8::

~~~~~
//##68. ensure that unrefing occurs to correct levels on multirefs
[8::, 8:, 8]

~~~~~
//##69. gets correctly on multilevels
[true, true]

~~~~~
//##70.a double get -prelem
true

~~~~~
//##70.b double get 
true

~~~~~
//##71. minor point
true

~~~~~
//##72. minor point - simple constr set
[7, 7, 7:, 7:]

~~~~~
//##73. implicit up and down
called8::called8:

~~~~~
//##73. explicit up and down
[8::, 8:]

~~~~~
//##73.b explicit up and down - change type -> sub to sup
8:8::

~~~~~
//##73.c explicit up and down - change type -> sup to sub
8:8::

~~~~~
//##73.d explicit up and down - change type -> sup to sub - initial null
[java.lang.Number:: cannot be cast to java.lang.Integer:, java.lang.Number: cannot be cast to java.lang.Integer::]

~~~~~
//##73.e explicit up and down - change type -> sub to sup- initial null
[ok, ok]

~~~~~
//##74. spot check
8:

~~~~~
//##74.a conv correctly sup to sub
[ok, java.lang.Double: cannot be cast to java.lang.Integer:]

~~~~~
//##74.b conv correctly sup to sub - alternate form
[ok, java.lang.Double: cannot be cast to java.lang.Integer:]

~~~~~
//##75. another thing to check
[8:, 8:, 8:, 8:]

~~~~~
//##76. minor syntax problem that doesnt belong here meh
[9, 9, 68.0]

~~~~~
//##77. so, youve manged to wrap up a ref inside an object
8::~8::=>true

~~~~~
//##78. double check
8

~~~~~
//##79. double check 2
5 true

~~~~~
//##80. to number is ok
8::~8:

~~~~~
//##81. module level fields
Hello world[5::, 5::]

~~~~~
//##81. clsas level fields
Hello world[5::, 5::, 5::]

~~~~~
//##81.a class fields
Hello world[12::, 13::]

~~~~~
//##81.b class fields more
[12, mate, 12, mate]
[12, mate, 12, mate]
[mate]~[12::, mate::, 12::, mate::]
[12::, mate::, 12::, mate::]
[mate::]

~~~~~
//##81.a module field correct behavour - simple case
[newValue, newValue2]
[initial, initial2]
[newValue::, initial2]

~~~~~
//##81.1.a module field correct behavour
[initial, newValue::, newValue]
[initial, initial::, newValue]
[initial, initial::, newValue]
[initial, newValue::, newValue]
[initial, initial::, more stuff, more stuff]

~~~~~
//##81.1.b module field correct behavour - with int
[1, 2::, 2]
[1, 1::, 2]
[1, 1::, 2]
[1, 1::, 2]
[1, 1::, 3, 3]

~~~~~
//##81.1 module field correct behavour - as mod
[initial, newValue::, newValue]
[initial, initial::, newValue]
[initial, initial::, newValue]
[newValue, newValue::, newValue]
[initial, initial::, more stuff, more stuff]

~~~~~
//##81.2 module field correct behavour - as mod int
[1, 2::, 2]
[1, 1::, 2]
[1, 1::, 2]
[2, 2::, 2]
[1, 1::, 3, 3]

~~~~~
//##81. sneak in double check
newValue: newValue::

~~~~~
//##81.1 as module fields in clinit
[initial, newValue::, newValue]
[initial, initial::, newValue]
[initial, initial::, newValue]
[initial, newValue::, newValue]
[initial, initial::, more stuff, more stuff]

~~~~~
//##81.2 as module fields in clinit - as int
[1, 2::, 2]
[1, 1::, 2]
[1, 1::, 2]
[1, 2::, 2]
[1, 1::, 3, 3]

~~~~~
//##81.2 as class fields direct access - ext
[initial, newValue::, newValue::]
[initial, initial::, newValue::]
[initial, initial::, newValue::]
[initial, newValue::, newValue::]
[initial, initial::, more stuff::, more stuff]

~~~~~
//##81.2.b as class fields direct access - ext as int
[1, 2::, 2::]
[1, 1::, 2::]
[1, 1::, 2::]
[1, 2::, 2::]
[1, 1::, 3::, 3]

~~~~~
//##81.3 as class fields direct access - internal
[initial, newValue::, newValue::]
[initial, initial::, newValue::]
[initial, initial::, newValue::]
[initial, newValue::, newValue::]
[initial, initial::, more stuff::, more stuff]

~~~~~
//##81.3.b as class fields direct access - internal - int
[1, 2::, 2::]
[1, 1::, 2::]
[1, 1::, 2::]
[1, 2::, 2::]
[1, 1::, 3::, 3]

~~~~~
//##82. overwrite n level ref - module
2 true
2 true

~~~~~
//##82.b overwrite n level ref - local
2 true
2 true

~~~~~
//##82.CC overwrite n level ref - sneak this in here
2 true

~~~~~
//##82.c overwrite n level ref - class var
2 true
2 true

~~~~~
//##82.d overwrite n level ref - class priv get set
2 true
2 true

~~~~~
//##83. simple inc
[6::, 6::, 4::, 4::]

~~~~~
//##83.b simple inc - with assignment
[6::, 6::, 4::, 4::][[5, 6, 5, 4]

~~~~~
//##83.c simple inc - with assignment - copy refs correctly
[6::, 6::, 4::, 4::][][6::, 6::, 4::, 4::][true, true, true, true]

~~~~~
//##83.d simple inc - in place operations
[6::, 4::, 10::]

~~~~~
//##83.d simple inc - string concatenation
5hi

~~~~~
//##83. inc operations - module level
[6::, 6::, 4::, 4::]
[6::, 6::, 4::, 4::][[5, 6, 5, 4]
[6::, 6::, 4::, 4::][][6::, 6::, 4::, 4::][true, true, true, true]
[6::, 4::, 10::]
5hi


~~~~~
//##83. inc operations - class inner level
[6::, 6::, 4::, 4::]
[6::, 6::, 4::, 4::][[5, 6, 5, 4]
[6::, 6::, 4::, 4::][][6::, 6::, 4::, 4::][true, true, true, true]
[6::, 4::, 10::]
5hi
[6::, 6::, 4::, 4::]


~~~~~
//##83. inc operations - class field
[6::, 6::, 4::, 4::]
[6::, 6::, 4::, 4::][[5, 6, 5, 4]
[6::, 6::, 4::, 4::][][6::, 6::, 4::, 4::][true, true, true, true]
[6::, 4::, 10::]
5hi


~~~~~
//##83. inc operations - class field get set
[6::, 6::, 4::, 4::]
[6::, 6::, 4::, 4::][[5, 6, 5, 4]
[6::, 6::, 4::, 4::][][6::, 6::, 4::, 4::][true, true, true, true]
[6::, 4::, 10::]
5hi


~~~~~
//##83. inc operations - class field get only
[6::, 6::, 4::, 4::]
[6::, 6::, 4::, 4::][[5, 6, 5, 4]
[6::, 6::, 4::, 4::][][6::, 6::, 4::, 4::][true, true, true, true]
[6::, 4::, 10::]
5hi


~~~~~
//##83. inc operations - class field set only
[6::, 6::, 4::, 4::]
[6::, 6::, 4::, 4::][[5, 6, 5, 4]
[6::, 6::, 4::, 4::][][6::, 6::, 4::, 4::][true, true, true, true]
[6::, 4::, 10::]
5hi


~~~~~
//##84. baby steps fix postfix ops - 1
[5, 6, 6]

~~~~~
//##84. baby steps fix postfix ops - 2
[5, 51]

~~~~~
//##84. baby steps fix postfix ops - 3
[12::, 6:, 7, 12::, 12:, 11, 12]

~~~~~
//##84. baby steps fix postfix ops - 4
[51111111::, 51111111:, 511, 51111111::, 51111111:, 5111111, 51111111]

~~~~~
//##84. baby steps fix postfix ops - 5
[51111111::, 51:, 511, 51111111::, 51111111:, 5111111, 51111111]

~~~~~
//##85. int is subtype of int: on ret
7:

~~~~~
//##383. simple inc
[8:::, 61:::, 6:::, 4:::, 4:::]

~~~~~
//##383.b simple inc - with assignment
[6:::, 6:::, 4:::, 4:::][[5, 6, 5, 4]

~~~~~
//##383.c simple inc - with assignment - copy refs correctly
[6:::, 6:::, 4:::, 4:::][][6:::, 6:::, 4:::, 4:::][true, true, true, true]

~~~~~
//##383.d simple inc - in place operations
[6:::, 4:::, 10:::]

~~~~~
//##383.d simple inc - string concatenation
5hi

~~~~~
//##383. inc operations - module level
[6:::, 6:::, 4:::, 4:::]
[6:::, 6:::, 4:::, 4:::][[5, 6, 5, 4]
[6:::, 6:::, 4:::, 4:::][][6:::, 6:::, 4:::, 4:::][true, true, true, true]
[6:::, 4:::, 10:::]
5hi


~~~~~
//##383. inc operations - class inner level
[6:::, 6:::, 4:::, 4:::]
[6:::, 6:::, 4:::, 4:::][[5, 6, 5, 4]
[6:::, 6:::, 4:::, 4:::][][6:::, 6:::, 4:::, 4:::][true, true, true, true]
[6:::, 4:::, 10:::]
5hi
[6:::, 6:::, 4:::, 4:::]


~~~~~
//##383. inc operations - class field
[6:::, 6:::, 4:::, 4:::]
[6:::, 6:::, 4:::, 4:::][[5, 6, 5, 4]
[6:::, 6:::, 4:::, 4:::][][6:::, 6:::, 4:::, 4:::][true, true, true, true]
[6:::, 4:::, 10:::]
5hi


~~~~~
//##383. inc operations - class field get set
[6:::, 6:::, 4:::, 4:::]
[6:::, 6:::, 4:::, 4:::][[5, 6, 5, 4]
[6:::, 6:::, 4:::, 4:::][][6:::, 6:::, 4:::, 4:::][true, true, true, true]
[6:::, 4:::, 10:::]
5hi


~~~~~
//##383. inc operations - class field get only
[6:::, 6:::, 4:::, 4:::]
[6:::, 6:::, 4:::, 4:::][[5, 6, 5, 4]
[6:::, 6:::, 4:::, 4:::][][6:::, 6:::, 4:::, 4:::][true, true, true, true]
[6:::, 4:::, 10:::]
5hi


~~~~~
//##383. inc operations - class field set only
[6:::, 6:::, 4:::, 4:::]
[6:::, 6:::, 4:::, 4:::][[5, 6, 5, 4]
[6:::, 6:::, 4:::, 4:::][][6:::, 6:::, 4:::, 4:::][true, true, true, true]
[6:::, 4:::, 10:::]
5hi


~~~~~
//##384. baby steps fix postfix ops - 1
[5, 6, 6]

~~~~~
//##384. baby steps fix postfix ops - 2
[5, 51]

~~~~~
//##384. baby steps fix postfix ops - 3
[12:::, 6:, 7, 12:::, 12:, 11, 12]

~~~~~
//##384. baby steps fix postfix ops - 4
[51111111:::, 51111111:, 511, 51111111:::, 51111111:, 5111111, 51111111]

~~~~~
//##384. baby steps fix postfix ops - 5
[51111111:::, 51:, 511, 51111111:::, 51111111:, 5111111, 51111111]

~~~~~
//##400. tidy - presever nested ref structure, dont replace
[true, true, true]

~~~~~
//##400.b tidy - presever nested ref structure, dont replace
[true, true, true]

~~~~~
//##401. tidy against all odds
[6:, 6::, 6:::, 6:, 6::, 6:::]

~~~~~
//##401.b tidy against all odds - str
[6:, 6::, 6:::, 6:, 6::, 6:::]

~~~~~
//##402. some more tidy of the monster
[6:::, 55:::, 67:::]

~~~~~
//##403. some more tidy of the monster
[false, true]

~~~~~
//##404. some more tidy of the monster
[1:, 1, 1:]

~~~~~
//##405. some more tidy of the monster
1

~~~~~
//##406. some more tidy of the monster
[8, 8, 8]

~~~~~
//##407.mmonster - double ref level array extract
[9::, 3::]

~~~~~
//##408. dunno what this test proves
[5, 5]

~~~~~
//##408.b dunno what this test proves
[[1, 2, 3, 4, 5, 6, 7, 8, 9, 10]]

~~~~~
//##408.c dunno what this test proves
[1, [1, 2, 3, 4, 5, 6, 7, 8, 9, 10]]

~~~~~
//##409. explainable behavour
[5:: 23: 456:]=>true[true, 23]

~~~~~
//##410. check ref setting
[[true], [true], [true], [true], [true], [true]]

~~~~~
//##411. dunno what this proves
[9, 9, true, true]

~~~~~
//##412. remove uneeded dup
hi

~~~~~
//##413. dont throw exception on null instsanceof check
(true, 9.0)

~~~~~
//##413. dont accidentally create a new ref
[true, true]

~~~~~
//##414. cast edge cases
[false, false, true, true]

~~~~~
//##415.a - taken from scope and type checks - 42.b no refied types thus following cast IS possible
[true, true]

~~~~~
//##415.b - taken from scope and type checks - 42.c no refied types thus following cast IS possible - null not ref
[false, true]

~~~~~
//##415.c - taken from scope and type checks - 42.c no refied types thus following cast IS possible - null not ref MORE
[false, true, false, true, false, true]

~~~~~
//##416. exception cases
{}
java.lang.ClassCastException: java.util.HashMap[java.lang.String, java.lang.Integer]: cannot be cast to java.lang.Integer:
java.lang.ClassCastException: java.util.HashMap[_, _] cannot be cast to java.lang.Integer:

~~~~~
//##417a. from sac - 43. cast checks -sac - bc
[
ok, 
ok, 
ok, 
ok]
==>9

~~~~~
//##418. double check break at mod vs local var level
[true, true, true]

~~~~~
//##419. double check ref creation type
false

~~~~~
//##420. ensure generic type chosen correctly on refs
[true, true][4.0:, 4:]

~~~~~
//##421. avoid accidental double ref
ok
ok -> [empty, empty]

~~~~~
//##422. dont blow up when casting ref on null obj
null:->null

~~~~~
//##423. null ref
[true, true, true, true, true]

~~~~~
//##424. null ref part 2
[null, null]

~~~~~
//##425. npe gets thrown as expected
npe expected ok

~~~~~
//##425. when passing a null to something expecting a ref, consider it of the wanted type
its fine no npe thrown=> null

~~~~~
//##426. ensure that field setter doesnt invent any extra code
its fine no npe thrown x2

~~~~~
//##427. ensure refs get created correctly
[true, true, true, true]

~~~~~
//##428. minor bug fix on referencings for multilevel
its fine no npe thrown x2

~~~~~
//##429.a up down refing - on new
[[8:::, 8:::]=>[8:, 8:], [true, true]=>[true, true], [false, false]=>[false, false]]

~~~~~
//##429.b up down refing - on assign existing
[true, true, true]
[true, true, true]
[true, true, true]

~~~~~
//##430. on assign existing ensure ref levels ok
[90:, 90:, 90:, 91:, true:]

~~~~~
//##431. misc - dunno how what this does something about null maybe
9=>9:

~~~~~
//##432. ensure no arbitary creation of refs going on
[true, true, true]

~~~~~
//##433. double check that u cannot stick a String into an int ref etc
[18, 18, ok exception expected]

~~~~~
//##434. ensure that ClassCastException tacktrace just refers to conc code
x434ensurethatClassCastExceptiontacktracejustreferstoconccode$Globals$.doings(x434ensurethatClassCastExceptiontacktracejustreferstoconccode.conc:12)

~~~~~
//##435. there is special logic to ensure that Object to refType: doesnt blow up
orig:

~~~~~
//##436. another thing to clean up
[true, true]

~~~~~
//##437. ref with generics
9=>9:

~~~~~
//##500. oops sort out isolation - mutable obj
[true, true, true]

~~~~~
//##501. oops sort out isolation - simple immutables 
3, 3:

~~~~~
//##501. oops sort out isolation - simple immutables - double 
3, 3:

~~~~~
//##502. oops sort out isolation - array inc
[99, 2, 3], [99, 2, 3]:

~~~~~
//##503. oops sort out isolation - array inc - double
[99, 2, 3], [99, 2, 3]:

~~~~~
//##504. oops sort out isolation - array str concat
[[_98, _], [_98, _]:, [_98, _], [_98, _]:]

~~~~~
//##505. oops sort out isolation - array postfix
[[2, 1], [2, 1]:, [2, 1], [2, 1]:]

~~~~~
//##506. oops sort out isolation - array prefix
[[2, 1], [2, 1]:, [2, 1], [2, 1]:]

~~~~~
//##507. oops sort out isolation - array postfix with ret
[[2, 1], [2, 1]:, [2, 1], [2, 1]:]->[1, 1]

~~~~~
//##508. oops sort out isolation - array prefix with ret
[[2, 1], [2, 1]:, [2, 1], [2, 1]:]->[2, 2]

~~~~~
//##509. oops sort out isolation - all variants with list
[[[99, 1], [99, 1]:, [99, 1], [99, 1]:], 
, [[_98, _], [_98, _]:, [_98, _], [_98, _]:], 
, [[2, 1], [2, 1]:, [2, 1], [2, 1]:], 
, [[2, 1], [2, 1]:, [2, 1], [2, 1]:], 
, [[2, 1], [2, 1]:, [2, 1], [2, 1]:]->[1, 1], 
, [[2, 1], [2, 1]:, [2, 1], [2, 1]:]->[2, 2]]

~~~~~
//##510. oops sort out isolation - all variants with map
[[{0->99, 1->2}, {0->99, 1->2}:, {0->99, 1->2}, {0->99, 1->2}:], 
, [{0->_98, 1->_}, {0->_98, 1->_}:, {0->_98, 1->_}, {0->_98, 1->_}:], 
, [{0->2, 1->2}, {0->2, 1->2}:, {0->2, 1->2}, {0->2, 1->2}:], 
, [{0->2, 1->2}, {0->2, 1->2}:, {0->2, 1->2}, {0->2, 1->2}:], 
, [{0->2, 1->2}, {0->2, 1->2}:, {0->2, 1->2}, {0->2, 1->2}:]->[1, 1], 
, [{0->2, 1->2}, {0->2, 1->2}:, {0->2, 1->2}, {0->2, 1->2}:]->[2, 2]]

~~~~~
//##511. explicit ref creation behavour
[8, 8, 8, 8, 8]

~~~~~
//##512. array ref setting tricky
[[55::, 5::], [12:, 5:], [5hi, 5], [hi:, 5hialso::], [99, 101]]

~~~~~
//##513.a check array refs again - simple
[[99 109 -89 99 99 109 100 100], [hi 10hi 10hithere hithere], [99.0 109.0 -89.0 100.0 99.0 99.0 109.0]]

~~~~~
//##513.b check array refs again - ref versions
[[99:, 109:, -89:, 99:, 99:, 109:, 100:, 100:], [hi:, 10hi:, 10hithere:, hithere:], [99.0:, 109.0:, -89.0:, 100.0:, 99.0:, 99.0:, 109.0:], [99:, 99:]]

~~~~~
//##513.a.a check array refs again - simple - on a ref array
[99, 109, -89, 99, 99, 109, 100, 100]

~~~~~
//##513.b.b check array refs again - ref versions - on a ref array
[99:, 109:, -89:, 99:, 99:, 109:, 100:, 100:]

~~~~~
//##514. check ref upgrade downgrade correct
[9, 9, 9, 9, 9, 9, 9, 9, 9, 9]

~~~~~
//##514.b creation via iso check implicit upgrade working ok
[[8, true, true], [8, true, true]]

~~~~~
//##515. upcast fail case - modules
[55, 55, 55][55:::, 55:::, 55:::]

~~~~~
//##516. upcast fail case - classes
[55, 55, 55][55:::, 55:::, 55:::]

~~~~~
//##517. assign array ref
[9, 9, 9, 9]->[9:::, 9:::, 9:::, 9:::]

~~~~~
//##517.b assign array ref - thing itself is a ref
[9, 9, 9, 9]->[9:::, 9:::, 9:::, 9:::]

~~~~~
//##517.c assign array ref - correct upcasting where needed
[9, 9, 9, 9]->[9:::, 9:::, 9:::, 9:::]

~~~~~
//##517.d assign array ref - array set on inc ops
[9, 9, 100, 0]->[9:::, 9:::, 100:::, 0:::]->onehi

~~~~~
//##517.e assign array ref - pre postfix
[1, -1, 1, -1]->[1:::, -1:::, 1:::, -1:::]

~~~~~
//##517.f assign array ref - pre postfix
[1, -1, 1, -1]->[1:::, -1:::, 1:::, -1:::]~[0, 0, 1, -1]

~~~~~
//##518.a list ref - assignment
[9, 9, 9, 9]->[9:::, 9:::, 9:::, 9:::]

~~~~~
//##518.b list ref - in place increment
[9, 9, 100, 0]->[9:::, 9:::, 100:::, 0:::]

~~~~~
//##518.c list ref - str concat multilevel
[one9]->[one9:::]

~~~~~
//##519.a map ref - assignemnt
[9, 9, 9, 9]->{0->9:::, 1->9:::, 2->9:::, 3->9:::}

~~~~~
//##519.b map ref - in place
[9, 9, 100, 0]->{0->9:::, 1->9:::, 2->100:::, 3->0:::}

~~~~~
//##519.c map ref - str concat
[09, 09]->{0->09:::, 1->09:::}

~~~~~
//##520.1 pre postfix multilevel - var 
[1, -1, 1, -1][0, 0, 1, -1][1, -1, 1, -1]

~~~~~
//##520.2 pre postfix multilevel - map
[{0->1:::}, {0->-1:::}, {0->1:::}, {0->-1:::}][0, 0, 1, -1][{0->1:::}, {0->-1:::}, {0->1:::}, {0->-1:::}]

~~~~~
//##520.3 pre postfix multilevel - list
[[1:::], [-1:::], [1:::], [-1:::]][0, 0, 1, -1][[1:::], [-1:::], [1:::], [-1:::]]

~~~~~
//##521. fix this syntax problem later
should be oks8

~~~~~
//##522. special case to handle waiting on refs of refs on creation
[8, 8, 8]

~~~~~
//##523. ensure local array assignment can be performned
[10:, 2:]

~~~~~
//##524. check that identity hash map can be iterated correctly without npe
true

~~~~~
//##525. ensure correct type inferance for refs when subtypes are possible
[[99.0: 99:], 99.0, 99, true]

~~~~~
//##526. no inf loop
works 6

~~~~~
//##527. conversion is ok
[me not ref, me not ref]

~~~~~
//##527. ensure return as ref not local
me not ref::

~~~~~
//##195 ensure mulitlevel return type chosen correctly	
[me not ref::, me not ref::][me not ref::, me not ref::]

~~~~~
//##196 3 refs down should be ok
[me not ref, me not ref]

~~~~~
//##197 check type set to refs
failed as expected: java.lang.ClassCastException: java.lang.Integer cannot be cast to java.lang.String

~~~~~
//##198 was playing up before
it's done: [50, 50, 52, 52]

~~~~~
//##199 def with arrays
[9, 9]

~~~~~
//##200 in the past lambda creation was failing
[me not ref::, me not ref::]

~~~~~
//##201 locking of refs ignored for prim operations - plus
[got: [454, 455, 455, 454, 454], got: [454, 455, 455, 454, 454]]

~~~~~
//##202 locking of refs ignored for prim operations - gt lt etc
[got: [true, true], got: [true, true]]

~~~~~
//##203 locking of refs ignored for prim operations - eq
[got: [true, true], got: [true, true]]

~~~~~
//##204 locking of refs ignored for prim operations - ref eq
[true, true, false, false]

~~~~~
//##205 locking of refs ignored for prim operations - and or
[true:, true:, true:, true:]

~~~~~
//##206 locking of refs ignored for prim operations - pow
[4, 4, 4]

~~~~~
//##207 pre and postfix ops on maps
[{0->25::}, [25::]]

~~~~~
//##208 mul ops etc on maps with explicit and non explicit def
[{0->46::}, {0->46::}, [46::], [46::]]

~~~~~
//##209 ensure that we can call non ref functions even when we're locked
[10, 10]

~~~~~
//##210 ensure that we can call non ref functions even when we're locked v2
[10, 10]

~~~~~
//##211 ensure that we can call non ref functions even when we're locked v3
[[10, 12, 10], [10, 12, 10]]

~~~~~
//##212 double check onchange ret of values
[false, 99, 100]

~~~~~
//##z213 failed before odd
[false, 99, 10, 66]

~~~~~
//##z214 pre does get ini
[100, 10, 66]

~~~~~
//##z215 async pre can ret
[100, 99, 66]

~~~~~
//##z216 onchange no ret
[100, 100, 66]

~~~~~
//##z217 ret in post block
[50, 101, 66]

~~~~~
//##z218 things non ret are not forced
[101, 101, 66]

~~~~~
//##z219 ensure that length on local arry can be called when the local array is a class field
[1]

~~~~~
//##z220 ensure local vars for for loops are captured for splicing
360

~~~~~
//##z221 double level ref array extract
[99, 99]

~~~~~
//##z222 double level ref array itr
[[[99:], [2:]], [[99:]]]

~~~~~
//##z223 ensure fields and funcs callable from refs etc
[hi, la thing, la thing]

~~~~~
//##z224 sync blocks
360

~~~~~
//##z225 sync blocks nested
[360 9999]

~~~~~
//##z226 sync blocks may return stuff just like normal blocks
[360 100]

~~~~~
//##z227 sync blocks can ret
[cool, hi, java.lang.Exception: xxx]

~~~~~
//##z228 to ref ref
[me not ref::, me not ref2::]

~~~~~
//##z229 ref tidy up
12:

~~~~~
//##z230 nice little thing which works ok now
[[12:], [5:]]

~~~~~
//##z231 ref bytecode
[1:, hi:][1, ok, err as epected, nice err: java.lang.Integer cannot be cast to java.lang.String]

~~~~~
//##z233 ref type create local
9:

~~~~~
//##z234 fixed bug in ref array where type array not created correctly - check instanceof
[true, true]

~~~~~
//##z235 fixed bug in ref array where type array not created correctly - check cast to a ref
[2:]:

~~~~~
//##z236 fixed bug in ref array where type array not created correctly - was missing cast
[[2:], [2:]]

~~~~~
//##z237 type extraction from for and while loops
[[[12, 12, 12], [12, 12], [12, 12, 12], [12, 12]], 
, [[12:, 12:, 12:], [12:, 12:], [12:, 12:, 12:], [12:, 12:]], 
, [[13, 13, 13], [13, 13]], 
, [[13:, 13:, 13:], [13:, 13:]]]

~~~~~
//##z238 to add get and set to ref types need support on arbitary classes
[one, one, two more]

~~~~~
//##z239 as above but multilevel support
{1->fred -> {2->thekid, 3->another kid}}

~~~~~
//##z240. ensure ref writes back to ref for array ops 1
[[23, 2], [1, 2]]

~~~~~
//##z241. ensure ref writes back to ref for array ops 2
[[23 2]: [1 2]:]

~~~~~
//##z242. ensure ref writes back to ref for array ops 3
[9:, 3:, 9:]

~~~~~
//##z243. was a bug now its ok
it's done: 50

~~~~~
//##z244. was suffering from conc modi exception
[9, 123, 66, true]

~~~~~
//##z245. parfor on void ret
[ok:, ok:, ok:, ok:, ok:]

~~~~~
//##z246. parforsync on void ret
[ok:, ok:, ok:, ok:, ok:]

~~~~~
//##z247. parforsync on void ret variant 2
[[true, true, true, true, true], [true, true, true, true, true]]

~~~~~
//##z248. user defined ref
nice: true

~~~~~
//##z249. bug concerning ref in closed state
ok

~~~~~
//##z250. bug concerning ref in closed state pt 2
called: [yup, true]

~~~~~
//##z251. bug concerning ref in closed state pt 3
ok

~~~~~
//##z252. onchange every options - onlyclose
done

~~~~~
//##z253. onevery onlyclose pickup change if first is closed - array
done

~~~~~
//##z254. onevery onlyclose pickup change if first is closed - array - in place
done

~~~~~
//##z255. onevery onlyclose pickup change if first is closed - ref set
done

~~~~~
//##z256. onevery onlyclose pickup change if first is closed - list set etc
done

~~~~~
//##z257. onevery onlyclose pickup change if first is closed - indivuduals
done

~~~~~
//##z258. add item to reference set, ensure changes monitored
done

~~~~~
//##z259. add item to reference set, ensure changes monitored - close both
done

~~~~~
//##z260. add item to reference set which is already closed post start
done

~~~~~
//##z261. onlyclose on refsets for onchange variants
done

~~~~~
//##z262. regular every with bugfixes for ref set as par above
done

~~~~~
//##z263. await listen to close
ok

~~~~~
//##z264. array of refs
[1: 2:]

~~~~~
//##z265. matrix of refs
[[1: 3:] [2: 4:]]

~~~~~
//##z266. ref on its own
ok false

~~~~~
//##z267. ref on its own
ok false

~~~~~
//##z268. ref array on its own
[1: 2:]

~~~~~
//##z269. ref array on its own
[1: 2:]

~~~~~
//##z270. ref array on its own mixed
ok [false true]

~~~~~
//##z271. ref array on its own mixed matrix
[[1: 3:] [2: 4:]]

~~~~~
//##z272. ref array on its own 3d matrix init
[[[2: 2: 2:] [2: 2: 2:]] [[2: 2: 2:] [2: 2: 2:]]]

