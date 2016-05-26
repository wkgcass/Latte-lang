import lt.compiler.*;
import lt.compiler.syntactic.Statement;
import lt.repl.ClassPathLoader;
import lt.repl.Evaluator;
import lt.repl.REPL;

import java.io.File;
import java.io.FileOutputStream;
import java.io.StringReader;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 */
public class SimpleTest {
        public static void main(String[] args) throws Exception {
                /*
                Scanner scanner = new Scanner("test.lt", new StringReader("" +
                        "class TestForTryBreak\n" +
                        "    static\n" +
                        "        method()\n" +
                        "            n=0\n" +
                        "            for i in 1..10\n" +
                        "                try\n" +
                        "                    if i==3\n" +
                        "                        break\n" +
                        "                    n+=i\n" +
                        "                finally\n" +
                        "                    ++n\n" +
                        "            <n"), new Scanner.Properties(), new ErrorManager(true));
                Parser parser = new Parser(scanner.scan(), new ErrorManager(true));
                Map<String, List<Statement>> map = new HashMap<>();
                map.put("test.lt", parser.parse());
                SemanticProcessor processor = new SemanticProcessor(map, Thread.currentThread().getContextClassLoader());
                CodeGenerator codeGen = new CodeGenerator(processor.parse());
                byte[] bs = codeGen.generate().get("TestForTryBreak");

                File f = new File("/Volumes/PROJECTS/openSource/LessTyping/target/TestTryCatch.class");
                FileOutputStream fos = new FileOutputStream(f);
                fos.write(bs);
                fos.close();
                */
                /*
                ClassPathLoader cpl = new ClassPathLoader(ClassLoader.getSystemClassLoader());
                Evaluator evaluator = new Evaluator(cpl);
                cpl.load(new URL(new File("/Volumes/PROJECTS/test/LessTypingOut").toURI().toString()));
                System.out.println(evaluator.eval("lt::demo::test::User(1,'cass')").result);
                */
                REPL.main(new String[]{
                        "/Volumes/PROJECTS/Test/LessTyping/hehe.lts"
                });
        }

        public void test() {
                for (int i = 0; i < 10; ++i) {
                        try {
                                if (i == 7)
                                        continue;
                                else if (i == 8) break;
                                i += 3;
                        } finally {
                                i += 1;
                        }
                }
        }
}
