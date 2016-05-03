package lt.compiler;

/**
 * unexpected token
 */
public class UnexpectedTokenException extends SyntaxException {

        public UnexpectedTokenException(String expected, String got, LineCol lineCol) {
                super("expecting " + expected + ", but got " + got, lineCol);
        }

        public UnexpectedTokenException(String token, LineCol lineCol) {
                super("unexpected token " + token, lineCol);
        }
}
