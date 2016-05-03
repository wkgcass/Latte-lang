package lt.lang;

/**
 * a wrapper containing required object
 */
public class Wrapper extends RuntimeException {
        public final Object object;

        public Wrapper(Object object) {
                this.object = object;
        }
}
