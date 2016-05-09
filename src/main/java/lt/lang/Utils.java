package lt.lang;

import lt.repl.Evaluator;
import lt.compiler.JarLoader;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * automatically import static this class
 */
@SuppressWarnings("unused")
public class Utils {
        private Utils() {
        }

        public static void println(Object o) {
                System.out.println(o);
        }

        public static Object eval(String e) throws Exception {
                Evaluator evaluator = new Evaluator(new JarLoader());
                return evaluator.eval(e).result;
        }

        public static Map<String, File> filesInDirectory(String dir) {
                return filesInDirectory(new File(dir), false);
        }

        public static Map<String, File> filesInDirectory(String dir, boolean recursively) {
                return filesInDirectory(new File(dir), recursively);
        }

        public static Map<String, File> filesInDirectory(File dir, boolean recursively) {
                if (dir == null) throw new NullPointerException("dir is null");
                Map<String, File> map = new LinkedHashMap<>();
                if (dir.isDirectory()) {
                        File[] listFiles = dir.listFiles();
                        if (listFiles != null) {
                                for (File f : listFiles) {
                                        if (f.isFile()) {
                                                if (f.getName().endsWith(".lt")) {
                                                        map.put(f.getName(), f);
                                                }
                                        } else if (f.isDirectory() && recursively) {
                                                Map<String, File> files = filesInDirectory(f, true);
                                                map.putAll(files);
                                        }
                                }
                        }
                } else throw new IllegalArgumentException(dir + " is not a directory");
                return map;
        }
}
