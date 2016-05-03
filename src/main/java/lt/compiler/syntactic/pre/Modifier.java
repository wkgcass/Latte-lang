package lt.compiler.syntactic.pre;

import lt.compiler.LineCol;
import lt.compiler.syntactic.Pre;

/**
 * modifier
 */
public class Modifier implements Pre {
        public final String modifier;
        private final LineCol lineCol;

        public Modifier(String modifier, LineCol lineCol) {
                this.modifier = modifier;
                this.lineCol = lineCol;
        }

        @Override
        public boolean equals(Object o) {
                if (this == o) return true;
                if (o == null || getClass() != o.getClass()) return false;

                Modifier modifier1 = (Modifier) o;

                return !(modifier != null ? !modifier.equals(modifier1.modifier) : modifier1.modifier != null);
        }

        @Override
        public int hashCode() {
                return modifier != null ? modifier.hashCode() : 0;
        }

        @Override
        public String toString() {
                return "(" + modifier + ")";
        }

        @Override
        public LineCol line_col() {
                return lineCol;
        }
}
