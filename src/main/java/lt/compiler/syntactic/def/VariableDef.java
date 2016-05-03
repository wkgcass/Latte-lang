package lt.compiler.syntactic.def;

import lt.compiler.LineCol;
import lt.compiler.syntactic.*;
import lt.compiler.syntactic.pre.Modifier;

import java.util.HashSet;
import java.util.Set;

/**
 * define a variable
 */
public class VariableDef implements Definition, Expression {
        private String name;
        private AST.Access type;
        private Expression init;
        private Set<Modifier> modifiers;
        private Set<AST.Anno> annos;

        private final LineCol lineCol;

        public VariableDef(String name, Set<Modifier> modifiers, Set<AST.Anno> annos, LineCol lineCol) {
                this.name = name;
                this.lineCol = lineCol;
                this.modifiers = new HashSet<>(modifiers);
                this.annos = new HashSet<>(annos);
        }

        public String getName() {
                return name;
        }

        public AST.Access getType() {
                return type;
        }

        public void setType(AST.Access type) {
                this.type = type;
        }

        public Expression getInit() {
                return init;
        }

        public void setInit(Expression init) {
                this.init = init;
        }

        public Set<Modifier> getModifiers() {
                return modifiers;
        }

        public Set<AST.Anno> getAnnos() {
                return annos;
        }

        @Override
        public String toString() {
                StringBuilder sb = new StringBuilder("VariableDef(");
                for (AST.Anno anno : annos) {
                        sb.append(anno).append(" ");
                }
                for (Modifier m : modifiers) {
                        sb.append(m).append(" ");
                }
                sb.append("(").append(name).append(")");
                if (type != null) {
                        sb.append(" : ").append(type);
                }
                if (init != null) {
                        sb.append(" = ").append(init);
                }
                sb.append(")");
                return sb.toString();
        }

        @Override
        public boolean equals(Object o) {
                if (this == o) return true;
                if (o == null || getClass() != o.getClass()) return false;

                VariableDef that = (VariableDef) o;

                if (!name.equals(that.name)) return false;
                if (type != null ? !type.equals(that.type) : that.type != null) return false;
                if (init != null ? !init.equals(that.init) : that.init != null) return false;
                if (!modifiers.equals(that.modifiers)) return false;
                return annos.equals(that.annos);
        }

        @Override
        public int hashCode() {
                int result = name.hashCode();
                result = 31 * result + (type != null ? type.hashCode() : 0);
                result = 31 * result + (init != null ? init.hashCode() : 0);
                result = 31 * result + modifiers.hashCode();
                result = 31 * result + annos.hashCode();
                return result;
        }

        @Override
        public LineCol line_col() {
                return lineCol;
        }
}
