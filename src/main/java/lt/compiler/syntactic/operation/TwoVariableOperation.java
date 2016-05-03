package lt.compiler.syntactic.operation;

import lt.compiler.LineCol;
import lt.compiler.syntactic.Expression;
import lt.compiler.syntactic.Operation;

import java.util.Arrays;
import java.util.List;

/**
 * two variable operation
 */
public class TwoVariableOperation implements Operation {
        private String operator;
        private List<Expression> expressions;
        private final LineCol lineCol;

        public TwoVariableOperation(String operator, Expression e1, Expression e2, LineCol lineCol) {
                this.operator = operator;
                this.lineCol = lineCol;
                expressions = Arrays.asList(e1, e2);
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
        public boolean equals(Object o) {
                if (this == o) return true;
                if (o == null || getClass() != o.getClass()) return false;

                TwoVariableOperation that = (TwoVariableOperation) o;

                return operator.equals(that.operator) && expressions.equals(that.expressions);
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
                return "(" + expressions.get(0) + " " + operator + " " + expressions.get(1) + ")";
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
