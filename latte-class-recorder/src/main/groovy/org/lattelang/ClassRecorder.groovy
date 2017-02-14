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

package org.lattelang

import org.gradle.api.Plugin
import org.gradle.api.Project

class ClassRecorderPlugin implements Plugin<Project> {
    @Override
    void apply(Project project) {
        def ext = project.extensions.create('recordConfig', ClassRecorderExtension)
        def task = project.task('recordClass') << {
            def path = project.buildDir.absolutePath
            ClassRecorder.apply(path + '/' + ext.directory, ext.file)
        }

        task.dependsOn project.tasks['classes']
        project.tasks['jar'].dependsOn task
    }
}

class ClassRecorderExtension {
    String directory
    String file
}

class ClassRecorder {
    /**
     * scan the directory (including its child directories) and record all class files
     * and write into the output file
     *
     * @param scanPath the directory to scan
     * @param outputFile the output file
     * @throws Exception exception
     */
    private static void doScanPath(String scanPath, String outputFile) throws Exception {
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
     * @param sb string builder
     * @param l the base length
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
     * @param outputDir the compile output (`classes` directory)
     * @param fileName the fileName of the text file that records the class names
     * @throws Exception exception
     */
    public static void apply(String outputDir, String fileName) throws Exception {
        String scanResult = outputDir + "/" + fileName;

        doScanPath(outputDir, scanResult);
    }
}
