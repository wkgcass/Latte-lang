#LessTyping

LessTyping is a JVM language. It's highly readable and extensible.

LessTyping supports 

* Operator Binding
* DSL
* Pre-Processing define/undef
* Inner Method
* Lambda
* JSON Literal
* Read Eval Print Loop
* many other features

[LessTyping WebSite](http://lesstyping.cassite.net/)

`LessTyping` is based on java 8. it's a hybrid of strong and weak typing language. It's compiled to JVM byte code, and can collaborate with any java library.

最下面有中文 ：）

##How to build

`JRE 8` is required. And use `Maven 3` to build automatically

clone the repository, and run

	mvn clean package
	
You will get a runnable jar (`repl.jar`), `lesstyping` and `lesstyping.bat`. The last two files can help you run the `repl`.

run:

	./lesstyping
	
then the [REPL](https://github.com/wkgcass/LessTyping/blob/master/src/main/java/lt/repl/REPL.java) starts
    
	Welcome to LessTyping
	Type in expressions and double Enter to have them evaluated.
	Type :help for more information.
	for syntax help, please visit https://github.com/wkgcass/LessTyping/
    
    >1+1
    |
    res0 : java.lang.Integer = 2
    
    >

##Compile `lt` Files

start the `REPL` interpreter, and construct a `Compiler`

	compiler = Compiler()

use `>>` operator to specify output directory

	compiler >> '...'
	// or
	compiler >> File('...')
	
use `+` operator to add class-path

	compiler + '...'

use `compile MAP` to specify source codes to be compiled and start compiling

	compiler compile {'fileName':FileObject}
	
usually `filesInDirectory('...')` is used, e.g.

	compiler compile filesInDirectory('...')
	
these method invocations can be chained up

	Compiler() + '...cp...' >> '...output...' compile filesInDirectory('...source...')
	
##Scripts

start the `REPL` interpreter  
type `:script <script file>` and Enter

then use `script run` or `script run ['string array']` to run the script

or, you can run a script when running the jar

	java -jar the_jar.jar script-file-path script-arguments...

##README

check `src/test/resources/lang-demo` for syntax tutorials.

here's what every file explains

* [advanced.lt](https://github.com/wkgcass/LessTyping/blob/master/src/test/resources/lang-demo/advanced.lt) -- something about `inner method`, `procedure`, and `lambda`
* [literals.lts](https://github.com/wkgcass/LessTyping/blob/master/src/test/resources/lang-demo/literals.lts) -- about all literals in LessTyping
* [ltFileStructure.lt](https://github.com/wkgcass/LessTyping/blob/master/src/test/resources/lang-demo/ltFileStructure.lt) -- the general idea of a LessTyping file structure
* [operator.lt](https://github.com/wkgcass/LessTyping/blob/master/src/test/resources/lang-demo/operator.lt) -- info about operator binding
* [statements.lts](https://github.com/wkgcass/LessTyping/blob/master/src/test/resources/lang-demo/statements.lts) -- available statements in LessTyping, e.g. for, if, while...
* [typeDef.lt](https://github.com/wkgcass/LessTyping/blob/master/src/test/resources/lang-demo/typeDef.lt) -- things about definition of classes and interfaces

Generally the syntax looks like `Python`,  4 spaces indentation is forced , and it doesn't require `;` at the end of a statement/expression

comments are started by `;` , so it's ok if you like writing a `;` at the end of a line

multiple line comments are started with `/*` and end with `*/`

##Syntax Hightlighting
click [here](https://github.com/wkgcass/Atom-LessTyping-Highlighting) for highlighting on ATOM.   
You can search for `Atom-LessTyping-Highlighting` in `atom`, or follow the instructions in the link to have the plugin installed.

![](http://lesstyping.cassite.net/images/highlight.png)

--

##Operator Binding

`LessTyping` supports operator override  
besides built in operator behavior, there are several operators can be overridden, which will be converted to method invocation

e.g.

	list + o
	; same as
	list.add(o)
	
>check [operator.lt](https://github.com/wkgcass/LessTyping/blob/master/src/test/resources/lang-demo/operator.lt) for more info

--

##DSL

`LessTyping` supports a syntax that makes method invocation looks like using operators. It's useful for DSL programming.

e.g.

    db select id, name from User where id > 10 and name != 'cass' orderBy id.desc
    
which would be converted into method invocation

    db.select(id,name).form(User).where(id>10).and(name!='cass').orderBy(id.desc)
    
>check [statements.lts](https://github.com/wkgcass/LessTyping/blob/master/src/test/resources/lang-demo/statements.lts) for more info

--

##define/undef
    
`LessTyping` supports pre processing `define` and `undef`

e.g.

    define 'CREATE TABLE' as 'class'
    define 'VARCHAR' as ':String'
    define 'NUMBER' as ':int'
    
    CREATE TABLE User(
        id NUMBER
        name VARCHAR
    )
    
    undef 'CREATE TABLE'
    undef 'VARCHAR'
    undef 'NUMBER'
   
>check [ltFileStructure.lt](https://github.com/wkgcass/LessTyping/blob/master/src/test/resources/lang-demo/ltFileStructure.lt) for more info

--

##Inner Method

an `Inner Method` means methods inside another method

e.g.

	outer()
		i=1
		j=2
		inner()
			return i + j + 3
		; the inner method returns i + j + 3
		res = inner()
		; res should be 6
		i=4
		res = inner()
		; res should be 9

NOTE THAT unlike may other languages, the inner method only captures the local variable values, but *cannot* change them. the changes made in the inner method cannot effect the local variables in the outer one

--

##Procedure

a `Procedure` is a list of statements surrounded by parenthese

e.g.

	res = (
		if a is null
			return null
		else
			return a+b
	)

if a is null, then the procedure's result would be null, otherwise, the result would be `a+b`. and the result would be assigned to variable `res`

--

##Lambda

in `LessTyping`, lambda can not only perform on `Functional Interfaces`, but also on `Functional Abstract Classes`.

a `Functional Abstract Class` means an abstract class with public constructor whose parameter count is 0, and the class have 1 and only 1 unimplemented method.

e.g.

	abstract class Func
		abstract apply(o)=...  ; defines an abstract method
		
	func1 : Func = (x) -> x+x
	func2 : java::util::function::Function = (o) -> o+1
	func3 = (o) -> o + 1
	
	list.stream().map((e)->e.toString).collect(Collectors.toList())

>func1 is type Func    
>func2 is type java.util.function.Function    
>func3 is type lt.lang.function.Function1

there are 27 Functions defined in `lt.lang.function`, ranges from Function0 to Function26. if no type is required, the lambda expression generates these functional interfaces' implementations.

>for more info about `inner method`, `procedure` and `lambda`, check [advanced.lt]([advanced.lt](https://github.com/wkgcass/LessTyping/blob/master/src/test/resources/lang-demo/advanced.lt))

--

##Types

`LessTyping` is a hybrid of strong and weak type language.

	variable : Type
	method() : ReturnType
	
	; or simple write :
	
	obj = 1
	method()=2
	
--

##JSON Literal

	map = {
		"id" : 23333,
		"name" : "cass",
		"repositories" : ["LessTyping", "Style", "JSONBind"]
	}

`,` at the end can be omitted

>check [literals.lts](https://github.com/wkgcass/LessTyping/blob/master/src/test/resources/lang-demo/literals.lts) for more info

--

##Class Definition

	class Foo(bar : int)
		field : int
		method()=...
	
defines a class. in java, we write :

	public class Foo {
		private int bar;
		private int field;
		
		public Foo(int bar){
			this.bar=bar;
		}
		
		public Object method(){
			return null;
		}
	}

>check [typeDef.lt](https://github.com/wkgcass/LessTyping/blob/master/src/test/resources/lang-demo/typeDef.lt) for more info

--

##REPL

run `lt.repl.REPL`, or start `jar` file generated via `maven package`, you will see

    lt>
   
Type expressions and double `Enter`, you will get the result

---------

##中文版 
Chinese Version README

LessTyping是一种JVM编程语言。 它非常可读，同时也非常可扩展。

LessTyping 支持如下功能

* 运算符绑定
* DSL
* 预处理 define/undef
* 内部方法
* Lambda
* JSON 字面量
* Read Eval Print Loop
* 许多其它特性

[LessTyping 网站](http://lesstyping.cassite.net/)

`LessTyping`基于java8。它既有强类型语言的特性，又有弱类型语言的特性。它被编译到JVM字节码，可以与任何Java类库完美互通。

##如何构建工程

环境需要 `JRE 8` 和 `maven 3`

clone这个仓库,然后执行

	mvn clean package
	
你将会获取一个可执行jar (`repl.jar`) 以及 `lesstyping` 和 `lesstyping.bat`, 可以快捷地在任何操作系统上开启`repl`.
                       
执行:
                       
	./lesstyping
	
接着, [REPL](https://github.com/wkgcass/LessTyping/blob/master/src/main/java/lt/repl/REPL.java) 将开始运行
    
	Welcome to LessTyping
	Type in expressions and double Enter to have them evaluated.
	Type :help for more information.
	for syntax help, please visit https://github.com/wkgcass/LessTyping/
        
    >1+1
    |
    res0 : java.lang.Integer = 2
    
    >

##Compile `lt` Files

开启`REPL`, 然后构造一个`Compiler`

	compiler = Compiler()
	
使用 `>>` 运算符来指定编译输出目录

	compiler >> '...'
	// or
	compiler >> File('...')
	
使用 `+` 运算符来添加 class-path

	compiler + '...'

使用 `compile MAP` 来确定源代码并立即开始编译

	compiler compile {'fileName':FileObject}
	
通常来说会使用 `filesInDirectory('...')`, e.g.

	compiler compile filesInDirectory('/Users/me/src')
	
这些方法调用可以被串联起来

	Compiler() + '...cp...' >> '...output...' compile filesInDirectory('...source...')
	
##Scripts

开启 `REPL` 解释器  
输入 `:script <script file>` 并回车

然后使用 `script run` 或者 `script run ['string array']` 来运行这个脚本

或者, 你可以在jar启动时运行脚本

	java -jar the_jar.jar script-file-path script-arguments...

##README

`src/test/resources/lang-demo` 中包含了语法说明

* [advanced.lt](https://github.com/wkgcass/LessTyping/blob/master/src/test/resources/lang-demo/advanced.lt) 有关 `内部方法`, `过程`, 和 `lambda`
* [literals.lts](https://github.com/wkgcass/LessTyping/blob/master/src/test/resources/lang-demo/literals.lts) 有关LessTyping的所有字面量
* [ltFileStructure.lt](https://github.com/wkgcass/LessTyping/blob/master/src/test/resources/lang-demo/ltFileStructure.lt) LessTyping文件的大体结构
* [operator.lt](https://github.com/wkgcass/LessTyping/blob/master/src/test/resources/lang-demo/operator.lt) 有关运算符绑定的一些信息
* [statements.lts](https://github.com/wkgcass/LessTyping/blob/master/src/test/resources/lang-demo/statements.lts) LessTyping中可用的表达式，例如 for, if, while 之类
* [typeDef.lt](https://github.com/wkgcass/LessTyping/blob/master/src/test/resources/lang-demo/typeDef.lt) 类和接口的定义方式

大体而言语法看起来很像 `Python`, 强制4空格缩进 , 它不需要行末的`;`

注释由`;`开始 , 所以如果你喜欢在每行的最后加上一个`;`也完全没问题

多行注释由 `/*` 开始, 以 `*/` 结束.

##语法高亮
在[这里](https://atom.io/packages/Atom-LessTyping-Highlighting)有一个针对`LessTyping`的语法高亮.  
您可以直接在`atom`中搜索`Atom-LessTyping-Highlighting`来完成安装, 也可以按照链接中的指示进行安装.

![](http://lesstyping.cassite.net/images/highlight.png)

--

##运算符绑定

`LessTyping`支持运算符的重载  
除了已有的运算符行为之外，还有一些运算符可以用来重载， 将会被转换为方法调用

e.g.

	list + o
	; 和如下表达式相同
	list.add(o)
	
>在[operator.lt](https://github.com/wkgcass/LessTyping/blob/master/src/test/resources/lang-demo/operator.lt)中获取更多信息

--

##DSL

`LessTyping`支持“看起来像运算符的方法调用”这种语法。它对于构建DSL非常有用。

e.g.

    db select id, name from User where id > 10 and name != 'cass' orderBy id.desc
    
它将被转化为如下方法调用

    db.select(id,name).form(User).where(id>10).and(name!='cass').orderBy(id.desc)
    
>在[statements.lts](https://github.com/wkgcass/LessTyping/blob/master/src/test/resources/lang-demo/statements.lts)中获取更多信息

--

##define/undef
    
`LessTyping`支持`define`/`undef`预处理

e.g.

    define 'CREATE TABLE' as 'class'
    define 'VARCHAR' as ':String'
    define 'NUMBER' as ':int'
    
    CREATE TABLE User(
        id NUMBER
        name VARCHAR
    )
    
    undef 'CREATE TABLE'
    undef 'VARCHAR'
    undef 'NUMBER'
   
>在[ltFileStructure.lt](https://github.com/wkgcass/LessTyping/blob/master/src/test/resources/lang-demo/ltFileStructure.lt)中获取更多信息

--

##内部方法

`内部方法`指的是在方法中定义的另一个方法

e.g.

	outer()
		i=1
		j=2
		inner()
			return i + j + 3
		; 内部方法返回值是 i + j + 3
		res = inner()
		; res 的值应当为 6
		i=4
		res = inner()
		; res 的值应当为 9

**注意** 与许多其他语言不同，内部方法仅*捕获*局部变量的*值*， 但是*不能*改变它们。 在内部方法中对这些值所做的修改不会影响外部的局部变量。

--

##过程

`过程`是指由小括号括起来的一系列的表达式

e.g.

	res = (
		if a is null
			return null
		else
			return a+b
	)

如果a是`null`，那么这个过程的结果为`null`。否则，结果将为`a+b`。最终结果将被赋值给`res`

--

##Lambda

在`LessTyping`中, lambda表达式不仅能够在`函数式接口`上进行, 还能在`函数式抽象类`上进行.

`函数式抽象类`指的是一个“带有无参可访问的构造函数且只有一个未实现方法的抽象类”

e.g.

	abstract class Func
		abstract apply(o)=...  ; defines an abstract method
		
	func1 : Func = (x) -> x+x
	func2 : java::util::function::Function = (o) -> o+1
	func3 = (o) -> o + 1

	func1 is type Func  
	func2 is type java.util.function.Function  
	func3 is type lt.lang.function.Function1

在`lt.lang.function`中定义了27种Function，从Funcion0到Function26. 如果没有指定类型，那么将生成这些接口的实现类。

>在[advanced.lt](https://github.com/wkgcass/LessTyping/blob/master/src/test/resources/lang-demo/advanced.lt)里查看更多有关`内部方法`, `过程` 和 `lambda` 的信息

--

##类型

`LessTyping`由强弱类型混合而成

	variable : Type
	method() : ReturnType
	
	; 或者这么写
	
	obj = 1
	method()=2

##JSON字面量

	map = {
		"id" : 23333,
		"name" : "cass",
		"repositories" : ["LessTyping", "Style", "JSONBind"]
	}

末尾的`,`可以省略

>查看 [literals.lts](https://github.com/wkgcass/LessTyping/blob/master/src/test/resources/lang-demo/literals.lts) 获取更多信息

--

##类的定义

	class Foo(bar : int)
		field : int
		method()=...
	
定义了一个类. 如果在java中，我们会这么写：

	public class Foo {
		private int bar;
		private int field;
		
		public Foo(int bar){
			this.bar=bar;
		}
		
		public Object method(){
			return null;
		}
	}

>查看 [typeDef.lt](https://github.com/wkgcass/LessTyping/blob/master/src/test/resources/lang-demo/typeDef.lt) 获取更多信息

--

##REPL

执行 `lt.repl.REPL`,或者执行 `maven package` 后的`jar`文件，你将会看到

    lt>
   
输入表达式,然后按两次`Enter`就可以获得结果