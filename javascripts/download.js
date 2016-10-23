$(document).ready(function () {
    // create sidebar and attach to menu open
    $('.ui.sidebar').sidebar('attach events', '.toc.item');

    var app = angular.module('download', []);
    app.controller('controller', ['$scope', '$sce', function ($scope, $sce) {
        var zh = useZh();
        $scope.navs = common_navs();
        $scope.navs[1].active = true;

        $scope.git_repo = common_git_repo();

        $scope.source_code = {
            title: zh?"源代码":"Source Code",
            contents:
            zh?[
                $sce.trustAsHtml("Latte源代码托管于 <a href='" + common_git_repo() + "'>Github</a>"),
                $sce.trustAsHtml("您可以下载整个 <code>master</code> 分支自行Build , 或者下载已经编译的 <a href='"
                    +
                    "https://github.com/wkgcass/Latte-lang/releases/download/0.0.1-alpha-2/repl.jar"
                    +"'><code>jar</code></a> 文件")
            ]:[
                $sce.trustAsHtml("Latte source code is managed on <a href='" + common_git_repo() + "'>Github</a>"),
                $sce.trustAsHtml("You can download the entire <code>master</code> branch and build yourself, or download compiled <a href='"
                                +
                                "https://github.com/wkgcass/Latte-lang/releases/download/0.0.1-alpha-2/repl.jar"
                                +"'><code>jar</code></a> file.")
            ],
            download: zh?"下载 Master 分支":"Download Master Branch",
            link: "https://github.com/wkgcass/Latte-lang/archive/master.zip"
        };
        $scope.build = {
            title: zh?"依赖项 与 Build":"Dependencies and Building",
            contents:
            zh
            ?[
                $sce.trustAsHtml("" +
                    "<ul class='ui list'>" +
                    "<li>依赖于 <code>JRE 8</code></li>" +
                    "<li>自动Build还需 <code>Maven 3</code></li>" +
                    "</ul>"),
                $sce.trustAsHtml("如果手动编译请编译 <code>/src/main/java</code> 目录"),
                $sce.trustAsHtml("建议以<code>lt.repl.REPL</code>为主类打包为jar. 并复制 <code>src/main/resources/latte.sh</code> 或 <code>latte.bat</code> 到打包的目录"),
                $sce.trustAsHtml("若需自动编译, 下载 Master 分支, 然后执行<code>mvn clean package</code>, 在根目录会生成 <code>repl.jar</code> 和批处理文件."),
                $sce.trustAsHtml("执行 <code>.&#47;latte</code> 即可启动 REPL, 当然也可将jar文件引入到项目中使用")
            ]
            :[
                $sce.trustAsHtml("" +
                    "<ul class='ui list'>" +
                    "<li><code>JRE 8</code> is required</li>" +
                    "<li>Automatic building also requires <code>Maven 3</code></li>" +
                    "</ul>"),
                    $sce.trustAsHtml("Compile the <code>/src/main/java</code> directory if you want to build manually."),
                    $sce.trustAsHtml("It's recommended to package <code>lt.repl.REPL</code> as the main class. And copy <code>src/main/resources/latte.sh</code> or <code>latte.bat</code> to the jar directory."),
                    $sce.trustAsHtml("If you want to build automatically, download the Master branch, and run <code>mvn clean package</code>, then, <code>repl.jar</code> and batch procedure files will be generated at root directory."),
                    $sce.trustAsHtml("run <code>.&#47;latte</code> to launch REPL, or you can add the jar to your project.")
            ]
        };
        $scope.compile_original = {
            title: zh?"编译":"Compile",
            contents:
            zh?[
                $sce.trustAsHtml("可以在REPL中, 也可以编写java代码进行编译"),
                $sce.trustAsHtml("<h3>REPL中编译</h3>"),
                $sce.trustAsHtml("在REPL环境中使用Compiler()构造一个编译器对象, 然后通过如下的方法调用进行编译"),
                $sce.trustAsHtml(common_compile_highlighting()),
                $sce.trustAsHtml("<h3>编写java代码编译</h3>"),
                $sce.trustAsHtml("" +
                    "<textarea id='compile_java'>\n" +
                    "Compiler compiler=new Compiler();\n" +
                    "compiler.add(\"class-path\")\n" +
                    "    .shiftRight(\"output directory\")\n" +
                    "    .compile(lt.lang.Utils.filesInDirectory(\n" +
                    "        \"source file directory\",\".*\\\\.lt\"\n" +
                    "    ))" +
                    "</textarea>" +
                    "<script>var editor = CodeMirror.fromTextArea(document.getElementById('compile_java'));editor.setSize('auto', '150px');</script>")
            ]
            :[
                $sce.trustAsHtml("You can compile files in REPL, or write java code to compile."),
                $sce.trustAsHtml("<h3>Compile in REPL</h3>"),
                $sce.trustAsHtml("Enter the REPL mode and use <code>Compiler()</code> to construct a <code>lt::repl::Compiler</code> object, then call the following methods to compile:"),
                $sce.trustAsHtml(common_compile_highlighting()),
                $sce.trustAsHtml("<h3>Compile in Java</h3>"),
                $sce.trustAsHtml("" +
                    "<textarea id='compile_java'>\n" +
                    "Compiler compiler=new Compiler();\n" +
                    "compiler.add(\"class-path\")\n" +
                    "    .shiftRight(\"output directory\")\n" +
                    "    .compile(lt.lang.Utils.filesInDirectory(\n" +
                    "        \"source file directory\",\".*\\\\.lt\"\n" +
                    "    ))" +
                    "</textarea>" +
                    "<script>var editor = CodeMirror.fromTextArea(document.getElementById('compile_java'));editor.setSize('auto', '160px');</script>")
            ]
        };
        $scope.compile = {
            title: zh?"Script 与 编译":"Script and Compilation",
            contents:
            zh?[
                $sce.trustAsHtml("Latte支持Script, 所以编译建议通过脚本完成"),
                $sce.trustAsHtml(common_script_highlighting()),
                $sce.trustAsHtml("可以使用如下方法运行脚本"),
                $sce.trustAsHtml("<code>.&#47;latte -s script-file-path script-arguments</code>"),
                $sce.trustAsHtml("或者进入REPL后, 输入"),
                $sce.trustAsHtml("<code>:script script-path<br>" +
                    "script run<br>" +
                    "script run ['string-array-as-arguments']" +
                    "</code>")
            ]:[
                $sce.trustAsHtml("Latte supports scripts. It's recommended to compile via using scripts."),
                $sce.trustAsHtml(common_script_highlighting()),
                $sce.trustAsHtml("Run the script in this way: "),
                $sce.trustAsHtml("<code>.&#47;latte -s script-file-path script-arguments</code>"),
                $sce.trustAsHtml("or enter the REPL, type in: "),
                $sce.trustAsHtml("<code>:script script-path<br>" +
                    "script run<br>" +
                    "script run ['string-array-as-arguments']" +
                    "</code>")
            ]
        };
        $scope.highlight = {
            title: zh?"语法高亮":"Syntax Highlighting",
            contents: zh?[
                $sce.trustAsHtml("Latte 支持 <code>HTML</code> 和 <code>Atom 编辑器</code> 的语法高亮"),
                $sce.trustAsHtml("<h3>HTML</h3>"),
                $sce.trustAsHtml("HTML语法高亮基于<code>Latte Scanner for JavaScript</code>. 在您的html文件中引入如下文件和代码即可获取高亮生成的内容"),
                $sce.trustAsHtml("<textarea id='highlighting_html'>\n" +
                    "<script src=\"javascripts/scanner.js\"></script>\n" +
                    "<script src=\"javascripts/code.js\"></script>\n" +
                    "<link rel='stylesheet' href=\"stylesheets/code.css\">\n\n" +
                    "<script>\n" +
                    "var code = ''\n" +
                    "var res = highlighting('', code, {});\n" +
                    "$('pre').html(res);\n" +
                    "</script>" +
                    "</textarea>\n" +
                    "<script>var editor = CodeMirror.fromTextArea(document.getElementById('highlighting_html'));editor.setSize('auto', '220px');</script>"),
                $sce.trustAsHtml("<h3>Atom</h3>"),
                $sce.trustAsHtml("在Atom中搜索并安装 <code>Atom-Latte-lang-Highlighting</code> 或者按照<a target='_blank' href='https://atom.io/packages/Atom-Latte-lang-Highlighting'>这里</a>的步骤进行安装"),
                $sce.trustAsHtml("<img src='images/highlight.png'>")
            ]:[
                $sce.trustAsHtml("Latte supports <code>HTML</code> and <code>Atom</code> syntax highlighting."),
                $sce.trustAsHtml("<h3>HTML</h3>"),
                $sce.trustAsHtml("HTML syntax highlighting is based on <code>Latte Scanner for JavaScript</code>. Introduce the following files and codes to retrieve the highlighted contents:"),
                $sce.trustAsHtml("<textarea id='highlighting_html'>\n" +
                    "<script src=\"javascripts/scanner.js\"></script>\n" +
                    "<script src=\"javascripts/code.js\"></script>\n" +
                    "<link rel='stylesheet' href=\"stylesheets/code.css\">\n\n" +
                    "<script>\n" +
                    "var code = ''\n" +
                    "var res = highlighting('', code, {});\n" +
                    "$('pre').html(res);\n" +
                    "</script>" +
                    "</textarea>\n" +
                    "<script>var editor = CodeMirror.fromTextArea(document.getElementById('highlighting_html'));editor.setSize('auto', '220px');</script>"),
                $sce.trustAsHtml("<h3>Atom</h3>"),
                $sce.trustAsHtml("Search and Install <code>Atom-Latte-lang-Highlighting</code> in Atom, or follow instructions <a target='_blank' href='https://atom.io/packages/Atom-Latte-lang-Highlighting'>here</a> to install."),
                $sce.trustAsHtml("<img src='images/highlight.png'>")
            ]
        };
        $scope.ide = {
            title: "IDE",
            contents: zh?[
                $sce.trustAsHtml("针对Atom开发了一个Latte的IDE"),
                $sce.trustAsHtml("在Atom中搜索并安装 <code>atom-latte-lang-ide</code> 并按照<a target='_blank' href='https://atom.io/packages/atom-latte-lang-ide'>这里</a>的步骤进行配置")
            ]:[
                $sce.trustAsHtml("Atom Latte IDE"),
                $sce.trustAsHtml("Search and Install <code>atom-latte-lang-ide</code> in Atom, and follow instructions <a target='_blank' href='https://atom.io/packages/atom-latte-lang-ide'>here</a> to configure settings.")
            ]
        }
    }
    ])
    ;
})
;