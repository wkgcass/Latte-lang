package lt.compiler.semantic.builtin;

import lt.compiler.semantic.PrimitiveTypeDef;

/**
 * boolean.class
 */
public class BoolTypeDef extends PrimitiveTypeDef {
        private BoolTypeDef() {
        }

        private static BoolTypeDef t = new BoolTypeDef();

        public static BoolTypeDef get() {
                return t;
        }

        @Override
        public String fullName() {
                return "boolean";
        }

        @Override
        public String toString() {
                return fullName();
        }
}
