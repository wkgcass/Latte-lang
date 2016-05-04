#LessTyping语法规则
##文件结构
###扩展名
LessTyping使用`.lt`作为后缀名。

词法有如下几个特性：

1. 不需要明确的结束符号，由换行结束。若到达行末，而一个表达式尚未结束，则继续取下一行的表达式。
2. 使用类似`python`的4空格强制缩进。拥有相同缩进的连续的行视为同一`层`
	
		┌────────────────┐
		│class A         │
		│    ┌─────────┐ │
		│    │a=1      │ │
		│    │static   │ │
		│    │    ┌───┐│ │
		│    │    │b=2││ │
		│    │    │c=3││ │
		│    │    └───┘│ │
		│    │d=4      │ │
		│    └─────────┘ │
		│class B         │
		└────────────────┘
	
	其中，`class A`和`class B`于同一层  
	`a=1`,`static`,`d=4`于同一层  
	`b=2`,`c=3`于同一层
		
3. 有几个符号，若遇到则后续内容视为+4缩进，并与其后连续的同缩进的行视为同一`层`。符号如下：`#`,`#>`,`(`,`[`,`{`。其中`(`,`[`,`{`有其对应`)`,`]`,`}`。无论该“对应”处于哪一层，后续同行内均视为开始时的那一层。
	
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
		
	其中，`classA(a)`中的`a`缩进为4，而之前的`classA(`和之后的`):B`缩进均为0  
	`arg0`，`arg1`，`arg2`，`arg3`所在层的缩进为8。(`,`前后视为同一层)

4. 每个换行将产生一个[EndNode]。在不产生歧义时，该节点也可被取消。每个`,`将产生一个[StrongEndNode],该结束节点不会被取消，也不会被[1]中的策略越过。

5. 一个比当前缩进+4的缩进将在词法分析时新开一个节点，且连续的同缩进的行中解析的节点将在这个新节点后附加。以[3]为例，将生成如下词法树（通常词法分析只会生成一个流，不过这里使用树可以极大的简化后续解析）

		-[class]-[A]-[(]-[│]-[)]-[:]-[B]-[│]-
		                  └[a]-    ┌──────┘
		┌──────────────────────────┘
		└────[method]-[(]-[│]-[)]-[│]-
		┌──────────────────┘       └───────[pass]-
		│
		└──[arg0]-[EndNode]-[arg1]-[EndNode]-[arg2]-[StrongEndNode]-[arg3]

###注释
注释由`;`开始。

	; comment
	
纯注释行不受缩进影响

###基本类型
java中的基本类型没有被舍弃。由于弱类型，而且在编译期和运行时都可以自动装拆包，所以基本类型和引用类型差别并不是很大。

* int
* short
* byte
* boolean
* char
* long
* float
* double

###运算符

大多数运算符都与方法进行了绑定。名称参考了`java.math.BigDecimal`和`java.math.BigInteger`

例如`a+b`，可以理解为`a.add(b)`

一元运算符如下：

	"++", "--", "!", "~", "+", "-"
	
一元运算符之间不存在优先级高低，但总是比二元运算符高。其中`++`,`--`可放置在前面(即unary)和后面。其他一元运算符只能放置在前面。

二元优先级从高到低如下：

	".."(从a到b，包括b), ".:"(从a到b，不包括b)
	"^^"(a的b次方)
	"*", "/", "%"
	"+", "-"
	"<<", ">>", ">>>"
	">", "<", ">=", "<="
	"=="(equals), "!="(!a.equals(b)), "==="(引用相等), "!=="(引用不等), "=:="(equal), "!:="(notEqual), "is", "not", "in"(b.contains(a))
	"&"
	"^"
	"|"
	"&&"
	"||"
	
除了明确标注的外，都与java运算符或者语意含义相同

对于比较运算符，若方法绑定找不到，则还会检查是否为`java.lang.Comparable`实例。若是，则会调用compareTo方法进行比较。

`LessTyping`不提供三元运算符(例如常见的`?:`)，因为有更好用的“闭包”可以代替之

###一元运算符
####++
自增运算符。可以放置在之前或之后。

	++a
	a++

`++a`被转换为`a=a+1`
`a++`被转换为

	JVM操作数栈.push(a)
	a=a+1
	JVM操作数栈.pop()

最后留在JVM操作数栈中的是运算之前的值

####--
自减运算符。可以放置在之前或之后

	--a
	a--

`--a`被转换为`a=a-1`
`a--`被转换为

	JVM操作数栈.push(a)
	a=a-1
	JVM操作数栈.pop()

最后留在JVM操作数栈中的是运算之前的值

####!

逻辑非运算，绑定了`logicNot`方法

####~

按位非，绑定了`not`方法

####+

一个不会进行任何操作的运算符

####-

取负，绑定了`negate`方法

####..
`range`运算符，左右只能为`int`或`java.lang.Integer`，生成一个从左开始到右结束的数字`List`

	1..9 // 1,2,3,4,5,6,7,8,9
	9..1 // 9,8,7,6,5,4,3,2,1
	1..1 // 1

####.:
`range`运算符，左右只能为`int`或`java.lang.Integer`，生成一个从左开始到右结束的数字`List`。

	1.:9 // 1,2,3,4,5,6,7,8
	9.:1 // 9,8,7,6,5,4,3,2
	1.:1 // empty
	1.:2 // 1

####`^^`
将左右均转换为double后调用`Math.pow(double,double)`

####数学运算符

	"*", "/", "%"
	"+", "-"
	
其行为与java一致。方法绑定如下

* `*` multiply
* `/` divide
* `%` remainder
* `+` add
* `-` subtract

####位移运算符

	"<<", ">>", ">>>"
	
其行为与java一致。方法绑定如下

* `<<` shiftLeft
* `>>` shiftRight
* `>>>` unsignedShiftRight

####比较运算符

	">", "<", ">=", "<="
	"==", "!=", "===", "!==", "=:=", "!:=", "is", "not", "in"
	
行为与java基本一致。

其中，`==`与`!=`调用对象的equals方法。对于基本类型将装包后再调用。`!=`可以理解为`!(a==b)`

`===`比较值或引用。若将基本类型和引用类型进行该比较，将产生一个编译期错误（和java一样）

`=:=`和`!:=`只是为了方法绑定，并看起来比较像等于号。

`is`和`not`行为见后文。

`in`也仅仅绑定方法，绑定了运算符后者的`contains`方法

方法绑定如下

* `>` gt
* `<` lt
* `>=` ge
* `<=` le
* `==` equals
* `!=` !(equals)
* `===` 无
* `!==` 无
* `=:=` equal
* `!:=` notEqual
* `is` lt.lang.Lang.is(a,b)
* `not` lt.lang.Lang.not(a,b)
* `in` 后者的contains

####is

从上到下，遇到true则返回，否则继续

1. a==null && b==null
2. a==b || a.equals(b)
3. b类型为Class，则b.isInstance(a)
4. 调用a.is(b) 然后转为布尔值(若不存在is(?)方法则返回false)

####not

从上到下，遇到true则返回false，否则继续

1. a==null && b==null
2. a==b || a.equals(b)
3. b类型为Class，则b.isInstance(a)
4. 调用a.not(b) 然后转为布尔值并返回(若不存在not(?)方法则返回true)

####逻辑运算

	"&"
	"^"
	"|"
	"&&"
	"||"

其行为与java一致。方法绑定如下

* `&` and
* `^` xor
* `|` or
* `&&` 无
* `||` 无

其中`&&`与`||`存在短路特性。即对于`&&`来说，只有左侧为`true`情况下，才会计算右侧的值。对于`||`来说，只有左侧为`false`情况下，才会计算右侧的值。

###类型规范
虽然是弱类型语言，但要想与java互通，则必须加入类型。

但是也不能少了弱类型语言的优势。

####类型转换
使用`as`进行转换。

	variable = 1 as long

将长整型的1赋值给variable

####类型匹配时
若类型匹配，例如可以取得字段，能找到要调用的方法等情况，采用与java一致的字节码来完成

例如

	variable:Number=1
	variable.intValue()

上述方法调用为`invokevirtual`。生成的字节码与java同语句毫无区别。

基本类型、数组也类似，将直接使用类似`iadd`，`aaload`之类的指令完成运算。

####类型不匹配时
弱类型总是会出现找不到字段，方法的情况。

对于找不到的方法，统一使用`invokedynamic`来完成调用。将在运行时寻找方法。也采用。若仍旧找不到，则在运行时报错。

对于找不到的字段，则会使用`lt.lang.Lang.getField(object,name,callerClass)`来完成。设置字段使用`lt.lang.Lang.putField(object,name,callerClass)`来完成。它们都采用反射获取字段。若找不到字段或者不可设置字段，则在运行时报错。

对于`[]`运算符，例如`arr[i]`，若arr不是数组类型，则调用`get(?)`方法。
`[]`内的变量即为`get`方法的参数。

对于`[?1]=?2`，例如`arr[i]=1`，若arr不是数组类型，则调用`set(?1,?2)`或者`put(?1,?2)`方法。优先查找`set`，若找不到则寻找`put`。其中`[]`内的变量为`?1`参数。`=`后的变量为`?2`参数。

（实际上invokedynamic调用`set`时若无法找到就会将方法名换为`put`再尝试一次）

###赋值
`LessTyping`的赋值附带返回值。  
而所谓的`+=`,`-=`,`*=`,`/=`,`%=`，都被转换为对应的完整的表达式，例如`a+=b` => `a=a+b`。  

对于前置`++`和`--`，若变量为数字类型，则进行对应运算，否则转换为`a=a+1`  
对于后置`++`和`--`，若变量为数字类型，则进行对应运算，否则转换为`a;(备份);a=a+1;`。而后置`++`和`--`返回值为运算之前的值。

###方法调用
方法调用与java一致，使用`方法名(参数表)`进行。

	a.equals(b)
	Collections.emptyList()
	add(e) // this.add(e) 或 ThisClass.add(e) 或 调用引入的static方法

不过要注意，java中，在对象上调用static方法是合法的，只不过会产生一个警告，但是`LessTyping`中，static方法只能在类型上调用。否则会转到`invokedynamic`再进行调用（依然可能调用到static方法）。

###命名空间
命名空间即为java中的包。

一个`lt`文件中的类型均处于同一个命名空间中。

若包为`(default package)`则无需做命名空间的声明。否则，`lt`文件的第一个有效行必须为命名空间声明。

	# packageName

嵌套的命名空间使用`::`分隔，例如

	# lt::lang

一个`lt`文件允许最多出现一次命名空间的声明。

###引入
类、静态成员、静态方法的引入使用`#>`符号。

	#> java::lang::_ ; 引入java::lang中的所有类型
	#> java::util::List ; 引入类 : java::util::List
	#> java::util::Collections._ ; 引入java::util::Collections中的静态成员和方法
	
默认将`当前命名空间`放在引入的首位, `java::lang::_`与`lt::lang::_`依次放在倒数2，1的位置引入

一个`#>`可以包含多个引入项。

	#>  java::lang::_
	    java::util::List
	    java::util::Collections._

在寻找类型时，将优先查找明确给出的类型。例如：

	#> java::awt::_
	#> java::util::List
	
此时`List`将匹配`java::util::List`而不是`java::awt::List`

若找不到，则从上往下依次寻找指定类型

`#>`可以放在0缩进层的任何位置。全局均可生效。

###类型
`LessTyping`允许定义类和接口。由于可以与java互通，其他类型可由java文件定义。

类型的定义允许放在0缩进层的任何位置。

####类

	[modifier1 [modifier2 [...]]] class <类名> [(构造函数参数表)][:[父类 [(父类构造函数参数)]][父接口1 [,父接口2, [...]]]]
	    [构造函数内容]
	
	abs class AbsCls(a,b):Parent(a),Interface1,Interface2
	
类定义由`class`为标志。其之前可附加修饰符。但是，类永远为public，其前的访问修饰符仅对构造函数生效。

构造函数参数表可以为空甚至不写。若不写则为无参构造函数。  
`:`后的父类为需要继承的类，若不写父类构造函数参数则默认为调用父类无参构造函数。  
若不存在父类则视为继承自`java.lang.Object`  
若同时出现父类和父接口，则父类必须出现在`:`后的首位。父类和(多个)父接口以`,`隔开。

类中的内容为构造函数内容或者方法的定义或者字段定义。

	class A(arg)
	    a:int
	    b=1
	    method():Unit

与如下java代码相同

	public class A {
	    private Object arg;
	    private int a;
	    private Object b;
	    public A(Object arg){
	        this.arg=arg;
	        this.b=1;
	    }
	    public void method(){ }
	}
	
也就是说，构造函数中不会出现局部变量，都会被视为java的字段(Field)。传入的参数也会直接当做字段并进行赋值。

static字段和方法使用如下方式定义

	class A
	    static
	        staticField=1
	        staticMethod():Unit

####接口
	
	[modifier1 [modifier2 [...]]] interface <接口名> [:父接口1 [,父接口2, [...]]]
	    [接口内容]
	
与`类`类似，接口中字段、方法定义方式也是如此。只不过接口中的字段均为public static final字段。方法可以不做实现。

在接口中，若方法为默认方法且为空方法则必须至少有一行`pass`语句。否则会被视为abstract方法

`method()=pass`并不会增加`pass`语句。它仅仅是为了不产生歧义(`method()`会被视为方法调用)

##变量

	[修饰符1 [修饰符2]] <变量名> [:类型] [=初始值]

例如

	i=1
	i:int=1
	i:int

若不指定类型，则类型为`java.lang.Object`

在`LessTyping`中，尽量模糊了字段和局部变量的区分。只不过由于需要与java互通，才不得不区分出字段。

在构造块中的变量（包括参数）都会被看做字段。字段默认是private的。所以即使这么写也不必担心被外界访问和修改。当然，也可以手动指定访问修饰符。

不过，若对参数赋值，改变的是局部变量而不是字段。

静态块中的变量也是字段，同样也是private。

方法中的变量均为局部变量。

实际上，即使考虑字段和局部变量，也可以总结为，内层可以访问外层的变量，反之则不可。

##方法
`LessTyping`不是一个函数式语言。函数/方法并不是第一公民。

方法可以在类型中进行定义

	[修饰符1 [修饰符2]] <方法名>([参数表])[:返回类型][=单条语句]
	    [语句]
	    
例如
	
	method(arg0,arg1:int):Unit=pass
	; 方法名method
	; 参数表 arg0:Object, arg1:int
	; 返回值 void
	; 方法内容为空
	    
其中，返回类型可选，若不加类型则返回类型为`java.lang.Object`

方法无须进行值的返回。若方法需要返回值，则字节码层面将自动加上返回值。基本类型将返回默认值，例如0，false等。引用类型将返回null。

##流程控制
`LessTyping`包含如下流程控制

###for

	for <variable> in <iterator/iterable/array/enumeration>
	    ...

for循环将遍历可迭代对象，然后执行对应操作

若需要访问下标，通常可以这么做

	for i in 0.:list.size()
	    o=list[i]

从 `0`到 `list.size()` `(exclusive)`取下标。

###while
while分为`while`和`do-while`

while
	
	while <expression>
	    ...

do-while
	
	do
	    ...
	while <expression>

规则和java一致。

其中`expression`类型任意，它将在运行时会被转化为`bool`型。(详见类型转换说明)

##条件分支if
	
	if <expression>
	    ...

其中`expression`类型任意，它将在运行时会被转化为`bool`型。

##异常Try-Catch

	try
	    ... ; statements might throw exception
	catch <exVar>
	    <ExceptionType1 [,ExceptionType2...]>
	        ... ; exception handle
	    <ExceptionType1 [,ExceptionType2...]>
	        ... ; exception handle
	    ...
	...

首先由try开始，后接catch，在catch后定义接收异常的变量名，然后再缩进4格声明异常类型

##同步块

	sync(var1,var2,var3,...)
	    ...

含义与java中的

	synchronized(var1){
	    synchronized(var2){
	        synchronized(var3){
	            ...
	        }
	    }
	}
	
相同。

##JSON Literal
###数组
在`LessTyping`中，数组这样定义

	[elem1, elem2, elem3, ...]

它会根据所需类型的不同而构造不同的类型

例如

	a:[]int = [1,2,3]

则会返回一个`int`数组

	a:[][]int = [[1,2],[],[3]]
	
则会返回一个`int`二维数组

	a:[]Object = [1,2,3]
	
则会返回一个含有`Integer(1)`,`Integer(2)`,`Integer(3)`的数组

	a = [1,2,3]
	
则会返回一个`java.util.LinkedList`，其中包含元素1,2,3

###Map
在`LessTyping`中，Map可以这样定义

	{
	    key1 : value1
	    key2 : value2
	}

末尾的逗号可加也可不加。

将返回一个`LinkedHashMap`，并向其中填充元素。

此特性与`json`格式一致。在写web时可以很好的利用此特性进行restful编程。