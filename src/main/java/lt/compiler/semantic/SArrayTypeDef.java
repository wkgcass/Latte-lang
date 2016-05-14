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
import lt.compiler.semantic.builtin.*;

import java.util.Collections;
import java.util.List;

/**
 * arrayTypeDef
 */
public class SArrayTypeDef extends STypeDef {
        private STypeDef type;
        private int dimension;

        public SArrayTypeDef() {
                super(LineCol.SYNTHETIC);
        }

        public void setDimension(int dimension) {
                this.dimension = dimension;
                rebuildFullName();
        }

        public void setType(STypeDef type) {
                this.type = type;
        }

        public STypeDef type() {
                return type;
        }

        @Override
        public List<SAnno> annos() {
                return Collections.emptyList();
        }

        public int dimension() {
                return dimension;
        }

        @Override
        public String fullName() {
                if (super.fullName() == null) {
                        rebuildFullName();
                }

                return super.fullName();
        }

        private void rebuildFullName() {
                if (null == type()) return;
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < dimension(); ++i) {
                        sb.append("[");
                }
                if (type() instanceof PrimitiveTypeDef) {
                        if (type() instanceof ByteTypeDef) {
                                sb.append("B");
                        } else if (type() instanceof CharTypeDef) {
                                sb.append("C");
                        } else if (type() instanceof DoubleTypeDef) {
                                sb.append("D");
                        } else if (type() instanceof FloatTypeDef) {
                                sb.append("F");
                        } else if (type() instanceof IntTypeDef) {
                                sb.append("I");
                        } else if (type() instanceof LongTypeDef) {
                                sb.append("L");
                        } else if (type() instanceof ShortTypeDef) {
                                sb.append("S");
                        } else if (type() instanceof BoolTypeDef) {
                                sb.append("B");
                        }
                } else {
                        sb.append("L").append(type().fullName()).append(";");
                }
                setFullName(sb.toString());
        }
}
