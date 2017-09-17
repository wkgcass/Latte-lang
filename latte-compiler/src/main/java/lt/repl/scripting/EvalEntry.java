package lt.repl.scripting;

/**
 * the entry containing variable name and it's corresponding object
 */
public class EvalEntry {
        public final String name;
        public final Object result;
        public final Class<?> type;

        public EvalEntry(String name, Object result, Class<?> type) {
                this.name = name;
                this.result = result;
                this.type = type;
        }
}
