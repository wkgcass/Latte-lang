package lt.compiler.syntactic;

import lt.compiler.LineCol;
import lt.compiler.SyntaxException;

/**
 * duplicate variable name
 */
public class DuplicateVariableNameException extends SyntaxException {
        public DuplicateVariableNameException(String name, LineCol lineCol) {
                super("duplicate name " + name, lineCol);
        }
}
