package lt.compiler.semantic.builtin;

import lt.compiler.semantic.PrimitiveValue;
import lt.compiler.semantic.STypeDef;

/**
 * short value
 */
public class ShortValue implements PrimitiveValue {
        private final short value;

        public ShortValue(short value) {
                this.value = value;
        }

        @Override
        public STypeDef type() {
                return ShortTypeDef.get();
        }

        @Override
        public boolean equals(Object o) {
                if (this == o) return true;
                if (o == null || getClass() != o.getClass()) return false;

                ShortValue that = (ShortValue) o;

                return value == that.value;
        }

        @Override
        public int hashCode() {
                return (int) value;
        }

        @Override
        public String toString() {
                return Short.toString(value);
        }

        public int getValue() {
                return value;
        }
}
