import lt.compiler.*;
import lt.compiler.semantic.STypeDef;
import lt.compiler.syntactic.Statement;

import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by wkgcass on 16/6/4.
 */
/*
public class SimpleTest {
        public static void main(String[] args) throws ScriptException {
                ScriptEngine engine = new ScriptEngineManager().getEngineByName("Latte-lang");
                engine.eval("a = ['key': 'value']");
                System.out.println(engine.get("a"));
        }
}
*/
public class SimpleTest {
        public static void main(String[] args) throws Exception {
                ErrorManager err = new ErrorManager(true);

                StringBuilder sb = new StringBuilder();
                sb.append("" +
                        "class A\n" +
                        " static\n" +
                        "  def method(args:[]String)\n" +
                        "   def method_try_catch_finally(func)\n" +
                        "    a=0\n" +
                        "    try\n" +
                        "        func.apply()\n" +
                        "        a=1\n" +
                        "    catch e\n" +
                        "        if e is type RuntimeException\n" +
                        "            a=2\n" +
                        "        elseif e is type Error or e is type Exception\n" +
                        "            a=3\n" +
                        "        elseif e is type Throwable\n" +
                        "            a=4\n" +
                        "    finally\n" +
                        "        ++a\n" +
                        "    return a"
                );

                lt.compiler.Scanner lexicalProcessor = new lt.compiler.IndentScanner("test.lt", new StringReader(sb.toString()), new Properties(), err);
                Parser syntacticProcessor = new Parser(lexicalProcessor.scan(), err);
                Map<String, List<Statement>> map = new HashMap<String, List<Statement>>();
                map.put("test.lt", syntacticProcessor.parse());
                SemanticProcessor semanticProcessor = new SemanticProcessor(map, Thread.currentThread().getContextClassLoader(), err);
                Set<STypeDef> types = semanticProcessor.parse();

                CodeGenerator codeGenerator = new CodeGenerator(types, semanticProcessor.getTypes());
                Map<String, byte[]> list = codeGenerator.generate();
                byte[] b = list.get("A");
                FileOutputStream fos = new FileOutputStream(new File("/Users/wkgcass/OpenSource/Latte-lang/hehe.class"));
                fos.write(b);
                fos.flush();
                fos.close();
        }

        public static boolean test() {
                Object a = new Object();
                Object b = new Object();
                return a == b;
        }
}
