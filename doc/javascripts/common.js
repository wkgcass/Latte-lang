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
    return "<pre class='code'>" +
        highlighting("compile.lt",
            "compiler = Compiler()\n" +
            "compiler << 'source file directory'\n" +
            "compiler >> 'output directory'\n" +
            "compiler compile\n" +
            "; or you can chain these invocations up\n" +
            "Compiler() << 'source file dir' >> 'output dir' compile", {}) +
        "</pre>";
}