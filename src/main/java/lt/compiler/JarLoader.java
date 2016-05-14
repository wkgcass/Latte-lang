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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * jar class loader
 */
public class JarLoader extends ClassLoader {
        private Map<String, byte[]> classNameToBytes = new HashMap<>();

        public void loadAll(JarFile jarFile) throws IOException, ClassNotFoundException {
                Enumeration<JarEntry> entries = jarFile.entries();
                while (entries.hasMoreElements()) {
                        JarEntry entry = entries.nextElement();
                        String entryName = entry.getName();

                        if (entryName.endsWith(".class")) {
                                String className = entryName.replace("/", ".");
                                className = className.substring(0, className.length() - ".class".length());

                                InputStream is = jarFile.getInputStream(entry);
                                ByteArrayOutputStream bao = new ByteArrayOutputStream();
                                byte[] buffer = new byte[4096];
                                int n;
                                while (-1 != (n = is.read(buffer))) {
                                        bao.write(buffer, 0, n);
                                }
                                byte[] bytes = bao.toByteArray();

                                classNameToBytes.put(className, bytes);
                        }
                }
        }

        @Override
        protected Class<?> findClass(String name) throws ClassNotFoundException {
                if (classNameToBytes.containsKey(name)) {
                        byte[] bytes = classNameToBytes.get(name);
                        return defineClass(name, bytes, 0, bytes.length);
                } else return super.findClass(name);
        }
}
