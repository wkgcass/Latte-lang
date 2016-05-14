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

package lt.compiler;

import lt.compiler.semantic.*;

import java.util.*;

/**
 * semantic scope
 */
public class SemanticScope {
        public static class MethodRecorder {
                public final SMethodDef method;
                public final int paramCount;

                public MethodRecorder(SMethodDef method, int paramCount) {
                        this.method = method;
                        this.paramCount = paramCount;
                }
        }

        public final SemanticScope parent;

        private final Map<String, LeftValue> leftValueMap = new LinkedHashMap<>();
        private final Map<String, MethodRecorder> innerMethodMap = new HashMap<>();

        private final STypeDef sTypeDef;

        private Ins.This aThis;

        public SemanticScope(SemanticScope parent) {
                this.parent = parent;
                sTypeDef = null;
        }

        public SemanticScope(STypeDef sTypeDef) {
                this.sTypeDef = sTypeDef;
                parent = null;
        }

        public LeftValue getLeftValue(String name) {
                if (leftValueMap.containsKey(name)) {
                        return leftValueMap.get(name);
                } else if (parent != null) {
                        return parent.getLeftValue(name);
                } else {
                        return null;
                }
        }

        public LinkedHashMap<String, STypeDef> getLocalVariables() {
                LinkedHashMap<String, STypeDef> map;
                if (parent != null) {
                        map = parent.getLocalVariables();
                } else {
                        map = new LinkedHashMap<>();
                }
                leftValueMap.forEach((k, v) -> map.put(k, v.type()));
                return map;
        }

        public void putLeftValue(String name, LeftValue v) {
                leftValueMap.put(name, v);
        }

        public void addMethodDef(String name, MethodRecorder innerMethod) {
                innerMethodMap.put(name, innerMethod);
        }

        /**
         * get a list of leftValue
         *
         * @param count size of the required list (may be less, but won't be more)
         * @return a list of LeftValue(s)
         */
        public List<LeftValue> getLeftValues(int count) {
                List<LeftValue> list;
                if (parent != null) list = parent.getLeftValues(count);
                else list = new ArrayList<>();
                Iterator<LeftValue> it = leftValueMap.values().iterator();
                while (list.size() != count && it.hasNext()) {
                        list.add(it.next());
                }
                return list;
        }

        public MethodRecorder getInnerMethod(String name) {
                if (innerMethodMap.containsKey(name)) {
                        return innerMethodMap.get(name);
                } else if (parent == null) {
                        return null;
                } else return parent.getInnerMethod(name);
        }

        public boolean containsInnerMethod(String name) {
                return innerMethodMap.containsKey(name);
        }

        public STypeDef type() {
                if (sTypeDef != null)
                        return sTypeDef;
                if (parent != null) return parent.type();
                return null;
        }

        public void setThis(Ins.This aThis) {
                this.aThis = aThis;
        }

        public Ins.This getThis() {
                if (aThis != null)
                        return aThis;
                if (parent != null) return parent.getThis();
                return null;
        }

        public int getIndex(LeftValue leftValue) {
                // try parent
                if (parent != null) {
                        try {
                                return parent.getIndex(leftValue);
                        } catch (RuntimeException ignore) {
                        }
                }
                int i = getThis() == null ? 0 : 1;

                SemanticScope scope = parent;

                while (scope != null) {
                        i += scope.leftValueMap.size();
                        scope = scope.parent;
                }

                for (LeftValue v : leftValueMap.values()) {
                        if (v.equals(leftValue)) return i;
                        ++i;
                }

                throw new RuntimeException(leftValue + " is not recorded in the scope");
        }

        public String generateTempName() {
                int i = 0;
                while (getLeftValue("*" + i) != null) ++i;
                return "*" + i;
        }
}
