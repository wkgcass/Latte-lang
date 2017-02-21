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

package lt.util;

import lt.repl.Evaluator;
import lt.repl.ClassPathLoader;

import java.io.*;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * automatically import static this class
 */
@SuppressWarnings("unused")
public class Utils {

        private Utils() {
        }

        /**
         * print the object and new line
         *
         * @param o the object to print
         */
        public static void println(Object o) {
                if (o instanceof char[]) {
                        System.out.println((char[]) o);
                } else {
                        System.out.println(o);
                }
        }

        /**
         * print the char array and new line
         *
         * @param o the char array to print
         */
        public static void println(char[] o) {
                System.out.println(o);
        }

        /**
         * print a new line
         */
        public static void println() {
                System.out.println();
        }

        /**
         * print the object
         *
         * @param o the object to print
         */
        public static void print(Object o) {
                System.out.print(o);
        }

        /**
         * evaluate the expression
         *
         * @param e expression or statement
         * @return if the input defines types, then the result is a list of {@link Class} objects.
         * otherwise, it's the evaluated result object.
         * @throws Exception exception
         */
        public static Object eval(String e) throws Exception {
                return eval(ClassLoader.getSystemClassLoader(), e);
        }

        /**
         * evaluate the expression
         *
         * @param loader the classloader
         * @param e      expression or statement
         * @return if the input defines types, then the result is a list of {@link Class} objects.
         * otherwise, it's the evaluated result object.
         * @throws Exception exception
         */
        public static Object eval(ClassLoader loader, String e) throws Exception {
                Evaluator evaluator = new Evaluator(new ClassPathLoader(loader));
                return evaluator.eval(e).result;
        }

        /**
         * get files in the directory. the result would be a fileName =&gt; File map.
         *
         * @param dir   the base directory
         * @param regex file pattern
         * @return a map.
         */
        public static Map<String, File> filesInDirectory(String dir, String regex) {
                return filesInDirectory(dir, regex, false);
        }

        /**
         * get files in the directory. the result would be a fileName =&gt; File map.
         *
         * @param dir   the base directory
         * @param regex file pattern
         * @return a map.
         */
        public static Map<String, File> filesInDirectory(String dir, Pattern regex) {
                return filesInDirectory(dir, regex, false);
        }

        /**
         * get files in the directory. the result would be a fileName =&gt; File map.
         *
         * @param dir         the base directory
         * @param regex       file pattern
         * @param recursively scan the children directories
         * @return a map.
         */
        public static Map<String, File> filesInDirectory(String dir, String regex, boolean recursively) {
                return filesInDirectory(new File(dir), regex, recursively);
        }

        /**
         * get files in the directory. the result would be a fileName =&gt; File map.
         *
         * @param dir         the base directory
         * @param regex       file pattern
         * @param recursively scan the children directories
         * @return a map.
         */
        public static Map<String, File> filesInDirectory(String dir, Pattern regex, boolean recursively) {
                return filesInDirectory(new File(dir), regex, recursively);
        }

        /**
         * get files in the directory. the result would be a fileName =&gt; File map.
         *
         * @param dir         the base directory
         * @param regex       file pattern
         * @param recursively scan the children directories
         * @return a map.
         */
        public static Map<String, File> filesInDirectory(File dir, String regex, boolean recursively) {
                return filesInDirectory(dir, Pattern.compile(regex), recursively);
        }

        /**
         * get files in the directory. the result would be a fileName =&gt; File map.
         *
         * @param dir         the base directory
         * @param regex       file name pattern
         * @param recursively scan the children directories
         * @return a map.
         */
        public static Map<String, File> filesInDirectory(File dir, Pattern regex, boolean recursively) {
                if (dir == null) throw new NullPointerException("dir is null");
                Map<String, File> map = new LinkedHashMap<String, File>();
                if (dir.isDirectory()) {
                        File[] listFiles = dir.listFiles();
                        if (listFiles != null) {
                                for (File f : listFiles) {
                                        if (f.isFile()) {
                                                if (regex.matcher(f.getName()).matches()) {
                                                        map.put(f.getName(), f);
                                                }
                                        } else if (f.isDirectory() && recursively) {
                                                Map<String, File> files = filesInDirectory(f, regex, true);
                                                map.putAll(files);
                                        }
                                }
                        }
                } else throw new IllegalArgumentException(dir + " is not a directory");
                return map;
        }
}
