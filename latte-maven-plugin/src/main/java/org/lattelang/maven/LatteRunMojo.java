package org.lattelang.maven;

import lt.repl.ScriptCompiler;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.*;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

/**
 * run a latte script.
 */
@Mojo(name = "run", requiresDependencyResolution = ResolutionScope.TEST)
@Execute(goal = "run", phase = LifecyclePhase.TEST)
@SuppressWarnings("unused")
public class LatteRunMojo extends AbstractMojo {
        /**
         * The script.
         */
        @Parameter(property = "script", required = true)
        private String script;
        /**
         * dependencies.
         */
        @Parameter(defaultValue = "${project.compileClasspathElements}", required = true)
        private List<String> dependencies;

        @SuppressWarnings("unchecked")
        @Override
        public void execute() throws MojoExecutionException, MojoFailureException {
                script = script.trim();
                if (script.startsWith("/")) {
                        script = script.substring(1);
                }

                ClassLoader loader = LoaderUtil.loadClassesIn(dependencies);
                InputStream scriptIS = loader.getResourceAsStream(script);
                if (scriptIS == null) {
                        throw new MojoExecutionException("script [" + script + "] not found!");
                }

                ScriptCompiler sc = new ScriptCompiler(loader);
                try {
                        ScriptCompiler.Script s =
                                sc.compile(script, new InputStreamReader(scriptIS));
                        s.run();
                } catch (Throwable t) {
                        throw new MojoFailureException("exception occurred when trying to run the script", t);
                }
        }
}
