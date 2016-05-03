package lt.compiler.syntactic.operation;

import lt.compiler.LineCol;
import lt.compiler.syntactic.Expression;
import lt.compiler.syntactic.Operation;

import java.util.Collections;
import java.util.List;

/**
 * one variable operation
 */
public class OneVariableOperation implements Operation {
        private String operator;
        private List<Expression> expressions;
        private final LineCol lineCol;

        public OneVariableOperation(String operator, Expression e1, LineCol lineCol) {
                this.operator = operator;
                this.lineCol = lineCol;
                expressions = Collections.singletonList(e1);
        }

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
                return false;
        }

        @Override
        public String toString() {
                return "(" + expressions.get(0) + " " + operator + ')';
        }

        @Override
        public boolean equals(Object o) {
                if (this == o) return true;
                if (o == null || getClass() != o.getClass()) return false;

                OneVariableOperation that = (OneVariableOperation) o;

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
