package lt.compiler.semantic.builtin;

import lt.compiler.semantic.ConstantValue;
import lt.compiler.semantic.SClassDef;
import lt.compiler.semantic.STypeDef;
import lt.compiler.semantic.Value;

/**
 *
 */
public class StringConstantValue implements ConstantValue, Value {
        private final String str;
        private STypeDef type;

        public StringConstantValue(String str) {
                this.str = str;
        }

        public void setType(SClassDef type) {
                assert type.fullName().equals("java.lang.String");
                this.type = type;
        }

        public String getStr() {
                return str;
        }

        @Override
        public byte[] getByte() {
                return str.getBytes();
        }

        @Override
        public STypeDef type() {
                return type;
        }

        @Override
        public boolean equals(Object o) {
                if (this == o) return true;
                if (o == null || getClass() != o.getClass()) return false;

                StringConstantValue that = (StringConstantValue) o;

                return str.equals(that.str);

        }

        @Override
        public int hashCode() {
                return str.hashCode();
        }

        @Override
        public String toString() {
                return str;
        }
}
