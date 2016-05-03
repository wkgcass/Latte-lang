package lt.compiler.semantic.builtin;

import lt.compiler.semantic.PrimitiveTypeDef;
import lt.compiler.semantic.STypeDef;

/**
 * long.class
 */
public class LongTypeDef extends PrimitiveTypeDef {
        private LongTypeDef() {
        }

        private static LongTypeDef t = new LongTypeDef();

        public static LongTypeDef get() {
                return t;
        }

        @Override
        public String fullName() {
                return "long";
        }

        @Override
        public boolean isAssignableFrom(STypeDef cls) {
                if (super.isAssignableFrom(cls)) return true;
                // cls is int or int is assignable from cls
                return cls instanceof IntTypeDef || IntTypeDef.get().isAssignableFrom(cls);
        }
}
