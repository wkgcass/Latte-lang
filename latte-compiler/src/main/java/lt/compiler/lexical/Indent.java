package lt.compiler.lexical;

/**
 * indentation
 */
public class Indent {
        public static final int FLEX = -1;

        private int indent;

        public Indent(int indent) {
                this.indent = indent;
        }

        public int getIndent() {
                return indent;
        }

        public void setIndent(int indent) {
                this.indent = indent;
        }

        @Override
        public boolean equals(Object o) {
                if (this == o) return true;
                if (o == null || getClass() != o.getClass()) return false;

                Indent indent1 = (Indent) o;

                return indent == indent1.indent;
        }

        @Override
        public int hashCode() {
                return indent;
        }

        @Override
        public String toString() {
                return "Indent(" + indent + ")";
        }
}
