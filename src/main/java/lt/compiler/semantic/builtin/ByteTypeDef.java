package lt.compiler.semantic.builtin;

import lt.compiler.semantic.PrimitiveTypeDef;

/**
 * byte.class
 */
public class ByteTypeDef extends PrimitiveTypeDef {
        private ByteTypeDef() {
        }

        private static ByteTypeDef t = new ByteTypeDef();

        public static ByteTypeDef get() {
                return t;
        }

        @Override
        public String fullName() {
                return "byte";
        }
}
