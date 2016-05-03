package lt.compiler.semantic;

import lt.compiler.LineCol;

/**
 * field definition
 */
public class SFieldDef extends SMember implements LeftValue {
        private String name;
        private STypeDef type;

        public SFieldDef(LineCol lineCol) {
                super(lineCol);
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

        @Override
        public boolean canChange() {
                return !modifiers().contains(SModifier.FINAL);
        }

        @Override
        public STypeDef type() {
                return type;
        }
}
