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

package lt.lang;

import java.util.Enumeration;
import java.util.Iterator;
import java.util.Map;

/**
 * iterable
 */
public abstract class LtIterator implements Iterator {
        static class ArrayIt extends LtIterator {
                final Object[] array;
                int index = 0;

                ArrayIt(Object[] array) {
                        this.array = array;
                }

                @Override
                public boolean hasNext() {
                        return index < array.length; // index from 0 to array.length
                }

                @Override
                public Object next() {
                        return array[index++]; // index from 0 to array.length
                }
        }

        static class It extends LtIterator {
                final Iterator it;

                It(Iterator it) {
                        this.it = it;
                }

                @Override
                public boolean hasNext() {
                        return it.hasNext();
                }

                @Override
                public Object next() {
                        return it.next();
                }
        }

        static class EnIt extends LtIterator {
                final Enumeration en;

                EnIt(Enumeration en) {
                        this.en = en;
                }

                @Override
                public boolean hasNext() {
                        return en.hasMoreElements();
                }

                @Override
                public Object next() {
                        return en.nextElement();
                }
        }

        public static LtIterator getIterator(Object o) {
                if (o.getClass().isArray()) {
                        return new ArrayIt((Object[]) o);
                } else if (o instanceof Iterable) {
                        return new It(((Iterable) o).iterator());
                } else if (o instanceof Iterator) {
                        return new It((Iterator) o);
                } else if (o instanceof Enumeration) {
                        return new EnIt((Enumeration) o);
                } else if (o instanceof Map) {
                        return new It(((Map) o).entrySet().iterator());
                } else throw new RuntimeException("cannot iterate on " + o);
        }

        public abstract boolean hasNext();

        public abstract Object next();
}
