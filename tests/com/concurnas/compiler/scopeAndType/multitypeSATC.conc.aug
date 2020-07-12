//##1. this one is ok
def plusTen(a int|double)  => a + 10

def doings(){
	"" + [""+plusTen(10.), ""+plusTen(10)]
}

~~~~~
//##2. cannot be used outside of func signautre
def plusTen(a int|double)  => a + 10

nope int|double = 88//not definable here of like this 

def doings(){
	"" + [""+plusTen(10.), ""+plusTen(10)]
}

~~~~~
//##3. if ret then others must
def plusTen(a int) int|double => a + 10

def doings(){
	"" 
}

~~~~~
//##4. multitype args count must match
def plusTen(a int|char|double) int|double => a + 10
def plusTen(a int|char|double, b int|char) => a + 10

def doings(){
	"" 
}

~~~~~
//##5. not for use in lambda

plusTen = def (a int|char|double)  => a + 10

def doings(){
	"" + ["" + plusTen(1), "" + plusTen(1.)]
}

~~~~~
//##6. check count for multitypes referenced in body
private typedef numericalz = short|int|long|float|double|char|byte
private typedef numericalzSub = short|int

def matmult(ax numericalz[2], b numericalz[2]){
	m1 = ax.length
	n1 = ax[0].length
	m2 = b.length
	n2 = b[0].length
	if (n1 <> m2){
		throw new RuntimeException("Illegal matrix dimensions.") 
	}
	
	c int|double[2] = new int|double[m1,n2]
	c2 numericalzSub[2] = new numericalzSub[m1,n2]
	for (i = 0; i < m1; i++){
	    for (j = 0; j < n2; j++){
	        for (k = 0; k < n1; k++){
	            c[i][j] += ax[i][k] * b[k][j]
	        }
		}
	}
	c
}

A=[1 2 3 ; 4 5 6]
B=[7 8 ; 9 10 ; 11 12]

def doings(){
	"" //+ matmult(A, B)
}
~~~~~
//##7. All arry dimensions must be qualified in order to use an element wise initialiser
def doings(){
	xxx = new int[2,3][](99)

	"" + xxx
}

