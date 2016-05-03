package lt.compiler.semantic.builtin;

import lt.compiler.semantic.PrimitiveValue;
import lt.compiler.semantic.STypeDef;

/**
 * byte value
 */
public class ByteValue implements PrimitiveValue {
        private final byte value;

        public ByteValue(byte value) {
                this.value = value;
        }

        @Override
        public STypeDef type() {
                return ByteTypeDef.get();
        }

        @Override
        public boolean equals(Object o) {
                if (this == o) return true;
                if (o == null || getClass() != o.getClass()) return false;

                ByteValue byteValue = (ByteValue) o;

                return value == byteValue.value;
        }

        @Override
        public int hashCode() {
                return (int) value;
        }

        @Override
        public String toString() {
                return Byte.toString(value);
        }

        public int getValue() {
                return value;
        }
}
