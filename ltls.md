#The Latte Language Specification
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
	1. Hybrid of Static and Dynamic typing
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
		* inner method
	13. class definition
	14. interface definition
	15. fun definition
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
	19. AnnoExpression
	20. require
7. Language Related Libraries
	1. global variables
	2. evaluator and script
8. Libraries
	1. html
	2. sql

#§1 File Structure
##1.1 indentation
`Latte` forces 4 spaces indentation as default, which can be respecified with `Properties#_INDENTATION_` when constructing `lt::compiler::Scanner`

This _Latte Language Specification_ uses 4 spaces indentation when giving examples.

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
`Latte` supports very simple `define` and `undef` pre processing commands.

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
	
>the sub packages are separated by `::` instead of `.` 

In this _Specification_ , packages / types are separated by `::` , so it might writes `java::util::LinkedList`, you should known that it's the same as `java.util.LinkedList` in java.

import can appear in any position where indentation is 0

import supports importing:

* all types from a package
* one specified type
* all static fields and methods from a type

e.g.

	import java::awt::_
	import java::util::List
	import java::util::Collections._
	
these import statements means import all types from package `java::awt`, and import type `java::util::List` and import all static fields and methods from `java::util::Collections`

when trying to retrieve a type, it firstly tries to seek from import that specifies type simple name.

e.g. `List`, it's specified in `java::util::List`, so this type is `java::util::List` instead of `java::awt::List`

#§2 Literals
Latte supports 5 kinds of literals

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
map starts with `{` and ends with `}`, the containing entries are separated by `,` or `NewLine`. the entry key-value is separated by `:`

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
##3.1 Hybrid of Static and Dynamic typing
`Latte` is a hybrid of Static and Dynamic typing.

you can specify the type/return type with `:` when defining variables and methods, or use `as` to cast primitives/references.

e.g.

	num1 : int = 1
	num2 : float = 1.2
	
	method():Unit
	toString():String
	
	1 as long
	
this design let Latte be able to extend java classes, implement java interfaces and use any java library.

##3.2 Literal Default Types
If required type is not set, literals are parsed into their default values.

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
* `''` (empty string) -- `java::lang::String`
* `""` (empty string) -- `java::lang::String`
* `'str'` -- `java::lang::String`
* `"str"` -- `java::lang::String`

###3.2.3. bool
bools are parsed into java `boolean`

###3.2.4. array
arrays are parsed into `lt::util::List`

the `List` extends `java::util::LinkedList` and provides functions that look like `Array` in `JavaScript`

###3.2.5. map
maps are parsed into `lt::util::Map`

##3.3 Requiring Type
if required types are set, then the literals would be parsed into corresponding types.

if the required type and literal entry not recorded in the following table, a compiling error would be thrown.

>Casting on Literals are very strict, but it's loose when casting a variable.  
>The casting on variables are not recorded in this table.

Required Type | Literal Type   | Constant pool or Method Invocation |
------------- | :-------------:| -------------------------------- |
int           | integer number | constant pool int                |
short   | integer number | constant pool int and convert to short |
byte    | integer number | constant pool int and convert to byte  |
char          | char string    | constant pool int and convert to char|
boolean       | bool           | constant pool int 1 or 0         |
long          | integer number | constant pool long               |
float         | any number     | constant pool float              |
double        | any number     | constant pool double             |
Integer       | integer number | Integer.valueOf(number as int)   |
Short         | integer number | Short.valueOf(number as short)   |
Byte          | integer number | Byte.valueOf(number as byte)     |
Character     | char string    | Character.valueOf(literal)       |
Boolean       | bool           | Boolean.valueOf(bool)            |
Long          | integer number | Long.valueOf(number as long)     |
Float         | any number     | Float.valueOf(number as float)   |
Double        | any number     | Double.valueOf(number as double) |
String        | any string     | String constant pool             |
array type    | array          | array                            |
List          | array          | List     |
Map           | map            | Map      |

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
`Latte` supports class and interface definitions

`Latte` can use java libraries directly, so, other types such as `enum` and `Annotation`, can be defined in java source code

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

in `Latte`, the types are always modified with `public`. 

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
`number` , `string` and `bool` literals can only be parsed into limited types, and might produce a compiling error. In other circumstances, `Latte` supports a large range of type casting methods when compiling and at runtime.

literals only support:

* `number` without dot can be `int`, `long`, `short`, `byte`, `double`, `float` and their boxing types
* `number` with dot can be `double`, `float` and their boxing types
* `string` can be `java::lang::String` or `char`
* `bool` can be java `boolean` or `Boolean`
* `array` can be `array types` or `List`
* `map` can be `Map`

###Compile
Compiling only supports auto boxing. other casts are done at Runtime

###Runtime
all cast for reference types to reference types are defined in `lt::lang::LtRuntime.cast(o, targetType, callerClass)`

all cast for reference types to primitive types are defined as "castToX", such as `castToInt`, `castToShort` ...

when the cast fails, a `java::lang::ClassCastException` would be thrown.

the following table show how `Latte` casts types

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
Function       | functional interface     | param length should be the same and use Proxy to generate new object
Function       | functional abstract class | param length should be the same and use `Latte` compiler to generate new object

##3.7 Pre Defined
###types
the following types are defined as default

* void
* Undefined

the `void` can also be written as `Unit`, and `Undefined` is defined as `lt::lang::Undefined`

`void` (or `Unit`) can only be used on method return types, which represents that the method doesn't have a return value

An `undefined` appears when trying to retrieve non-exist fields or trying to get return value of a `void` method

###values
the following values are defined as default

* null
* undefined

`null` can be assigned to any type, and `undefined` is used as a symbol of non-exist fields or `void` methods' return value. The type of `undefined` is `Undefined`.

#§4 Keywords
all java keywords are `Latte` keywords :

	"abstract", "assert", "boolean", "break", "byte", "case",
	"catch", "char", "class", "const", "continue", "default",
	"do", "double", "else", "enum", "extends", "final", "finally",
	"false", "float", "for", "if", "implements", "import", "instanceof",
	"int", "interface", "long", "native", "new", "null", "package",
	"private", "protected", "public", "return", "short", "static",
	"strictfp", "throw", "true", "try", "while"
	
there're a few more keywords defined in `Latte` :

	"is", "not", "bool", "yes", "no", "type", "as",
	"undefined", "in", "elseif", "Unit", "data", "val",
	"fun", "require"
	
note that `define` and `undef` are not keywords, they only enables if the first characters of the line is `define` or `undef`.

`boolean` is a keyword, but invalid in `Latte`, use `bool` instead.

Write

	`valid java name`

to use those names defined in java but happend to be `Latte` keywords.

e.g.

	System.`in`
	
##4.1 Modifiers
`Latte` modifiers are almost the same `Java` modifiers, but `val` represents `final`, `pkg` and `data` are new modifiers.

`pkg` is a access modifier, it's the same with "no access modifiers" in java

`data` only modifies `class`, generates some methods to build a java bean.

Here are all `Latte` modifiers:

	"public", "protected", "private", "pkg", "data",
	"abstract", "val", "native", "synchronized", "transient", 
	"volatile", "strictfp"
	
###access modifiers
At most one access modifier can exist on one object

`public` `private` `protected` `pkg` are access modifiers

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
`Latte` support the following statements

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
14. interface definition
15. fun definition

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
		    
`Latte` doesn't support traditional C-like for statement, use `range list` instead.

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

the return value of `void` method is `undefined` in `Latte`, but it won't return anything in `Java`.

##5.8 synchronized

	synchronized(m1, m2, m3, ...)
	    ...
	    
the current thread retrieve the lock of object `m1`, `m2` and `m3`

and they are released when the execution of statements is finished.

##5.9 throw

	throw anyObject
	
`Latte` allow you to throw any object. The objects whose types are not sub-class of `Throwable` are filled into `lt::lang::Wrapper`, and can be retrieved with it's `public` field : `object`. So if `Latte` methods are called in `Java`, catch `lt.lang.Wrapper` to retrieve the wrapped objects.

When catching exceptions in `Latte`, the wrapped object is automatically retrieved and assigned to the exceptionVariable

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

### inner method
An inner method means a method defined inside another method.

`Latte` inner method can capture local variables, and the values _inside the inner method_ can be modified, but the modifications won't have effect on the outer local variables.

e.g.

	outer(a)
		b = 1
		inner(c)
			return a+b+c
		d = 2
		return inner(3) + d

##5.13 class definition
The class definitions can be found in chapter 3.5

	class ClassName
	class ClassName(params)
	class ClassName(params):ParentClass
	class ClassName(params):ParentClass(args)
	class ClassName(params):ParentClass(args),Interface1,Interface2
	
The static fields and methods are defined as:

	class ClassName
	    static
	        ... ; fields and methods

##5.14 interface definition
The interface definitions can be found in chapter 3.5

	class InterfaceName
	class InterfaceName:SuperInterface1,SuperInterface2

The static fields and methods are defined in the same way that class does.

##5.15 fun definition
`fun` means function. It's a simple way of implementing a functional interface/abstract class.

e.g.

	@FunctionalInterface
	interface Function1
	    apply(o)=...
	    
if you want to build a class `Fun1Impl` which implements the interface, you can write:

	fun Fun1Impl(o):Function1
	    ...
	    return xxx

The `:Function1` can be omitted. The implemented type is `lt::lang::function::FunctionX` as default where `X` is arguments count. It can be assigned to any places requires a lambda with the support for casting from Functions to required functional types.

e.g.

	fun printElem(o)
	    println(o)

	[1,2,3].forEach(printElem)

#§6 Expressions
`Latte` supports the following expressions

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
19. AnnoExpression
20. require

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

If the variable is defined in a method, it's considered as a local variable. The local variable __must__ have a initial value.

##6.5 invocation
Invoke a method or an inner method. Or construct a new object.

* `method(args)`
	
	invoke a method of the current object (non-static)  
	invoke a method of the current Type (static)  
	invoke a method from `import static`  
	construct a new object
	
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
Accesses a variable. or Construct a new object.

	variable
	Type.field
	object.field

get value of the variable or field.

if the variable/field is not found, the compiler checks whether the input is a type. If it's a type and constructor without parameters can be accessed, then the compiler parse the expression as _constructing a new object_.

if still not correctly parsed, then it's handled by the runtime:

1. get field via reflection
2. invoke `o.name()`
3. invoke `o.getName()`
4. invoke `o.get(name)`
5. check global variables.

>Note that, global variables would only be checked if the `access` is simply a name. e.g. `this.xxx` is _NOT_ a name, global variable check won't be perfomed.

if the variable is still not found, an `undefined` would be returned.

For arrays, `arr.length` result is the length of the array.

Check chapter 7.1 for more info about `global variables`.

##6.8 index

	arr[i]
	
get array element value at index `i`

if `arr` is not an array __OR__ `i` is not integer, then invoke `get(i)` on `arr`.

Also, you can write `arr[i,j,k]`, which means `arr.get(i,j,k)`. If the `arr` is an array and `i` is integer, it's converted into `arr[i][j,k]`. If `arr[i]` is array and `j` is integer, it's converted into `(arr[i][j])[k]`, and so on.

e.g.

	arr:[][][]int = 
	[
	    [
	        [1,2,3]
	        [4,5,6]
	    ]
	    [
	        [7,8,9]
	    ]
	]
	
the value of `arr[0,1,2]` is `6`.

##6.9 one variable operation
`Latte` supports the following one variable operators

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
`Latte` supports the following two variable operations, and their priorities are listed as below: (top to bottom, the priority reduces)

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
	{":="}
	
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

###assign
`:=` is bond to method `assign(o)`.

##6.11 assignment
Assignment writes

	variable = value
	
assign the `value` to the `variable`. The variable may look like accessing a field, e.g. `o.fieldName`. The compiler checks whether field exists. If it exists, then assign the field directly. In other circumstances, it tries to assign the field at runtime, then invoke `setFieldName(value)`, then `set(fieldName, value)`, then `put(fieldName, value)`.

e.g.

	class C
	    public f
	    setG(o)=...
	
	c = C ; construct an object
	c.f = 1
	
	c.g = 2

	map = {
	    "a" : 1
	}
	map.a = 3
	
where :

* `c.f = 1` directly assign the field with given value.
* `c.g = 2` means `c.setG(2)`.
* `map.a = 3` means `map.put("a", 3)`.

if the `variable` accesses index, e.g. `arr[i] = 1`, the compiler checks whether `arr` is an array. If it's an array, then the `i th` element would be set to `1`. Otherwise, it tries to invoke `arr.set(i, 1)`, then `arr.put(i, 1)` at runtime.

##6.12 undefined
It's a pre defined value. Check chapter 3.7.

##6.13 null
It's a pre defined value. Check chapter 3.7.

##6.14 array expression
chapter 3.2.4.

##6.15 map expression
chapter 3.2.5.

##6.16 procedure
`procedure` is a list of statements that returns a value (or null as default).

The `procedure` should have _at least one statement_ or _at least two expressions_.

The `procedure` is surrounded by `(` and `)`.

	; one statement in same line
	(return value)
	
	; one statement in a new line
	(
	    return value
	)
	
	; two expressions
	(
	    exp1()
	    exp2()
	)

The `procedure` is a `value`, so it can appear anywhere that requires a value.

e.g.

	"User(" + (
	    if id
	    	return "id=" + id
	    else
	    	return ""
	) + ")"
	
it's used to generate a string `"User(...)"`, and only when id is not null, the `id=?` would be filled into `...`.

##6.17 lambda
`Latte` supports lambda on `Functional Interfaces` and `Functional Abstract Classes`.

`Functional Abstract Class` means an abstract class with a public constructor whose parameter count is 0, and it have only one unimplemented method and the method is accessible.

The lambda writes:

	(x)-> ...
	
	(x)->
	    ...
	    
	(x,y,z)-> ...
	
	(x,y,z)->
	    ...
	    
You can specify a type for these lambdas, e.g.

	list forEach (
	    ((x)->println(x)) as java::util::function::Consumer
	)

	func : java::util::function::Function = (x) -> x+1
	
The type can be omitted, the default type would be `lt::lang::function::FunctionX`, where `X` is parameter count.  
The lambda would be converted into right type at runtime. So you can directly write :

	list.forEach((x)-> println(x))

The lambda in Latte is almost the same as in Java, but it supports functional abstract classes.

##6.18 type

	type TypeName

retrieve the `Class` object of the `TypeName`, it's the same as `TypeName.class` in Java. It appears frequently when using Reflection Library.

e.g.

	(type List).getMethods()

##6.19 AnnoExpression
It's used on annotations.

e.g.

	@SomeAnno(value=@AnotherAnno)
	
where `AnotherAnno` is an `AnnoExpression`.

##6.20 require
It have similar effect as Node.js `require(...)`.

	require 'script-file'
	
	require 'cp:script-in-class-path'
	
The require returns a value, it's retrieved from the script.

The script may write:

	return 1+1
	
save the script as `demo.lts`, then the result of `require 'demo.lts'` is `2`.

Each script would be performed only once, the result would be recorded and would be directly retrieved when called twice.

When using `cp:...`, the script file would be retrieved using `XX.class.getResourceAsStream(...)`, and a `/` would be added to the most front place of the script path string if it doesn't start with `/`.

#7 Language Related Libraries
##7.1 global variables
The global variables can be assigned with
	
	GLOBALS['name'] = value
	
and retrieved with

	GLOBALS['name']
	; or use the method described in chapter 6.7
	
The `GLOBALS` is a `public static` field defined in `lt::lang::Utils`, which is automatically imported into any latte files.

##7.2 evaluator and script
You can write `eval('...')` in `Latte`, which is backed up by `lt::repl::Evaluator`.  
The `eval` method is defined in `lt::lang::Utils`, which is automatically imported into any latte files.

Also, scripts are supported, and you can `require` any scripts in `Latte`.  
`require` uses `lt::repl::ScriptCompiler` to run scripts and retrieve results.

#8 Libraries
##8.1 html
##8.2 sql