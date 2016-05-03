package lt.compiler.semantic.builtin;

import lt.compiler.semantic.PrimitiveTypeDef;
import lt.compiler.semantic.STypeDef;

/**
 * double.class
 */
public class DoubleTypeDef extends PrimitiveTypeDef {
        private DoubleTypeDef() {
        }

        private static DoubleTypeDef t = new DoubleTypeDef();

        public static DoubleTypeDef get() {
                return t;
        }

        @Override
        public String fullName() {
                return "double";
        }

        @Override
        public boolean isAssignableFrom(STypeDef cls) {
                if (super.isAssignableFrom(cls)) return true;
                // cls is float or long or assignable from float
                return cls instanceof FloatTypeDef || cls instanceof LongTypeDef || FloatTypeDef.get().isAssignableFrom(cls);
        }
}
