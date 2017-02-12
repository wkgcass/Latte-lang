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

        public boolean eq(Object that) {
                return o.equals(that);
        }

        public boolean ne(Object that) {
                return !this.eq(that);
        }
}
