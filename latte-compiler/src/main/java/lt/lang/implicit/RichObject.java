package lt.lang.implicit;

/**
 * rich Object
 */
public class RichObject {
        private Object o;

        public RichObject(Object o) {
                this.o = o;
        }

        public String add(String s) {
                return o + s;
        }
}
