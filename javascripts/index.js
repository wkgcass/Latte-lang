var cursor = 0;

$(document).ready(function () {

    // fix menu when passed
    $('.masthead')
        .visibility({
            once: false,
            onBottomPassed: function () {
                $('.fixed.menu').transition('fade in');
            },
            onBottomPassedReverse: function () {
                $('.fixed.menu').transition('fade out');
            }
        })
    ;

    // create sidebar and attach to menu open
    $('.ui.sidebar').sidebar('attach events', '.toc.item');

    var app = angular.module('index', []);
    app.controller('controller', ['$scope', '$sce', function ($scope, $sce) {
        var isZh=useZh();

        $scope.git_repo = common_git_repo();

        $scope.header = "The Latte Programming Language";
        $scope.header_descr = "100% interoperable with Java™";
        $scope.header_button = "Download Latte";
        $scope.navs = common_navs();
        $scope.navs[0].active = true;

        $scope.introduction = {
            title: "Latte",
            content: $sce.trustAsHtml(
                isZh
                    ?"Latte是一种可读,灵活,简单,易扩展的JVM编程语言. 正如它的名字一样,可以与java完美融合."
                    :"Latte is a readable, flexible, simple and extensive JVM programming language. It's 100% interoperable with Java."
            )
        };
        $scope.feature = {
            title: isZh?"特性":"Features",
            features: [
                {
                    name: isZh?"运算符绑定":"Operator Binding"
                },
                {
                    name: "DSL"
                },
                {
                    name: isZh?"预处理 define/undef":"Pre Processing define/undef"
                },
                {
                    name: isZh?"内部方法":"inner methods"
                },
                {
                    name: "Lambda"
                },
                {
                    name: isZh?"JSON 字面量":"JSON Literals"
                },
                {
                    name: "REPL"
                }
            ],
            content: $sce.trustAsHtml(
                isZh
                    ?"更多特性请查看 <a href='syntax.html'>语法</a>"
                    :"Please visit <a href='syntax.html'>Syntax</a> for more features"
            )
        };
        $scope.codes_head = isZh?"您可以在REPL中尝试大部分语句":"You can try out most of these statements in REPL";
        $scope.codes = [
            {
                code: $sce.trustAsHtml(highlighting("example_1.lts", "" +
                    (isZh?"; 运算符绑定\n":"; Operator Binding\n") +
                    "list = ArrayList()\n" +
                    "list + 1\n" +
                    (isZh?"; 与 list.add(1) 相同\n":"; same as list.add(1)\n") +
                    "list[0]\n" +
                    (isZh?"; 与 list.get(0) 相同\n":"; same as list.get(0)\n") +
                    "\n" +
                    "a = BigInteger(\"1\")\n" +
                    "b = BigInteger(\"2\")\n" +
                    "\n" +
                    "addRes = a + b\n" +
                    (isZh?"; 相当于 a.add(b)\n":"; same as a.add(b)\n") +
                    "subRes = a - b\n" +
                    (isZh?"; 相当于 a.subtract(b)\n":"; same as a.subtract(b)\n") +
                    "a << 1\n" +
                    (isZh?"; 相当于 a.shiftLeft(1)":"; same as a.shiftLeft(1)")
                    , {}))
            },
            {
                code: $sce.trustAsHtml(highlighting("example_8.lts", "" +
                    "; Data Class\n" +
                    "data class User(id:int, name)\n" +
                    "/*\n" +
                    (isZh?"将会自动定义 一个无参构造函数,\n":"a constructor with 0 params,\n") +
                    "getId(), getName(), setId(id), setName(name)\n" +
                    "toString():String, equals(o), hashCode():int\n" +
                    (isZh?"":"will be automatically defined\n") +
                    "*/\n" +
                    (isZh?"; 可以使用如下方式实例化\n":"; Can be instantiated in this way\n") +
                    "User(id=1, name='cass')"
                    , {}))
            },
            {
                code: $sce.trustAsHtml(highlighting("example_2.lts", "" +
                    "; DSL\n" +
                    "list = ArrayList()\n" +
                    "list add 'str'\n" +
                    (isZh?"; 与 list.add('str')相同\n":"; same as list.add('str')\n") +
                    "list set 0, 'string'\n" +
                    (isZh?"; 与 list.set(0, 'string')相同\n":"; same as list.set(0, 'string')\n") +
                    "list clear\n" +
                    (isZh?"; 与 list.clear()相同\n":"; same as list.clear()\n") +
                    "\n" +
                    (isZh?"; DSL特性甚至允许您组成这样的表达式\n":"; DSL allow you to form an exp like this\n") +
                    "sql select user.id, user.name, user.age from user\n" +
                    (isZh?"; 将被转换为\n":"; will be considered as\n") +
                    "sql.select(user.id, user.name, user.age).from(user)"
                    , {}))
            },
            {
                code: $sce.trustAsHtml(highlighting("example_3.lts", "" +
                    (isZh?"; 预处理 define/undef\n":"; Pre Processing define/undef\n") +
                    "define 'CREATE TABLE' as 'class'\n" +
                    "define 'VARCHAR' as ': String'\n" +
                    "define 'NUMBER' as ': int'\n" +
                    "\n" +
                    "CREATE TABLE User(\n" +
                    "    id NUMBER\n" +
                    "    name VARCHAR\n" +
                    ")\n" +
                    (isZh?"; 与如下定义功能一致\n":"; same as the following definition\n") +
                    "class User(id : int, name : String)"
                    , {}))
            },
            {
                code: $sce.trustAsHtml(highlighting("example_4.lts", "" +
                    (isZh?"; 内部方法\n":"inner methods\n") +
                    "class Test\n" +
                    "    outerMethod(i, j)\n" +
                    "        innerMethod(k)=i+j+k\n" +
                    (isZh
                    ?"        ; 定义了一个\"内部方法\", 可以访问局部变量\n"
                    :"        ; defines an inner method\n        ; which can capture the local variables\n"
                    ) +
                    "        return innerMethod(3)\n" +
                    (isZh
                    ?"        ; 调用该内部方法并返回\n"
                    :"        ; invoke the inner method and return\n"
                    )
                    , {}))
            },
            {
                code: $sce.trustAsHtml(highlighting("example_5.lts", "" +
                    "; Lambda\n" +
                    "func1 = (x)->x+2\n" +
                    (isZh
                    ?"; 默认的Lambda实现接口为lt::lang::function::FunctionX\n"
                    :"; the default implemented interface of Lambda is\n; lt::lang::function::FunctionX\n"
                    ) +
                    (isZh?"; X有27个, 从0到26\n":"; X ranges from 0 to 26\n") +
                    "\n" +
                    "func2 : java::util::function::Function = (x)->x+3\n" +
                    "\n" +
                    (isZh
                    ?"; Lambda不光可以用在函数式接口上,还可以用在函数式抽象类上\n"
                    :"; Lambda can work not only on functional interfaces\n; but also on functional abstract classes\n"
                    ) +
                    "abstract class Func\n" +
                    "    abstract apply()=...\n" +
                    "func3 : Func = (x)->x+4"
                    , {}))
            },
            {
                code: $sce.trustAsHtml(highlighting("example_6.lts", "" +
                    (isZh?"; JSON 字面量\n":"; JSON Literals\n") +
                    "map = {\n" +
                    "    'id' : 1,\n" +
                    "    'name' : 'cass',\n" +
                    "    'repo' : ['Latte', 'Pure.IoC']\n" +
                    "}\n" +
                    (isZh?"; map末尾的逗号也可以省略\n":"; comma at the end can be omitted.\n") +
                    "\n" +
                    "list = ['a', 'b', 'c']"
                    , {}))
            },
            {
                code: $sce.trustAsHtml(highlighting("example_7.lts", "" +
                    (isZh?"; 类型定义\n":"; Type Definition\n") +
                    (isZh?"; 类\n":"; classes\n") +
                    "class User(id,name)\n" +
                    "    getId()=id\n" +
                    "    getName()=name\n" +
                    "    toString():String=\"User(\"+id+\", \"+name+\")\"\n" +
                    "    equals(o):bool\n" +
                    "        if o is type User\n" +
                    "            return id==o.id and name==o.name\n" +
                    "        return false\n" +
                    (isZh?"; 接口\n":"; interfaces\n") +
                    "interface AnInterface\n" +
                    "    apply(o)=..."
                    , {}))
            }
        ];

        $scope.build = {
            title: isZh?"如何Build":"How to Build",
            contents:
            isZh
            ?[
                $sce.trustAsHtml("Latte 只需要 <code>JRE 8</code>"),
                $sce.trustAsHtml("建议以<code>lt.repl.REPL</code>为主类打包为jar. 并复制 <code>src/main/resources/latte.sh</code> 或 <code>latte.bat</code> 到打包的目录"),
                $sce.trustAsHtml("本工程通过Maven管理, 所以您也可以使用 <code>Maven 3</code> 进行自动Build"),
                $sce.trustAsHtml("clone <a href='" + $scope.git_repo + "'>该仓库</a>, 然后执行 <code>mvn clean package</code> , 在根目录会生成 <code>repl.jar</code> 和批处理文件."),
                $sce.trustAsHtml("直接执行 <code>.&#47;latte</code> , REPL将启动")
            ]
            :[
                $sce.trustAsHtml("Latte only requires <code>JRE 8</code>."),
                $sce.trustAsHtml("It's recommended to package <code>lt.repl.REPL</code> as the main class. And copy <code>src/main/resources/latte.sh</code> or <code>latte.bat</code> to the jar directory."),
                $sce.trustAsHtml("The project is managed by Maven, so you can use <code>Maven 3</code> to build automatically."),
                $sce.trustAsHtml("clone <a href='" + $scope.git_repo + "'>the repository</a>, and run <code>mvn clean package</code> , then, <code>repl.jar</code> and batch procedure files will be generated at root directory."),
                $sce.trustAsHtml("run <code>.&#47;latte</code> , and REPL will launch")
            ]
        };
        $scope.compile = {
            title: isZh?"REPL 与 编译":"REPL and Compiling",
            contents:
            isZh
            ?[
                $sce.trustAsHtml("Latte提供一个REPL, 您可以运行自动Build后生成的jar包,或者直接执行 <code>lt.repl.REPL</code> 类以使用"),
                $sce.trustAsHtml("REPL环境除去任何Latte代码都会执行的导入以外, 还会自动导入 <ul><li><code>lt::util::_</code></li><li><code>java::util::_</code></li><li><code>java::math::_</code></li><li><code>lt::repl::_</code></li></ul>"),
                $sce.trustAsHtml("若需要编译Latte源代码文件(<code>*.lt</code>), 您可以在REPL环境下使用 <code>Compiler()</code> 构造一个 <code>lt::repl::Compiler</code> 对象. 您也可以在java代码中用类似的方式进行 <code>lt</code> 文件的编译"),
                $sce.trustAsHtml(common_compile_highlighting())
            ]
            :[
                $sce.trustAsHtml("Latte provides an REPL, you can run the jar generated via automatic building, or execute <code>lt.repl.REPL</code> class directly."),
                $sce.trustAsHtml("Besides the importings of Latte, the REPL also imports : <ul><li><code>lt::util::_</code></li><li><code>java::util::_</code></li><li><code>java::math::_</code></li><li><code>lt::repl::_</code></li></ul>"),
                $sce.trustAsHtml("If Latte source files (<code>*.lt</code>) are to be compiled, you can enter the REPL mode and use <code>Compiler()</code> to construct a <code>lt::repl::Compiler</code> object. You can compile <code>lt</code> files in similar way in java."),
                $sce.trustAsHtml(common_compile_highlighting())
            ],
            help: $sce.trustAsHtml(
                isZh?"<a href=''>运算符绑定</a> 可能对您有帮助":"<a href=''>Operator Binding</a> may be helpful."
            )
        };
        $scope.highlight = {
            title: isZh?"Atom 语法高亮与IDE":"Atom Highlighting and IDE",
            contents:
            isZh
            ?[
                $sce.trustAsHtml("针对Atom编辑器开发了<a target='_blank' href='https://atom.io/packages/Atom-Latte-Highlighting'>语法高亮</a>和" +
                    "<a target='_blank' href='https://atom.io/packages/atom-latte-ide'>IDE</a>"),
                $sce.trustAsHtml("可以更加方便的编写Latte源代码")
            ]
            :[
                $sce.trustAsHtml("a <a target='_blank' href='https://atom.io/packages/Atom-Latte-Highlighting'>Syntax Highlighting</a> and an " +
                    "<a target='_blank' href='https://atom.io/packages/atom-latte-ide'>IDE</a> on Atom"),
                $sce.trustAsHtml("which help you write Latte source code conveniently")
            ]
        }
    }]);

    function sw() {
        var codes = $(".code.play");
        var timeout = 300;
        if (cursor == codes.length - 1) {
            $(".code.play:eq(" + cursor + ")").fadeOut(timeout);
            setTimeout(function () {
                $(".code.play:eq(0)").fadeIn(timeout);
            }, timeout + 100);
            cursor = 0;
        } else {
            $(".code.play:eq(" + cursor + ")").fadeOut(timeout);
            setTimeout(function () {
                $(".code.play:eq(" + ++cursor + ")").fadeIn(timeout);
            }, timeout + 100);
        }
    }

    setInterval(sw, 25000);
});

function next() {
    var codes = $(".code.play");
    var timeout = 300;
    if (cursor == codes.length - 1) {
        $(".code.play:eq(" + cursor + ")").fadeOut(timeout);
        setTimeout(function () {
            $(".code.play:eq(0)").fadeIn(timeout);
        }, timeout + 100);
        cursor = 0;
    } else {
        $(".code.play:eq(" + cursor + ")").fadeOut(timeout);
        setTimeout(function () {
            $(".code.play:eq(" + ++cursor + ")").fadeIn(timeout);
        }, timeout + 100);
    }
}

function previous() {
    var codes = $(".code.play");
    var timeout = 300;
    if (cursor == 0) {
        $(".code.play:eq(0)").fadeOut(timeout);
        setTimeout(function () {
            $(".code.play:eq(" + (codes.length - 1) + ")").fadeIn(timeout);
        }, timeout + 100);
        cursor = codes.length - 1;
    } else {
        $(".code.play:eq(" + cursor + ")").fadeOut(timeout);
        setTimeout(function () {
            $(".code.play:eq(" + --cursor + ")").fadeIn(timeout);
        }, timeout + 100);
    }
}