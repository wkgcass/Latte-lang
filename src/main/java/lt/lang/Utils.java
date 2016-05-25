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

import lt.repl.Evaluator;
import lt.repl.ClassPathLoader;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * automatically import static this class
 */
@SuppressWarnings("unused")
public class Utils {
        private Utils() {
        }

        public static void println(Object o) {
                System.out.println(o);
        }

        public static void println() {
                System.out.println();
        }

        public static Object eval(String e) throws Exception {
                Evaluator evaluator = new Evaluator(new ClassPathLoader());
                return evaluator.eval(e).result;
        }

        public static Map<String, File> filesInDirectory(String dir) {
                return filesInDirectory(new File(dir), false);
        }

        public static Map<String, File> filesInDirectory(String dir, boolean recursively) {
                return filesInDirectory(new File(dir), recursively);
        }

        public static Map<String, File> filesInDirectory(File dir, boolean recursively) {
                if (dir == null) throw new NullPointerException("dir is null");
                Map<String, File> map = new LinkedHashMap<>();
                if (dir.isDirectory()) {
                        File[] listFiles = dir.listFiles();
                        if (listFiles != null) {
                                for (File f : listFiles) {
                                        if (f.isFile()) {
                                                if (f.getName().endsWith(".lt")) {
                                                        map.put(f.getName(), f);
                                                }
                                        } else if (f.isDirectory() && recursively) {
                                                Map<String, File> files = filesInDirectory(f, true);
                                                map.putAll(files);
                                        }
                                }
                        }
                } else throw new IllegalArgumentException(dir + " is not a directory");
                return map;
        }
}
