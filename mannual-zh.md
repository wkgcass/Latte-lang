# Latte-lang 规范

# 目录

1. [起步](#p1)
	1. [基础语法](#p1-1)
	2. [文件结构](#p1-2)
2. [基础](#p2)
	1. [字面量](#p2-1)
	2. [基本类型](#p2-2)
	3. [控制流](#p2-3)
3. [类和对象](#p3)
	1. [类与继承](#p3-1)
	2. [接口](#p3-2)
	3. [字段和方法](#p3-3)
	4. [修饰符](#p3-4)
	5. [Data Class](#p3-5)
	6. [实例化](#p3-6)
	7. [Object Class](#p3-7)
	8. [隐式转换](#p3-8)
4. [函数类和Lambda](#p4)
	1. [函数类](#p4-1)
	2. [高阶函数和Lambda](#p4-2)
5. [其他](#p5)
	1. [Json 集合](#p5-1)
	2. [范围](#p5-2)
	3. [类型检查与转换](#p5-3)
	4. [运算符绑定](#p5-4)
	5. [异常](#p5-5)
	6. [注解](#p5-6)
	7. [过程(Procedure)](#p5-7)
    8. [参数可用性检查](#p5-8)
    9. [解构](#p5-9)
    10. [模式匹配](#p5-10)
6. [Java 交互](#p6)
	1. [在Latte中调用Java代码](#p6-1)
	2. [在Java中调用Latte代码](#p6-2)

<h1 id="p1">1. 起步</h1>

`Latte-lang`是一种JVM语言，基于JDK 1.6。它支持Java的所有语义，能够与Java完美互通，并提供比Java更多的函数式特性。

<h2 id="p1-1">1.1 基础语法</h2>

Latte-lang（后文简称Latte）借鉴了主流语言的语法特征。如果您熟悉`Java`，或者了解过`Kotlin`,`Scala`,`Python`,`JavaScript`,`Swift`中的一到两种，那么阅读`Latte-lang`代码是很轻松的。

> 对Latte影响最大的语言应该是Kotlin和Scala

<h3 id="p1-1-1">1.1.1 注释</h3>

单行注释使用`//`开头。例如

```java
// i am a comment
```

多行注释以`/*`开头，以`*/`结尾。例如

```java
/* comment */
/*
multiple line comment
*/
a/* comment */=1
/* the comment splits an expression */
```

<h3 id="p1-1-2">1.1.2 包与导入</h3>

```kotlin
package lt::spec

import java::util::_
```

或者使用`.`分割

```kotlin
package lt.spec

import java.util._
```

代码段由`package`开始。一个latte文件只能包含一个`package`。它定义了其中定义的类、接口所在的“包”。包是一个java概念，可以理解为一个名字空间。

声明了这个文件中定义的所有类和接口都在`lt::spec`包下。子包的名称通过`::`，或者`.`分隔。

文件也可以不声明包，那么包名将被视为空字符串`""`

在导入包时使用`import`关键字，有三种导入方式：

```kotlin
import java::awt::_               /* 导入所有java::awt包中的所有类 */
import java::util::List           /* 导入类java::util::List */
import java::util::Collections._  /* 导入Collections类的所有静态字段和方法 */
```

>使用`.`无法在语法上区分包与字段访问，所以在latte中建议使用`::`分割包名。但是考虑到其他JVM语言均使用`.`，所以也提供了`.`以符合使用习惯。

<h3 id="p1-1-3">1.1.3 类</h3>

定义类User

```kotlin
class User(id: int, name: String)
```

定义MyList类，并继承了LinkedList类

```kotlin
class MyList(ls:List):LinkedList(ls)
```

定义抽象类MyList，并实现了List接口

```kotlin
abstract class MyList:List
```

详见[3.1 类与继承](#p3-1)

定义`data class`

```kotlin
data class User(id: int, name: String)
```

定义data class后，编译器会自动生成所有字段的getter/setter，类的toString/hashCode/equals方法，并继承和实现Serializable、Cloneable接口。

详见[3.5 Data Class](#p3-5)

定义`object class`

```kotlin
object Singleton
```

可以直接使用类名获取该对象。

详见[3.7 object class](#p3-7)

<h3 id="p1-1-4">1.1.4 接口</h3>

定义接口Consumer

```kotlin
interface Supplier
    def supply
```

详见[3.2 接口](#p3-2)

<h3 id="p1-1-5">1.1.5 函数类</h3>

定义函数类sum

```kotlin
fun sum(a, b)
    return a+b
```

详见[4.1 函数类](#p4-1)

<h3 id="p1-1-6">1.1.6 变量</h3>

只读变量

```kotlin
val a:int = 1
val b = 1
```

可变变量

```kotlin
var x = 5
x += 1
```

可变变量的`var`可以省略

```python
y = 6
```

<h3 id="p1-1-7">1.1.7 字符串模板</h3>

```kotlin
main(args : []String)
    println("First argument: ${args[0]}")
```

<h3 id="p1-1-8">1.1.8 使用条件表达式</h3>

```python
if a > b
    return a
else
    return b
```

或者

```kotlin
result = (if a > b {a} else {b})
```

>一个方法需要返回值，那么只要末尾一个值为表达式，Latte就会自动生成方法的return语句

详见[2.3.1 If 语句](#p2-3-1)

<h3 id="p1-1-9">1.1.9 for循环</h3>

```swift
for item in list
    println(item)
```

或者

```swift
for i in 0 until list.size
    println(list[i])
```

详见[2.3.2 For 语句](#p2-3-2)

<h3 id="p-1-1-10">1.1.10 while循环</h3>

```swift
i = 0
while i < list.size
    println(list[i++])
```

详见[2.3.3 While 语句](#p2-3-3)

<h3 id="p-1-1-11">1.1.11 范围</h3>

检查x是否在范围中

```kotlin
if x in 1 to y-1
    print("OK")
```

从1到5进行循环

```swift
for x in 1 to 5
    print(x)
```

详见[5.2 范围](#p5-2)

<h3 id="p1-1-12">1.1.12 Lambda</h3>

```kotlin
list.stream.
filter{it.startsWith("A")}.
map{it.toUpperCase()}.
forEach{print(it)}

f = a -> 1 + a
f(2) /* 结果为 3 */
```

详见[4.2 高阶函数和Lambda](#p4-2)

<h3 id="p1-1-13">1.1.13 Json语法</h3>

```js
list = [1, 2, 3]
map = [
    "a": 1
    "b": 2
]
```

详见[5.1 Json 集合](#p5-1)

<h3 id="p1-1-14">1.1.14 返回对象的"或"</h3>

拥有JavaScript的`||`的功能。

```js
a = a || 1
```

<h3 id="p1-1-15">1.1.15 指定生成器</h3>

```
#js
    def method(a)
        return a+1
```

将会被转换成如下JavaScript代码

```js
function method(a) {
    return a + 1;
}
```

<h3 id="p1-1-16">1.1.16 模式匹配</h3>

```scala
val (x,y) = Bean(1,2)
```

详见[5.9 解构](#p5-9)

```scala
o match
    case Bean(a,b) => ...
    case People(name, age) if age > 20 => ...
    case _ => ...
```

详见[5.10 解构](#p5-10)

<h2 id="p1-2">1.2 文件结构</h2>

Latte源文件以`.lt`或者`.lts`为扩展名。不过实际上后缀名并不重要，手动构造编译器时附加特定参数即可。

<h3 id="p1-2-1">1.2.1 定义层次结构</h3>

编译器首先会检查文件的第一行。第一行可以标注该文件 使用缩进定义层次结构，还是 使用大括号定义层次结构。

例如：

```java
/// :scanner-brace
```

表示使用大括号定义层次结构。

```java
/// :scanner-indent
```

表示使用缩进定义层次结构。

**默认为缩进**。提供这个选项是为了满足不同人的编码喜好。这两者的选择在词法上完全一样。

>注意：Latte并不限定缩进数量。比上一行“更大的缩进”均代表一个新层次。

<h3 id="p1-2-2">1.2.2 层次结构</h3>

使用*大括号*定义层次结构时，有如下几个符号会开启一个新层：

```
(
[
{
```

当其对应的符号出现时，这个开启的层将关闭：

```
)
]
}
```

使用*缩进*定义层次结构时，还有两个符号会开启一个新层: `->`、`=>`

由于层将在缩进“变得更小”时关闭。所以，如下代码不加上括号也可以正确的工作：

```java
f = x->x+1
f()
```

>注：使用大括号定义层次结构时，`->`不会开启新层，若要书写多行语句请使用{...}。此时规则和java完全一致。

这里有一个示例，帮助理解“层”的概念：

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

该源码将被解析成如下结构：

	-[class]-[A]-[(]-[│]-[)]-[:]-[B]-[│]
	                  └[a]-    ┌──────┘
	┌──────────────────────────┘
	└────[method]-[(]-[│]-[)]-[│]-
	┌──────────────────┘       └───────[pass]-
	│
	└──[arg0]-[EndNode]-[arg1]-[EndNode]-[arg2]-[StrongEndNode]-[arg3]

**注意:** 本文除非注明，否则均使用“缩进”进行描述。在注明为“大括号”时，会在示例代码开头标注`/// :scanner-brace`。

<h3 id="p1-2-4">1.2.4 层次控制字符</h3>

Latte支持直接使用`{`和`}`定义层次结构。不过要注意，如果写为`{}`则表示空Map(映射)

例如：

```kotlin
if a > b {1} else {2}
/* 相当于 */
if a > b
    1
else
    2
```

```js
var map = {} /* 这是一个map */
```

```js
/* 可以正常编译，定义了一个方法，其中定义一个局部变量，赋值一个map */
def method {
    map = [
        "a": 1
        "b": 2
    ]
}
```

<h1 id="p2">2. 基础</h1>

<h2 id="p2-1">2.1 字面量</h2>

Latte中有6种字面量：

1. number
2. string
3. bool
4. array
5. map

<h3 id="p2-1-1">2.1.1 number</h3>

数字可以分为整数和浮点数

例如：

	1
	1.2

`1`是一个整数，`1.2`是一个浮点数。

整数字面量可以赋值给任意数字类型，而浮点数字面量只能赋值给`float`和`double`。详见 [2.2 基本类型](#p2-2)。

<h3 id="p2-1-2">2.1.2 string</h3>

字符串可以以`'`或`"`开头，并以同样的字符结尾。

例如：

	'a string'
	"a string"

使用`\`作为转义字符

例如：

```
'escape \''
"escape \""
```

字符串字面量分为两种，若字符串长度为1，则它可以赋值给`char`类型或`java.lang.String`类型。否则只能赋值给`java.lang.String`类型。

<h3 id="p2-1-3">2.1.3 bool</h3>

布尔型有下述4种书写形式：

	true
	false
	yes
	no

`true`和`yes`表示逻辑真，`false`和`no`表示逻辑假。

布尔值字面量只能赋值给`bool`类型。

<h3 id="p2-1-4">2.1.4 array</h3>

数组以`[`开头，并以`]`结尾，其中包含的元素可以用`,`分隔，也可以通过换行分隔。

例如：

```js
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
```

<h3 id="p2-1-5">2.1.5 map</h3>

映射(字典)像swift一样，以`[`开头，以`]`结尾。

键值对通过类型符号`:`分隔，不同的entry通过`,`或者换行进行分隔。

例如：

```swift
['a':1, 'b':2, 'c':3]

[
    'a':1
    'b':2
    'c':3
]

[
    'a':1,
    'b':2,
    'c':3
]
```

<h2 id="p2-2">2.2 基本类型</h2>

Latte保留了Java的8种基本类型。由于Latte是动态类型语言，并且可以在编译期和运行时自动装包和拆包，所以基本类型与包装类型可以认为没有差异。

八种基本类型：

```
int
long
float
double
short
byte
char
bool
```

其中`int/long/float/double/short/byte`为数字类型。

注意，与Java不同的是，Latte使用`bool`而非`boolean`。

<h3 id="p2-2-1">2.2.1 基本类型转换</h3>

在转换时，所有基本类型都可以互相转换（除了bool，它可以被任何类型转换到，但不能转换为其他类型，详见[5.8 参数可用性检查](#p5-8)）而不会出现任何错误。但是，在高精度向低精度转换时可能会丢失信息。例如：

```scala
i:int = 3.14   /* i == 3 丢失了小数部分 */

b:bool = 10  /* b == true 除了数字不是0外，信息都丢失了 (只有0在转换为bool时才会是false) */
```

<h3 id="p2-2-2">2.2.2 基本类型运算</h3>

Latte支持所有Java的运算符，并在其基础上有所扩展

对于数字的基本运算，其结果均为“精度较高的值”的类型，且最低为`int`型。例如：

```c#
r1 = (1 as long) + (2 as int)    /* r1 是 long */
r2 = (1 as byte) + (2 as short)  /* r2 是 int */
```

由于Latte可以任意转换基本类型，所以这么写也可以正常编译并运行：

```java
a:short = 1
a+=1  /* a == 2 */
a++   /* a == 3 */
```

>由于Latte支持基本类型的互相转换，你可以直接把`int`的结果赋值给`short`。

Latte支持所有Java运算符，当然也包括位运算。和`java`一样，位运算必须作用于整数上。Latte支持所有整数类型的位运算：`int/long/short/byte`。  
此外，Latte还支持乘方运算：

```groovy
a ^^ b
```

结果均为`double`型。

> 本质来说，Latte不存在“运算符”。所有运算符都是方法调用。基本类型的运算是由其包装类型隐式转换后调用方法完成的。

<h2 id="p2-3">2.3 控制流</h2>

<h3 id="p2-3-1">2.3.1 If 语句</h3>

和Java一样，if是一个语句而非像Kotlin，Scala那样作为表达式。

但是，Latte支持`Procedure`，并且可以自动添加返回语句，所以使用起来和作为表达式区别并不大。

```ruby
if a > b
    return 1
else
    return 2

val result = (if a>b {1} else {2})
```

<h3 id="p2-3-2">2.3.2 For 语句</h3>

for语句格式如下：

```kotlin
for item in iter
    ...
```

其中`iter`可以是数组、Iterable对象、Iterator对象、Enumerable对象、Map对象。

当`iter`为前4种时，for语句将把其包含的对象依次赋值给`item`并执行循环体。当`iter`为Map对象时，`item`是一个Entry对象，它来自`Map#entrySet()`。

你也可以使用`to`或者`until`，并在循环体内使用下标来访问元素:

```kotlin
for i in 0 until arr.length
    val elem = arr[i]
    ...
```

<h3 id="p2-3-3">2.3.3 While 语句</h3>

while语句格式如下：

```python
while boolExp
    ...

do
    ...
while boolExp
```

它的含义和Java完全一致。

<h3 id="p2-3-4">2.3.4 break, continue, return</h3>

在循环中可以使用 `break` 和 `continue` 来控制循环。break将直接跳出循环，continue会跳到循环末尾，然后立即开始下一次循环。它的含义与Java完全一致。

`return`可以用在lambda、方法（包括“内部方法”）、Procedure、脚本、函数类中:

```kotlin
/* lambda */
foo = ()->return 1

/* 方法 */
def bar()
    return 2

/* Procedure */
(
    return 3
)

/* 函数类 */
fun Fun1
    return 4
```

脚本中的return语句表示将这个值返回到外部，在`require`这个脚本时将返回这个值。

如果`return`是这个函数/方法最后的一条语句，或者该函数任意一条逻辑分支的最末尾，那么`return`都可以被省略:

```kotlin
fun add(a, b)
    a+b

val result = add(1, 2)
/* result is 3 */
```

转换方式很简单，首先取出这个函数/方法的最后一条语句，如果是表达式，而且这个函数/方法要求返回值，则直接将其包装在`AST.Return`中。  
如果最后一条语句是`if`，那么对其每一个逻辑分支进行该算法。

<h1 id="p3">3. 类和对象</h1>

<h2 id="p3-1">3.1 类与继承</h2>

<h3 id="p3-1-1">3.1.1 类</h3>

类通过`class`关键字进行定义

当类内部不需要填充任何内容时，可以非常简单的书写为：

```kotlin
class Empty
```

当需要提供构造函数参数时，写为：

```kotlin
class User(id, name)
```

当然，你也可以为参数指定类型：

```scala
class User(id:int, name:String)
```

如果不指定类型则类型视为`java.lang.Object`  

Latte不支持在类内部再定义构造函数，不过，你可以指定参数默认值来创建多个构造函数：

```scala
class Rational(a:int, b:int=1)
```

此时你可以使用`Rational(1)`或者`Rational(1, 2)`来实例化这个类。

--

构造函数内容直接书写在class内：

```kotlin
class Customer(name: String)
    logger = Logger.getLogger('')
    logger.info("Customer initialized with value ${name}")
```

直接定义在类中的变量，以及构造函数参数，将直接视为字段(Field)。也就是说，上述例子中定义的name和logger都是字段。详见 [3.3 字段和方法](#p3-3) 。

--

使用`private`修饰符来确保类不会被实例化：

```kotlin
private class DontCreateMe
```

在Latte中，所有类都是`public`的，所以，在`class`前的任何“访问关键字”均为该类构造函数的访问关键字。

<h3 id="p3-1-2">3.1.2 继承</h3>

和Java一样：Latte是单继承，并且所有的类都默认继承自`java.lang.Object`。你可以使用类型符号`:`来指定继承的类。继承的规则和Java完全一致。

```kotlin
class Base(p:int)
class Derived(p:int) : Base(p)
```

父类的构造函数参数直接在父类类型后面的括号中指定。

如果使用了父类的无参构造函数，那么可以省略括号：

```kotlin
class Example : Object
```

如果想指定一个类不可被继承，那么需要在它前面加上`val`修饰符：

```kotlin
val class NoInher
```

<h3 id="p3-1-3">3.1.3 抽象类</h3>

使用`abstract`关键字定义抽象类：

```kotlin
abstract class MyAbsClass
    abstract f()
```

抽象类规则与Java完全一致。抽象类可以拥有未实现的方法。

继承一个抽象类：

```kotlin
class MyImpl : MyAbsClass
    @Override
    def f=1
```

<h3 id="p3-1-4">3.1.4 静态成员</h3>

使用`static`定义静态成员。static可以“看作”一个修饰符，也可以“看作”一个结构块的起始。例如：

```js
class TestStatic
    static
        public val STATIC_FIELD = 'i am a static field'
    static func()=1
```

<h2 id="p3-2">3.2 接口</h2>

Latte接口遵循Java的接口定义。使用`interface`关键字:

```kotlin
interface MyInterface
    foo()=...
```

定义了`abstract`方法`foo()`。

让一个类实现接口，也使用类型符号`:`。

```kotlin
class Child : MyInterface
    foo()=456

child = Child
child.foo /* result is 456 */
child.bar /* result is 123 */
```

接口可以拥有字段，但是和Java规则一样，字段必须是`static public val`（默认也是）。

```kotlin
interface MyInterface
    FLAG = 1
```

接口也可以拥有`static`方法（和Java一样）

```js
interface TestStaticMethod
    static
        method()=111

TestStaticMethod.method() /* result is 111 */
```

<h2 id="p3-3">3.3 字段和方法</h2>

<h3 id="p3-3-1">3.3.1 定义字段</h3>

你可以在类或接口中定义字段：

```kotlin
class Address(name)
    public street
    public city
    public state
    public zip

interface MyInterface
    FLAG = 1
```

在类中定义的字段默认被`private`修饰，可选的访问修饰符还有`public`, `protected`, `internal`。

字段可以为`static`，只要写在static块中即可（接口默认就是static的，不需要修改）。也可以为不可变的，使用`val`修饰即可。由于构造函数参数也是字段，所以这些修饰符可以直接写在构造函数参数中。例如：

```kotlin
class User(protected val id, public val name)
```

使用字段很简单，直接使用`.`符号访问即可，和Java一致。

```kotlin
val user = User(1, 'latte')
user.name   /* result is 'latte' */

val address = Address('home')
address.city = 'hz'
```

Latte提供所谓的`property`支持：使用`getter`和`setter`来定义`property`。详情见 [3.3.3 Accessor](#p3-3-3)

<h3 id="p3-3-2">3.3.2 方法</h3>

Latte支持多种定义方法的语法，先看一个最完整的方法定义：

```scala
def foo(x:int, y:int):int
    return x + y
```

如果方法没有参数，那么可以省略里面的内容，甚至省略括号：

```scala
/* 无参数 */
def foo:int
    return 1
```

返回类型可以不指定，默认为`java.lang.Object`

```scala
def foo
    return 1
```

如果方法体只有一行并返回一个值，可以把在方法定义后直接接`=value`

```scala
def foo = 1
```

如果方法体不存在（即空方法），可以只写一个方法名称（如果有参数再把参数加上）

```scala
def foo
```

上述定义的缩写方式可以混合使用。

此外，如果在定义方法的时候（使用了括号）并且（附带一个修饰符/注解，或者定义了返回值，或者使用了`=`语法），那么可以省略`def`

```
bar():int
foobar()='hello'
fizz():int=1
```

--

如果明确方法不返回值，那么可以附加`Unit`类型。Latte中只能写作`Unit`，代表了Java中的`void`。  
但是，在Latte中所有方法都会返回一个值，对于Unit类型的方法，虽然会被编译为void类型，但是依然会返回`Unit`，它是`lt.lang.Unit`类型。

和构造函数参数一样，方法参数也可以设定默认值：

```scala
foo(a, b=1)=a+b
foo(1)  /* result is 2 */
```

**注意**，`def`实际上是一个“修饰符”(虽然它什么都不做)，并不属于“关键字”。设置`def`是为了和“省略参数的lambda”进行语法上的区分。

例如：

```js
foo(x)
    ...
```

实际上会被转化为[4.2.2 Lambda](#p4-2-2)中描述的形式：

```js
foo(x)(it->...)
```

所以使用类似于这种方式(`VALID_NAME ( [PARAM [, PARAM, ...]] ) { ... }`)定义的方法，如果没有注解，也没有其他修饰符，会造成歧义，所以不可省略`def`。

方法返回类型为`Unit`时，你依然可以书写`return value`，这个value会被求值，但是不会被返回。  
方法非`Unit`时，你也可以直接书写`return`，这时默认返回一个`Unit`。当然，如果返回类型不匹配，编译期依然会报错。

#### 内部方法

Latte支持“内部方法”。所谓内部方法是指在方法内部再定义一个方法。

```
def outer
    def inner
```

内部方法可以访问外部的所有变量，并且可以修改它们。

>实际上，Lambda、Procedure都基于“内部方法”特性。所以它们也可以修改捕获到的所有变量。

<h3 id="p3-3-3">3.3.3 Accessor</h3>

accessor分为两种，一种是取值：getter，一种是赋值：setter。

对于getter有两种定义方式：

1. 定义为`get{Name}()`
2. 定义为`{name}()`

对于setter只有一种定义方式：定义为`set{Name}(name)`

```scala
class User(id, name)
    def getId=id
    setId(id)
        this.id = id
    def name=name  /* 放心，这么写是正确的 */
    setName(name)
        this.name = name
```

如此定义就可以像直接访问field一样来调用这几个方法了。

```kotlin
user = User(1, 'latte')
println("user_id is ${user.id} and user_name is ${user.name}")
user.id = 2
user.name = 'jvm'
```

不过要注意的是：如果字段暴露给访问者，那么还是优先直接取字段或者对字段赋值。

此外，还有一对特殊的accessor:

* `set(String, ?)` 方法签名要求方法名为"set"，第一个参数接受一个String，第二个参数接受一个值，类型没有限制。（只不过使用时只能赋值为该类型的子类型）。
* `get(String)` 方法签名要求方法名为"get"，第一个参数接受一个String。

定义有上述accessor的类的实例，在取`o.field`时，将转换为`o.get('field')`。在设置`o1.field = o2`时，将转换为`o1.set('field', o2)`。

<h2 id="p3-4">3.4 修饰符</h2>

<h3 id="p3-4-1">3.4.1 访问修饰符</h3>

Latte有4种访问修饰符：

* public 公有，所有实例均可访问
* protected 受保护，包名相同的类型，或者子类可访问
* internal 包内可访问，包名相同的类型可以访问
* private 私有，只有本类型可访问

访问修饰符可以用来修饰:

* 类
* 接口
* 字段
* 方法
* 构造函数的参数

其中，类访问修饰符并不是规定给类用的。Latte中，类的访问修饰符永远为`public`，这个修饰符是作为构造函数而存在的。

| 位置   | public | protected | internal | private |
|-------|--------|-----------|----------|--------|
| 类    |  √     |     √     |     √    |   √    |
| 接口  |  √     |           |          |        |
| 字段  |  √     |     √     |     √    |   √    |
| 方法  |  √     |     √     |     √    |   √    |
| 构造函数参数 |  √ |   √     |     √    |   √    |

<h3 id="p3-4-2">3.4.2 其他修饰符</h3>

Latte支持所有Java的修饰符，但是名称可能有改动：

* var 表示可变变量（可省略，默认即为可变）
* val 表示不可变变量，或者不可被重载的方法，或者不可被继承的类
* abstract 抽象类/方法
* native 本地方法
* synchronized 同步方法
* transient 不持久化的字段
* volatile 原子性的字段
* strictfp 方法内的符点计算完全遵循标准
* data 类是一个data class：详见 [3.5 data class](#p3-5)

>`val` 其实就是Java的 `final`

<h2 id="p3-5">3.5 Data Class</h2>

编译器会为data class的每一个字段生成一个getter和setter。并生成无参构造函数，`toString()`, `hashCode()`, `equals(Object)`方法。此外，还会实现Serializable和Cloneable接口。

```kotlin
data class User(val name: String, val age: int)

user = User('cass', 22)
user.toString()   /* result is User(name='cass', age=22) */
```

你也可以定义自己的getter/setter/toString/hashCode/equals，编译器将跳过对应方法的生成。

<h2 id="p3-6">3.6 实例化</h2>

Latte不需要`new`关键字就可以实例化一个类。对于无参数的实例化，甚至不需要附加括号：

```kotlin
class Empty
empty = Empty

class User(id, name)
user = User(1, 'latte')
```

当然，Latte也允许你加上`new`：

```scala
empty = new Empty
user = new User(1, "latte")
```

不过，Latte中的`new`的“优先级”非常低，java中的`new X().doSth()`的写法在Latte中必须写为`(new X).doSth`这样的写法。

> Latte中建议不要写new

此外，Latte提供另外一种特殊的实例化方式  
调用无参构造函数，并依次赋值：

```python
class open(file, mode)
    public encoding

f = open('/User/a', "r", encoding='utf-8')
```

这是一个语法糖，相当于如下Latte代码：

```kotlin
f = open('/User/a', "r")
f.encoding = 'utf-8'
```

即：首先使用不带`=`的参数进行类型的实例化，然后把剩余“参数”看作对accessor的赋值操作。

这个语法糖不光适用于Latte定义的data class，还可以支持任意具有无参构造函数，并有可访问的field或者[accessor](#p3-3-3)的对象。例如标准Java Bean就可以使用这个语法。

```java
class User {
    private int id;
    private String name;
    public int getId() {
        return id;
    }
    public void setId(int id) {
        this.id = id;
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
}
```

<h2 id="p3-7">3.7 Object Class</h2>

```scala
object DataProviderManager
    def registerDataProvider(provider: DataProvider)
        ...
    val allDataProviders: Collection = ...
```

本质上，`object class`定义了一个类，但是这个类不能拥有构造函数的参数，构造函数为private，并拥有一个`static public val`的字段来存放单例。

object class可以继承父类、实现接口，其规则与普通的class完全相同

```scala
object DefaultListener : MouseAdapter()
    def mouseClicked(e: MouseEvent):Unit
        ...
    def mouseEntered(e: MouseEvent):Unit
        ...
```

你可以直接使用类名来获取这个单例对象：

```
val o = DataProviderManager
```

<h2 id="p3-8">3.8 隐式转换</h2>

隐式转换可以帮助你为对象扩展方法，从而更灵活的进行编程。

使用隐式转换分为四步

### 1. 定义一个类用来表示隐式转换后的类型

这一步是可选的，您可以直接隐式转换到已有的类型。这里为了说明方便，单独定义一个：

```scala
class RichInteger(i:Integer)
    minutes() = i + " minute" + ( if i != 1 { "s" } else { "" } )
```

### 2. 定义一个隐式对象：

隐式对象是一个普通的`object class`，不过需要用`implicit`修饰它。  
在其中定义一些"隐式方法"，用来进行隐式类型转换。

“隐式方法”就是普通的方法，不过需要用`implicit`修饰它。  
隐式方法有一些要求：参数只能有一个，其类型为隐式转换的源类型（或源类型的父类型）；返回类型为目标类型。

例如：我们想要将`Integer`转换为`RichInteger`

```scala
implicit object TestImplicitCast
    implicit def cast(i:Integer):RichInteger = RichInteger(i)
```

### 3. 启用隐式类

```scala
import implicit TestImplicitCast
```

使用`import implicit`引入隐式类。这里注意，隐式类必须使用类名引入，而且，即使隐式类与使用者定义在同一个包下，也必须显式引入。这样错用几率会比较小。

### 4. 使用

```
val x = 30 minutes
```

此时，`x`的值即为`"30 minutes"`。运行时会寻找可用的隐式转换，并判断转换后是否能够调用指定方法：发现`Integer => RichInteger`可用，且RichInteger可以调用`minutes()`方法；那么最终执行时的代码相当于`val x = TestImplicitCast.cast(30).minutes()`

<h1 id="p4">4. 函数类和Lambda</h1>

<h2 id="p4-1">4.1 函数类</h2>

<h3 id="p4-1-1">4.1.1 函数式接口 和 函数式抽象类</h3>

Java8中规定了：只有一个未实现方法的接口为“函数式接口”。例如：

```java
interface Consumer {
    void consume(Object o);
}
```

只有一个`consume`方法没有被实现，所以它是函数式接口。

类似的，Latte中除了支持Java的函数式接口外，还支持定义“函数式抽象类”。

如果一个类有无参的`public`构造函数，且只有一个未被实现的方法，那么这个类是一个函数式抽象类。例如：

```kotlin
abstract class F
    abstract apply()=...
```

<h3 id="p4-1-2">4.1.2 定义函数类</h3>

要使用函数式接口/抽象类，必须有特定的实现。Latte提供一种简便的方式来书写其实现类：

```kotlin
fun Impl(x, y)
    return x + y
```

使用`fun`关键字定义“函数类”，参数即为函数式类型未实现方法的参数，内部语句即为实现的方法的语句。

函数类默认可以不附加类型，它在编译期将被视为`FunctionX`，其中X为参数个数。在运行时，`FunctionX`可以转化为任何参数个数相同的类型。

如果确定其实现的类型，可以使用类型符号`:`定义：

```kotlin
fun MyTask : Runnable
    println('hello world')
```

<h3 id="p4-1-3">4.1.3 使用函数类</h3>

函数类会被编译为Java的类(`class`)，所以，Latte中，它既有函数的特征，又有类的特征：

* 函数类可以被import（就像类一样）
* 可以直接调用这个类，例如：`Impl(1, 2)`。这条语句将实例化`Impl`并执行它实现的方法（这个用法就像lambda一样）
* 可以将它作为值赋值给变量（实际上是调用了无参数构造函数，就像类一样）

总之，对于函数类，你完全可以把它当做一个变量来考虑。

```kotlin
Thread(MyTask).start()
```

同时由于它是正常的类，所以也具有父类型的所有字段/方法：

```kotlin
task = MyTask
task.run()
```

<h3 id="p4-1-4">4.1.4 函数式对象</h3>

在Latte中，对于所有的函数式对象，都可以使用“像调用方法那样的语法”。  

```js
task()
```

一个对象是“函数式对象”，有三种情形：

1. 它的类(`.getClass`)的直接父类为函数式抽象类
2. 它的类(`.getClass`)只实现了一个接口，且这个接口是函数式接口
3. 这个对象具有`apply(...)`方法

所以，在Latte中，函数式对象就是函数。

<h2 id="p4-2">4.2 高阶函数和Lambda</h2>

<h3 id="p4-2-1">4.2.1 高阶函数</h3>

如果一个函数可以接受另一个函数作为参数，或者返回一个函数，那么这个函数就是高阶函数。

[4.1.3 使用函数类](#p4.1.3) 中提到过“函数式对象”就是“函数”，所以，任何能够接收(并处理)函数式对象，或者返回函数式对象的函数/方法，就是高阶函数。

如下代码是Java使用stream api的做法：

```java
List<String> strList = ...;
strList.stream().
	map(s->Integer.parseInt(s)).
	filter(n->n>10).
	collect(Collectors.toList())
```

可以看到，map，filter都接受一个函数作为参数，所以它们也可以看作高阶函数。

在Latte中，上述代码可以用Latte的`lambda`语法来表达：

```kotlin
/// :scanner-brace

strList = ...
strList.stream.
	map { Integer.parseInt(it) }.
	filter { it > 10 }.
	collect(Collectors.toList())
```

<h3 id="p4-2-2">4.2.2 Lambda</h3>

Latte支持和Java完全一样的Lambda语法：

```java
strList.stream().
map(s->Integer.parseInt(s)).
filter(n->n>10).
collect(Collectors.toList())
```

从外观上看不出任何差别？没错，语法上完全一致（特别是使用大括号区分层次的时候）。  
使用缩进的情况下，多行lambda可以这么写:

```coffee
strList.stream.map(
    s-> s = s.subString(1)
        Integer.parseInt(s)
)
```

>注：可以不写return，因为Latte会帮你把需要的return补上。这个特性适用于任何“编译为JVM方法”的语法。

如果lambda只有一个参数（例如上述代码），那么名称和`->`可以被省略。其中，名称会被标记为`it`。

```js
strList.stream.map
    it = it.subString(1)
    Integer.parseInt(it)
```

这个特性可以让代码更简洁，同时也可以构造更灵活的内部DSL

```python
latteIsWrittenInJava
    if it is great
        star the repo
```

>做一丁点处理后，这是可以正常编译的代码！（不需要hack编译器）

可以写成一行

```coffee
latteIsWrittenInJava { if it is great { star the repo }}
```

此外Lambda的变量捕捉机制和Java不同。Latte可以在Lambda中的任何地方修改被捕获的变量。

```coffee
var count = 0
(1 to 10).forEach { count+=it }
println(count)
```

<h1 id="p5">5. 其他</h1>

<h2 id="p5-1">5.1 Json 集合</h2>

Latte支持Json格式的字面量。

Json数组：

```js
var list = [1, 2, 3, 4]
```

使用Json的数组语法，可以创建一个`java.util.LinkedList`实例，也可以创建一个数组。这取决于你将它赋值给什么类型的变量，或者使用`as`符号把它转换为什么类型。

int数组：

```c#
[1, 2, 3, 4] as []int
```

Object数组:

```c#
[1, 2, 3, 4] as []Object
```

在Latte中，你可以将一个`java.util.List`类型的对象转换为其它种类的Object。该特性将尝试使用无参构造函数构造目标类型对象，然后对每一个List中的元素，调用add方法。

```kotlin
class JsonArray
    list = []
    def add(o)=list.add(o)
res = [1,2,3] as JsonArray
/*
 same as:
 res = JsonArray()
 for item in [1,2,3]
     res.add(item)
 */
```

--

Json对象：

```swift
var map = [
    'one': 1,
    'two': 2,
    'three', 3
]
```

其中`,`是不必须的。“换行”和`,`都可以用来来分割list的元素，以及map的entry

在Latte中，你还可以把一个"所有键都是string"的map转换为指定类型的对象。

```kotlin
data class Bean(hello, foo)
res = [
    "hello" : "world"
    "foo"   : "bar
] as Bean

/* res will be Bean(hello=world, foo=bar) */
```

该转换将首先用无参构造函数构造指定类型，然后对map中每一个键，进行Latte的赋值操作。  
不光可以显式的转换，还可以作为方法参数隐式转换过去。

<h2 id="p5-2">5.2 范围</h2>

在Latte中可以使用`to`或者`until`运算符来定义一个“范围”（range）。这两个运算符只接受整数作为参数。它的结果是一个`java.util.List`实例。

使用`to`可以定义一个包含头和尾的范围，使用`until`可以定义一个只包含头，不包含尾的范围：

```scala
oneToTen = 1 to 10   /* [1, 2, 3, 4, 5, 6, 7, 8, 9, 10] */
oneToNine = 1 until 10  /* [1, 2, 3, 4, 5, 6, 7, 8, 9] */
```

range也支持尾比头更小：

```scala
tenToOne = 10 to 1   /* [10, 9, 8, 7, 6, 5, 4, 3, 2, 1] */
tenToTwo = 10 to 1   /* [10, 9, 8, 7, 6, 5, 4, 3, 2] */
```

>范围的实现基于“隐式类型转换”，Latte默认定义的RichInt提供了`to`和`until`方法。

<h2 id="p5-3">5.3 类型检查与转换</h2>

<h3 id="p5-3-1">5.3.1 类型检查</h3>

Latte为静态类型和动态类型混合的语言。总体来说，类型检查比较宽松，并且不一定在编译期检查。

Latte不要求显式转换，在赋值、方法return时，值会自动的尝试转换为需要的类型。

```kotlin
class Base
class Sub:Base

var x:Base = Sub
var y:Sub = x
```

其中在赋值给`y`时，自动增加了一个类型转换。

<h3 id="p5-3-2">5.3.2 类型转换</h3>

在调用方法时，自动转换并不会做，因为并不确定变量的类型，也不确定方法参数需要哪种类型。  
如果编译期没有找到方法，则会在运行时再获取参数对象的类型并进行方法的寻找。如果一定要在编译期确定调用何种方法，可以手动转换类型。

```kotlin
class Data
    i:int
    def setI(i:int) { this.i=i }

fun call(x)
    var data:Data = Data
    data.setI(x as int)
```

上述例子中，x类型不确定，但是可以显式地转化为int。

<h2 id="p5-4">5.4 运算符绑定</h2>

Latte支持运算符“绑定”。Latte的运算符绑定策略非常简单。每个运算符都看作方法调用。`a+b`看作`a.add(b)`，`a*b`看作`a.multiply(b)`，`!a`看作`a.not()`

这些运算符绑定依照`BigInteger`和`BigDecimal`的命名，所以你可以直接使用运算符来计算大数。

```kotlin
a = BigInteger(3)
b = BigInteger(4)
c = a + b
```

若需要绑定运算符，只需要写出签名相符的方法即可，例如定义一个`Rational`类来表示分数：

```kotlin
class Rational(a, b)
    add(that: Rational)=Rational(this.a * that.b + that.a * this.b, this.a * that.b)
    toString():String="${a}/${b}"

a = Rational(1, 4)  /* 1/4 */
b = Rational(3, 7)  /* 3/7 */
c = a + b           /* 19/28 */
```

有一些运算符是“复合”的，即：它们可以由多个操作构成。例如`++a`，就可以由`(a = a + 1 , return a)`构成。这类运算符不提供绑定，如果有需要，请绑定它们的展开式用到的运算符。

下表描述了所有提供绑定的运算符，以及一些展开式规则：

|  运算符  |   方法签名               |
|----------|-------------------------|
| a:::b    | a.concat(b)             |
| a * b    | a.multiply(b)           |
| a / b    | a.divide(b)             |
| a % b    | a.remainder(b)          |
| a + b    | a.add(b)                |
| a - b    | a.subtract(b)           |
| a << b   | a.shiftLeft(b)          |
| a >> b   | a.shiftRight(b)         |
| a >>> b  | a.unsignedShiftRight(b) |
| a > b    | a.gt(b)       |
| a < b    | a.lt(b)       |
| a >= b   | a.ge(b)       |
| a <= b   | a.le(b)       |
| a == b   | a.eq(b)   |
| a != b   | !a.ne(b)  |
| a in b   | b.contains(a) |
| a & b    | a.`and`(b)    |
| a ^ b    | a.xor(b)      |
| a | b    | a.`or`(b)     |
| !a       | a.logicNot()  |
| ~a       | a.not()       |
| -a       | a.negate()    |
| a\[0\]    | a.get(0)      |
| a\[0, 1\] | a.get(0, 1)   |

> `+a` 这种用法在Latte中不对`+`做任何处理，当做`a`  
> 上面的`a[0, 1]`用法，如果a是一个二维数组，则相当于java的`a[0][1]`  
> Latte对所有对象均可隐式转换到`RichObject`，在其中提供了`==`和`!=`的绑定，其中会调用对象的`equals`方法

Latte的运算符优先级和Java完全一致，而Latte特有的运算符优先级如下：

* `in` 和 `==` 优先级相同
* `:::` 优先级最高

由于`==`被绑定到equals方法，所以检查引用相同使用`===`，引用不同使用`!==`。  
此外，Latte还提供两个运算符`is`和`not`，它除了可以检查引用、equals，在右侧对象是一个Class实例时还可以检查左侧对象是否为右侧对象的实例。

> 虽然`==`绑定到equals方法，但是如果写为`null==x`，编译器能够知道左侧一定为null，这时会检查null值而不是调用equals
> `!=`同理。

| 运算符  |  展开式                          |
|--------|----------------------------------|
| a?=b   |  a = a ? b                       |
| a++    | tmp = a , a = a + 1 , return tmp |
| ++a    | a = a + 1 , return a             |
| a--    | tmp = a , a = a - 1 , return tmp |
| --a    | a = a - 1 , return a             |

> 其中`?=`的`?`代表任何二元运算符

---

Latte中，和普通的方法调用不同，运算符前不需要附加`.`，也不需要对参数包裹括号。但是因为Latte的运算符和方法调用是一回事，所以为了一致性，普通方法调用也可以将方法名看作运算符来书写：

```scala
list isEmpty // list.isEmpty()
map put "Feb", 2 // map.put("Feb", 2)
.println o // println(o)
```

使用逗号分隔多个参数。使用`.`表示直接调用方法（而不是在某个对象或者某个类上调用）。

<h2 id="p5-5">5.5 异常</h2>

Latte和Java总体上是类似的，但是仍有多处不同：

* Latte没有`checked exception`
* Latte可以`throw`任何类型的对象，比如`throw 'error-message'`
* Latte可以`catch`任何类型的对象
* 由于上一条，Latte不提供`catch(Type e)`这种写法，需使用if-elseif-else来处理

```kotlin
fun isGreaterThanZero(x)
    if x <= 0 { throw '${x} is littler than 0' }

var a = -1
try
    isGreaterThanZero(a)
catch e
    e.toCharArray  /* succeed. `e` is a String */
finally
    a = 1
```

<h2 id="p5-6">5.6 注解</h2>

Latte使用`annotation`关键字定义注解

```kotlin
annotation Anno
    a:int = 1
    b:long
```

Latte中的注解默认为运行时可见的（而Java中默认不可见）。  
可以使用`java::lang::annotation::Retention`注解重新规定可见性。  
同时默认为可以标注在所有地方（和Java一样）。  
可以使用`java::lang::annotation::Target`注解重新规定标注位置。

> 因为annotation是一个关键字，所以导入包时需要用 点号 包围 `annotation`这个单词。

---

Latte的注解使用方式和Java一致

```kotlin
class PrintSelf
    @Override
    toString():String='PrintSelf'
```

不过有一点要注意，Latte的注解不能和其所标注的对象在同一行，除非加一个逗号，比如：

```scala
@Anno1,@Anno2,method()=...
```

和Java一样，注解的value参数可以省略`value`这个键本身。

```java
@Value1('value')
@Value2(value='value')
```

<h2 id="p5-7">5.7 过程(Procedure)</h2>

Latte支持把一组语句当做一个值，这个特性称作“过程”。  
过程由小括号开始，小括号结束。

用这个特性可以省略不必要的中间变量声明。

```kotlin
class Rational(a, b)
    toString():String = a + (
        if b == 1
            return ""
        else
            return "/" + b
    )
```

过程最终也是编译为“方法”的，可以省略最后的`return`，所以可以写为：

```kotlin
;; :scanner-brace
class Rational(a, b)
    toString():String = a + ( if b==1 {""} else {"/" + b} )
```

<h2 id="p5-8">5.8 参数可用性检查</h2>

Latte支持**参数**上的null值或“空”值检查，分别使用`nonnull`和`nonempty`修饰符。

由于Latte的`Unit`方法也返回一个值（`Unit`），所以在`nonnull`中不光会检查null值，还会检查Unit。  
如果出现null则会立即抛出`java.lang.NullPointerException`异常  
如果出现Unit则会立即抛出`java.lang.IllegalArgumentException`异常

```scala
def add(nonnull a, nonnull b)= a + b

add(null, 1)      /* 抛出NullPointerException */
add(Unit, 2) /* 抛出IllegalArgumentException */
```

对于`nonempty`，检查的范围更广。首先Latte会将这个值转换为`bool`类型（Latte中任何类型都可以转为bool)  
如果结果为`false`则会抛出异常`java.lang.IllegalArgumentException`

```scala
def listNotEmpty(nonempty list)

listNotEmpty([])  /* 抛出 IllegalArgumentException */
```

在转换为`bool`时，Latte会尝试

1. 如果是null，则返回false
2. 如果是Unit，则返回false
3. 如果是Boolean类型，则返回其对应的`bool`值
4. 如果是数字类型，则：如果转换为`double`的结果是0，那么返回false，否则返回true
5. 如果是Character类型，则：如果转换为`int`的结果是0，那么返回false，否则返回true
6. 如果这个对象带有`def isEmpty:bool`或者`def isEmpty:Boolean`方法，那么调用之，并返回相应结果
7. 返回true

<h2 id="p5-9">5.9 解构</h2>

<h3 id="p5-9-1">5.9.1 解构用法</h3>

解构指的是将一个对象分解为其组成部分的多个对象。

例如有如下定义和实例化：

```kotlin
data class Bean(a,b)

val bean = Bean(1,2)
```

可以知道，bean是由`1`和`2`组成的，它应当被分解为(1,2)。  
Latte提供这样简化的分解：

```scala
val (x,y) = bean
```

定义了x和y，并分别赋值为1、2。

<h3 id="p5-9-2">5.9.2 解构实现方式</h3>

使用解构，首先需要定义一个static方法`unapply`：

```java
class X {
    static {
	    unapply(o)=...
	}
}
```

这个方法需要接受一个参数，表示被解构的对象，并返回`null`或一个`java::util::List`实例。

如果返回`null`则说明解构失败，如果返回`List`实例，则表示会被分解为存在于列表中的对象。

如果解构失败，则解构表达式返回`false`，否则返回`true`。

可以指定使用“带有unapply方法的类”来执行解构：

```scala
Bean(x,y) <- bean
```

如果没有指定，则尝试使用右侧对象的类中的unapply方法进行解构。

```scala
(x,y) <- bean /* 相当于 Bean(x,y) <- bean */
```

如果没有指定类型，则可以将`<-`替换为`=`。

解构可以放在`if`中使用：

```scala
if List(a,b,c) <- o
    println("result is ${a},${b},${c}")
else
    println("destruct failed!")
```

<h2 id="p5-10">5.10 模式匹配</h2>

和`scala`一样，`Latte`不提供`java`的`switch`语句，但是提供更强大的模式匹配。

```scala
def doMatch(o) = o match
    case 1 => ... /* 根据值匹配 */
    case b:Apple => ... /* 检查类型并定义一个新的变量 */
    case _:Banana => ... /* 根据类型匹配 */
    case Bean(x,y) => ... /* 根据解构匹配 */
    case Bean(1, Bean(x, _:Integer)) => ... /* 多重模式 */
    case Bean(x,y) if x > 0 => ... /* 解构后再做判断 */
    case _ => ... /* 匹配所有（默认行为） */
```

模式匹配会从上到下依次尝试匹配。如果匹配成功则进入该分支执行语句，最终返回一个值（也可能返回`Unit`）。如果匹配失败，则会抛出`lt::lang::MatchError`。

任何匹配模式都可以添加if语句，仅当if判断成立时才会进入执行。

<h1 id="p6">6. Java交互</h1>

在设计时就考虑了Latte和Java的互操作。所以它们基本是无缝衔接的。

<h2 id="p6-1">6.1 在Latte中调用Java代码</h2>

实际上这里不会出现任何问题。Latte源代码最终是编译到Java字节码的，所以Latte调用Java就像Latte调用自己一样。  
而设计时也考虑到了互通性，几乎所有Latte特性都可以通过编写Java源代码来模拟。

这里给出一些Latte与Java相同语义的表达：

### 1. 规定变量和它的类型

在Field操作时会有交互。

java:

```java
      Integer integer;
      List    list;
final int     anInt;
      Object  obj;
```

latte:

```scala
    integer : Integer
    list    : List
val anInt   : int
    obj
```

Latte可以使用`var`表示可变变量，不过也可以不写，默认即为可变变量。Object类型不需要写，同样也是默认值。

### 2. 定义方法、参数类型和返回类型

在调用方法时会有交互。

java:

```java
void method1() {}
Object method2() { return null; }
int method3(int x) { return x; }
```

latte:

```kotlin
method1():Unit=...
method2()=null
method3(x:int):int = x
```

### 3. 获取java类，判断类型

java:

```java
Class c = Object.class;

if (s instanceof String) {}
```

latte:

```typescript
c = type Object

if s is type String
```

<h2 id="p6-2">6.2 在Java中调用Latte代码</h2>

如果是已编译的Latte二进制文件，那么加载到class-path中，直接在Java中调用即可。Latte在设计时非常小心的不暴露任何“不一致状态”给Java，所以除了反射访问private外，尽管放心的调用吧。

<h3 id="p6-2-1">6.2.1 在Java中编译Latte</h3>

如果是Latte源文件，则需要使用Latte-compiler编译。

```java
import lt.repl.Compiler;

Compiler compiler = new Compiler();
ClassLoader cl = compiler.compile(new HashMap<String, Reader>(){{
		put('source-name.lt', new InputStreamReader(...));
}});
Class<?> cls = cl.loadClass('...');
```

<h3 id="p6-2-2">6.2.2 在Java中执行eval</h3>

Latte支持`eval`，在latte代码中`eval('...')`即可。在Java中，你也可以直接调用

```java
lt.lang.Utils.eval("[\"id\":1,\"lang\":\"java\"]");
```

或者使用`Evaluator`获取完整的eval支持:

```java
List<Integer> list = Arrays.asList(1, 2, 3, 4, 5);

Evaluator evaluator = new Evaluator(new ClassPathLoader(Thread.currentThread().getContextClassLoader()));
evaluator.setScannerType(Evaluator.SCANNER_TYPE_BRACE);
evaluator.put("list", list); // 把list对象放进Evaluator上下文中
Evaluator.Entry entry = evaluator.eval("" +
	"import java::util::stream::Collectors._\n" +
	"list.stream.filter{it > 0}.collect(toList())");
List newList = (List) entry.result;
// newList is [3, 4, 5]
```

<h3 id="p6-2-3">6.2.3 在Java中执行Latte脚本</h3>

Latte支持脚本，脚本以源代码形式呈现。所以你可以构造一个`ScriptCompiler`来执行并取得脚本结果。

```java
ScriptCompiler.Script script = scriptCompiler.compile("script", "return 1");
script.run().getResult();
// 或者 run(new String[]{...}) 来指定启动参数
```

ScriptCompiler有多个`compile`的重载，各种情况都可以方便的调用。
