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

package org.lattelang.compiler.maven;

import lt.lang.Utils;
import lt.repl.Compiler;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.*;

import java.io.File;
import java.util.List;

/**
 * compile latte test files.
 */
@Mojo(name = "test-compile", requiresDependencyResolution = ResolutionScope.TEST)
@Execute(phase = LifecyclePhase.TEST_COMPILE, goal = "test-compile")
@SuppressWarnings("unused")
public class TestCompileMojo extends AbstractMojo {
        /**
         * The test output directory.
         */
        @Parameter(defaultValue = "${project.build.testOutputDirectory}", required = true)
        private File testOutputDirectory;
        /**
         * The test output directory.
         */
        @Parameter(defaultValue = "${project.build.testSourceDirectory}", required = true)
        private File testSourceDirectory;
        /**
         * dependencies.
         */
        @Parameter(defaultValue = "${project.compileClasspathElements}", required = true)
        private List<String> dependencies;

        @SuppressWarnings("unchecked")
        @Override
        public void execute() throws MojoExecutionException, MojoFailureException {
                File latteTestSourceDirectory = new File(testSourceDirectory.getParent() + "/latte");

                getLog().info("Compiling latte test source files to " + testOutputDirectory);
                Compiler testCompiler = new Compiler(LoaderUtil.loadClassesIn(dependencies));

                testCompiler.config.fastFail = false;
                testCompiler.config.result.outputDir = testOutputDirectory;

                try {
                        testCompiler.compile(Utils.filesInDirectory(latteTestSourceDirectory, ".*\\.lt", true));
                } catch (Exception e) {
                        getLog().error("Compilation failed!");
                        throw new MojoFailureException("Compilation failed!", e);
                }

                getLog().info("Compilation succeeded!");
        }
}
