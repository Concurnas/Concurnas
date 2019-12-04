//##1. simple translate to getter
[9, 9, 9, 9]

~~~~~
//##2. advanced translate to getter
[98, 98, 98, 98]

~~~~~
//##2b. advanced translate to getter - arrays
[88, 88, 88, 88]

~~~~~
//##3. setter - simple
[2, 99, 91]

~~~~~
//##3b. setter - simple arrays
[99, 29]

~~~~~
//##3c. setter - simple arrays rhs
[99, 299, 3]

~~~~~
//##4. setter - advanced
12

~~~~~
//##5. no setter, no getter defined
[8, 9, 9]

~~~~~
//##6. no setter, no getter if in self
1080

~~~~~
//##7. no setter, no getter if reference to self
88

~~~~~
//##7b. do setter and gett if refernce to a super or a different child
[88, 88]

~~~~~
//##7c. no setter and getter if ref via super
0

~~~~~
//##10.1. non eq setters
[1, 70][20.0]

~~~~~
//##10.1.b non eq setters - require cast to setter type
[1, 70]

~~~~~
//##10.2 advanced non eq setters
[20, 20]

~~~~~
//##10.3 advanced non eq setters via arrastuff
32

~~~~~
//##10.3b advanced non eq setters via otherarray stuff
13

~~~~~
//##10.4 inc dec operations
[12, 14, 14, 8, 6, 6]

~~~~~
//##10.4a advanced inc dec operations
[91, 100, 99]

~~~~~
//##10.4b advanced inc dec operations
[89, 10, 10]

~~~~~
//##10.5 neg should be left as a pre-post op
-10

~~~~~
//##10.6 string add
hi there

~~~~~
//##12.1 setter and getter different types - ok case
[89, 99]

~~~~~
//##12.3 setter and getter different types - increment ok case
[99, em:70hi:9926:991]

~~~~~
//##13 def with ifexpr
[89, 90]

~~~~~
//##14 field ops with cast
[138, 16202, 88, 90]

~~~~~
//##14.b field ops with cast Int -> Int
[138, 16202, 88, 90]

~~~~~
//##14.c field ops with cast Int -> int
[138, 16202, 88, 90]

~~~~~
//##14.d field ops with cast int -> int
[138, 16202, 88, 90]

~~~~~
//##14.e with str
[902, 902]

~~~~~
//##15. ensure that overrite occurs even when doing nested access
3

~~~~~
//##7bb. call method setter getter on field of type superclass
99