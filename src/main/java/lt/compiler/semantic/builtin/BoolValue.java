package lt.compiler.semantic.builtin;

import lt.compiler.semantic.PrimitiveValue;
import lt.compiler.semantic.STypeDef;

/**
 * boolean value
 */
public class BoolValue implements PrimitiveValue {
        private final boolean value;

        public BoolValue(boolean value) {
                this.value = value;
        }

        @Override
        public STypeDef type() {
                return BoolTypeDef.get();
        }

        @Override
        public boolean equals(Object o) {
                if (this == o) return true;
                if (o == null || getClass() != o.getClass()) return false;

                BoolValue boolValue = (BoolValue) o;

                return value == boolValue.value;
        }

        @Override
        public int hashCode() {
                return (value ? 1 : 0);
        }

        @Override
        public String toString() {
                return String.valueOf(value);
        }

        public int getValue() {
                return value ? 1 : 0;
        }
}
