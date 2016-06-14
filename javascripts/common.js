function useZh(){
    var paramUseZh=GetQueryString('useZh');
    if(paramUseZh){
        if(paramUseZh=='false' || paramUseZh=='no' || paramUseZh=='0') return false;
        else return true;
    }

    var lang = navigator.language;
    if(!lang) navigator.browserLanguage;
    if(!lang) lang="en-us"
    return lang.length>2 && lang.toLowerCase().substring(0,2)=='zh';
}

function GetQueryString(name)
{
     var reg = new RegExp("(^|&)"+ name +"=([^&]*)(&|$)");
     var r = window.location.search.substr(1).match(reg);
     if(r!=null)return  unescape(r[2]); return null;
}

function common_git_repo() {
    return "https://github.com/wkgcass/Latte-lang";
}
function common_navs() {
    var isZh=useZh();
    return [
        {
            name: isZh?"主页":"Home",
            active: false,
            link: "index.html"
        },
        {
            name: isZh?"下载":"Download",
            active: false,
            link: "download.html"
        },
        {
            name: isZh?"教程":"Intro",
            active: false,
            link: "tutorial.html"
        },
        {
            name: isZh?"语法":"Syntax",
            active: false,
            link: "syntax.html"
        },
        {
            name: isZh?"示例":"Examples",
            active: false,
            link: "example.html"
        },
        {
            name: isZh?"开发":"Develop",
            active: false,
            link: "dev.html"
        },
        {
            name: "Switch Language",
            active: false,
            link: "?useZh=" + (isZh?"false":"true")
        }
    ]
}
function common_compile_highlighting() {
    return "<pre class='code' style='height:200px'>" +
        highlighting("compile.lt",
            "compiler = Compiler()\n" +
            "compiler + 'class-path'\n" +
            "compiler >> 'output directory'\n" +
            "compiler compile filesInDirectory('source file directory', '.*\\\\.lt')\n" +
            "\n" +
            "; or you can chain these invocations up\n" +
            "\n" +
            "Compiler() + 'class-path' >> 'output dir' compile filesInDirectory('source file directory', '.*\\\\.lt')", {}) +
        "</pre>";
}

function common_script_highlighting() {
    var txt;
    $.ajax({
        url: 'https://raw.githubusercontent.com/wkgcass/Latte-lang/master/src/main/resources/build.lts',
        async: false,
        dataType: 'text',
        success: function (res) {
            txt = highlighting('build.lts', res, {});
        }
    });
    return "<pre>\n" + txt + "\n</pre>";
}