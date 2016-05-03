package lt.compiler.syntactic.operation;

import lt.compiler.LineCol;
import lt.compiler.syntactic.Expression;
import lt.compiler.syntactic.Operation;

import java.util.Collections;
import java.util.List;

/**
 *
 */
public class UnaryOneVariableOperation implements Operation {
        public UnaryOneVariableOperation(String operator, Expression e1, LineCol lineCol) {
                this.operator = operator;
                this.lineCol = lineCol;
                this.expressions = Collections.singletonList(e1);
        }

        private String operator;
        private List<Expression> expressions;
        private final LineCol lineCol;

        @Override
        public String operator() {
                return operator;
        }

        @Override
        public List<Expression> expressions() {
                return expressions;
        }

        @Override
        public int invokeOn() {
                return 0;
        }

        @Override
        public boolean isUnary() {
                return true;
        }

        @Override
        public String toString() {
                return "(" + operator() + " " + expressions().get(0) + ")";
        }

        @Override
        public boolean equals(Object o) {
                if (this == o) return true;
                if (o == null || getClass() != o.getClass()) return false;

                UnaryOneVariableOperation that = (UnaryOneVariableOperation) o;

                return !(operator != null ? !operator.equals(that.operator) : that.operator != null) && !(expressions != null ? !expressions.equals(that.expressions) : that.expressions != null);
        }

        @Override
        public int hashCode() {
                int result = operator != null ? operator.hashCode() : 0;
                result = 31 * result + (expressions != null ? expressions.hashCode() : 0);
                return result;
        }

        @Override
        public LineCol line_col() {
                return lineCol;
        }
}
