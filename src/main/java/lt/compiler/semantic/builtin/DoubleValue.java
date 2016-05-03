package lt.compiler.semantic.builtin;

import lt.compiler.semantic.ConstantValue;
import lt.compiler.semantic.PrimitiveValue;
import lt.compiler.semantic.STypeDef;

/**
 * double value
 */
public class DoubleValue implements PrimitiveValue, ConstantValue {
        private final double value;

        public DoubleValue(double value) {
                this.value = value;
        }

        @Override
        public STypeDef type() {
                return DoubleTypeDef.get();
        }

        @Override
        public boolean equals(Object o) {
                if (this == o) return true;
                if (o == null || getClass() != o.getClass()) return false;

                DoubleValue that = (DoubleValue) o;

                return Double.compare(that.value, value) == 0;
        }

        @Override
        public int hashCode() {
                long temp = Double.doubleToLongBits(value);
                return (int) (temp ^ (temp >>> 32));
        }

        @Override
        public byte[] getByte() {
                long data = Double.doubleToLongBits(value);
                byte[] bytes = new byte[8];
                bytes[0] = (byte) (data & 0xff);
                bytes[1] = (byte) ((data >> 8) & 0xff);
                bytes[2] = (byte) ((data >> 16) & 0xff);
                bytes[3] = (byte) ((data >> 24) & 0xff);
                bytes[4] = (byte) ((data >> 32) & 0xff);
                bytes[5] = (byte) ((data >> 40) & 0xff);
                bytes[6] = (byte) ((data >> 48) & 0xff);
                bytes[7] = (byte) ((data >> 56) & 0xff);
                return bytes;
        }

        @Override
        public String toString() {
                return Double.toString(value);
        }

        public double getValue() {
                return value;
        }
}
