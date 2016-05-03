package lt.compiler.semantic.builtin;

import lt.compiler.semantic.ConstantValue;
import lt.compiler.semantic.PrimitiveValue;
import lt.compiler.semantic.STypeDef;

/**
 * float value
 */
public class FloatValue implements PrimitiveValue, ConstantValue {
        private final float value;

        public FloatValue(float value) {
                this.value = value;
        }

        @Override
        public STypeDef type() {
                return FloatTypeDef.get();
        }

        @Override
        public boolean equals(Object o) {
                if (this == o) return true;
                if (o == null || getClass() != o.getClass()) return false;

                FloatValue that = (FloatValue) o;

                return Float.compare(that.value, value) == 0;
        }

        @Override
        public int hashCode() {
                return (value != +0.0f ? Float.floatToIntBits(value) : 0);
        }

        @Override
        public byte[] getByte() {
                int data = Float.floatToIntBits(value);
                byte[] bytes = new byte[4];
                bytes[0] = (byte) (data & 0xff);
                bytes[1] = (byte) ((data & 0xff00) >> 8);
                bytes[2] = (byte) ((data & 0xff0000) >> 16);
                bytes[3] = (byte) ((data & 0xff000000) >> 24);
                return bytes;
        }

        @Override
        public String toString() {
                return Float.toString(value);
        }

        public float getValue() {
                return value;
        }
}
