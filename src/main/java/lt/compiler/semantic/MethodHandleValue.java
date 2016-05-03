package lt.compiler.semantic;

/**
 * method handle
 */
public class MethodHandleValue implements Value {
        private final SMethodDef method;
        private final int mode;
        private final STypeDef type;

        /**
         * construct a new method handle object
         *
         * @param method the method handle method
         * @param mode   defined in {@link lt.lang.Dynamic}
         * @param type   MethodHandle_Class
         */
        public MethodHandleValue(SMethodDef method, int mode, STypeDef type) {
                this.method = method;
                this.mode = mode;
                this.type = type;
        }

        @Override
        public STypeDef type() {
                return type;
        }

        public SMethodDef method() {
                return method;
        }

        public int mode() {
                return mode;
        }
}
