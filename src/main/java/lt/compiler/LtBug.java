package lt.compiler;

/**
 * bug for less typing
 */
public class LtBug extends Error {
        public LtBug(String msg) {
                super(msg);
        }

        public LtBug(Throwable t) {
                super(t);
        }
}
