function common_git_repo() {
    return "https://github.com/wkgcass/LessTyping";
}
function common_navs() {
    return [
        {
            name: "主页",
            active: false,
            link: "index.html"
        },
        {
            name: "下载",
            active: false,
            link: "download.html"
        },
        {
            name: "教程",
            active: false,
            link: "tutorial.html"
        },
        {
            name: "语法",
            active: false,
            link: "syntax.html"
        },
        {
            name: "示例",
            active: false,
            link: "example.html"
        },
        {
            name: "开发",
            active: false,
            link: "dev.html"
        }
    ]
}
function common_compile_highlighting() {
    return "<pre class='code' style='height:200px'>" +
        highlighting("compile.lt",
            "compiler = Compiler()\n" +
            "compiler + 'class-path'\n" +
            "compiler >> 'output directory'\n" +
            "compiler compile filesInDirectory('source file directory')\n" +
            "\n" +
            "; or you can chain these invocations up\n" +
            "\n" +
            "Compiler() + 'class-path' >> 'output dir' compile filesInDirectory('source file directory')", {}) +
        "</pre>";
}

function common_script_highlighting() {
    var txt;
    $.ajax({
        url: 'https://raw.githubusercontent.com/wkgcass/LessTyping/master/src/main/resources/build.lts',
        async: false,
        dataType: 'text',
        success: function (res) {
            txt = highlighting('build.lts', res, {});
        }
    });
    return "<pre>\n" + txt + "\n</pre>";
}