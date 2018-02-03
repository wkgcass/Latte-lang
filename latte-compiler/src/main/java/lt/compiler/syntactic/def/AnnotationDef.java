package lt.compiler.syntactic.def;

import lt.compiler.CompileUtil;
import lt.compiler.LineCol;
import lt.compiler.syntactic.AST;
import lt.compiler.syntactic.Statement;
import lt.lang.function.Function1;

import java.util.List;
import java.util.Set;

/**
 * annotation type def
 */
public class AnnotationDef implements Statement {
        public final String name;
        public final List<AST.Access> generics;
        public final Set<AST.Anno> annos;
        public final List<Statement> stmts;
        private final LineCol lineCol;

        public AnnotationDef(String name, List<AST.Access> generics, Set<AST.Anno> annos, List<Statement> stmts, LineCol lineCol) {
                this.name = name;
                this.generics = generics;
                this.annos = annos;
                this.stmts = stmts;
                this.lineCol = lineCol;
        }

        @Override
        public LineCol line_col() {
                return lineCol;
        }

        @Override
        public void foreachInnerStatements(Function1<Boolean, ? super Statement> f) throws Exception {
                CompileUtil.visitStmt(annos, f);
                CompileUtil.visitStmt(stmts, f);
        }

        @Override
        public boolean equals(Object o) {
                if (this == o) return true;
                if (o == null || getClass() != o.getClass()) return false;

                AnnotationDef that = (AnnotationDef) o;

                if (!name.equals(that.name)) return false;
                if (!generics.equals(that.generics)) return false;
                if (!annos.equals(that.annos)) return false;
                //
                return stmts.equals(that.stmts);
        }

        @Override
        public int hashCode() {
                int result = name.hashCode();
                result = 31 * result + generics.hashCode();
                result = 31 * result + annos.hashCode();
                result = 31 * result + stmts.hashCode();
                return result;
        }

        @Override
        public String toString() {
                StringBuilder sb = new StringBuilder();
                sb.append("((").append(annos).append(") annotation ").append(name);
                if (!generics.isEmpty()) {
                        sb.append("<:").append(generics).append(":>");
                }
                sb.append("( ").append(stmts).append(" ))");
                return sb.toString();
        }
}
