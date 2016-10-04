function useZh() {
    var paramUseZh = getQueryString('useZh');
    if (paramUseZh) {
        if (paramUseZh == 'false' || paramUseZh == 'no' || paramUseZh == '0') return false;
        else return true;
    }

    var lang = navigator.language;
    if (!lang) lang = navigator.browserLanguage;
    if (!lang) lang = "en-us"
    return lang.length > 2 && lang.toLowerCase().substring(0, 2) == 'zh';
}

function getQueryString(name) {
    var reg = new RegExp("(^|&)" + name + "=([^&]*)(&|$)");
    var h = window.location.href;
    if (h.indexOf('#') == -1) {
        h = '';
    } else {
        h = h.substring(h.indexOf('#') + 1);
    }
    var r = h.match(reg);
    if (r != null)return unescape(r[2]);
    return null;
}

function common_git_repo() {
    return "https://github.com/wkgcass/Latte-lang";
}
function common_navs() {
    var isZh = useZh();
    var suffix = "#useZh=" + (isZh ? "1" : "0");
    return [
        {
            name: isZh ? "主页" : "Home",
            active: false,
            link: "index.html" + suffix
        },
        {
            name: isZh ? "下载" : "Download",
            active: false,
            link: "download.html" + suffix
        },
        {
            name: isZh ? "介绍" : "Intro",
            active: false,
            link: "tutorial.html" + suffix
        },
        {
            name: isZh ? "基础语法" : "Basic Syntax",
            active: false,
            link: "syntax.html" + suffix
        },
        {
            name: isZh ? "示例" : "Examples",
            active: false,
            link: "example.html" + suffix
        },
        {
            name: isZh ? "开发" : "Develop",
            active: false,
            link: "dev.html" + suffix
        },
        {
            name: "Switch Language",
            active: false,
            link: "#useZh=" + (isZh ? "false" : "true")
        }
    ];
}
function common_compile_highlighting() {
    return "<pre class='code' style='height:200px'>" +
        highlighting("compile.lt",
            "compiler = Compiler()\n" +
            "compiler + 'class-path'\n" +
            "compiler >> 'output directory'\n" +
            "compiler compile filesInDirectory('source file directory', //.*\\.lt//)\n" +
            "\n" +
            "; or you can chain these invocations up\n" +
            "\n" +
            "Compiler() + 'class-path' >> 'output dir' compile filesInDirectory('source file directory', //.*\\.lt//)", {}) +
        "</pre>";
}

function common_script_highlighting() {
    var txt;
    $.ajax({
        url: 'https://raw.githubusercontent.com/wkgcass/Latte-lang/master/latte-compiler/src/main/resources/build.lts.template',
        async: false,
        dataType: 'text',
        success: function (res) {
            txt = highlighting('build.lts', res, {});
        }
    });
    return "<pre>\n" + txt + "\n</pre>";
}

window.addEventListener('hashchange', function () {
    window.location.reload();
});