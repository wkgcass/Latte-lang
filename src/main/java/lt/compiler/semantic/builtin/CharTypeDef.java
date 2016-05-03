package lt.compiler.semantic.builtin;

import lt.compiler.semantic.PrimitiveTypeDef;

/**
 * char.class
 */
public class CharTypeDef extends PrimitiveTypeDef {
        private CharTypeDef() {
        }

        private static CharTypeDef t = new CharTypeDef();

        public static CharTypeDef get() {
                return t;
        }

        @Override
        public String fullName() {
                return "char";
        }
}
