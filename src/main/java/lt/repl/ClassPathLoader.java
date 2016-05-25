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

package lt.repl;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;

/**
 * jar class loader
 */
public class ClassPathLoader extends ClassLoader {
        private URLClassLoader urlClassLoader;

        public ClassPathLoader() {
                super(null);
                this.urlClassLoader = URLClassLoader.newInstance(new URL[0]);
        }

        public ClassPathLoader(ClassLoader loader) {
                super(null);
                this.urlClassLoader = URLClassLoader.newInstance(new URL[0], loader);
        }

        /**
         * add the url for loading in the future
         *
         * @param url url
         * @throws IOException            exception
         * @throws ClassNotFoundException exception
         */
        public void load(URL url) throws IOException, ClassNotFoundException {
                urlClassLoader = URLClassLoader.newInstance(new URL[]{url}, urlClassLoader);
        }

        /**
         * load a class, invoking {@link #urlClassLoader#loadClass(String, boolean)}
         *
         * @param name    name
         * @param resolve whether to resolve
         * @return the class
         * @throws ClassNotFoundException exceptions
         */
        public Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
                Class<?> cls = urlClassLoader.loadClass(name);
                if (resolve) {
                        resolveClass(cls);
                }
                return cls;
        }
}
