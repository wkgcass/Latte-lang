package lt.lang;

import java.util.HashMap;
import java.util.Map;

/**
 * value container.
 */
public class ValueContainer {
        private Map<String, Object> map = new HashMap<>();

        public void set(String s, Object o) {
                map.put(s, o);
        }

        public Object get(String s) {
                return map.get(s);
        }

        public boolean containsKey(String s) {
                return map.containsKey(s);
        }

        public void set(char c, Object o) {
                set(String.valueOf(c), o);
        }

        public Object get(char c) {
                return get(String.valueOf(c));
        }

        public boolean containsKey(char c) {
                return containsKey(String.valueOf(c));
        }
}
