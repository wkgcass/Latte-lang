package lt.compiler.semantic.builtin;

import lt.compiler.semantic.PrimitiveValue;
import lt.compiler.semantic.STypeDef;

/**
 * char value
 */
public class CharValue implements PrimitiveValue {
        private final char value;

        public CharValue(char value) {
                this.value = value;
        }

        @Override
        public STypeDef type() {
                return CharTypeDef.get();
        }

        @Override
        public boolean equals(Object o) {
                if (this == o) return true;
                if (o == null || getClass() != o.getClass()) return false;

                CharValue charValue = (CharValue) o;

                return value == charValue.value;
        }

        @Override
        public int hashCode() {
                return (int) value;
        }

        @Override
        public String toString() {
                return Character.toString(value);
        }

        public int getValue() {
                return value;
        }
}
