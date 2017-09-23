package lt.compiler.semantic;

/**
 * a value without actual value, only a type
 */
public class DummyValue implements Value {
        private final STypeDef aType;

        public DummyValue(STypeDef aType) {
                this.aType = aType;
        }

        @Override
        public STypeDef type() {
                return aType;
        }
}
