package lt.compiler.semantic;

import lt.compiler.LineCol;

/**
 * value considered as another type
 */
public class ValueAnotherType implements Value, Instruction {
        private final STypeDef type;
        private final Value value;
        private final LineCol lineCol;

        public ValueAnotherType(STypeDef type, Value value, LineCol lineCol) {
                this.type = type;
                this.value = value;
                this.lineCol = lineCol;
        }

        @Override
        public STypeDef type() {
                return type;
        }

        public Value value() {
                return value;
        }

        @Override
        public LineCol line_col() {
                return lineCol;
        }
}
