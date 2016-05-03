package lt.compiler.semantic.builtin;

import lt.compiler.semantic.ConstantValue;
import lt.compiler.semantic.STypeDef;
import lt.compiler.semantic.Value;

/**
 * class value
 */
public class ClassValue implements Value, ConstantValue {
        private STypeDef type;
        private String className;

        public void setClassName(String className) {
                this.className = className;
        }

        /**
         * java.lang.Class
         *
         * @param type java.lang.Class
         */
        public void setType(STypeDef type) {
                this.type = type;
        }

        public String className() {
                return className;
        }

        /**
         * java.lang.Class
         *
         * @return java.lang.Class
         */
        @Override
        public STypeDef type() {
                return type;
        }

        @Override
        public byte[] getByte() {
                return className().replace(".", "/").getBytes();
        }

        @Override
        public String toString() {
                return "ClassValue(" + className + ')';
        }
}
