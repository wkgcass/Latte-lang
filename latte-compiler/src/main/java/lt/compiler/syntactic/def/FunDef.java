package lt.compiler.syntactic.def;

import lt.compiler.CompileUtil;
import lt.compiler.LineCol;
import lt.compiler.syntactic.AST;
import lt.compiler.syntactic.Definition;
import lt.compiler.syntactic.Statement;
import lt.compiler.syntactic.pre.Modifier;
import lt.lang.function.Function1;

import java.util.List;
import java.util.Set;

/**
 * function definitions.
 */
public class FunDef implements Definition {
        public final String name;
        public final List<VariableDef> params;
        public final AST.Access superType;
        public final Set<AST.Anno> annos;
        public final List<Statement> statements;

        private final LineCol lineCol;

        public FunDef(String name, List<VariableDef> params, AST.Access superType, Set<AST.Anno> annos, List<Statement> statements, LineCol lineCol) {
                this.name = name;
                this.params = params;
                this.superType = superType;
                this.annos = annos;
                this.statements = statements;
                this.lineCol = lineCol;
        }

        @Override
        public LineCol line_col() {
                return lineCol;
        }

        @Override
        public void foreachInnerStatements(Function1<Boolean, ? super Statement> f) throws Exception {
                CompileUtil.visitStmt(params, f);
                CompileUtil.visitStmt(superType, f);
                CompileUtil.visitStmt(annos, f);
                CompileUtil.visitStmt(statements, f);
        }

        @Override
        public boolean equals(Object o) {
                if (this == o) return true;
                if (o == null || getClass() != o.getClass()) return false;

                FunDef funDef = (FunDef) o;

                if (name != null ? !name.equals(funDef.name) : funDef.name != null) return false;
                if (params != null ? !params.equals(funDef.params) : funDef.params != null) return false;
                if (superType != null ? !superType.equals(funDef.superType) : funDef.superType != null) return false;
                if (annos != null ? !annos.equals(funDef.annos) : funDef.annos != null) return false;
                //
                return statements != null ? statements.equals(funDef.statements) : funDef.statements == null;
        }

        @Override
        public int hashCode() {
                int result = name != null ? name.hashCode() : 0;
                result = 31 * result + (params != null ? params.hashCode() : 0);
                result = 31 * result + (superType != null ? superType.hashCode() : 0);
                result = 31 * result + (annos != null ? annos.hashCode() : 0);
                result = 31 * result + (statements != null ? statements.hashCode() : 0);
                return result;
        }

        @Override
        public String toString() {
                StringBuilder sb = new StringBuilder("(");
                for (AST.Anno anno : annos) {
                        sb.append(anno).append(" ");
                }
                sb.append("fun ").append(name).append("(");
                boolean isFirst = true;
                for (VariableDef v : params) {
                        if (isFirst) {
                                isFirst = false;
                        } else {
                                sb.append(",");
                        }
                        sb.append(v);
                }
                sb.append(") : ").append(superType).append(" ").append(statements).append(")");
                return sb.toString();
        }
}
