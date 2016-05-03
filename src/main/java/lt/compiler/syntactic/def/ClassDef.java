package lt.compiler.syntactic.def;

import lt.compiler.LineCol;
import lt.compiler.syntactic.*;
import lt.compiler.syntactic.pre.Modifier;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * class definition
 */
public class ClassDef implements Definition {
        public final String name;
        public final Set<Modifier> modifiers;
        public final List<VariableDef> params;
        public final AST.Invocation superWithInvocation;
        public final List<AST.Access> superWithoutInvocation;
        public final Set<AST.Anno> annos;
        public final List<Statement> statements;

        private final LineCol lineCol;

        public ClassDef(String name, Set<Modifier> modifiers, List<VariableDef> params, AST.Invocation superWithInvocation, List<AST.Access> superWithoutInvocation, Set<AST.Anno> annos, List<Statement> statements, LineCol lineCol) {
                this.name = name;
                this.lineCol = lineCol;
                this.modifiers = new HashSet<>(modifiers);
                this.params = params;
                this.superWithInvocation = superWithInvocation;
                this.superWithoutInvocation = superWithoutInvocation;
                this.annos = new HashSet<>(annos);
                this.statements = statements;
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
                sb.append("class ").append(name).append("(");
                boolean isFirst = true;
                for (VariableDef v : params) {
                        if (isFirst) {
                                isFirst = false;
                        } else {
                                sb.append(",");
                        }
                        sb.append(v);
                }
                sb.append(")");
                if (superWithInvocation != null || !superWithoutInvocation.isEmpty()) {
                        sb.append(" : ");
                }

                isFirst = true;
                if (superWithInvocation != null) {
                        sb.append(superWithInvocation);
                        isFirst = false;
                }
                for (AST.Access a : superWithoutInvocation) {
                        if (isFirst) {
                                isFirst = false;
                        } else {
                                sb.append(",");
                        }
                        sb.append(a);
                }

                sb.append(" ").append(statements).append(")");
                return sb.toString();
        }

        @Override
        public boolean equals(Object o) {
                if (this == o) return true;
                if (o == null || getClass() != o.getClass()) return false;

                ClassDef classDef = (ClassDef) o;

                if (!name.equals(classDef.name)) return false;
                if (!modifiers.equals(classDef.modifiers)) return false;
                if (!params.equals(classDef.params)) return false;
                if (superWithInvocation != null ? !superWithInvocation.equals(classDef.superWithInvocation) : classDef.superWithInvocation != null)
                        return false;
                if (!superWithoutInvocation.equals(classDef.superWithoutInvocation)) return false;
                if (!annos.equals(classDef.annos)) return false;
                return statements.equals(classDef.statements);
        }

        @Override
        public int hashCode() {
                int result = name.hashCode();
                result = 31 * result + modifiers.hashCode();
                result = 31 * result + params.hashCode();
                result = 31 * result + (superWithInvocation != null ? superWithInvocation.hashCode() : 0);
                result = 31 * result + superWithoutInvocation.hashCode();
                result = 31 * result + annos.hashCode();
                result = 31 * result + statements.hashCode();
                return result;
        }

        @Override
        public LineCol line_col() {
                return lineCol;
        }
}
