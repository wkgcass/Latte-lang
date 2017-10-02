package lt.compiler;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;

/**
 * proxies multiple class loaders
 */
public class MultipleClassLoader extends ClassLoader {
        private final Set<ClassLoader> classLoaders = new HashSet<ClassLoader>();
        private final Method mLoadClass;

        public MultipleClassLoader(ClassLoader... classLoaders) {
                this.classLoaders.addAll(Arrays.asList(classLoaders));
                try {
                        mLoadClass = ClassLoader.class.getDeclaredMethod("loadClass", String.class, boolean.class);
                } catch (NoSuchMethodException e) {
                        throw new LtBug(e);
                }
                mLoadClass.setAccessible(true);
        }

        @Override
        public Class<?> loadClass(String s) throws ClassNotFoundException {
                for (ClassLoader cl : classLoaders) {
                        try {
                                return cl.loadClass(s);
                        } catch (ClassNotFoundException ignore) {
                        }
                }
                throw new ClassNotFoundException(s);
        }

        @Override
        protected synchronized Class<?> loadClass(String s, boolean b) throws ClassNotFoundException {
                for (ClassLoader cl : classLoaders) {
                        try {
                                return (Class<?>) mLoadClass.invoke(cl, s, b);
                        } catch (IllegalAccessException e) {
                                throw new LtBug(e);
                        } catch (InvocationTargetException e) {
                                // ignore class not found exception
                                Throwable t = e.getCause();
                                if (!(t instanceof ClassNotFoundException)) {
                                        if (t instanceof RuntimeException) {
                                                throw (RuntimeException) t;
                                        } else if (t instanceof Error) {
                                                throw (Error) t;
                                        } else {
                                                // should not happen
                                                throw new LtBug("uncaught exception", e);
                                        }
                                }
                        }
                }
                throw new ClassNotFoundException(s);
        }

        @Override
        public URL getResource(String s) {
                for (ClassLoader cl : classLoaders) {
                        URL url = cl.getResource(s);
                        if (url != null) {
                                return url;
                        }
                }
                return null;
        }

        @Override
        public Enumeration<URL> getResources(String s) throws IOException {
                return new ProxyEnumeration(classLoaders.iterator(), s);
        }

        class ProxyEnumeration implements Enumeration<URL> {
                final Iterator<ClassLoader> clIt;
                final String resourceS;
                Enumeration<URL> current;

                ProxyEnumeration(Iterator<ClassLoader> clIt, String resourceS) {
                        this.clIt = clIt;
                        this.resourceS = resourceS;
                }

                @Override
                public boolean hasMoreElements() {
                        return clIt.hasNext() || (current != null && current.hasMoreElements());
                }

                @Override
                public URL nextElement() {
                        if (!hasMoreElements()) {
                                throw new NoSuchElementException();
                        }
                        if (current == null) {
                                try {
                                        current = clIt.next().getResources(resourceS);
                                } catch (IOException e) {
                                        throw new RuntimeException(e);
                                }
                        }
                        URL res = current.nextElement();
                        if (!current.hasMoreElements()) {
                                current = null;
                        }
                        return res;
                }
        }

        @Override
        public InputStream getResourceAsStream(String s) {
                for (ClassLoader cl : classLoaders) {
                        InputStream is = cl.getResourceAsStream(s);
                        if (is != null) {
                                return is;
                        }
                }
                return null;
        }
}
