package lt.lang.implicit;

import lt.lang.Implicit;

/**
 * rich String
 */
@Implicit
public class RichString {
        private String s;

        public RichString(String s) {
                this.s = s;
        }

        public String add(Object o) {
                return s + o;
        }
}
