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
import lt.util.Utils
import lt.repl.Compiler
import org.gradle.api.Action
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.FileTreeElement
import org.gradle.api.internal.file.SourceDirectorySetFactory
import org.gradle.api.internal.plugins.DslObject
import org.gradle.api.internal.tasks.DefaultSourceSet
import org.gradle.api.logging.Logger
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.specs.Spec
import org.gradle.api.tasks.SourceSet

import javax.inject.Inject

class LatteGradlePlugin implements Plugin<Project> {

    private static File[] filterSourceDirs(Object[] srcDirs, boolean fastFail) {
        List<File> list = new ArrayList<>()
        for (Object x : srcDirs) {
            File f = (File) x;
            if (checkSourceDir(f, fastFail)) {
                list.add(f)
            }
        }
        return list.toArray(new File[list.size()])
    }

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

    private static void doCompile(Logger logger, ClassLoader cl, File[] sourceDirs, File outputDir, boolean fastFail) {
        Map<String, File> fileMap = new HashMap<>()
        for (File dir : sourceDirs) {
            fileMap.putAll(Utils.filesInDirectory(dir, '.*\\.(lt|latte)', true))
        }
        Compiler compiler = new Compiler(cl)
        compiler.config.fastFail = fastFail
        compiler.config.result.outputDir = outputDir

        logger.println("Compiling latte source files from " + Arrays.toString(sourceDirs) + " to [" + outputDir.absolutePath + "]")
        try {
            compiler.compile(fileMap)
        } catch (SyntaxException e) {
            logger.error("Compilation failed!")
            throw e
        }

        logger.info("Compilation succeeded!")
    }

    private
    static void compile(Project project, boolean fastFail, boolean isTest) {
        def mainSrc = project.sourceSets.main.latte.srcDirs
        def testSrc = project.sourceSets.test.latte.srcDirs

        File mainOutputDir = new File(project.buildDir.absolutePath + '/classes/main')
        File testOutputDir = new File(project.buildDir.absolutePath + '/classes/test')

        def theSourceDirs = isTest ? testSrc : mainSrc
        File theOutputDir = isTest ? testOutputDir : mainOutputDir

        File[] sourceDirs = filterSourceDirs(theSourceDirs.toArray(theSourceDirs.size()), false)
        if (sourceDirs.length > 0 && checkOutputDir(theOutputDir)) {
            Collection<URL> compileURLs = project.sourceSets[isTest ? 'test' : 'main'].compileClasspath.files.collect {
                it.toURI().toURL()
            }
            if (isTest) {
                compileURLs.add(testOutputDir.toURI().toURL())
            }
            compileURLs.add(mainOutputDir.toURI().toURL())
            ClassLoader classpath = LoaderUtil.loadClassesIn(compileURLs)
            doCompile(project.logger, classpath, sourceDirs, theOutputDir, fastFail)
        }
    }

    private Project project;
    private final SourceDirectorySetFactory sourceDirectorySetFactory;

    @Inject
    public LatteGradlePlugin(SourceDirectorySetFactory sourceDirectorySetFactory) {
        this.sourceDirectorySetFactory = sourceDirectorySetFactory;
    }

    @Override
    void apply(Project project) {
        this.project = project
        project.getPluginManager().apply(JavaBasePlugin)

        configureSourceSetDefaults()
        registerExtensionAndTasks()
    }

    private void registerExtensionAndTasks() {
        def ext = project.extensions.create('latteConfig', LatteGradlePluginExtension)
        def compileLatte = project.task('compileLatte').doLast { t ->
            compile(project, ext.fastFail, false)
        }

        def compileTestLatte = project.tasks.create('compileTestLatte').doLast { t ->
            compile(project, ext.fastFail, true)
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

    private void configureSourceSetDefaults() {
        project.getConvention().getPlugin(JavaPluginConvention).getSourceSets().all(new Action<SourceSet>() {
            public void execute(SourceSet sourceSet) {
                final DefaultLatteSourceSet groovySourceSet = new DefaultLatteSourceSet(((DefaultSourceSet) sourceSet).getDisplayName(), sourceDirectorySetFactory);
                new DslObject(sourceSet).getConvention().getPlugins().put("groovy", groovySourceSet);

                groovySourceSet.getLatte().srcDir("src/" + sourceSet.getName() + "/latte");
                sourceSet.getResources().getFilter().exclude(new Spec<FileTreeElement>() {
                    public boolean isSatisfiedBy(FileTreeElement element) {
                        return groovySourceSet.getLatte().contains(element.getFile());
                    }
                });
                sourceSet.getAllJava().source(groovySourceSet.getLatte());
                sourceSet.getAllSource().source(groovySourceSet.getLatte());
            }
        });
    }
}

class LatteGradlePluginExtension {
    boolean afterJava = true
    boolean afterGroovy = false
    boolean fastFail = false
}
