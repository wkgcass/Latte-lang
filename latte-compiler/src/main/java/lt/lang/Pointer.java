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

/**
 * the pointer object.
 * use p[] to access/modify the contained value.
 *
 * @param <T> the contained object type
 */
@SuppressWarnings("unused")
public class Pointer<T> {
        private T item;
        private final boolean nonnull;
        private final boolean nonempty;

        public Pointer(boolean nonnull, boolean nonempty) {
                this.nonnull = nonnull;
                this.nonempty = nonempty;
        }

        /**
         * retrieve the contained object (or null)
         *
         * @return the contained object
         */
        public T get() {
                return item;
        }

        /**
         * cast type and set the contained object and return the pointer itself.
         *
         * @param item input object (or null)
         * @return input object
         * @throws Throwable exception when trying to cast the object
         */
        public Pointer set(T item) throws Throwable {
                if (nonempty) {
                        if (!LtRuntime.castToBool(item)) throw new IllegalArgumentException();
                }
                if (nonnull) {
                        if (item == null) throw new NullPointerException();
                        if (item instanceof Unit) throw new IllegalArgumentException();
                }
                this.item = item;
                return this;
        }
}
