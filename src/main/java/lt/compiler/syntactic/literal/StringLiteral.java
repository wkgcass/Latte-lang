package lt.compiler.syntactic.literal;

import lt.compiler.LineCol;
import lt.compiler.syntactic.Literal;

/**
 * string literal, "str", 'str'
 */
public class StringLiteral extends Literal {
        public StringLiteral(String literal, LineCol lineCol) {
                super(STRING, literal, lineCol);
        }
}
