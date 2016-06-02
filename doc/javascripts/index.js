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
        $scope.git_repo = common_git_repo();

        $scope.header = "The Latte Programming Language";
        $scope.header_descr = "100% interoperable with Java™";
        $scope.header_button = "Fork me on Github";
        $scope.navs = common_navs();
        $scope.navs[0].active = true;

        $scope.introduction = {
            title: "Latte",
            content: $sce.trustAsHtml("Latte是一种可读,灵活,简单,易扩展的JVM编程语言. 正如它的名字一样,可以与java完美融合.")
        };
        $scope.feature = {
            title: "特性",
            features: [
                {
                    name: "运算符绑定"
                },
                {
                    name: "DSL"
                },
                {
                    name: "预处理 define/undef"
                },
                {
                    name: "内部方法"
                },
                {
                    name: "Lambda"
                },
                {
                    name: "JSON 字面量"
                },
                {
                    name: "REPL"
                }
            ],
            content: $sce.trustAsHtml("更多特性请查看 <a href='syntax.html'>语法</a>")
        };
        $scope.codes_head = "您可以在REPL中尝试大部分语句";
        $scope.codes = [
            {
                code: $sce.trustAsHtml(highlighting("example_1.lts", "" +
                    "; 运算符绑定\n" +
                    "list = ArrayList()\n" +
                    "list + 1\n" +
                    "; 与 list.add(1) 相同\n" +
                    "list[0]\n" +
                    "; 与 list.get(0) 相同\n" +
                    "\n" +
                    "a = BigInteger(\"1\")\n" +
                    "b = BigInteger(\"2\")\n" +
                    "\n" +
                    "addRes = a + b\n" +
                    "; 相当于 a.add(b)\n" +
                    "subRes = a - b\n" +
                    "; 相当于 a.subtract(b)\n" +
                    "a << 1\n" +
                    "; 相当于 a.shiftLeft(1)"
                    , {}))
            },
            {
                code: $sce.trustAsHtml(highlighting("example_8.lts", "" +
                    "; Data Class\n" +
                    "data class User(id:int, name)\n" +
                    "/*\n" +
                    "将会自动定义 一个无参构造函数\n" +
                    "getId():int, getName(),\n" +
                    "toString():String, equals(o), hashCode():int\n" +
                    "*/\n" +
                    "; 可以使用如下方式实例化\n" +
                    "User(id=1, name='cass')"
                    , {}))
            },
            {
                code: $sce.trustAsHtml(highlighting("example_2.lts", "" +
                    "; DSL\n" +
                    "list = ArrayList()\n" +
                    "list add 'str'\n" +
                    "; 与 list.add('str')相同\n" +
                    "list set 0, 'string'\n" +
                    "; 与 list.set(0, 'string')相同\n" +
                    "list clear\n" +
                    "; 与 list.clear()相同\n" +
                    "\n" +
                    "; DSL特性甚至允许您组成这样的表达式\n" +
                    "sql select user.id, user.name, user.age from user\n" +
                    "; 将被转换为\n" +
                    "sql.select(user.id, user.name, user.age).from(user)"
                    , {}))
            },
            {
                code: $sce.trustAsHtml(highlighting("example_3.lts", "" +
                    "; 预处理 define/undef\n" +
                    "define 'CREATE TABLE' as 'class'\n" +
                    "define 'VARCHAR' as ': String'\n" +
                    "define 'NUMBER' as ': int'\n" +
                    "\n" +
                    "CREATE TABLE User(\n" +
                    "    id NUMBER\n" +
                    "    name VARCHAR\n" +
                    ")\n" +
                    "; 与如下定义功能一致\n" +
                    "class User(id : int, name : String)"
                    , {}))
            },
            {
                code: $sce.trustAsHtml(highlighting("example_4.lts", "" +
                    "; 内部方法\n" +
                    "class Test\n" +
                    "    outerMethod(i, j)\n" +
                    "        innerMethod(k)=i+j+k\n" +
                    "        ; 定义了一个\"内部方法\", 可以访问局部变量\n" +
                    "        return innerMethod(3)\n" +
                    "        ; 调用该内部方法并返回\n"
                    , {}))
            },
            {
                code: $sce.trustAsHtml(highlighting("example_5.lts", "" +
                    "; Lambda\n" +
                    "func1 = (x)->x+2\n" +
                    "; 默认的Lambda实现接口为lt::lang::function::FunctionX\n" +
                    "; X有27个, 从0到26\n" +
                    "\n" +
                    "func2 : java::util::function::Function = (x)->x+3\n" +
                    "\n" +
                    "; Lambda不光可以用在函数式接口上,还可以用在函数式抽象类上\n" +
                    "abstract class Func\n" +
                    "    abstract apply()=...     ; ...是一个有效的符号,不是指省略\n" +
                    "func3 : Func = (x)->x+4"
                    , {}))
            },
            {
                code: $sce.trustAsHtml(highlighting("example_6.lts", "" +
                    "; JSON 字面量\n" +
                    "map = {\n" +
                    "    'id' : 1,\n" +
                    "    'name' : 'cass',\n" +
                    "    'repo' : ['Latte', 'Pure.IoC']\n" +
                    "}\n" +
                    "; map末尾的逗号也可以省略\n" +
                    "\n" +
                    "list = ['a', 'b', 'c']"
                    , {}))
            },
            {
                code: $sce.trustAsHtml(highlighting("example_7.lts", "" +
                    "; 类型定义\n" +
                    "; 类\n" +
                    "class User(id,name)\n" +
                    "    getId()=id\n" +
                    "    getName()=name\n" +
                    "    toString():String=\"User(\"+id+\", \"+name+\")\"\n" +
                    "    equals(o):bool\n" +
                    "        if o is type User\n" +
                    "            return id==o.id and name==o.name\n" +
                    "        return false"
                    , {}))
            }
        ];

        $scope.build = {
            title: "如何Build",
            contents: [
                $sce.trustAsHtml("Latte只需要 <code>JRE 8</code>"),
                $sce.trustAsHtml("建议以<code>lt.repl.REPL</code>为主类打包为jar"),
                $sce.trustAsHtml("本工程通过Maven管理, 所以您也可以使用 <code>Maven 3</code> 进行自动Build"),
                $sce.trustAsHtml("clone<a href='" + $scope.git_repo + "'>该仓库</a>, 然后执行 <code>mvn clean package</code> , 在 <code>target</code> 目录下将生成一个可执行的jar文件, 并且在跟目录生成批处理文件."),
                $sce.trustAsHtml("直接执行 <code>.&#47;latte</code> , REPL将启动")
            ]
        };
        $scope.compile = {
            title: "REPL 与 编译",
            contents: [
                $sce.trustAsHtml("Latte提供一个REPL, 您可以运行自动Build后生成的jar包,或者直接执行 <code>lt.repl.REPL</code> 类以使用"),
                $sce.trustAsHtml("REPL环境除去任何Latte代码都会执行的导入以外, 还会自动导入 <ul><li><code>java::util::_</code></li><li><code>java::math::_</code></li><li><code>lt::repl::_</code></li></ul>"),
                $sce.trustAsHtml("若需要编译Latte源代码文件(<code>*.lt</code>), 您可以在REPL环境下使用 <code>Compiler()</code> 构造一个 <code>lt::repl::Compiler</code> 对象. 您也可以在java代码中用类似的方式进行 <code>lt</code> 文件的编译"),
                $sce.trustAsHtml(common_compile_highlighting())
            ],
            help: $sce.trustAsHtml("<a href=''>运算符绑定</a> 可能对您有帮助")
        };
        $scope.highlight = {
            title: "Atom 语法高亮与IDE",
            contents: [
                $sce.trustAsHtml("针对Atom编辑器开发了<a target='_blank' href='https://atom.io/packages/Atom-Latte-Highlighting'>语法高亮</a>和" +
                    "<a target='_blank' href='https://atom.io/packages/atom-latte-ide'>IDE</a>"),
                $sce.trustAsHtml("可以更加方便的编写Latte源代码")
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

    setInterval(sw, 18000);
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