package lt.lang;

import lt.repl.Evaluator;
import lt.repl.JarLoader;

/**
 * automatically import static this class
 */
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
}
