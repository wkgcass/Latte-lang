#LessTyping

最下面有中文 ：）

check `src/test/resources/lang-demo` for syntax tutorials.

here's what every file explains

* `advanced.lt` -- something about `inner method`, `procedure`, and `lambda`
* `literals.lts` -- about all literals in LessTyping
* `ltFileStructure.lt` -- the general idea of a LessTyping file structure
* `operator.lt` -- info about operator binding
* `statements.lts` -- avaliable statements in LessTyping, e.g. for, if, while...
* `typeDef.lt` -- things about definition of classes and interfaces

Generally the syntax looks like `Python`,  4 spaces indentation is forced , and it doesn't require `;` at the end of a statement/expression

comments are started by `;` , so it's ok if you like writing a `;` at the end of a line

##Syntax Hightlighting
the `language-lesstyping` directory contains a syntax-highlighting for `Atom`.

simply move `language-lesstyping` directory to `~/.atom/packages/`

--

##Operator Binding

`LessTyping` supports operator override  
besides built in operator behavior, there are several operators can be overridden, which will be converted to method invocation

e.g.

	list + o
	; same as
	list.add(o)
	
>check `src/test/resources/lang-demo/operator.lt` for more info

--

##Operator-Like Invocation

`LessTyping` supports a syntax that makes method invocation looks like using operators. It's useful for DSL programming.

e.g.

    db select id, name from User where id > 10 and name != 'cass' orderBy id.desc
    
which would be converted into method invocation

    db.select(id,name).form(User).where(id>10).and(name!='cass').orderBy(id.desc)
    
>check `/lang-demo/statements.lts` for more info

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
   
>check `src/test/resources/lang-demo/ltFileStructure.lt` for more info

--

##Inner Method

an `Inner Method` means methods inside another method

e.g.

	outer()
		i=1
		j=2
		inner()
			< i + j + 3
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
			< null
		else
			< a+b
	)

if a is null, then the precedure's result would be null, otherwise, the result would be `a+b`. and the result would be assigned to variable `res`

--

##Lambda

in `LessTyping`, lambda can not only perform on `Functional Interfaces`, but also on `Functional Abstract Classes`.

a `Functional Abstract Class` means an abstract class with accessible constructor whose parameter count is 0, and the class have 1 and only 1 unimplemented method.

e.g.

	abs class Func
		abs apply(o)=...  ; defines an abstract method
		
	func1 : Func = (x) -> x+x
	func2 : java::util::function::Function = (o) -> o+1
	func3 = (o) -> o + 1

	func1 is type Func  
	func2 is type java.util.function.Function  
	func3 is type lt.lang.function.Function1

there are 27 Functions defined in `lt.lang.function`, ranges from Function0 to Function26. if no type is required, the lambda expression generates these functional interfaces' implementations.

>for more info about `inner method`, `procedure` and `lambda`, check `src/test/resources/lang-demo/advanced.lt`

--

##Types

`LessTyping` is a hibrid of strong and weak typing.

	variable : Type
	method() : ReturnType
	
	; or simple write :
	
	obj = 1
	method()=2

##JSON Literal

	map = {
		"id" : 23333,
		"name" : "cass",
		"repositories" : ["LessTyping", "Style", "JSONBind"]
	}

`,` at the end can be omitted

>check `src/test/resources/lang-demo/literals.lts` for more info

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

>check `src/test/resources/lang-demo/typeDef.lt` for more info

--

##Scripts and REPL (still developing)

`LessTyping` supports scripts. the suffix is usually `lt`, but for scripts, it should be `lts`

the scripts would be compiled to classes as well,   
the class name is the same as `lts` file name, and a `main` method would be added into the class  
the scripts would be filled into `class constructing block`

this feature is still in development, but i'm sure it won't take much time.

run `lt.repl.REPL` then you will see

    LessTyping REPL
    >
   
write expressions and double `Enter`, you will get the result

---------

##中文版 
Chinese Version README

`LessTyping`基于java8。它既有强类型语言的特性，又有弱类型语言的特性。它被编译到JVM字节码，可以与任何Java类库完美互通。

`src/test/resources/lang-demo` 中包含了语法说明

* `advanced.lt` 有关 `内部方法`, `过程`, 和 `lambda`
* `literals.lts` 有关LessTyping的所有字面量
* `ltFileStructure.lt` LessTyping文件的大体结构
* `operator.lt` 有关运算符绑定的一些信息
* `statements.lts` LessTyping中可用的表达式，例如 for, if, while 之类
* `typeDef.lt` 类和接口的定义方式

大体而言语法看起来很像 `Python`, 强制4空格缩进 , 它不需要行末的`;`

注释由`;`开始 , 所以如果你喜欢在每行的最后加上一个`;`也完全没问题

##语法高亮
`language-lesstyping`目录包含了一个针对`Atom`的语法高亮.

只需要将`language-lesstyping`目录整个挪到`~/.atom/packages/`中即可

--

##运算符绑定

`LessTyping`支持运算符的重载  
除了已有的运算符行为之外，还有一些运算符可以用来重载， 将会被转换为方法调用

e.g.

	list + o
	; 和如下表达式相同
	list.add(o)
	
>在`src/test/resources/lang-demo/operator.lt`中获取更多信息

--

##看起来像运算符的方法调用

`LessTyping`支持“看起来像运算符的方法调用”这种语法。它对于构建DSL非常有用。

e.g.

    db select id, name from User where id > 10 and name != 'cass' orderBy id.desc
    
它将被转化为如下方法调用

    db.select(id,name).form(User).where(id>10).and(name!='cass').orderBy(id.desc)
    
>在`/lang-demo/statements.lts`中获取更多信息

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
   
>在`src/test/resources/lang-demo/ltFileStructure.lt`中获取更多信息

--

##内部方法

`内部方法`指的是在方法中定义的另一个方法

e.g.

	outer()
		i=1
		j=2
		inner()
			< i + j + 3
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
			< null
		else
			< a+b
	)

如果a是`null`，那么这个过程的结果为`null`。否则，结果将为`a+b`。最终结果将被赋值给`res`

--

##Lambda

在`LessTyping`中, lambda表达式不仅能够在`函数式接口`上进行, 还能在`函数式抽象类`上进行.

`函数式抽象类`指的是一个“带有无参可访问的构造函数且只有一个未实现方法的抽象类”

e.g.

	abs class Func
		abs apply(o)=...  ; defines an abstract method
		
	func1 : Func = (x) -> x+x
	func2 : java::util::function::Function = (o) -> o+1
	func3 = (o) -> o + 1

	func1 is type Func  
	func2 is type java.util.function.Function  
	func3 is type lt.lang.function.Function1

在`lt.lang.function`中定义了27种Function，从Funcion0到Function26. 如果没有指定类型，那么将生成这些接口的实现类。

>在`src/test/resources/lang-demo/advanced.lt`里查看更多有关`内部方法`, `过程` 和 `lambda` 的信息

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

>查看 `src/test/resources/lang-demo/literals.lts` 获取更多信息

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

>查看 `src/test/resources/lang-demo/typeDef.lt` 获取更多信息

--

##脚本和REPL(仍在开发中)

`LessTyping`支持脚本. 通常来说后缀为`lt`, 不过对于脚本，后缀名为`lts`

脚本也会被编译到类，  
该类的名称是`lts`文件的文件名， 并且会自动添加一个`main`方法到该类中  
脚本内容讲被写入到`类构造块`中

该特性仍在开发，不过我想应该花不了太长时间

执行 `lt.repl.REPL` ,你将会看到

    LessTyping REPL
    >
   
输入表达式,然后按两次`Enter`就可以获得结果