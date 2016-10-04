$(document).ready(function () {
    // create sidebar and attach to menu open
    $('.ui.sidebar').sidebar('attach events', '.toc.item');

    var app = angular.module('tutorial', []);
    app.controller('controller', ['$scope', '$sce', function ($scope, $sce) {
        var zh = useZh();

        $scope.navs = common_navs();
        $scope.navs[2].active = true;

        $scope.git_repo = common_git_repo();

        $scope.descr = zh ? "Latte是一种JVM语言, 它与Java有许多相似之处. 这里给出Latte与Java相同语义的比较. 详细语法请参考语言规范."
            : "Latte is a JVM language, it have many similarities with java. Here's some comparisons between Latte and Java. For detailed syntax please read the language specification.";

        function getLatteHtml(str) {
            return $sce.trustAsHtml(highlighting("", str, {}));
        }

        $scope.tutorials = [
            {
                title: "Hello World",
                java: "System.out.println(\"hello world\");",
                latte: getLatteHtml("println('hello world')"),
                note: zh ? $sce.trustAsHtml("Latte隐式导入 <code>lt.lang.Utils</code> 下的所有static方法.")
                    : $sce.trustAsHtml("Latte implicitly import all static methods of <code>lt.lang.Utils</code>.")
            },
            {
                title: zh ? "注释" : "Comments",
                java: "" +
                "// comment\n" +
                "/*\n" +
                "multiple line comment\n" +
                "*/",
                latte: getLatteHtml("" +
                    "; comment\n" +
                    "/*\n" +
                    "multiple line comment\n" +
                    "*/")
            },
            {
                title: zh ? "值" : "Values",
                java: "" +
                "int i = 1;\n" +
                "float f =  1.2\n" +
                "Object o = new Object()\n" +
                "int[] arr = {1,2,3}\n" +
                "\n" +
                "List list = new LinkedList();\n" +
                "list.add(1);  list.add(2);  list.add(3)\n" +
                "\n" +
                "Map map = new LinkedHashMap()\n" +
                "map.put(\"id\", 12);\n" +
                "map.put(\"name\", \"cass\");\n" +
                "map.put(\"age\", 22);",
                latte: getLatteHtml("" +
                    "i : int = 1\n" +
                    "f : float = 1.2\n" +
                    "o = Object()\n" +
                    "arr : []int = [1,2,3]\n" +
                    "\n" +
                    "list = [1,2,3]\n" +
                    "\n" +
                    "map = {\n" +
                    "    'id' : 12\n" +
                    "    'name' : 'cass'\n" +
                    "    'age' : 22\n" +
                    "}"),
                note: zh ? $sce.trustAsHtml("Latte是动态静态类型结合的. 对于没有声明类型的变量将视为 <code>java.lang.Object</code>")
                    : $sce.trustAsHtml("Latte is a hybrid of dynamic and static typing. The variables without type declarations are considered as <code>java.lang.Object</code>")
            },
            {
                title: zh ? "正则表达式" : "Regular Expression",
                java: "" +
                "Pattern pattern = Pattern.compile(\"\\\\d+\")",
                latte: getLatteHtml("" +
                    "var pattern = //\\d+//")
            },
            {
                title: zh ? "类型定义" : "Type Definition",
                java: "" +
                "public class User {\n" +
                "    private int id\n" +
                "    private String name\n" +
                "\n" +
                "    public User(int id, String name) {\n" +
                "        this.id=id;\n" +
                "        this.name=name;\n" +
                "    }\n" +
                "    public String toString() {\n" +
                "        return \"User(id=\"+id+\", name=\"+name+\")\";\n" +
                "    }\n" +
                "}",
                latte: getLatteHtml("" +
                    "class User(id : int, name : String)\n" +
                    "    toString():String='User(id='+id+', name='+name+')'\n" +
                    "\n" +
                    (zh ? "; 若不考虑与java的交互, 可以省略类型\n" : "; the types can be omitted\n") +
                    (zh ? "; 更偏向弱类型的写法\n" : "; the way looks more like weak typing language\n") +
                    "class User(id,name)\n" +
                    "\n" +
                    (zh ? "; 可以将参数通过换行隔开\n" : "; params can be separated with new line\n") +
                    "class User(\n" +
                    "    id : int\n" +
                    "    name : String\n" +
                    ")"),
                note: zh ? $sce.trustAsHtml("构造块中的参数与变量都将视为类的字段")
                    : $sce.trustAsHtml("parameters in constructing blocks are considered as fields")
            },
            {
                title: "Data Class",
                java: "" +
                "public class User {\n" +
                "    private int id;\n" +
                "    private String name;\n" +
                "    public User() {\n" +
                "    }\n" +
                "    public User(int id, String name) {\n" +
                "        this.id=id;\n" +
                "        this.name=name;\n" +
                "    }\n" +
                "    public boolean equals(Object o) {\n" +
                "        return o instanceof User\n" +
                "            && ((User)o).id==this.id\n" +
                "            && ((User)o).name.equals(this.name);\n" +
                "    }\n" +
                "    public int hashCode() {\n" +
                "        return id+name.hashCode();\n" +
                "    }\n" +
                "    public String toString() {\n" +
                "        return \"User(id=\"+id+\",name=\"+name+\")\";\n" +
                "    }\n" +
                "    public int getId() {\n" +
                "        return id;\n" +
                "    }\n" +
                "    public String getName() {\n" +
                "        return name;\n" +
                "    }\n" +
                "    public void setId(int id) {\n" +
                "        this.id=id;\n" +
                "    }\n" +
                "    public void setName(String name) {\n" +
                "        this.name=name;\n" +
                "    }\n" +
                "}",
                latte: getLatteHtml("" +
                    "data class User(id:int, name:String)")
            },
            {
                title: zh ? "`fun` 语法" : "`fun` Syntax",
                java: "" +
                "public class printElem implements java.util.function.Consumer {\n" +
                "    public void accept(Object o) {\n" +
                "        System.out.println(o);\n" +
                "    }\n" +
                "}",
                latte: getLatteHtml("" +
                    "fun printElem(o)\n" +
                    "    println(o)")
            },
            {
                title: zh ? "运算符重载" : "Operator Binding",
                java: "" +
                "BigInteger a = new BigInteger(\"16\");\n" +
                "BigInteger b = new BigInteger(\"3\");\n" +
                "BigInteger addRes = a.add(b);\n" +
                "BigInteger subRes = a.subtract(b);\n" +
                "\n" +
                "List list = new LinkedList();\n" +
                "list.add(1);",
                latte: getLatteHtml("" +
                    "a = BigInteger(\"16\")\n" +
                    "b = BigInteger(\"3\")\n" +
                    "addRes = a + b\n" +
                    "subRes = a - b\n" +
                    "\n" +
                    "list = []\n" +
                    "list + 1")
            },
            {
                title: "DSL",
                java: "" +
                "StringBuilder sb = new StringBuilder();\n" +
                "sb.append(\"Hello \").append(\"world\").toString();\n" +
                "\n" +
                "Map map = new LinkedHashMap()\n" +
                "map.put(\"cass\", 22);",
                latte: getLatteHtml("" +
                    "sb = StringBuilder()\n" +
                    "sb append 'Hello ' append 'world' toString\n" +
                    "\n" +
                    "map = {}\n" +
                    "map put 'cass', 22")
            },
            {
                title: zh ? "条件语句" : "Condition Statements",
                java: "" +
                "if(list != null) {\n" +
                "    a = list\n" +
                "} else {\n" +
                "    a = new LinkedList();\n" +
                "    map.put('key', a);\n" +
                "}",
                latte: getLatteHtml("" +
                    "if list\n" +
                    "    a = list\n" +
                    "else\n" +
                    "    a = []\n" +
                    "    map['key']=a"),
                note: zh ? $sce.trustAsHtml("<code>null, undefined, 0</code> 都将转化为 <code>false</code>")
                    : $sce.trustAsHtml("<code>null, undefined, 0</code> are converted into <code>false</code>")
            },
            {
                title: "For Loop",
                java: "" +
                "for(User u : userList) {\n" +
                "    System.out.println(u);\n" +
                "}\n" +
                "\n" +
                "for(int i=0;i<arr.length;++i) {\n" +
                "    System.out.println(\"the \"+i+\"th element is \"+arr[i]);" +
                "}",
                latte: getLatteHtml("" +
                    "for u in userList\n" +
                    "    println(u)\n" +
                    "\n" +
                    "for i in 0.:arr.length\n" +
                    "    println(\"the ${i}th element is ${arr[i]}\")"),
                note: zh ? $sce.trustAsHtml("for语句可以接受 <code>Iterable, Iterator, Enumeration, 数组, Map</code> 作为循环依据")
                    : $sce.trustAsHtml("for statement can accept <code>Iterable, Iterator, Enumeration, Arrays, Map</code>")
            },
            {
                title: "While/Do-While Loop",
                java: "" +
                "while(line != null) {\n" +
                "    if(line.equals(\"\") {\n" +
                "        // do something\n" +
                "    }\n" +
                "    line = reader.readLine()\n" +
                "}\n" +
                "\n" +
                "do {\n" +
                "    // do something\n" +
                "} while(boolValue)",
                latte: getLatteHtml("" +
                    "while line\n" +
                    "    if line == ''\n" +
                    "        ...\n" +
                    "    line = reader readLine\n" +
                    "\n" +
                    "do\n" +
                    "    ...\n" +
                    "while boolValue"),
                note: zh ? $sce.trustAsHtml("<code>null, undefined, 0</code> 都将转化为 <code>false</code>")
                    : $sce.trustAsHtml("<code>null, undefined, 0</code> are converted into <code>false</code>")
            },
            {
                title: "Try-Catch-Finally",
                java: "" +
                "try {\n" +
                "    stream.read();\n" +
                "} catch(IOException e) {\n" +
                "    LOGGER.info(e.getMessage());\n" +
                "} catch(RuntimeException e) {\n" +
                "    LOGGER.debug(e.getMessage());\n" +
                "} finally {\n" +
                "    stream.close();\n" +
                "}",
                latte: getLatteHtml("" +
                    "try\n" +
                    "    stream read\n" +
                    "catch e\n" +
                    "    if e is type IOException\n" +
                    "        LOGGER.info(e.getMessage())\n" +
                    "    elseif e is type RuntimeException\n" +
                    "        LOGGER.debug(e.getMessage())\n" +
                    "    else\n" +
                    "        throw e\n" +
                    "finally\n" +
                    "    stream close\n" +
                    "\n" +
                    "throw 'a string'"),
                note: zh ? $sce.trustAsHtml("Latte允许throw与catch任何类型, 例如 throw \"error message\", 所以并不直接提供java的catch Type")
                    : $sce.trustAsHtml("Latte allows throwing and catching any type, e.g. throw \"error message\". As a result, Latte doesn't provide `catch Type`")
            },
            {
                title: "Synchronized",
                java: "" +
                "synchronized(a) {\n" +
                "    synchronized(b) {\n" +
                "        // do something\n" +
                "    }\n" +
                "}",
                latte: getLatteHtml("" +
                    "synchronized(a,b)\n" +
                    "    ...")
            },
            {
                title: "Lambda",
                java: "" +
                "list.stream().map(e->e.toString())\n" +
                ".collect(Collectors.toList());\n" +
                "\n" +
                "list.forEach(e->{\n" +
                "    if(e<10){\n" +
                "        System.out.println(e);\n" +
                "    }\n" +
                "});",
                latte: getLatteHtml("" +
                    "list.stream().map(\n" +
                    "    (e)->e.toString\n" +
                    ").collect(Collectors.toList())\n" +
                    "\n" +
                    "list.forEach(\n" +
                    "    (e)->\n" +
                    "        if e < 10\n" +
                    "            println(e)\n" +
                    ")")
            }
        ];
        $scope.printScriptForJava = function (index, java) {
            var lineCount = java.split(/\n|\r/g).length;
            return "var editor = CodeMirror.fromTextArea(document.getElementById('java_" + index + "'));editor.setSize('auto', '" + (lineCount * 20 + 30) + "');";
        }
    }]);
});