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

import lt.compiler.SyntaxException
import lt.lang.Utils
import lt.repl.Compiler
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.logging.Logger

class LatteGradlePlugin implements Plugin<Project> {

    private static boolean checkSourceDir(File sourceDir, boolean fastFail) {
        if (sourceDir.exists()) {
            if (sourceDir.isDirectory()) {
                return true
            } else throw new Exception("File should be latte source file directory [" + sourceDir.absolutePath + "]")
        } else {
            if (fastFail) throw new Exception("Cannot find latte source file directory [" + sourceDir.absolutePath + "]")
            else return false
        }
    }

    private static boolean checkOutputDir(File outputDir) {
        if (outputDir.exists()) {
            if (outputDir.isDirectory()) {
                return true
            } else throw new Exception("File should be output directory [" + outputDir.absolutePath + "]")
        } else {
            if (outputDir.mkdirs()) {
                return true
            } else throw new IOException("Cannot create directory [" + outputDir.absolutePath + "]")
        }
    }

    private static void doCompile(Logger logger, ClassLoader cl, File sourceDir, File outputDir, boolean fastFail) {
        Map<String, File> fileMap = Utils.filesInDirectory(sourceDir, '.*\\.lt', true)
        Compiler compiler = new Compiler(cl)
        compiler.config.fastFail = fastFail
        compiler.config.result.outputDir = outputDir

        logger.info("Compiling latte source files from [" + sourceDir.absolutePath + "] to [" + outputDir.absolutePath + "]")
        try {
            compiler.compile(fileMap)
        } catch (SyntaxException e) {
            logger.error("Compilation failed!")
            throw e
        }

        logger.info("Compilation succeeded!")
    }

    private
    static void compile(Project project, String sourceSetName, String latteSrcName, boolean fastFail, boolean isTest) {
        File sourceDir = new File(project.projectDir.absolutePath + '/src/' + sourceSetName + '/' + latteSrcName)
        File mainOutputDir = new File(project.buildDir.absolutePath + '/classes/main')
        File testOutputDir = new File(project.buildDir.absolutePath + '/classes/test')

        File theOutputDir = isTest ? testOutputDir : mainOutputDir

        if (checkSourceDir(sourceDir, fastFail) && checkOutputDir(theOutputDir)) {
            Collection<URL> compileURLs = project.sourceSets[sourceSetName].compileClasspath.files.collect {
                it.toURI().toURL()
            }
            if (isTest) {
                compileURLs.add(testOutputDir.toURI().toURL())
            }
            compileURLs.add(mainOutputDir.toURI().toURL())
            ClassLoader classpath = LoaderUtil.loadClassesIn(compileURLs)
            doCompile(project.logger, classpath, sourceDir, theOutputDir, fastFail)
        }
    }

    @Override
    void apply(Project project) {
        def ext = project.extensions.create('latteConfig', LatteGradlePluginExtension)

        def compileLatte = project.task('compileLatte') << {
            compile(project, ext.mainSourceSet, ext.src, ext.fastFail, false)
        }

        def compileTestLatte = project.tasks.create('compileTestLatte') << {
            compile(project, ext.testSourceSet, ext.testSrc, ext.fastFail, true)
        }

        // dependencies
        // after latte
        if (ext.afterJava) {
            compileLatte.dependsOn project.tasks['compileJava']
            compileTestLatte.dependsOn project.tasks['compileTestJava']
        }
        if (ext.afterGroovy) {
            compileLatte.dependsOn project.tasks['compileGroovy']
            compileTestLatte.dependsOn project.tasks['compileTestGroovy']
        }
        // before latte
        project.tasks['classes'].dependsOn compileLatte
        project.tasks['testClasses'].dependsOn compileTestLatte
    }
}

class LatteGradlePluginExtension {
    String src = 'latte'
    String testSrc = 'latte'
    String mainSourceSet = 'main'
    String testSourceSet = 'test'
    boolean afterJava = true
    boolean afterGroovy = false
    boolean fastFail = false
}
