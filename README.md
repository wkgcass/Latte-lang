#Latte-Lang

![](http://latte-lang.org/images/highlight.png)

Latte is a JVM language. It's highly readable and extensible.

[Syntax Mannual (NEW)](https://github.com/wkgcass/Latte-lang/blob/master/mannual-zh.md)  
[Latte WebSite](http://latte-lang.org/)

`Atom` Extensions :

[atom-latte-lang-highlighting](https://atom.io/packages/Atom-Latte-lang-Highlighting)  
[atom-latte-lang-ide](https://atom.io/packages/atom-latte-lang-ide)

`Gradle` Plugin :

[latte-gradle-plugin](#gradle-plugin)

##中文版戳[这里](#readme-ch)

###Latte supports

* Operator Binding
* DSL
* Data Class
* Pattern Matching
* Inner Method
* Lambda and High-Order Function
* JSON Literal
* Regular Expression
* Read Eval Print Loop
* Compiling to JavaScript
* Latte Gradle Plugin
* many other features

`Latte` is based on java 6. It's compiled to JVM byte code, and can collaborate with any java library.

#How to build

`JDK 1.6` or higher is the only thing required.

The project is managed by `Gradle`, you can use `Gradle 2.12` (or higher) to build automatically  
A build script is also provided.

clone the repository, and run

	./build.py

You will get a shell scripts (`latte` or `latte.bat`). The shell scripts can help you run the `repl`.

run:

	./latte

then the [REPL](https://github.com/wkgcass/Latte-lang/blob/master/latte-build/src/main/java/lt/repl/REPL.java) starts

	Welcome to Latte lang
	Type in expressions and double Enter to have them evaluated.
	Type :help for more information.
	for syntax help, please visit https://github.com/wkgcass/Latte-lang/

    lt> 1+1
      |
    res0 : java.lang.Integer = 2

    lt>

#Compile `lt` Files

There are two ways of compiling `lt` files

* use program command

		latte -c <source-directory>

	the detailed commands and options can be found in

		latte -help

or:

* start the `REPL` interpreter, and construct a `Compiler`

		compiler = Compiler()

	use `>>` operator to specify output directory

		compiler >> '...'
		// or
		compiler >> File('...')

	use `+` operator to add class-path

		compiler + '...'

	use `compile MAP` to specify source codes to be compiled and start compiling

		compiler compile {'fileName':FileObject}

	usually `filesInDirectory('...', regex)` is used, e.g.

		compiler compile filesInDirectory('...', '.\*\\.lt'.r)

	these method invocations can be chained up

		Compiler() + '...cp...' >> '...output...' compile filesInDirectory('...source...', '.\*\\.lt'.r)

	You can write a `script` to configure the settings. Check [build.lts.template](https://github.com/wkgcass/Latte-lang/blob/master/latte-compiler/src/main/resources/build.lts.template) for more info.

#Scripts

* you can run a script directly

		latte -s script-location script-arguments...

or:

* start the `REPL` interpreter  
	type `:script <script file>` and Enter

	then use `script run` or `script run ['string array']` to run the script

<h1 id='gradle-plugin'>Gradle Plugin</h1>

A plugin for `Gradle` is provided, which helps you compile latte source codes.

###How to use

###step1

add the plugin configuration:

```groovy
buildscript {
    dependencies {
        classpath 'org.latte-lang:latte-gradle-plugin:$VERSION'
    }
}

apply plugin: 'latte'

sourceSets {
    main {
        java
        latte.srcDirs = ['src/main/latte']
        resources
    }
    test {
        java
        latte
        resources
    }
}

latteConfig {
    afterJava = true
    afterGroovy = false
    fastFail = false
}
```

> all configurations are optional

The plugin adds `compileLatte` and `compileTestLatte` tasks, where `compileLatte` is before `classes` task, and `compileTestLatte` is before `testClasses` task

###step2
create a folder named `latte` in the same parent directory. The directory tree should be:

	src
	├── main
	│   ├── java
	│   │   └── \*.java    ; java source
	│   ├── latte
	│   │   └── \*.lt      ; latte source
	│   └── resources
	│       │── \*.lts     ; latte scripts
	│       └── other resources
	└── test
	    ├── java
	    │   └── \*.java
	    ├── latte
	    │   └── \*.lt
	    └── resources
	        ├── \*.lts
	        └── other resources

###step3
run

	gradle clean jar

#Syntax

visit the [Latte WebSite](http://latte-lang.org/)

or read the [mannual](https://github.com/wkgcass/Latte-lang/blob/master/mannual-zh.md)

<h1 id='readme-ch'>中文版 Chinese Version README</h1>

Latte是一种JVM编程语言。 它非常可读，同时也非常可扩展。

[语法规则](https://github.com/wkgcass/Latte-lang/blob/master/mannual-zh.md)  
[Latte 主页](http://latte-lang.org/)

`Atom`上的扩展插件:

[atom-latte-lang-highlighting](https://atom.io/packages/Atom-Latte-lang-Highlighting)  
[atom-latte-lang-ide](https://atom.io/packages/atom-latte-lang-ide)

`Gradle` Plugin :

[latte-gradle-plugin](#gradle-plugin-ch)

###Latte 支持如下功能

* 运算符绑定
* DSL
* Data Class
* 模式匹配
* 内部方法
* Lambda 和 高阶函数
* JSON 字面量
* 正则表达式
* Read Eval Print Loop
* 编译到JavaScript
* Latte Gradle Plugin
* 许多其它特性

`Latte`基于java6。它被编译到JVM字节码，可以与任何Java类库完美互通。

#如何构建工程

环境仅仅需要 `JDK 1.6` 或更高

本工程使用 `Gradle` 进行管理，所以您也可以使用 `Gradle 2.12`(或更高) 进行自动Build  
此外还提供了一个Build脚本

clone这个仓库,然后执行

	./build.py

你将会获取一个shell脚本 (`latte` 或 `latte.bat`), shell脚本可以快捷地开启`repl`.

执行:

	./latte

接着, [REPL](https://github.com/wkgcass/Latte-lang/blob/master/latte-build/src/main/java/lt/repl/REPL.java) 将开始运行

	Welcome to Latte lang
	Type in expressions and double Enter to have them evaluated.
	Type :help for more information.
	for syntax help, please visit https://github.com/wkgcass/Latte-lang/

    lt> 1+1
      |
    res0 : java.lang.Integer = 2

    lt>

#编译 `lt` 文件

* 使用程序命令

		latte -c <source-directory>

	详细的命令与选项可以这样找到

		latte -help

或者:

* 开启`REPL`, 然后构造一个`Compiler`

		compiler = Compiler()

	使用 `>>` 运算符来指定编译输出目录

		compiler >> '...'
		// or
		compiler >> File('...')

	使用 `+` 运算符来添加 class-path

		compiler + '...'

	使用 `compile MAP` 来确定源代码并立即开始编译

		compiler compile {'fileName':FileObject}

	通常来说会使用 `filesInDirectory('...', regex)`, e.g.

		compiler compile filesInDirectory('/Users/me/src', '.\*\\.lt'.r)

	这些方法调用可以被串联起来

		Compiler() + '...cp...' >> '...output...' compile filesInDirectory('...source...', '.\*\\.lt'.r)

	您可以编写一个脚本 `script` 来配置这些属性。查看 [build.lts.template](https://github.com/wkgcass/Latte-lang/blob/master/latte-compiler/src/main/resources/build.lts.template) 以获取更多信息。

#Scripts

* 你可以直接运行脚本

		latte -s script-file-path script-arguments...

或者：

*  开启 `REPL` 解释器  
	输入 `:script <script file>` 并回车

	然后使用 `script run` 或者 `script run ['string array']` 来运行这个脚本

<h1 id='gradle-plugin-ch'>Gradle 插件</h1>

提供了一个`Gradle`的插件， 这个插件可以用来编译和运行`latte`源文件和脚本（script）。

###如何使用

###step1

添加如下gradle plugin配置：

```groovy
buildscript {
    dependencies {
        classpath 'org.latte-lang:latte-gradle-plugin:$VERSION'
    }
}

apply plugin: 'latte'

sourceSets {
    main {
        java
        latte.srcDirs = ['src/main/latte']
        resources
    }
    test {
        java
        latte
        resources
    }
}

latteConfig {
    afterJava = true
    afterGroovy = false
    fastFail = false
}
```

> 所有的配置项都是可选的

插件添加了 `compileLatte` 和 `compileTestLatte` 任务。`compileLatte` 在 `classes` 任务之前, `compileTestLatte` 在 `testClasses` 任务之前

###step2
在同一个上级目录中创建名称为`latte`的目录。目录结构树应当为：

	src
	├── main
	│   ├── java
	│   │   └── \*.java    ; java source
	│   ├── latte
	│   │   └── \*.lt      ; latte source
	│   └── resources
	│       │── \*.lts     ; latte scripts
	│       └── other resources
	└── test
	    ├── java
	    │   └── \*.java
	    ├── latte
	    │   └── \*.lt
	    └── resources
	        ├── \*.lts
	        └── other resources

###step3
运行

	gradle clean jar

# 语法

您可以从这两个地方获取语法规则

[语法规则](https://github.com/wkgcass/Latte-lang/blob/master/mannual-zh.md)

[Latte 主页](http://latte-lang.org/)
