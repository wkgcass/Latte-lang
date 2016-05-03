package lt.compiler.semantic.builtin;

import lt.compiler.semantic.ConstantValue;
import lt.compiler.semantic.PrimitiveValue;
import lt.compiler.semantic.STypeDef;

/**
 * int value
 */
public class IntValue implements PrimitiveValue, ConstantValue {
        private final int value;

        public IntValue(int value) {
                this.value = value;
        }

        @Override
        public STypeDef type() {
                return IntTypeDef.get();
        }

        @Override
        public boolean equals(Object o) {
                if (this == o) return true;
                if (o == null || getClass() != o.getClass()) return false;

                IntValue intValue = (IntValue) o;

                return value == intValue.value;
        }

        @Override
        public int hashCode() {
                return value;
        }

        @Override
        public byte[] getByte() {
                byte[] bytes = new byte[4];
                bytes[0] = (byte) (value & 0xff);
                bytes[1] = (byte) ((value & 0xff00) >> 8);
                bytes[2] = (byte) ((value & 0xff0000) >> 16);
                bytes[3] = (byte) ((value & 0xff000000) >> 24);
                return bytes;
        }

        @Override
        public String toString() {
                return Integer.toString(value);
        }

        public int getValue() {
                return value;
        }
}
