package lt.lang.implicit;

import lt.lang.Implicit;

/**
 * rich Object
 */
@Implicit
public class RichObject {
        private Object o;

        public RichObject(Object o) {
                this.o = o;
        }

        public String add(String s) {
                return o + s;
        }
}
