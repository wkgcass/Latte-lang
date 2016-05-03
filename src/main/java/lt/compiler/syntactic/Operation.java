package lt.compiler.syntactic;

import java.util.List;

/**
 * operation + - * /
 */
public interface Operation extends Expression {
        /**
         * the operator string
         *
         * @return operator
         */
        String operator();

        /**
         * expressions to the operator
         *
         * @return an immutable list containing expressions
         */
        List<Expression> expressions();

        /**
         * the index of expression to invoke this operation on
         *
         * @return index of expression
         */
        int invokeOn();

        /**
         * the operator is an unary operator
         *
         * @return true/false
         */
        boolean isUnary();
}
