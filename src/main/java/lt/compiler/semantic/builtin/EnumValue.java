package lt.compiler.semantic.builtin;

import lt.compiler.semantic.STypeDef;
import lt.compiler.semantic.Value;

/**
 * enum value
 */
public class EnumValue implements Value {
        private String enumStr;
        private STypeDef type;

        public void setType(STypeDef type) {
                this.type = type;
        }

        public void setEnumStr(String enumStr) {
                this.enumStr = enumStr;
        }

        @Override
        public STypeDef type() {
                return type;
        }

        public String enumStr() {
                return enumStr;
        }

        @Override
        public String toString() {
                return type().fullName() + "." + enumStr();
        }
}
