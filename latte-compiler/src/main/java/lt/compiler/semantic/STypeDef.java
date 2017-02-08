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
import lt.compiler.semantic.builtin.NullTypeDef;

import java.util.ArrayList;
import java.util.List;

/**
 * type definition
 */
public class STypeDef implements SAnnotationPresentable {
        private String pkg;
        private String fullName;
        private List<SAnno> annos = new ArrayList<SAnno>();
        private final LineCol lineCol;

        public STypeDef(LineCol lineCol) {
                this.lineCol = lineCol;
        }

        public void setFullName(String fullName) {
                this.fullName = fullName;
        }

        public void setPkg(String pkg) {
                this.pkg = pkg;
        }

        public String fullName() {
                return fullName;
        }

        @Override
        public List<SAnno> annos() {
                return annos;
        }

        public String pkg() {
                return pkg;
        }

        public boolean isAssignableFrom(STypeDef cls) {
                if (cls == null) throw new NullPointerException();
                if (cls instanceof NullTypeDef)
                        return !(this instanceof PrimitiveTypeDef);
                return cls.equals(this);
        }

        public LineCol line_col() {
                return lineCol;
        }
}
