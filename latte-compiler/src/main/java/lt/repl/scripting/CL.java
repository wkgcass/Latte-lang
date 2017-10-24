package lt.repl.scripting;

import java.util.HashMap;
import java.util.Map;

public class CL extends ClassLoader {
        private Map<String, byte[]> byteCodes = new HashMap<String, byte[]>();

        public CL(ClassLoader cl) {
                super(cl);
        }

        @Override
        protected Class<?> findClass(String name) throws ClassNotFoundException {
                byte[] byteCode = byteCodes.get(name);
                if (byteCode == null) throw new ClassNotFoundException(name);
                return defineClass(name, byteCode, 0, byteCode.length);
        }

        public void addByteCodes(String name, byte[] bytes) {
                byteCodes.put(name, bytes);
        }
}
