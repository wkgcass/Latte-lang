package lt.compiler.syntactic.def;

import lt.compiler.LineCol;
import lt.compiler.syntactic.AST;
import lt.compiler.syntactic.Statement;

import java.util.List;
import java.util.Set;

/**
 * annotation type def
 */
public class AnnotationDef implements Statement {
        public final String name;
        public final Set<AST.Anno> annos;
        public final List<Statement> stmts;
        private final LineCol lineCol;

        public AnnotationDef(String name, Set<AST.Anno> annos, List<Statement> stmts, LineCol lineCol) {
                this.name = name;
                this.annos = annos;
                this.stmts = stmts;
                this.lineCol = lineCol;
        }

        @Override
        public LineCol line_col() {
                return lineCol;
        }

        @Override
        public boolean equals(Object o) {
                if (this == o) return true;
                if (o == null || getClass() != o.getClass()) return false;

                AnnotationDef that = (AnnotationDef) o;

                if (!name.equals(that.name)) return false;
                if (!annos.equals(that.annos)) return false;
                //
                return stmts.equals(that.stmts);
        }

        @Override
        public int hashCode() {
                int result = name.hashCode();
                result = 31 * result + annos.hashCode();
                result = 31 * result + stmts.hashCode();
                return result;
        }

        @Override
        public String toString() {
                return "((" + annos + ") annotation " + name + "( " + stmts + " ))";
        }
}
