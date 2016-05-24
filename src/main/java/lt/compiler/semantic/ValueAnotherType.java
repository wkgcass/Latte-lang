package lt.compiler.semantic;

/**
 * value considered as another type
 */
public class ValueAnotherType implements Value {
        private final STypeDef type;
        private final Value value;

        public ValueAnotherType(STypeDef type, Value value) {
                this.type = type;
                this.value = value;
        }

        @Override
        public STypeDef type() {
                return type;
        }

        public Value value() {
                return value;
        }
}
