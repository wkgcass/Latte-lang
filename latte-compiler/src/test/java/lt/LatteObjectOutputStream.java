package lt;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;

/**
 * Object input stream for Latte<br>
 * The latte classes that built at runtime are loaded via customized class loaders,
 * so it cannot be located by the default class loader.<br>
 * The {@link java.io.ObjectInputStream} looks for the class loader via
 * {@link ObjectInputStream#latestUserDefinedLoader()}, which might not get
 * the correct class loader.
 */
public class LatteObjectOutputStream extends ObjectInputStream {
        private final ClassLoader cl;

        public LatteObjectOutputStream(ClassLoader cl, InputStream in) throws IOException {
                super(in);
                this.cl = cl;
        }

        @Override
        protected Class<?> resolveClass(ObjectStreamClass desc) throws IOException, ClassNotFoundException {
                return Class.forName(desc.getName(), true, cl);
        }
}
