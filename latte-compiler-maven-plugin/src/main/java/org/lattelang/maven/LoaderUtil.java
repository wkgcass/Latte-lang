package org.lattelang.maven;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;

/**
 * loader utilities
 */
public class LoaderUtil {
        public static ClassLoader loadClassesIn(List<File> deps, File... dirs) {
                URL[] urls = new URL[dirs.length + deps.size()];
                for (int i = 0; i < dirs.length; ++i) {
                        try {
                                urls[i] = dirs[i].toURI().toURL();
                        } catch (MalformedURLException e) {
                                throw new RuntimeException(e);
                        }
                }
                for (int i = 0; i < deps.size(); ++i) {
                        try {
                                urls[i + dirs.length] = deps.get(i).toURI().toURL();
                        } catch (MalformedURLException e) {
                                throw new RuntimeException(e);
                        }
                }
                return new URLClassLoader(urls, Thread.currentThread().getContextClassLoader());
        }
}
