package lt.compiler.semantic;

/**
 * local variable
 */
public class LocalVariable implements LeftValue {
        private final boolean canChange;
        private final STypeDef type;

        public LocalVariable(STypeDef type, boolean canChange) {
                this.type = type;
                this.canChange = canChange;
        }

        @Override
        public boolean canChange() {
                return canChange;
        }

        @Override
        public STypeDef type() {
                return type;
        }
}
