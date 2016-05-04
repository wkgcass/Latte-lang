package lt.repl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * jar class loader
 */
public class JarLoader extends ClassLoader {
        private Map<String, byte[]> classNameToBytes = new HashMap<>();

        public void loadAll(JarFile jarFile) throws IOException, ClassNotFoundException {
                Enumeration<JarEntry> entries = jarFile.entries();
                while (entries.hasMoreElements()) {
                        JarEntry entry = entries.nextElement();
                        String entryName = entry.getName();

                        if (entryName.endsWith(".class")) {
                                String className = entryName.replace("/", ".");
                                className = className.substring(0, className.length() - ".class".length());

                                InputStream is = jarFile.getInputStream(entry);
                                ByteArrayOutputStream bao = new ByteArrayOutputStream();
                                byte[] buffer = new byte[4096];
                                int n;
                                while (-1 != (n = is.read(buffer))) {
                                        bao.write(buffer, 0, n);
                                }
                                byte[] bytes = bao.toByteArray();

                                classNameToBytes.put(className, bytes);
                        }
                }
        }

        @Override
        protected Class<?> findClass(String name) throws ClassNotFoundException {
                if (classNameToBytes.containsKey(name)) {
                        byte[] bytes = classNameToBytes.get(name);
                        return defineClass(name, bytes, 0, bytes.length);
                } else return super.findClass(name);
        }
}
