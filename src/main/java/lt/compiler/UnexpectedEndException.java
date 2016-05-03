package lt.compiler;

/**
 * unexpected end
 */
public class UnexpectedEndException extends SyntaxException {
        public UnexpectedEndException(LineCol lineCol) {
                super("unexpected end", lineCol);
        }
}
