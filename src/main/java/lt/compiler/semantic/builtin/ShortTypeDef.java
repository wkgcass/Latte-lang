package lt.compiler.semantic.builtin;

import lt.compiler.semantic.PrimitiveTypeDef;

/**
 * short.class
 */
public class ShortTypeDef extends PrimitiveTypeDef {
        private ShortTypeDef() {
        }

        private static ShortTypeDef t = new ShortTypeDef();

        public static ShortTypeDef get() {
                return t;
        }

        @Override
        public String fullName() {
                return "short";
        }
}
