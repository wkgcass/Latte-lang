package lt.compiler.semantic.builtin;

import lt.compiler.semantic.PrimitiveTypeDef;
import lt.compiler.semantic.STypeDef;

/**
 * float.class
 */
public class FloatTypeDef extends PrimitiveTypeDef {
        private FloatTypeDef() {
        }

        private static FloatTypeDef t = new FloatTypeDef();

        public static FloatTypeDef get() {
                return t;
        }

        @Override
        public String fullName() {
                return "float";
        }

        @Override
        public boolean isAssignableFrom(STypeDef cls) {
                if (super.isAssignableFrom(cls)) return true;
                // cls is int or is assignable from int
                return cls instanceof IntTypeDef || IntTypeDef.get().isAssignableFrom(cls);
        }
}
