package lt.compiler.semantic.builtin;

import lt.compiler.semantic.ConstantValue;
import lt.compiler.semantic.PrimitiveValue;
import lt.compiler.semantic.STypeDef;

/**
 * long value
 */
public class LongValue implements PrimitiveValue, ConstantValue {
        private final long value;

        public LongValue(long value) {
                this.value = value;
        }

        @Override
        public STypeDef type() {
                return LongTypeDef.get();
        }

        @Override
        public boolean equals(Object o) {
                if (this == o) return true;
                if (o == null || getClass() != o.getClass()) return false;

                LongValue longValue = (LongValue) o;

                return value == longValue.value;
        }

        @Override
        public int hashCode() {
                return (int) (value ^ (value >>> 32));
        }

        @Override
        public byte[] getByte() {
                byte[] bytes = new byte[8];
                bytes[0] = (byte) (value & 0xff);
                bytes[1] = (byte) ((value >> 8) & 0xff);
                bytes[2] = (byte) ((value >> 16) & 0xff);
                bytes[3] = (byte) ((value >> 24) & 0xff);
                bytes[4] = (byte) ((value >> 32) & 0xff);
                bytes[5] = (byte) ((value >> 40) & 0xff);
                bytes[6] = (byte) ((value >> 48) & 0xff);
                bytes[7] = (byte) ((value >> 56) & 0xff);
                return bytes;
        }

        @Override
        public String toString() {
                return Long.toString(value);
        }

        public long getValue() {
                return value;
        }
}
