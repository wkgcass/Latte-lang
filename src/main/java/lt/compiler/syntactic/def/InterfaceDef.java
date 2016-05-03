package lt.compiler.syntactic.def;

import lt.compiler.LineCol;
import lt.compiler.syntactic.*;
import lt.compiler.syntactic.pre.Modifier;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * interface definition
 */
public class InterfaceDef implements Definition {
        public final String name;
        public final Set<Modifier> modifiers;
        public final List<AST.Access> superInterfaces;
        public final List<Statement> statements;
        public final Set<AST.Anno> annos;

        private final LineCol lineCol;

        public InterfaceDef(String name, Set<Modifier> modifiers, List<AST.Access> superInterfaces, Set<AST.Anno> annos, List<Statement> statements, LineCol lineCol) {
                this.name = name;
                this.lineCol = lineCol;
                this.modifiers = new HashSet<>(modifiers);
                this.superInterfaces = superInterfaces;
                this.statements = statements;
                this.annos = new HashSet<>(annos);
        }

        @Override
        public String toString() {
                StringBuilder sb = new StringBuilder("(");
                for (AST.Anno anno : annos) {
                        sb.append(anno).append(" ");
                }
                for (Modifier m : modifiers) {
                        sb.append(m).append(" ");
                }
                sb.append("interface ").append(name);
                if (!superInterfaces.isEmpty()) {
                        sb.append(" : ");
                        boolean isFirst = true;
                        for (AST.Access access : superInterfaces) {
                                if (isFirst) {
                                        isFirst = false;
                                } else {
                                        sb.append(",");
                                }
                                sb.append(access);
                        }
                }
                sb.append(" ").append(statements).append(")");
                return sb.toString();
        }

        @Override
        public boolean equals(Object o) {
                if (this == o) return true;
                if (o == null || getClass() != o.getClass()) return false;

                InterfaceDef that = (InterfaceDef) o;

                if (!name.equals(that.name)) return false;
                if (!modifiers.equals(that.modifiers)) return false;
                if (!superInterfaces.equals(that.superInterfaces)) return false;
                if (!statements.equals(that.statements)) return false;
                return annos.equals(that.annos);
        }

        @Override
        public int hashCode() {
                int result = name.hashCode();
                result = 31 * result + modifiers.hashCode();
                result = 31 * result + superInterfaces.hashCode();
                result = 31 * result + statements.hashCode();
                result = 31 * result + annos.hashCode();
                return result;
        }

        @Override
        public LineCol line_col() {
                return lineCol;
        }
}
