package lt.compiler.semantic.builtin;

import lt.compiler.semantic.STypeDef;
import lt.compiler.semantic.Value;

/**
 * null value
 */
public class NullValue implements Value {
        private NullValue() {
        }

        private static NullValue nullValue = new NullValue();

        public static NullValue get() {
                return nullValue;
        }

        @Override
        public STypeDef type() {
                return NullTypeDef.get();
        }

        @Override
        public String toString() {
                return "null";
        }
}
