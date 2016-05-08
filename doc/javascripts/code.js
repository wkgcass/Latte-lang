Array.prototype.addAll = function ($array) {
    if ($array == null || $array.length == 0)
        return;
    for (var $i = 0; $i < $array.length; $i++)
        this.push($array[$i]);
};
String.prototype.startsWith = function (str) {
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

function highlighting(file, code, config) {
    var root = new Scanner(file, code, config).parse();
    var lines = code.split(/\n|\r/);
    var map = {};

    function travelStartNode(startNode) {
        if (startNode.hasLinkedNode()) {
            var linked = startNode.linkedNode;
            if (linked.type == TYPE_ElementStartNode) {
                travelStartNode(linked);
            }
            travelNormal(linked);
        }
    }

    function travelNormal(node) {
        if (node) {
            if (node.type == TYPE_ElementStartNode) {
                travelStartNode(node);
            } else if (node.elem) {
                // it's an element
                var useDefine = node.getLineCol().useDefine;
                var isEmpty = true;
                for (k in useDefine) {
                    isEmpty = false;
                    break;
                }
                if (isEmpty) {
                    var lineCol = node.getLineCol();

                    if (node.original) {
                        node.elem = node.original;
                    }

                    var replacement = "<span class='" + getDomClass(node.type) + "'>" + node.elem + "</span>";
                    replace(lineCol.line - 1, lineCol.column - 1, lineCol.length, replacement);
                }
            }
            travelNormal(node.next);
        }
    }

    function getDomClass(type) {
        /**
         * var TYPE_EndingNode = 0;
         * var TYPE_EndingNodeStrong = 1;
         * var TYPE_ElementStartNode = 2;
         * var TYPE_STRING = 3;
         * var TYPE_NUMBER = 4;
         * var TYPE_BOOL = 5;
         * var TYPE_VALID_NAME = 6;
         * var TYPE_MODIFIER = 7;
         * var TYPE_KEY = 8;
         * var TYPE_SYMBOL = 9;
         */
        if (type == TYPE_STRING) return 'str';
        if (type == TYPE_NUMBER) return 'num';
        if (type == TYPE_BOOL) return 'bool';
        if (type == TYPE_MODIFIER) return 'modifier';
        if (type == TYPE_KEY) return 'key';
        if (type == TYPE_SYMBOL)
            return 'symbol';
        if (type == TYPE_VALID_NAME) return 'valid';
        throw "unknown type " + type;
    }

    function replace(line,
                     start, /* inclusive */
                     len,
                     replacement) {
        var inc;
        if (map[line]) {
            inc = map[line];
        } else {
            map[line] = 0;
            inc = 0;
        }

        var l = lines[line];
        var left = l.substring(0, start + inc);
        var right = l.substring(start + len + inc);
        lines[line] = left + replacement + right;

        inc += (replacement.length - len);
        map[line] = inc;
    }

    travelStartNode(root);

    var res = "";
    for (var i = 0; i < lines.length; ++i) {
        res += (lines[i] + "<br>");
    }
    return res;
}