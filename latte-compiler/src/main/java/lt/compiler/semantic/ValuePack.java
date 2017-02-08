/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 KuiGang Wang
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package lt.compiler.semantic;

import lt.compiler.LineCol;

import java.util.ArrayList;
import java.util.List;

/**
 * a list of instructions that finally return a value
 */
public class ValuePack implements Value, Instruction {
        private final List<Instruction> instructions = new ArrayList<Instruction>();
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
