#The LessTyping Language Sepcification
#Contents
1. File Structure
	1. indentation
	2. Layer
	3. define/undef
	4. comment
	5. package and import
2. Literals
	1. number
	2. string
	3. bool
	4. array
	5. map
3. Type System
	1. Hybrid of Weak and Strong type
	2. Literal Default Types
		1. number
		2. string
		3. bool
		4. array
		5. map
	3. Requiring Type
	4. Variable/Return Type
	5. Type Definition
		1. class
		2. interface
	6. Cast
	7. Pre Defined
4. Keywords
	1. Modifiers
5. Statements
	1. (...)
	2. for
	3. while / do-while
	4. continue
	5. break
	6. if
	7. return
	8. synchronized
	9. try
	10. throw
	11. annotation
	12. method definition
	13. class definition
	14. interface defintion
	15. modifiers
6. Expressions
	1. number literals
	2. bool literals
	3. string literals
	4. variable definition
	5. invocation
	6. as
	7. access
	8. index
	9. one variable operation
	10. two variable operation
	11. assignment
	12. undefined
	13. null
	14. array expression
	15. map expression
	16. procedure
	17. lambda
	18. type

#§1 File Structure
##1.1 indentation
`LessTyping` forces 4 spaces indentation as default, which can be respecified with `Properties#_INDENTATION_` when constructing `lt::compiler::Scanner`

This _LessTyping Language Specification_ uses 4 spaces indentation when giving examples.

##1.2 Layer
Consistent lines with the same indentation are in the same `Layer`

+4 indentation starts a new Layer, and there're a few symbols defined as `Layer Starter`

* `{`
* `[`
* `(`
* `->`

the scanner is forced to create a new Layer when meets these tokens

the following tokens are meant to be appear in pairs

* `{` and `}`
* `[` and `]`
* `(` and `)`

the new layer created by `{` `[` `(` ends with `}` `]` `)`

in other circumstances the layer ends when indentation reduces.

the following graph shows how `Layer` works

		┌───────────────────────┐
		│       ┌─┐             │
		│classA(│a│):B          │
		│       └─┘             │
		│    ┌─────────────────┐│
		│    │       ┌────────┐││
		│    │method(│arg0    │││
		│    │    ┌──┘        │││
		│    │    │arg1       │││
		│    │    │arg2, arg3 │││
		│    │    └───────────┘││
		│    │):Unit           ││
		│    │    ┌────┐       ││
		│    │    │pass│       ││
		│    │    └────┘       ││
		│    └─────────────────┘│
		└───────────────────────┘

the code would be parsed into the following structure

		-[class]-[A]-[(]-[│]-[)]-[:]-[B]-[│]
		                  └[a]-    ┌──────┘
		┌──────────────────────────┘
		└────[method]-[(]-[│]-[)]-[│]-
		┌──────────────────┘       └───────[pass]-
		│
		└──[arg0]-[EndNode]-[arg1]-[EndNode]-[arg2]-[StrongEndNode]-[arg3]
		
you can consider the new layer as one token, and the contents are "filled" into the token.

##1.3 define/undef
`LessTyping` supports very simple `define` and `undef` pre processing commands.

`define` should be used with caution. The Scanner can __NOT__ provide precise error report about columes on lines where `define` has effect on, and `define` command might pollute the string literals.

`define` simply replaces the `target` with `replacement` in text level

`define` and `undef` should be the first characters of the line.

e.g.

	define "CREATE TABLE" as "class"
	define "NUMBER" as ": int"
	define "VARCHAR" as ": String"
	
	CREATE TABLE User(
	    id    NUMBER  ,
	    name  VARCHAR
	)
	
	undef "CREATE TABLE"
	undef "NUMBER"
	undef "VARCHAR"
	
##1.4 comment
single line comments start with `;`  
the characters in this line and after this symbol are ignored

	; this is a comment

multiple line comments start with `/*` and end with `*/`  
the characters between `/*` and `*/` are ignored

	/* comment */
	/*
	multiple line comment
	*/
	a/* comment */=1
	/* the comment splits an expression */
	
##1.5 package and import
package declaration should be the first statement of the file

	package lt::spec
	
the sub packages are separated with `::` instead of `.` 

In this _Specification_ , packages / types are separated with `::` , so it might writes `java::util::LinkedList`, you should known that it's the same as `java.util.LinkedList` in java.

import can appear in any position where indentation is 0

import supports importing

* all types from a package
* one specified type
* all static fields and methods from a type

e.g.

	import java::awt::_
	import java::util::List
	import java::util::Collections._
	
these import statements means import all types from package `java::awt`, and import type `java::util::List` and import all static fields and methods from `java::util::Collections._`

when trying to retrieve a type, it firstly tries to seek from import that specifies type simple name.

e.g. `List`, it's specified in `java::util::List`, so this type is `java::util::List` instead of `java::awt::List`

#§2 Literals
LessTyping supports 5 kinds of literals

1. number
2. string
3. bool
4. array
5. map

##2.1 number
number is divided into integer number and float number

e.g.

	1
	1.2
	
where `1` is an integer number, and `1.2` is a float number

##2.2 string
a string starts with `'` or `"`, and ends with same character as the _starter_.

string is divided into char string and string

e.g.

	'a string'
	"a string"
	
the escape character is `\`

e.g.

	'escape \''
	"escape \""
	
##2.3 bool
all following tokens are considered as bool value

	true
	false
	yes
	no
	
where `true` and `yes` represents logic true, while `false` and `no` represents logic false

##2.4 array
the array starts with `[` and ends with `]`, the containing elements are seperated with `,` or `NewLine`

e.g.

	[1,2,3]
	
	[
	    object1
	    object2
	    object3
	]
	
	[
	    object1,
	    object2,
	    object3
	]

##2.5 map
map starts with `{` and ends with `}`, the containing entries are separated with `,` or `NewLine`. the entry key-value is separated with `:`

e.g.

	{'a':1, 'b':2, 'c':3}
	
	{
	    'a':1
	    'b':2
	    'c':3
	}
	
	{
	    'a':1,
	    'b':2,
	    'c':3
	}
	
#§3 TypeSystem
##3.1 Hybrid of Weak and Strong type
`LessTyping` is a hybrid of weak and strong type language.

you can specify the type/return type with `:` when defining variables and methods, or use `as` to cast primitives/references.

e.g.

	num1 : int = 1
	num2 : float = 1.2
	
	method():Unit
	toString():String
	
	1 as long
	
this design let LessTyping be able to extend java classes, implement java interfaces and use any java library.

##3.2 Literal Default Types
If rquired type is not set, literals are parsed into their default values.

###3.2.1 number
numbers without a dot are parsed into `int`  
numbers with a dot are parsed into `double`

e.g. 

* `1` -- `int`
* `1.2` -- `double`
	
###3.2.2. string
only when the string is started with `'` and string length is 1, it's parsed into `char`. In other circumstances, the string is parsed into `java::lang::String`

e.g.

* `'a'` -- `char`
* `"a"` -- `java::lang::String`
* `''` -- `java::lang::String`
* `""` -- `java::lang::String`
* `'str'` -- `java::lang::String`
* `"str"` -- `java::lang::String`

###3.2.3. bool
bools are parsed into java `boolean`

###3.2.4. array
arrays are parsed into `lt::lang::List`

the `List` extends `java::util::LinkedList` and provides functions that look like `Array` in `JavaScript`

###3.2.5. map
maps are parsed into `java::util::LinkedHashMap`

##3.3 Requiring Type
if required types are set, then the literals would be parsed into corresponding types.

if the required type and literal tuple not recorded in the following table, a cast would be performed and might cause compiling error, or `java.lang.ClassCastException` at runtime.

Required Type | Literal Type   | Parsed Type or Method Invocation |
------------- | :-------------:| -------------------------------- |
int           | integer number | constant pool int                |
short   | integer number | constant pool int and convert to short |
byte    | integer number | constant pool int and convert to byte |
char    | integer number/char string | constant pool int and convert to char|
boolean       | bool           | constant pool int 1 or 0         |
long          | integer number | constant pool long               |
float         | any number     | constant pool float              |
double        | any number     | constant pool double             |
Integer       | integer number | Integer.valueOf(number as int)   |
Short         | integer number | Short.valueOf(number as short)   |
Byte          | integer number | Byte.valueOf(number as byte)     |
Character |integer number/char string| Character.valueOf(literal) |
Boolean       | bool           | Boolean.valueOf(bool)            |
Long          | integer number | Long.valueOf(number as long)     |
Float         | any number     | Float.valueOf(number as float)   |
Double        | any number     | Double.valueOf(number as double) |
String        | any string     | String constant pool             |
array type    | array          | array                            |

>reference types in the table also contain their super classes/implemented interfaces

##3.4 Variable/Return Type
the type of the variable can be specified with `:`

	variable : int
	str : String
	
	method():Unit
	toString():String
	
if it's not specified, the type will be considered as `java::lang::Object`

	variable = 1
	str = "string"
	method()
	    ...

##3.5 Type Definition
`LessTyping` supports class and interface definitions

`LessTyping` can use java libraries directly, so, other types such as `enum` and `Annotation`, can be defined in java source code

###class

here's all definitions of `class`

original type definition statement  | transformed to
:----------------------------------:|:-----------:
class ClassName                     | class ClassName()
class ClassName(parameters)         | class ClassName(parameters):java::lang::Object
class ClassName(parameters):SuperClass | class ClassName(parameters):SuperClass()
class ClassName(parameters):SuperClass(constructingArgs) |
class ClassName:SuperInterface1,SuperInterface2  |

the parameters and `SuperClassArguments` can be separated by `,` or `NewLine`

e.g.

	class User(
	    id : int
	    name : String
	)
	
	class User(id:int, name:String)
	
	class User(
	    id : int ,
	    name : String
	)
	
the parameters are considered as constructor parameters when generating jvm bytecode. and `Fields` with the same name as the parameters are generated and assigned.

the `User` class definition is the same as the following java source code

	public class User {
	    private int id;
	    private String name;
	    public User(int id, String name) {
	        this.id = id;
	        this.name = name;
	    }
	}

in `LessTyping`, the types are always modified with `public`. 

`private`, `protected`, `public`, `pkg` modifying the `class` are considered as constructor modifiers.

in `class` :

* constructors' default access modifier is `public`
* fields' default access modifier is `private`
* methods' default access modifier is `public`

###interface
here's all definitions of `interface`

* interface InterfaceName
* interface InterfaceName : SuperInterface1, SuperInterface2

in `interface` :

* fields' default access modifier is `public`
* methods' default access modifier is `public`

##3.6 Cast
`number` , `string` and `bool` literals can only be parsed into limited types, and might produce a compiling error. In other circumstances, `LessTyping` supports a large range of type casting methods when compiling and at runtime.

* `number` without dot can be `int`, `long`, `short`, `byte`, `double`, `float` and their boxing types
* `number` with dot can be `double`, `float` and their boxing types
* `string` can be `java::lang::String` or `char`
* `bool` can be java `boolean` or `Boolean`

###Compile
Compiling only supports auto boxing. other casts are done at Runtime

###Runtime
all cast for reference types to reference types are defined in `lt::lang::Lang.cast(o, targetType)`

all cast for reference types to primitive types are defined as "castToX", such as `castToInt`, `castToShort` ...

when the cast fails, a `java::lang::ClassCastException` would be thrown.

the following table show how `LessTyping` casts types

###primitives
type           | required type            | method
---------------|--------------------------|-----------
int/short/byte/char | long                | i2l
int/short/byte/char | flaot               | i2f
int/short/byte/char | double              | i2d
int/short/byte/char | bool                | box the type then cast to bool
long           | int                      | l2i
long           | short                    | l2i and i2s
long           | byte                     | l2i and i2b
long           | char                     | l2i and i2c
long           | float                    | l2f
long           | double                   | l2d
long           | bool                     | box the type then cast to bool
float          | int                      | f2i
float          | short                    | f2i and i2s
float          | byte                     | f2i and i2b
float          | char                     | f2i and i2c
float          | long                     | f2l
float          | double                   | f2d
float          | boolean                  | box the type then cast to bool
double         | int                      | d2i
double         | short                    | d2i and i2s
double         | byte                     | d2i and i2b
double         | char                     | d2i and i2c
double         | long                     | d2l
double         | float                    | d2f
double         | boolean                  | box the type then cast to bool

###auto boxing

type           | required type            | method
---------------|--------------------------|----------
int            | Integer                  | Integer.valueOf(int)
short          | Short                    | Short.valueOf(short)
byte           | Byte                     | Byte.valueOf(byte)
char           | Character                | Character.valueOf(char)
bool           | Boolean                  | Boolean.valueOf(bool)
long           | Long                     | Long.valueOf(long)
float          | Float                    | Float.valueOf(float)
double         | Double                   | Double.valueOf(double)

###auto unboxing

type           | required type            | method
---------------|--------------------------|---------
Number         | int                      | number.intValue()
Number         | short                    | number.shortValue()
Number         | byte                     | number.byteValue()
Character      | char                     | character.charValue()
Boolean        | bool                     | boolean.booleanValue()
Number         | long                     | number.longValue()
Number         | float                    | number.floatValue()
Number         | double                   | number.doubleValue()

###bool
type           | required type            | method
---------------|--------------------------|-----------
Number         | bool                     | number.doubleValue()!=0
Reference      | bool                     | o not null and o not undefined

###char
type           | required type            | method
---------------|--------------------------|-----------
Number         | char                     | number.intValue() as char
CharSequence   | char                     | length is 1 and charAt(0)

###references
type           | required type            | method
---------------|--------------------------|-----------
java::util::List | array                  | cast every element into component type
Function       | functional interface     | param length should be the same and use Proxy to generate new object
Function       | functional abstract class | param length should be the same and use `LessTyping` compiler to generate new object

##3.7 Pre Defined
###types
the following types are defined as default

* void
* Undefined

the `void` can also be written as `Unit`, and `Undefined` is defined as `lt::lang::Undefined`

`void` (or `Unit`) can only be used on method return types, which represents that the method doesn't have a return value

An `undefined` appears when trying to retrieve non-exist fields or use invocation of a `void` method as value

###values
the following values are defined as default

* null
* undefined

`null` can be assigned to any type, and `undefined` is used as a symbol of non-exist fields or `void` methods return

#§4 Keywords
all java keywords are `LessTyping` keywords :

	"abstract", "assert", "boolean", "break", "byte", "case",
	"catch", "char", "class", "const", "continue", "default",
	"do", "double", "else", "enum", "extends", "final", "finally",
	"false", "float", "for", "if", "implements", "import", "instanceof",
	"int", "interface", "long", "native", "new", "null", "package",
	"private", "protected", "public", "return", "short", "static",
	"strictfp", "throw", "true", "try", "while"
	
there're a few more keywords defined in `LessTyping` :

	"is", "not", "bool", "yes", "no", "type", "as",
	"undefined", "in", "elseif", "Unit", "data", "val"
	
note that `define` and `undef` are not keywords, they only enables if the first characters of the line is `define` or `undef`.

`boolean` is a keyword, but invalid in `LessTyping`, use `bool` instead.

write

	`valid java name`

to use those names defined in java but happend to be `LessTyping` keywords.

e.g.

	System.`in`
	
##4.1 Modifiers
`LessTyping` modifiers are almost the same `Java` modifiers, but `val` represents `final`, `pkg` and `data` are new modifiers.

`pkg` is a access modifier, it's the same with "no access modifiers" in java

`data` only modifies `class`, generates some methods to build a java bean.

	"public", "protected", "private", "pkg", "data",
	"abstract", "val", "native", "synchronized", "transient", 
	"volatile", "strictfp"
	
###access modifiers
`public` `private` `protected` `pkg` are access modifiers

At most one access modifier can exist on one object

* `public` means any member can visit this object
* `private` means only the type itself can have access to the object
* `protected` means only the type itself or it's sub-classes or types in the same package can have access to the object
* `pkg` means only types in the same package can have access to the object

###class
`abstract` `val` `data` and all access modifiers

###class parameter
`val` and all access modifiers

###interface
`abstract` and `public`

###method
`abstract` `val` and all access modifiers

###method parameter
`val`

###local variable
`val`

#§5 Statements
`LessTyping` support the following statements

1. (...)
2. for
3. while / do-while
4. continue
5. break
6. if
7. return
8. synchronized
9. throw
10. try
11. annotation
12. method definition
13. class definition
14. interface defintion

##5.1 (...)
The symbol `...` means "do nothing".

e,g.

	method()=...
	
	while b
	    ...

the statement might generate a `Nop` JVM instruction or simply do nothing.

the _Specification_ might use `...` to represent "some statements" when explaining statements

##5.2 for
`for` is a __foreach__ statement

	for variable in iterator/iterable/array/enumeration/map
	    ...
	    
if the input is

* iterator, it's the same as

		while iterator.hasNext
		    variable = iterator.next

* iterable
	
		tmp = iterable.iterator
		while tmp.hasNext
		    variable = tmp.next
			
* array

		cursor = 0
		while cursor < array.length
		    variable = array[cursor++]
		    
* enumeration

		while enumeration.hasMoreElements
		    variable = enumeration.nextElement
		    
* map

		tmp = map.entrySet.iterator
		while tmp.hasNext
		    variable = tmp.next
		    
`LessTyping` doesn't support traditional C-like for statement. Instead, use `range list` instead

##5.3 while / do-while

###while 

	while boolValue
	    ...
	    
the `while` statement executes statements only when `boolValue` can be cast to `true`
	
###do-while
	do
	    ...
	while boolValue

the `do-while` statement executes the statements at least once. only when the `boolValue` can be cast to true, the statements loops.

##5.4 continue
`continue` can only be used in `for`, `while` and `do-while`.

`continue` jumps to the end of statements in the loop

##5.5 break
`break` can only be used in `for`, `while` and `do-while`.

`break` jumps out of the loop

##5.6 if

	if boolValue1
	    ... ; stmt 1
	elseif boolValue2
	    ... ; stmt 2
	elseif boolValue3
	    ... ; stmt 3
	else
	    ... ; stmt 4
	    
* the `stmt1` executes only when `boolValue1` is true
* the `stmt2` executes only when `boolValue1` is false and `boolValue2` is true
* the `stmt3` executes only when `boolValue1` and `boolValue2` are false and `boolValue3` is true
* the `stmt4` executes only when `boolValue1`, `boolValue2` and `boolValue3` are false

`elseif` and `else` can be omitted

`elseif` can NOT appear after `else`

##5.7 return

	return
	; or
	return aValue

Methods whose return types are `void` (also known as `Unit`) can NOT return with a value.

the `return` can be omitted.

for those methods with primitive return type, return the primitive type's default value e.g. `int 0` `bool false`

for those methods with reference return type, return `null`

the return value of `void` method is `undefined` in `LessTyping`, but it won't return anything in `Java`.

##5.8 synchronized

	synchronized(m1, m2, m3, ...)
	    ...
	    
the current thread retrieve the lock of object `m1`, `m2` and `m3`

and they are released when the execution of statements is finished.

##5.9 throw

	throw anyObject
	
`LessTyping` allow you to throw any object. The objects whose types are not sub-class of `Throwable` are filled into `lt::lang::Wrapper`, and can be retrieved with it's `public` field : `object`. So if `LessTyping` methods are called in `Java`, catch `lt.lang.Wrapper` to retrieve the wrapped objects.

When catching exceptions in `LessTyping`, the wrapped object is automatically retrieved and assigned to the exceptionVariable

##5.10 try

	try
	    ... ; A
	catch exceptionVariable
	    ... ; B
	finally
	    ... ; C
	    
Execute code `A`, if any exceptions thrown, goto code `B`

and `C` will always execute

the `catch` or `finally` can be omitted.

NOTE THAT the caught variable can be __ANY__ type. 

e.g.

	try
	    throw "an exception"
	catch e
	    println(e)

the variable `e` is `java::lang::String`

##5.11 annotation

	@Annotation ; same as @Annotation()
	@Annotation(v) ; same as @Annotation(value=v)
	@Annotation(k1=v1, k2=v2, ...)
	
define an annotation instance and present on the target below

	@Override
	toString():String=''
	
	@FunctionalAbstractClass
	abstract class Func
	    abstract apply()=...
	    
##5.12 method invocation
There're multiple ways of defining a method

1. noraml

		method(params)
		    ...

	the method's return type is `java::lang::Object`

2. type spec

		method(params):Type
		    ...

3. type spec one statement

		method(params):Type=expression
		
	the method returns the expression
	
4. one statement

		method(params)=expression
		
	the method's return type is `java::lang::Object` and returns the expression
	
NOTE THAT `abstract` method should be defined as :

	abstract method(params)=...
	; or
	abstract method(params):Type=...
	
if the `one statement` method's expression is `...`, it means the method body is empty

##5.13 class definition
The class definitions can be found in chapter 3.5

	class ClassName
	class ClassName(params)
	class ClassName(params):ParentClass
	class ClassName(params):ParentClass(args)
	class ClassName(params):ParentClass(args),Interface1,Interface2

##5.14 interface defintion
The interface definitions can be found in chapter 3.5

	class InterfaceName
	class InterfaceName:SuperInterface1,SuperInterface2

#§6 Expressions
`LessTyping` supports the following expressions

1. number literals
2. bool literals
3. string literals
4. variable definition
5. invocation
6. as
7. access
8. index
9. one variable operation
10. two variable operation
11. assignment
12. undefined
13. null
14. array expression
15. map expression
16. procedure
17. lambda
18. type

##6.1 number literals
chapter 3.2.1

##6.2 bool literals
chapter 3.2.3

##6.3 string literals
chapter 3.2.2

##6.4 variable definition

* `variableName:Type`
	
	defines a variable with type specified
	
* `variableName=initValue`
	
	defines a variable with initial value
	
* `variableName:Type=initValue`
	
	defines a variable with type specified and with initial value

If the variable is defined in direct sub layer of a class or an interface, it's considered as a `Field`

	class User
	    id : int

defines a class `User` with one Field `id`, and the field type is `int`

If the variable is defined in a method, it's considered as a local variable. The variable must have a initial value.

##6.5 invocation
Invoke a method or an inner method.

* `method(args)`
	
	invoke a method of the current object (non-static)  
	invoke a method of the current Type (static)  
	invoke a method from `import static`
	
* `this.method(args)`

	invoke a method of the current object (non-static)

* `SuperType.this.method(args)`

	invoke a method of the current object from SuperType (non-static)

* `Type.method(args)`

	invoke a method of the Type (static)
	
##6.6 as

	expression as Type
	
cast the expression result to the given `Type`

For literals, the cast may produce an error when compiling. Check chapter 3.2 for avaliable types for different kinds of literals.

In other circumstances, the cast may be done when compiling or at runtime. However, `bool` can never be cast to other __primitive__ types. Check chapter 3.3 for avaliable type conversions.

##6.7 access
accesses a variable.

	variable
	Type.field
	object.field

get value of the variable or field.

if the variable is not found, the runtime tries:

1. get field via reflection
2. invoke `o.get(name)`
3. invoke `o.name()`
4. invoke `o.getName()`

if the variable is still not found, an `undefined` would be returned.

For arrays, `arr.length` result is the length of the array.

##6.8 index

	arr[i]
	
get array element value at index `i`

if `arr` is not an array __OR__ `i` is not integer, then invoke `get(i)` on `arr`.

NOTE THAT in `LessTyping`, the `i` might not be integer when compiling, but might be integer at runtime. So, when meets `get(int/Integer)` invocation, if the `arr` is array, the expression is still considered as getting array element value.

##6.9 one variable operation
`LessTyping` supports the following one variable operators

	"++", "--", "!", "~", "+", "-"
	
`++` and `--` can be put before or after the _Left Value_

`++a` means 

	(
	    a=a+1
	    return a
	)
	
`a++` means

	(
	    tmp=a
	    a=a+1
	    return tmp
	)
	
it's the same for `--` operator

`!`, `~`, `+` and `-` can only be put before the variable

`+` does NOT do anything. the compiler simply ignore the operator and parse the expression after the operator.

`!`, `~` and `-` supports `Operator Binding`

* `-` :  
	numbers : makes the number negative  
	other : invokes `negative()`
	
* `~` :  
	numbers : bitwise not  
	other : invokes `not`
	
* `!` :
	bools : returns the opposite logic value  
	other : invokes `logicNot()`
	
##6.10 two variable operation
`LessTyping` supports the following two variable operations, and their priorities are listed as below: (top to bottom, the priority reduces)

	{"..", ".:"},
	{":::"},
	{"^^"},
	{"*", "/", "%"},
	{"+", "-"},
	{"<<", ">>", ">>>"},
	{">", "<", ">=", "<="},
	{"==", "!=", "===", "!==", "=:=", "!:=", "is", "not", "in"},
	{"&"},
	{"^"},
	{"|"},
	{"&&", "and"},
	{"||", "or"}
	
A higher priority operator would reduce to a value faster than a low priority one.

	1 + 2 * 3
	
means `(1 + (2 * 3))`

	[]:::1..5
	
means `[]:::(1..5)`
	
###range list
`..` and `.:` are range list operators. They only takes integers as parameter.

`..` creates a list with end inclusive, and `.:` creates a list with end exclusive.

###concat
`:::` is bond to `concat(?)`. e.g.

	"abc":::"def"
	
the result would be `"abcdef"`

	[1,2,3]:::[4,5]
	
the result would be `[1,2,3,4,5]`

###pow
`^^` first cast the left and right expressions to `double`, and invoke `Math.pow(?,?)`

###math
all these operators do the same thing as in Java

* `*` is bond to `multiply(?)`
* `/` is bond to `divide(?)`
* `%` is bond to `remainder(?)`
* `+` is bond to `add(?)`
* `-` is bond to `subtract(?)`

###bitwise
all these operators does the same as in Java

* `<<` is bond to `shiftLeft(?)`
* `>>` is bond to `shiftRight(?)`
* `>>>` is bond to `unsignedShiftRight(?)`

###compare
all these operators do the same thing as in Java

* `>` is bond to `gt(?)`
* `<` is bond to `lt(?)`
* `>=` is bond to `ge(?)`
* `<=` is bond to `le(?)`
* `==` is bond to `equals(?)`
* `!=` means `!(a==b)`
* `===` checks references, same as `==` in java
* `!==` checks references, same as `!=` in java

the following operators are simply bond to methods

* `=:=` is bond to `equal(?)`
* `!:=` is bond to `notEqual(?)`
* `is` means `lt::lang::Lang.is(a,b)`
* `not` means `lt::lang::Lang.not(a,b)`
* `in` is bond to `contains(?)` and it's invoked on the right expression instead of left

###logic and bitwise
all these operators do the same thing as in Java

* `&` is bond to `and(?)`
* `^` is bond to `xor`
* `|` is bond to `or`

###and and or
`&&` is the same as `and`  
`||` is the same as `or`

they do the same thing as in Java.

	e1 && e2
	
e1 is firstly evaluated. if e1 is `false`, then the expression result would be `false`, e2 would not be evaluated.

	e1 || e2
	
e1 is firstly evaluated. if e1 is `true`, then the expression result would be `true`, e2 would not be evaluated.

##6.11 assignment
