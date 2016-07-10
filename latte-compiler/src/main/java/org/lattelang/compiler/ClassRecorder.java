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

package org.lattelang.compiler;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * compiles all Latte libraries. The main method is called by maven.
 * This class also contains settings about files to be added into generated jars.
 */
public class ClassRecorder {
        /**
         * test whether the compiled lt files already been loaded.
         */
        private static final String classToTest = "lt.dsl.html.HTMLElement";

        /**
         * scan the directory (including its child directories) and record all class files
         * and write into the output file
         *
         * @param scanPath   the directory to scan
         * @param outputFile the output file
         * @throws Exception exception
         */
        public static void scanPath(String scanPath, String outputFile) throws Exception {
                StringBuilder sb = new StringBuilder();
                scanPathRecursive(new File(scanPath), sb, scanPath.length() + 1);
                File f = new File(outputFile);
                if (!f.exists()) {
                        if (!f.createNewFile()) throw new IOException("cannot create file " + outputFile);
                }
                FileOutputStream fos = new FileOutputStream(f);
                fos.write(sb.toString().getBytes());
        }

        /**
         * scan path recursively
         *
         * @param dir directory
         * @param sb  string builder
         * @param l   the base length
         */
        private static void scanPathRecursive(File dir, final StringBuilder sb, final int l) {
                File[] files = dir.listFiles();
                if (files == null) return;
                for (File f : files) {
                        if (f.isDirectory()) {
                                scanPathRecursive(f, sb, l);
                        } else if (f.getName().endsWith(".class")) {
                                sb.append(f.getAbsolutePath().substring(l)).append("\n");
                        }
                }
        }

        /**
         * the main method called by maven
         *
         * @param args the args array length should be 2. ['outputDir', 'fileName']
         * @throws Exception exception
         */
        public static void main(String[] args) throws Exception {
                try {
                        Class.forName(classToTest);
                } catch (ClassNotFoundException e) {
                        String outputDir = args[0];
                        String scanPath = args[0];
                        String fileName = args[1];

                        String scanResult = outputDir + File.separator + fileName;

                        scanPath(scanPath, scanResult);
                }
        }
}