package lt.compiler.syntactic;

import lt.compiler.LineCol;

/**
 * literal "string", 'string', 1, 2.3, true/false/yes/no
 */
public abstract class Literal implements Expression {
        public static final int NUMBER = 0;
        public static final int STRING = 1;
        public static final int BOOL = 2;

        private int type;
        private String literal;

        private final LineCol lineCol;

        public Literal(int type, String literal, LineCol lineCol) {
                this.type = type;
                this.literal = literal;
                this.lineCol = lineCol;
        }

        public int type() {
                return type;
        }

        public String literal() {
                return literal;
        }

        @Override
        public String toString() {
                return literal;
        }

        @Override
        public boolean equals(Object o) {
                if (this == o) return true;
                if (o == null || getClass() != o.getClass()) return false;

                Literal literal1 = (Literal) o;

                return type == literal1.type && !(literal != null ? !literal.equals(literal1.literal) : literal1.literal != null);
        }

        @Override
        public int hashCode() {
                int result = type;
                result = 31 * result + (literal != null ? literal.hashCode() : 0);
                return result;
        }

        @Override
        public LineCol line_col() {
                return lineCol;
        }
}
