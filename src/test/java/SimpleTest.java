import lt.compiler.*;
import lt.compiler.syntactic.Statement;

import java.io.File;
import java.io.FileOutputStream;
import java.io.StringReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 */
public class SimpleTest {
        public static void main(String[] args) throws Exception {
                Scanner scanner = new Scanner("test.lt", new StringReader("" +
                        "class TestTryCatch\n" +
                        "    static\n" +
                        "        method(func)\n" +
                        "            try\n" +
                        "                func.apply()\n" +
                        "            catch e\n" +
                        "                if e type NullPointerException or e type ClassCastException\n" +
                        "                    <1\n" +
                        "                elseif e type Error\n" +
                        "                    <e.getMessage()\n" +
                        "                elseif e type Throwable\n" +
                        "                    <3\n" +
                        "            <4"), new Scanner.Properties(), new ErrorManager(true));
                Parser parser = new Parser(scanner.scan(), new ErrorManager(true));
                Map<String, List<Statement>> map = new HashMap<>();
                map.put("test.lt", parser.parse());
                SemanticProcessor processor = new SemanticProcessor(map, Thread.currentThread().getContextClassLoader());
                CodeGenerator codeGen = new CodeGenerator(processor.parse());
                byte[] bs = codeGen.generate().get("TestTryCatch");

                File f = new File("/Volumes/PROJECTS/openSource/LessTyping/target/TestTryCatch.class");
                FileOutputStream fos = new FileOutputStream(f);
                fos.write(bs);
                fos.close();
        }

        public Object test() {
                try {
                        Integer.parseInt("1");
                } catch (Throwable e) {
                        if (e instanceof NullPointerException || e instanceof ClassCastException) {
                                return 1;
                        } else if (e instanceof Error) {
                                return e.getMessage();
                        } else if (e instanceof Throwable) {
                                return 3;
                        }
                } finally {
                        Integer.parseInt("2");
                }
                return 4;
        }
}
