package lt.compiler.semantic;

import lt.compiler.LineCol;

import java.util.ArrayList;
import java.util.List;

/**
 * a list of instructions that finally return a value
 */
public class ValuePack implements Value, Instruction {
        private final List<Instruction> instructions = new ArrayList<>();
        private STypeDef type;
        private final boolean autoPop;

        public ValuePack(boolean autoPop) {
                this.autoPop = autoPop;
        }

        public boolean autoPop() {
                return autoPop;
        }

        public void setType(STypeDef type) {
                this.type = type;
        }

        @Override
        public LineCol line_col() {
                return instructions.isEmpty() ? LineCol.SYNTHETIC : instructions.get(instructions.size() - 1).line_col();
        }

        @Override
        public STypeDef type() {
                if (type == null) {
                        if (instructions.isEmpty()) {
                                return null;
                        }
                        Instruction ins = instructions.get(instructions.size() - 1);
                        if (ins instanceof Value) {
                                return ((Value) ins).type();
                        } else return null;
                } else {
                        return type;
                }
        }

        public List<Instruction> instructions() {
                return instructions;
        }
}
