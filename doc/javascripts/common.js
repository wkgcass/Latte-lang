Array.prototype.addAll = function ($array) {
    if ($array == null || $array.length == 0)
        return;
    for (var $i = 0; $i < $array.length; $i++)
        this.push($array[$i]);
};
String.prototype.startWith = function (str) {
    if (str == null || str == "" || this.length == 0 || str.length > this.length)
        return false;
    if (this.substr(0, str.length) == str)
        return true;
    else
        return false;
    return true;
};
String.prototype.endsWith = function (s) {
    if (s == null || s == "" || this.length == 0 || s.length > this.length)
        return false;
    if (this.substring(this.length - s.length) == s)
        return true;
    else
        return false;
    return true;
}
String.prototype.contains = function (str) {
    return this.indexOf(str) != -1;
}
Array.prototype.contains = function (item) {
    for (i = 0; i < this.length; i++) {
        if (this[i] == item) {
            return true;
        }
    }
    return false;
};
String.prototype.trim = function () {
    return this.replace(/(^\s*)|(\s*$)/g, "");
};
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
            link: "examples.html"
        },
        {
            name: "开发",
            active: false,
            link: "dev.html"
        }
    ]
}
