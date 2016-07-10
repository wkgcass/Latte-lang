package org.lattelang.compiler.maven;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;

/**
 * loader utilities
 */
public class LoaderUtil {
        public static ClassLoader loadClassesIn(List<String> dependencies) {
                URL[] urls = new URL[dependencies.size()];
                for (int i = 0; i < dependencies.size(); ++i) {
                        try {
                                urls[i] = new File(dependencies.get(i)).toURI().toURL();
                        } catch (MalformedURLException e) {
                                throw new RuntimeException(e);
                        }
                }
                return new URLClassLoader(urls, Thread.currentThread().getContextClassLoader());
        }
}
