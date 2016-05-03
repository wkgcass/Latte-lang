package lt.compiler.semantic.builtin;

import lt.compiler.semantic.PrimitiveTypeDef;
import lt.compiler.semantic.STypeDef;

/**
 * int.class
 */
public class IntTypeDef extends PrimitiveTypeDef {
        private IntTypeDef() {
        }

        private static IntTypeDef t = new IntTypeDef();

        public static IntTypeDef get() {
                return t;
        }

        @Override
        public String fullName() {
                return "int";
        }

        @Override
        public boolean isAssignableFrom(STypeDef cls) {
                if (super.isAssignableFrom(cls)) return true;
                // cls is char/byte/short
                return cls instanceof CharTypeDef || cls instanceof ByteTypeDef || cls instanceof ShortTypeDef;
        }
}
