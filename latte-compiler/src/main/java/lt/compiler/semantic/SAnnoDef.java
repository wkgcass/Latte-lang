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
import lt.compiler.LtBug;
import lt.compiler.SyntaxException;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.List;

/**
 * annotation definition
 */
public class SAnnoDef extends STypeDef {
        private final List<SAnnoField> annoFields = new ArrayList<>();
        private final List<SModifier> modifiers = new ArrayList<>();

        public SAnnoDef() {
                // annotations cannot be defined in Latte
                super(LineCol.SYNTHETIC);
        }

        public List<SAnnoField> annoFields() {
                return annoFields;
        }

        public List<SModifier> modifiers() {
                return modifiers;
        }

        public boolean canPresentOn(ElementType type) throws SyntaxException {
                String name = fullName();
                try {
                        Class<?> cls = Class.forName(name);
                        Annotation[] annotations = cls.getAnnotations();
                        for (Annotation a : annotations) {
                                if (a instanceof Target) {
                                        Target target = (Target) a;
                                        ElementType[] types = target.value();
                                        for (ElementType t : types) {
                                                if (t.equals(type)) return true;
                                        }
                                        return false;
                                }
                        }
                        return true;
                } catch (ClassNotFoundException e) {
                        throw new LtBug(e);
                }
        }

        @Override
        public String toString() {
                return "@" + fullName();
        }
}
