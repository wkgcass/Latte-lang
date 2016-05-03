package lt.compiler.semantic;

import java.util.Arrays;

/**
 * array value
 */
public class SArrayValue implements Value {
        private int dimension;
        private Value[] values;
        private SArrayTypeDef type;

        public void setDimension(int dimension) {
                this.dimension = dimension;
        }

        public void setType(SArrayTypeDef type) {
                this.type = type;
        }

        public void setValues(Value[] values) {
                this.values = values;
        }

        public int dimension() {
                return dimension;
        }

        public int length() {
                return values().length;
        }

        public Value[] values() {
                return values;
        }

        @Override
        public SArrayTypeDef type() {
                return type;
        }

        @Override
        public String toString() {
                return Arrays.toString(values());
        }
}
