package lt.lang.implicit;

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
}
