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

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * annotation
 */
public class SAnno implements Value {
        private SAnnoDef annoDef;
        private final Map<SAnnoField, Value> valueMap = new LinkedHashMap<>();
        private final Map<String, Object> alreadyCompiledAnnotationValueMap = new LinkedHashMap<>();
        private SAnnotationPresentable present;

        public void setAnnoDef(SAnnoDef annoDef) {
                this.annoDef = annoDef;
        }

        public void setPresent(SAnnotationPresentable present) {
                this.present = present;
        }

        @Override
        public SAnnoDef type() {
                return annoDef;
        }

        public Map<SAnnoField, Value> values() {
                return valueMap;
        }

        public SAnnotationPresentable present() {
                return present;
        }

        @Override
        public String toString() {
                StringBuilder sb = new StringBuilder();
                sb.append(type().fullName()).append("(");
                boolean isFirst = true;
                for (SAnnoField f : valueMap.keySet()) {
                        Value v = valueMap.get(f);
                        if (isFirst) {
                                isFirst = false;
                        } else {
                                sb.append(",");
                        }
                        sb.append(f.name()).append("=").append(v);
                }
                sb.append(")");
                return sb.toString();
        }

        public Map<String, Object> alreadyCompiledAnnotationValueMap() {
                return alreadyCompiledAnnotationValueMap;
        }
}
