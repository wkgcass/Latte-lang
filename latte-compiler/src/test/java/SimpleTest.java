import lt.compiler.*;
import lt.compiler.semantic.STypeDef;
import lt.compiler.syntactic.Statement;

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
public class SimpleTest {
        public static void main(String[] args) throws Exception {
                System.out.println(((int)(char)-123));
                /*
                ErrorManager err = new ErrorManager(true);

                StringBuilder sb = new StringBuilder();
                sb.append("" +
                        "import implicit X\n" +
                        "class hehe\n" +
                        "@Implicit\n" +
                        "class X(x:Integer)\n" +
                        "    def s = x + ' s'\n");

                lt.compiler.Scanner lexicalProcessor = new lt.compiler.IndentScanner("test.lt", new StringReader(sb.toString()), new Properties(), err);
                Parser syntacticProcessor = new Parser(lexicalProcessor.scan(), err);
                Map<String, List<Statement>> map = new HashMap<>();
                map.put("test.lt", syntacticProcessor.parse());
                SemanticProcessor semanticProcessor = new SemanticProcessor(map, Thread.currentThread().getContextClassLoader(), err);
                Set<STypeDef> types = semanticProcessor.parse();

                CodeGenerator codeGenerator = new CodeGenerator(types, semanticProcessor.getTypes());
                Map<String, byte[]> list = codeGenerator.generate();
                byte[] b = list.get("hehe");
                FileOutputStream fos = new FileOutputStream(new File("/Volumes/PROJECTS/openSource/LessTyping/hehe.class"));
                fos.write(b);
                fos.flush();
                fos.close();
                */
        }

        public static boolean test() {
                Object a = new Object();
                Object b = new Object();
                return a == b;
        }
}
