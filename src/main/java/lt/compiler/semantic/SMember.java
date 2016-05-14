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
 * define a member
 */
abstract public class SMember implements SAnnotationPresentable {
        private final List<SModifier> modifiers = new ArrayList<>();
        private STypeDef declaringType;
        private final List<SAnno> annos = new ArrayList<>();
        private LineCol lineCol;

        public SMember(LineCol lineCol) {
                this.lineCol = lineCol;
        }

        public void setDeclaringType(STypeDef declaringType) {
                this.declaringType = declaringType;
        }

        public List<SModifier> modifiers() {
                return modifiers;
        }

        public STypeDef declaringType() {
                return declaringType;
        }

        @Override
        public List<SAnno> annos() {
                return annos;
        }

        public LineCol line_col() {
                return lineCol;
        }
}
