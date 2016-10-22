package lt.compiler.syntactic.def;

import lt.compiler.LineCol;
import lt.compiler.syntactic.AST;
import lt.compiler.syntactic.Definition;
import lt.compiler.syntactic.Statement;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * defines an object
 */
public class ObjectDef implements Definition {
        public final String name;
        public final AST.Invocation superWithInvocation;
        public final List<AST.Access> superWithoutInvocation;
        public final Set<AST.Anno> annos;
        public final List<Statement> statements;

        private final LineCol lineCol;

        public ObjectDef(String name,
                         AST.Invocation superWithInvocation,
                         List<AST.Access> superWithoutInvocation,
                         Set<AST.Anno> annos,
                         List<Statement> statements,
                         LineCol lineCol) {
                this.name = name;
                this.superWithInvocation = superWithInvocation;
                this.superWithoutInvocation = superWithoutInvocation;
                this.annos = annos;
                this.statements = new ArrayList<>(statements);
                this.lineCol = lineCol;
        }

        @Override
        public LineCol line_col() {
                return lineCol;
        }

        @Override
        public String toString() {
                StringBuilder sb = new StringBuilder("object ").append(name);
                if (superWithInvocation != null || superWithoutInvocation.isEmpty()) {
                        sb.append(":");
                }
                if (superWithInvocation != null) {
                        sb.append(superWithInvocation);
                        if (!superWithoutInvocation.isEmpty()) {
                                sb.append(",");
                        }
                }
                if (superWithoutInvocation.isEmpty()) {
                        sb.append(superWithoutInvocation);
                }
                sb.append(statements);
                return sb.toString();
        }

        @Override
        public boolean equals(Object o) {
                if (this == o) return true;
                if (o == null || getClass() != o.getClass()) return false;

                ObjectDef objectDef = (ObjectDef) o;

                if (!name.equals(objectDef.name)) return false;
                if (superWithInvocation != null ? !superWithInvocation.equals(objectDef.superWithInvocation) : objectDef.superWithInvocation != null)
                        return false;
                if (!superWithoutInvocation.equals(objectDef.superWithoutInvocation)) return false;
                if (!annos.equals(objectDef.annos)) return false;
                //
                return statements.equals(objectDef.statements);

        }

        @Override
        public int hashCode() {
                int result = name.hashCode();
                result = 31 * result + (superWithInvocation != null ? superWithInvocation.hashCode() : 0);
                result = 31 * result + superWithoutInvocation.hashCode();
                result = 31 * result + annos.hashCode();
                result = 31 * result + statements.hashCode();
                return result;
        }
}
