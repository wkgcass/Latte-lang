package lt.compiler.semantic;

import java.util.List;

/**
 * method type
 */
public class MethodTypeValue implements Value {
        private final List<STypeDef> parameters;
        private final STypeDef returnType;
        private final STypeDef type;

        /**
         * construct a method type object
         *
         * @param parameters parameter types
         * @param returnType method return type
         * @param type       MethodHandle_Class
         */
        public MethodTypeValue(List<STypeDef> parameters, STypeDef returnType, STypeDef type) {
                this.parameters = parameters;
                this.returnType = returnType;
                this.type = type;
        }

        public List<STypeDef> parameters() {
                return parameters;
        }

        public STypeDef returnType() {
                return returnType;
        }

        @Override
        public STypeDef type() {
                return type;
        }
}
