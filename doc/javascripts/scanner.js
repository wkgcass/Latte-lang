var LAYER = ["->"];
var SPLIT_X = [
    ".", // class positioning or method access
    ":", // type specification or generic extends
    "::", // package::package
    "=", "+=", "-=", "*=", "/=", "%=", // assignment
    "<<", ">>", ">>>", // shift
    "&", "^", "|", "~", // bit logic
    "^^", // pow
    "!", "&&", "||", // logic
    "!=", "==", "!==", "===", // equals/reference equals
    "<", ">", "<=", ">=", // comparison or generic extends/super
    "+", "-", "*", "/", "%", // operators
    "++", "--",
    "@", // @Annotation
    "=:=", "!:=", // equal or not$equal
    "..", ".:", // list generator
    "..." // pass
];
var STRING = ["\"", "'", "`"];
var ESCAPE = "\\";
var NO_RECORD = [" "];
var ENDING = ",";
var COMMENT = ";";
var PAIR = {
    "{": "}",
    "[": "]",
    "(": ")"
};
SPLIT = [];

SPLIT_X.addAll(NO_RECORD);

SPLIT.addAll(LAYER);
SPLIT.addAll(SPLIT_X);
SPLIT.addAll(STRING);
SPLIT.push(ENDING);
SPLIT.push(COMMENT);
for (var k in PAIR) {
    SPLIT.push(k);
    SPLIT.push(PAIR[k]);
}
SPLIT.sort(function (a, b) {
    return b.length - a.length
});

function Node(args, type, indent, elem) {
    this.type = type;
    this.lineCol = args.generateLineCol();
    this.indent = indent;
    this.elem = elem;
    this.original = elem;
    this.previous = args.previous;
    this.next = null;

    if (this.elem) {
        this.lineCol.length = this.elem.length;
    } else {
        this.lineCol.length = 0;
    }

    this.getTokenType = function () {
        return this.type;
    };

    this.hasNext = function () {
        return this.next != null;
    };

    this.hasPrevious = function () {
        return this.previous != null;
    };

    this.getLineCol = function () {
        return this.lineCol;
    };

    this.setLineCol = function (lineCol) {
        this.lineCol = lineCol;
    };

    this.setNext = function (next) {
        this.next = next;
    };

    this.setPrevious = function (previous) {
        this.previous = previous;
    };

    // variables for ElementStartNode
    this.indent = indent;
    this.linkedNode = null;

    this.hasLinkedNode = function () {
        return this.linkedNode != null;
    };

    this.setLinkedNode = function (linkedNode) {
        this.linkedNode = linkedNode;
    };

    this.getLinkedNode = function () {
        return this.linkedNode;
    };

    // elem
    this.checkWhetherIsValidName = function () {
        if (this.type == TYPE_VALID_NAME) {
            this.elem = validateValidName(this.elem);
        }
    };

    // construct

    if (this.hasPrevious()) {
        this.previous.next = this;
    } else if (args.startNodeStack.length != 0) {
        args.startNodeStack[args.startNodeStack.length - 1].setLinkedNode(this);
    }
}

/**
 * @return {string}
 */
function SyntaxException(msg, lineCol) {
    return msg + " at " + lineCol.fileName + "(" + lineCol.line + "," + lineCol.column + ")";
}

/**
 * @return {string}
 */
function IllegalIndentationException(indent, lineCol) {
    return SyntaxException("Illegal Indentation " + indent, lineCol);
}

/**
 * @return {string}
 */
function UnexpectedTokenException(expected, got, lineCol) {
    return SyntaxException("expecting " + expected + ", but got " + got, lineCol);
}

/**
 * @return {string}
 */
function UnknownTokenException(token, lineCol) {
    return SyntaxException("unknown token " + token, lineCol);
}

var javaKeys = ["abstract", "assert", "boolean", "break", "byte", "case",
    "catch", "char", "class", "const", "continue", "default",
    "do", "double", "else", "enum", "extends", "final", "finally",
    "float", "for", "if", "implements", "import", "instanceof",
    "int", "interface", "long", "native", "new", "null", "package",
    "private", "protected", "public", "return", "short", "static",
    "strictfp", "throw", "try", "while"];

var keys = ["is", "not", "bool", "yes", "no",
    "type", "as", "undefined", "in", "elseif",
    "package", "import", "break", "continue", "return"];

var modifiers = ["public", "protected", "private", "pkg",
    "abstract", "val", "native", "synchronized", "transient", "volatile", "strictfp"];

var oneVarOperatorsPost = ["++", "--"];

var oneVarOperatorsPreWithoutCheckingExps = ["!", "~"];

var oneVarOperatorsPreMustCheckExps = ["++", "--", "!", "~", "+", "-"];

var twoVar_priority = [
    // 1..5 means [1,2,3,4,5]
    // 1.:5 means [1,2,3,4]
    ["..", ".:"],
    ["^^"], // pow
    ["*", "/", "%"],
    ["+", "-"],
    ["<<", ">>", ">>>"],
    [">", "<", ">=", "<="],
    ["==", "!=", "===", "!==", "=:=", "!:=", "is", "not", "in"],
    ["&"],
    ["^"],
    ["|"],
    ["&&", "and"],
    ["||", "or"]
];
var twoVarOperators = [];
for (var i = 0; i < twoVar_priority.length; ++i) {
    for (var j = 0; j < twoVar_priority[i].length; ++j) {
        twoVarOperators.push(twoVar_priority[i][j]);
    }
}

keys.addAll(modifiers);
keys.addAll(javaKeys);

var primitives = ["int", "double", "float", "short", "long", "byte", "char", "bool"];

var TYPE_EndingNode = 0;
var TYPE_EndingNodeStrong = 1;
var TYPE_ElementStartNode = 2;
var TYPE_STRING = 3;
var TYPE_NUMBER = 4;
var TYPE_BOOL = 5;
var TYPE_VALID_NAME = 6;
var TYPE_MODIFIER = 7;
var TYPE_KEY = 8;
var TYPE_SYMBOL = 9;

function isNumber(s) {
    var res = s.match(/(\b[0-9]+\.[0-9]+\b|\b[0-9]+\b)/);
    return res && res.length != 0 && res[0] == s;
}

function validateValidName(validName) {
    if (validName.startsWith("`")) return validName.substring(1, validName.length - 1);
    return validName;
}

function isBoolean(str) {
    return ["yes", "no", "true", "false"].contains(str);
}
function isModifier(str) {
    return modifiers.contains(str);
}
function isString(str) {
    return (str.startsWith("\"") && str.endsWith("\"")) || (str.startsWith("'") && str.endsWith("'"));
}
function isKey(str) {
    return keys.contains(str);
}
function isAssign(s) {
    return s == ("=") || s == ("+=") || s == ("-=") || s == ("*=") || s == ("/=") || s == ("%=");
}

function isOneVariableOperatorPost(str) {
    return oneVarOperatorsPost.contains(str);
}

function isTwoVariableOperator(str) {
    return twoVarOperators.contains(str);
}

function isOneVariableOperatorPreWithoutCheckingExps(str) {
    return oneVarOperatorsPreWithoutCheckingExps.contains(str);
}

function isOneVariableOperatorPreMustCheckExps(str) {
    return oneVarOperatorsPreMustCheckExps.contains(str);
}

function isSymbol(str) {
    return isTwoVariableOperator(str)
        || isOneVariableOperatorPost(str)
        || isOneVariableOperatorPreMustCheckExps(str)
        || isOneVariableOperatorPreWithoutCheckingExps(str)
        || isAssign(str);
}

function isValidNameStartChar(c) {
    return (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || c == '$' || c == '_';
}

function isValidNameChar(c) {
    return isValidNameStartChar(c) || (c >= '0' && c <= '9');
}

function isJavaValidName(str) {
    if (str.length == 0) return false;
    if (javaKeys.contains(str)) return false;
    var first = str[0];
    if (isValidNameStartChar(first)) {
        for (var i = 1; i < str.length; ++i) {
            var c = str[i];
            if (!isValidNameChar(c)) return false;
        }
        return true;
    } else {
        return false;
    }
}

function isValidName(str) {
    if (str.startsWith("`") && str.endsWith("`")) {
        return isJavaValidName(str.substring(1, str.length - 1));
    }
    return isJavaValidName(str) && !keys.contains(str);
}

function Scanner(filename, input, config) {
    var properties = {
        _INDENTATION_: config._INDENTATION_ == undefined ? 4 : config._INDENTATION_,
        _LINE_BASE_: config._LINE_BASE_ == undefined ? 0 : config._LINE_BASE_,
        _COLUMN_BASE_: config._COLUMN_BASE_ == undefined ? 0 : config._COLUMN_BASE_
    };

    this.parse = function () {
        var lines = input.split(/\n|\r/);
        var args = {
            fileName: filename,
            previous: null,
            currentLine: properties._LINE_BASE_,
            currentCol: properties._COLUMN_BASE_,
            startNodeStack: [],
            pairEntryStack: [],
            useDefine: {},
            generateLineCol: function () {
                return {
                    fileName: this.fileName,
                    line: this.currentLine,
                    column: this.currentCol,
                    useDefine: this.useDefine
                }
            },
            defined: {}
        };

        var elementStartNode = new Node(args, TYPE_ElementStartNode, 0);
        args.startNodeStack.push(elementStartNode);
        parse0(args, lines);
        finalCheck(elementStartNode);
        return elementStartNode;
    };

    function getStringForPreProcessing(line, args, originalString, command) {
        var str = line.trim();
        if (str.length == 0) throw SyntaxException("illegal " + command + " command " + originalString,
            args.generateLineCol());

        var token = str[0];
        if (!STRING.contains(token))
            throw SyntaxException("illegal " + command + " command " + originalString, args.generateLineCol());
        args.currentCol += line.indexOf(token); // set current column

        line = str;

        var lastIndex = 0;
        var lineCol = args.generateLineCol();
        while (true) {
            var index = line.indexOf(token, lastIndex + 1);
            if (line.length <= 1 || index == -1)
                throw SyntaxException("end of string not found", lineCol);
            var c = line[index - 1];
            if (ESCAPE != c) {
                // the string starts at minIndex and ends at index
                s = line.substring(0, index + 1);

                args.currentCol += (index + token.length);
                line = line.substring(index + 1);

                token = s;

                break;
            }

            lastIndex = index;
        }
        return {
            line: line,
            str: token,
            lineCol: lineCol
        };
    }

    function parse0(args, lines) {
        var cursor = 0;
        var line = lines.length == cursor ? null : lines[cursor++];
        var rootIndent = -1;
        while (line != null) {
            ++args.currentLine;
            args.useDefine = {};

            // pre processing
            if (line.startsWith("define")) {
                if (line != "define" &&
                    SPLIT.contains(line.substring("define".length, "define".length + 1))) {
                    ++args.currentCol;

                    var originalString = line;

                    line = line.substring("define".length);
                    args.currentCol += "define".length;
                    var las1 = getStringForPreProcessing(line, args, originalString, "define");
                    line = las1.line;
                    target = las1.str.substring(1, las1.str.length - 1);
                    if (target.length == 0) throw SyntaxException("define <target> length cannot be 0", las1.lineCol);
                    if (target.contains(ESCAPE)) throw SyntaxException("define <target> cannot contain escape char", las1.lineCol);

                    if (!line.trim().startsWith("as")) {
                        throw SyntaxException("illegal define command " +
                            "(there should be an `as` between <target> and <replacement>)",
                            args.generateLineCol());
                    }

                    var asPos = line.indexOf("as");
                    line = line.substring(asPos + 2);

                    if (line.length == 0) throw SyntaxException("illegal define command " + originalString,
                        args.generateLineCol());
                    if (!SPLIT.contains(line[0]))
                        throw SyntaxException("illegal define command " +
                            "(there should be an `as` between <target> and <replacement>)",
                            args.generateLineCol());

                    args.currentCol += (asPos + 2);

                    var las2 = getStringForPreProcessing(line, args, originalString, "define");
                    line = las2.line;
                    var replacement = las2.str.substring(1, las2.str.length - 1); // defined replacement
                    if (replacement.contains(ESCAPE))
                        throw SyntaxException("define <replacement> cannot contain escape char", las2.lineCol);
                    if (line.trim().length != 0) throw SyntaxException("illegal define command " +
                        "(there should not be characters after <replacement>)", args.generateLineCol());

                    args.defined[target] = replacement;

                    line = lines.length == cursor ? null : lines[cursor++];
                    args.currentCol = properties._COLUMN_BASE_;
                    continue;
                }
            } else if (line.startsWith("undef")) {
                if (line != "undef" &&
                    SPLIT.contains(line.substring("undef".length, "undef".length + 1))) {
                    ++args.currentCol;

                    var lineStart = args.generateLineCol();
                    originalString = line;

                    line = line.substring("undef".length);
                    args.currentCol += "undef".length;
                    las1 = getStringForPreProcessing(line, args, originalString, "undef");
                    line = las1.line;
                    var target = las1.str.substring(1, las1.str.length - 1);
                    if (target.length == 0) throw SyntaxException("undef <target> length cannot be 0", las1.lineCol);
                    if (target.contains(ESCAPE)) throw SyntaxException("undef <target> cannot contain escape char", las1.lineCol);

                    if (line.trim().length != 0) throw SyntaxException("illegal undef command " +
                        "(there should not be characters after <target>)", args.generateLineCol());

                    if (!args.defined[target]) throw SyntaxException("\"" + target + "\" is not defined", lineStart);

                    args.defined[target] = undefined;

                    line = lines.length == cursor ? null : lines[cursor++];
                    args.currentCol = properties._COLUMN_BASE_;
                    continue;
                }
            }

            // the line is nothing but comment
            if (line.trim().startsWith(COMMENT)) {
                line = lines.length == cursor ? null : lines[cursor++];
                continue;
            }

            var COMMENT_index = line.indexOf(COMMENT);
            if (COMMENT_index != -1) {
                var pre = line.substring(0, COMMENT_index);
                var post = line.substring(COMMENT_index);
                for (var key in args.defined) {
                    var tmp = pre.replace(key, args.defined[key]);
                    if (tmp != pre) args.useDefine[key] = args.defined[key];
                    pre = tmp;
                }
                line = pre + post;
            } else {
                for (var key in args.defined) {
                    var tmp = line.replace(key, args.defined[key]);
                    if (tmp != line) args.useDefine[key] = args.defined[key];
                    line = tmp;
                }
            }

            // get front spaces
            var spaces = 0;
            for (var i = 0; i < line.length; ++i) {
                if (line[i] != ' ') {
                    spaces = i;
                    break;
                }
            }

            // set root indent
            if (rootIndent == -1) {
                rootIndent = spaces;
                spaces = 0;
            } else {
                spaces -= rootIndent;
            }

            // check space indent
            if (spaces % properties._INDENTATION_ != 0) throw IllegalIndentationException(
                properties._INDENTATION_, args.generateLineCol());

            // remove spaces
            line = line.trim();

            args.currentCol = spaces + 1 + rootIndent + properties._COLUMN_BASE_;

            // check it's an empty line
            if (line.length == 0) {
                line = lines.length == cursor ? null : lines[cursor++];
                continue;
            }

            // check start node
            if (args.startNodeStack[args.startNodeStack.length - 1].indent != spaces) {
                if (args.startNodeStack[args.startNodeStack.length - 1].indent > spaces) {
                    // smaller indent
                    redirectToStartNodeByIndent(args, spaces + properties._INDENTATION_);
                } else if (args.startNodeStack[args.startNodeStack.length - 1].indent == spaces - properties._INDENTATION_) {
                    // greater indent
                    createStartNode(args);
                } else {
                    throw IllegalIndentationException(properties._INDENTATION_, args.generateLineCol());
                }
            }

            // start parsing
            parse1(line, args);

            if (args.previous instanceof Element) {
                args.previous = new Node(args, TYPE_EndingNode);
            }

            line = lines.length == cursor ? null : lines[cursor++];
        }
    }

    function redirectToStartNodeByIndent(args, indent) {
        var startNode = args.startNodeStack.pop();

        if (startNode == null) throw "NoSuchElementException";
        if (startNode.indent == indent) {
            if (startNode.hasNext()) {
                throw UnexpectedTokenException("null", startNode.next, args.generateLineCol());
            }
            // do redirect
            args.previous = startNode;
        } else {
            if (startNode.indent < indent || args.startNodeStack.length == 0) {
                throw "NoSuchElementException(position=" + args.currentLine + ":" + args.currentCol + ",indent=" + indent;
            }
            redirectToStartNodeByIndent(args, indent);
        }
    }

    function createStartNode(args) {
        elementStartNode = new Node(args, TYPE_ElementStartNode, args.startNodeStack[args.startNodeStack.length - 1].indent + properties._INDENTATION_);
        args.previous = null;
        args.startNodeStack.push(elementStartNode);
    }

    function getTokenType(str, lineCol) {
        if (isBoolean(str)) return TYPE_BOOL;
        if (isModifier(str)) return TYPE_MODIFIER;
        if (isNumber(str)) return TYPE_NUMBER;
        if (isString(str)) return TYPE_STRING;
        if (isKey(str)) return TYPE_KEY; // however in/is/not are two variable operators, they are marked as keys
        if (isSymbol(str)) return TYPE_SYMBOL;
        if (SPLIT.contains(str)) return TYPE_SYMBOL;
        if (isValidName(str)) return TYPE_VALID_NAME;
        throw UnknownTokenException(str, lineCol);
    }

    function parse1(line, args) {
        if (line.length == 0) return;
        // check SPLIT
        // find the pattern at minimum location index and with longest words
        var minIndex = line.length;
        var token = null; // recorded token
        for (var i = 0; i < SPLIT.length; ++i) {
            var s = SPLIT[i];
            if (line.contains(s)) {
                var index = line.indexOf(s);
                if (index != -1 && index < minIndex) {
                    minIndex = index;
                    token = s;
                }
            }
        }

        if (token == null) {
            // not found, append to previous
            args.previous = new Node(args, getTokenType(line, args.generateLineCol()), undefined, line);
        } else {
            var copyOfLine = line;
            var str = line.substring(0, minIndex);
            if (str.length != 0) {
                // record text before the token
                args.previous = new Node(args, getTokenType(str, args.generateLineCol()), undefined, str);
                args.currentCol += str.length;
            }

            if (LAYER.contains(token)) {
                // start new layer
                args.previous = new Node(args, getTokenType(token, args.generateLineCol()), undefined, token);
                createStartNode(args);
            } else if (SPLIT_X.contains(token)) {
                // do split check
                if (!NO_RECORD.contains(token)) {
                    // record this token
                    args.previous = new Node(args, getTokenType(token, args.generateLineCol()), undefined, token);
                }
            } else if (STRING.contains(token)) {
                // string literal
                var lastIndex = minIndex;
                while (true) {
                    index = line.indexOf(token, lastIndex + 1);
                    if (index == -1)
                        throw SyntaxException("end of string not found", args.generateLineCol());
                    var c = line[index - 1];
                    if (ESCAPE != c) {
                        // the string starts at minIndex and ends at index
                        s = line.substring(minIndex, index + 1);

                        args.previous = new Node(args, getTokenType(s, args.generateLineCol()), undefined, s);
                        args.currentCol += (index - minIndex);
                        line = line.substring(index + 1);
                        break;
                    }

                    lastIndex = index;
                }
            } else if (ENDING == token) {
                // ending
                if (args.previous.elem) {
                    args.previous = new Node(args, TYPE_EndingNodeStrong);
                }
            } else if (COMMENT == token) {
                // comment
                line = ""; // ignore all
            } else if (PAIR[token]) {
                // pair start
                args.previous = new Node(args, getTokenType(token, args.generateLineCol()), undefined, token);
                createStartNode(args);
                args.pairEntryStack.push({
                    key: token,
                    startNode: args.startNodeStack[args.startNodeStack.length - 1]
                });
            } else if (containsValue(PAIR, token)) {
                // pair end
                var entry = args.pairEntryStack.pop();
                var start = entry.key;
                if (!token == PAIR[start]) {
                    throw UnexpectedTokenException(PAIR.get(start), token, args.generateLineCol());
                }

                var startNode = entry.startNode;
                if (startNode.hasNext()) {
                    throw UnexpectedTokenException("null", startNode.next.toString(), args.generateLineCol());
                }

                if (args.startNodeStack[args.startNodeStack.length - 1].indent >= startNode.indent) {
                    redirectToStartNodeByIndent(args, startNode.indent);
                } else if (args.startNodeStack[args.startNodeStack.length - 1].indent == startNode.indent - properties._INDENTATION_) {
                    args.previous = startNode;
                } else {
                    throw SyntaxException(
                        "indentation of " + token + " should >= " + start + "'s indent or equal to " + start + "'s indent - " + properties._INDENTATION_,
                        args.generateLineCol());
                }
                args.previous = new Node(args, getTokenType(token, args.generateLineCol()), undefined, token);
            } else {
                throw UnexpectedTokenException(token, args.generateLineCol());
            }

            // column
            args.currentCol += token.length;
            if (copyOfLine == line) {
                // line hasn't changed, do default modification
                line = line.substring(minIndex + token.length);
            }
            // recursively parse
            parse1(line, args);
        }
    }

    function containsValue(map, v) {
        for (var s in map) {
            if (map[s] == v) return true;
        }
        return false;
    }

    function finalCheck(root) {
        if (root.hasLinkedNode()) {
            var n = root.getLinkedNode();
            while (n != null) {
                if (n.type == TYPE_ElementStartNode) {
                    finalCheck(n);
                }
                if ((n.type == TYPE_EndingNode || n.type == TYPE_EndingNodeStrong)
                    && (!n.hasNext() || !(n.next.elem))) {
                    if (n.hasPrevious()) {
                        n.previous.setNext(n.next);
                    }
                    if (n.hasNext()) {
                        n.next.setPrevious(n.previous);
                    }
                } else if (n.elem) {
                    n.checkWhetherIsValidName();
                    if (n.elem == "."
                        && n.hasPrevious()
                        && n.hasNext()
                        && n.previous.elem
                        && n.next.elem
                        && isNumber((n.previous).elem)
                        &&
                        isNumber((n.next).elem)
                        && !(n.previous).elem.contains(".")
                        && n.next.elem != ".") {
                        var pre = n.previous;
                        var ne = n.next;
                        var s = pre.elem + "." + ne.elem;
                        var element = new Node({
                            startNodeStack: [],
                            generateLineCol: function () {
                                return {
                                    fileName: pre.getLineCol().fileName,
                                    line: pre.getLineCol().line,
                                    column: pre.getLineCol().column,
                                    useDefine: pre.getLineCol().useDefine
                                }
                            }
                        }, getTokenType(s, pre.getLineCol()), undefined, s);
                        element.setLineCol(pre.getLineCol());
                        element.getLineCol().length = element.elem.length;

                        element.setPrevious(pre.previous);
                        element.setNext(ne.next);

                        if (element.hasPrevious()) {
                            element.previous.setNext(element);
                        } else {
                            root.setLinkedNode(element);
                        }
                        if (element.hasNext()) {
                            element.next.setPrevious(element);
                        }
                    }
                }

                n = n.next;
            }
            n = root.getLinkedNode();
            while (n != null) {
                if (n.type == TYPE_ElementStartNode && n.hasNext()) {
                    var next = n.next;
                    var args = {
                        fileName: n.getLineCol().fileName,
                        previous: n,
                        currentLine: n.getLineCol().line,
                        currentCol: n.getLineCol().column,
                        useDefine: n.getLineCol().useDefine,
                        generateLineCol: function () {
                            return {
                                fileName: this.fileName,
                                line: this.currentLine,
                                column: this.currentCol,
                                useDefine: this.useDefine
                            }
                        }
                    };
                    var endingNode = new Node(args, TYPE_EndingNode);

                    endingNode.setNext(next);
                    next.setPrevious(endingNode);
                }
                n = n.next;
            }
        } else {
            if (root.hasPrevious()) {
                root.previous.setNext(root.next);
            }
            if (root.hasNext()) {
                root.next.setPrevious(root.previous);
            }
        }
    }
}