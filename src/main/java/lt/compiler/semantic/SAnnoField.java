package lt.compiler.semantic;

import lt.compiler.LineCol;

/**
 * annotation field
 */
public class SAnnoField extends SMethodDef {
        private String name;
        private STypeDef type;
        private Value defaultValue;

        public SAnnoField() {
                super(LineCol.SYNTHETIC);
        }

        public void setDefaultValue(Value defaultValue) {
                this.defaultValue = defaultValue;
        }

        public void setName(String name) {
                this.name = name;
        }

        public void setType(STypeDef type) {
                this.type = type;
        }

        public String name() {
                return name;
        }

        public STypeDef type() {
                return type;
        }

        public Value defaultValue() {
                return defaultValue;
        }

        @Override
        public String toString() {
                StringBuilder sb = new StringBuilder();
                sb.append(type.fullName()).append(" ").append(name).append("()");
                if (defaultValue != null) {
                        sb.append(" default ").append(defaultValue).append(";");
                }
                return sb.toString();
        }
}
