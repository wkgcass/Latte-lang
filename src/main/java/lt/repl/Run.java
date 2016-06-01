package lt.repl;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * run the main method
 */
public class Run {
        private List<URL> classPath;
        private String mainClass;

        /**
         * construct the Run class
         */
        public Run() {
        }

        /**
         * construct the Run class with class paths and main class
         *
         * @param classPath class paths
         * @param mainClass main class
         * @throws MalformedURLException    the string in the list is not a file/directory
         * @throws IllegalArgumentException the list contain elements that are not URL nor String
         */
        public Run(List<?> classPath, String mainClass) throws MalformedURLException {
                this.setMainClass(mainClass);
                this.setClassPath(classPath);
        }

        /**
         * retrieve class path
         *
         * @return a list of URL representing the class paths
         */
        public List<URL> getClassPath() {
                return classPath;
        }

        /**
         * set class path
         *
         * @param classPath a list of URL or String representing the class paths
         * @throws MalformedURLException    the string in the list is not a file/directory
         * @throws IllegalArgumentException the list contain elements that are not URL nor String
         */
        public void setClassPath(List<?> classPath) throws MalformedURLException {
                List<URL> urls = new ArrayList<>();
                for (Object o : classPath) {
                        if (o instanceof URL) {
                                urls.add((URL) o);
                        } else if (o instanceof String) {
                                urls.add(new URL(new File((String) o).toURI().toString()));
                        } else throw new IllegalArgumentException(o == null ? null : o.getClass() + " is not URL");
                }
                this.classPath = urls;
        }

        /**
         * retrieve the main class
         *
         * @return main class
         */
        public String getMainClass() {
                return mainClass;
        }

        /**
         * set main class
         *
         * @param mainClass main class
         */
        public void setMainClass(String mainClass) {
                this.mainClass = mainClass;
        }

        /**
         * execute with given arguments
         *
         * @param args arguments
         * @throws Throwable exception
         */
        public void exec(List<String> args) throws Throwable {
                exec(args.toArray(new String[args.size()]));
        }

        /**
         * execute with given arguments
         *
         * @param args arguments
         * @throws Throwable exception
         */
        public void exec(String[] args) throws Throwable {
                URLClassLoader urlClassLoader = new URLClassLoader(classPath.toArray(new URL[classPath.size()]));
                Class<?> cls = urlClassLoader.loadClass(mainClass);
                Method method = cls.getDeclaredMethod("main", String[].class);
                method.setAccessible(true);
                try {
                        method.invoke(null, new Object[]{args});
                } catch (InvocationTargetException e) {
                        throw e.getTargetException();
                }
        }

        /**
         * execute
         *
         * @throws Throwable exception
         */
        public void exec() throws Throwable {
                exec(Collections.emptyList());
        }
}
