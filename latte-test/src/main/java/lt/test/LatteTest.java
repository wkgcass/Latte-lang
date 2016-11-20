package lt.test;

import lt.lang.Utils;
import lt.repl.Compiler;

import java.util.Map;

import java.io.File;
import java.util.HashMap;
import java.util.regex.Pattern;

/**
 * latte test util
 */
public class LatteTest {
        public static ClassLoader compileIfNotExist(String expectClassName, File... dirs) throws Exception {
                try {
                        Class.forName(expectClassName);
                        return Thread.currentThread().getContextClassLoader();
                } catch (ClassNotFoundException e) {
                        Compiler compiler = new Compiler(Thread.currentThread().getContextClassLoader());
                        Map<String, File> compileList = new HashMap<>();
                        for (File dir : dirs) {
                                compileList.putAll(Utils.filesInDirectory(dir, Pattern.compile(".*\\.lt"), true));
                        }
                        return compiler.compile(compileList);
                }
        }
}
