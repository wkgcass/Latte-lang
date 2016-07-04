package lt.lang;

/**
 * runtime exception thrown by Latte-lang.
 */
public class LtRuntimeException extends RuntimeException {
        public LtRuntimeException() {
        }

        public LtRuntimeException(String message) {
                super(message);
        }

        public LtRuntimeException(String message, Throwable cause) {
                super(message, cause);
        }

        public LtRuntimeException(Throwable cause) {
                super(cause);
        }
}
