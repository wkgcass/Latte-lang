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

import java.util.*;
import java.util.stream.Collectors;

/**
 * interface definition
 */
public class SInterfaceDef extends SRefTypeDef {
        private final List<SInterfaceDef> superInterfaces = new ArrayList<>();

        private final List<Instruction> staticStatements = new ArrayList<>();
        private final List<ExceptionTable> staticExceptionTable = new ArrayList<>();

        public SInterfaceDef(LineCol lineCol) {
                super(lineCol);
        }

        public List<SInterfaceDef> superInterfaces() {
                return superInterfaces;
        }

        public List<Instruction> staticStatements() {
                return staticStatements;
        }

        public List<ExceptionTable> staticExceptionTable() {
                return staticExceptionTable;
        }

        @Override
        public String toString() {
                StringBuilder sb = new StringBuilder();
                for (SModifier m : modifiers()) {
                        sb.append(m.toString().toLowerCase()).append(" ");
                }
                sb.append("interface ").append(fullName());
                if (!superInterfaces().isEmpty()) {
                        sb.append(" extends ");
                }
                boolean isFirst = true;
                for (SInterfaceDef i : superInterfaces()) {
                        if (isFirst) {
                                isFirst = false;
                        } else {
                                sb.append(",");
                        }
                        sb.append(i.fullName());
                }
                return sb.toString();
        }

        public boolean isAssignableFrom(STypeDef cls) {
                if (super.isAssignableFrom(cls)) return true;
                Queue<SInterfaceDef> q = new ArrayDeque<>();
                Set<SInterfaceDef> used = new HashSet<>();
                if (cls instanceof SClassDef) {
                        SClassDef tmp = (SClassDef) cls;
                        while (tmp != null) {
                                q.addAll(tmp.superInterfaces().stream().filter(used::add).collect(Collectors.toList()));
                                tmp = tmp.parent();
                        }
                } else if (cls instanceof SInterfaceDef) {
                        SInterfaceDef tmp = (SInterfaceDef) cls;
                        q.addAll(tmp.superInterfaces().stream().collect(Collectors.toList()));
                }
                while (!q.isEmpty()) {
                        SInterfaceDef i = q.remove();
                        if (isAssignableFrom(i)) return true;
                        q.addAll(i.superInterfaces().stream().collect(Collectors.toList()));
                }
                return false;
        }
}
