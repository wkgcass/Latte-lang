package lt.lang.implicit;

import java.util.regex.Pattern;

/**
 * rich String
 */
public class RichString {
        private String s;

        public RichString(String s) {
                this.s = s;
        }

        public String add(Object o) {
                return s + o;
        }

        public Pattern r() {
                return Pattern.compile(s);
        }

        public Pattern r(int flags) {
                return Pattern.compile(s, flags);
        }
}
