package lt.compiler;

/**
 * exceptions during compile
 */
public class CompileException extends Exception {
        public CompileException() {
        }

        public CompileException(Throwable cause) {
                super(cause);
        }

        public CompileException(String message) {
                super(message);
        }

        public CompileException(String message, Throwable cause) {
                super(message, cause);
        }
}
